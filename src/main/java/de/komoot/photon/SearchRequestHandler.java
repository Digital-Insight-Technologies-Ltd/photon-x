package de.komoot.photon;

import de.komoot.photon.logging.PhotonLogger;
import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.PhotonRequestFactory;
import de.komoot.photon.searcher.GeocodeJsonFormatter;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.SearchHandler;
import de.komoot.photon.searcher.StreetDupesRemover;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static spark.Spark.halt;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class SearchRequestHandler extends RouteImpl {
    private final PhotonRequestFactory photonRequestFactory;
    private final SearchHandler requestHandler;
    private final Tracer tracer;

    SearchRequestHandler(String path, SearchHandler handler, String[] languages, String defaultLanguage, OpenTelemetry otel) {
        super(path);
        this.photonRequestFactory = new PhotonRequestFactory(Arrays.asList(languages), defaultLanguage);
        this.requestHandler = handler;
        this.tracer = otel.getTracer(SearchRequestHandler.class.getName());
    }

    @Override
    public String handle(Request request, Response response) throws IOException {
        Span validateRequestSpan = tracer.spanBuilder("validateRequest").startSpan();

        // We need to initialize this as the compiler does not recognise that `halt` will exit the method
        PhotonRequest photonRequest = null;
        try (Scope scope = validateRequestSpan.makeCurrent()){
            photonRequest = photonRequestFactory.create(request);
        } catch (BadRequestException e) {
            validateRequestSpan.recordException(e);
            JSONObject json = new JSONObject();
            json.put("message", e.getMessage());
            halt(e.getHttpStatus(), json.toString());
        } catch (Exception e) {
            validateRequestSpan.recordException(e);
            throw e;
        } finally {
            validateRequestSpan.end();
        }

        Span querySpan = tracer.spanBuilder("query").startSpan();
        List<PhotonResult> results;
        try (Scope scope = querySpan.makeCurrent()){
            results = requestHandler.search(photonRequest, querySpan);
        } catch (Exception e) {
            querySpan.recordException(e);
            throw e;
        } finally {
            querySpan.end();
        }

        Span postProcessSpan = tracer.spanBuilder("postProcess").startSpan();
        String output;
        try (Scope scope = postProcessSpan.makeCurrent()){
            // Further filtering
            results = new StreetDupesRemover(photonRequest.getLanguage()).execute(results);

            // Restrict to the requested limit.
            if (results.size() > photonRequest.getLimit()) {
                results = results.subList(0, photonRequest.getLimit());
            }

            String debugInfo = null;
            if (photonRequest.getDebug()) {
                debugInfo = requestHandler.dumpQuery(photonRequest);
            }

            output = new GeocodeJsonFormatter(photonRequest.getDebug(), photonRequest.getLanguage()).convert(results, debugInfo);
        } catch (Exception e) {
            postProcessSpan.recordException(e);
            throw e;
        } finally {
            postProcessSpan.end();
        }

        return output;
    }
}