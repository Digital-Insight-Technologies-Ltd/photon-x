package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.ReverseRequestFactory;
import de.komoot.photon.searcher.GeocodeJsonFormatter;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.ReverseHandler;
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
 * @author svantulden
 */
public class ReverseSearchRequestHandler extends RouteImpl {
    private final ReverseRequestFactory reverseRequestFactory;
    private final ReverseHandler requestHandler;
    private final Tracer tracer;

    ReverseSearchRequestHandler(String path, ReverseHandler handler, String[] languages, String defaultLanguage, OpenTelemetry otel) {
        super(path);
        this.reverseRequestFactory = new ReverseRequestFactory(Arrays.asList(languages), defaultLanguage);
        this.requestHandler = handler;
        this.tracer = otel.getTracer(ReverseSearchRequestHandler.class.getName());
    }

    ReverseSearchRequestHandler(String path, ReverseHandler handler, String[] languages, String defaultLanguage) {
        this(path, handler, languages, defaultLanguage, OpenTelemetry.noop());
    }

    @Override
    public String handle(Request request, Response response) throws IOException {
        Span validateRequestSpan = tracer.spanBuilder("validateRequest").startSpan();

        // We need to initialize this as the compiler does not recognise that `halt` will exit the method
        ReverseRequest photonRequest = null;
        try (Scope scope = validateRequestSpan.makeCurrent()){
            photonRequest = reverseRequestFactory.create(request);
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
            results = requestHandler.reverse(photonRequest, querySpan);
        } catch (Exception e) {
            querySpan.recordException(e);
            throw e;
        } finally {
            querySpan.end();
        }

        Span postProcessSpan = tracer.spanBuilder("postProcess").startSpan();
        String output;
        try (Scope scope = postProcessSpan.makeCurrent()){
            // Restrict to the requested limit.
            if (results.size() > photonRequest.getLimit()) {
                results = results.subList(0, photonRequest.getLimit());
            }

            String debugInfo = null;
            if (photonRequest.getDebug()) {
                debugInfo = requestHandler.dumpQuery(photonRequest);
            }

            output = new GeocodeJsonFormatter(false, photonRequest.getLanguage()).convert(results, debugInfo);
        } catch (Exception e) {
            postProcessSpan.recordException(e);
            throw e;
        } finally {
            postProcessSpan.end();
        }

        return output;
    }
}
