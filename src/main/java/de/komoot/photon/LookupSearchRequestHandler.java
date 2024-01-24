package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.LookupRequest;
import de.komoot.photon.query.LookupRequestFactory;
import de.komoot.photon.searcher.GeocodeJsonFormatter;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.LookupHandler;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static spark.Spark.halt;

public class LookupSearchRequestHandler extends RouteImpl {
    private final LookupRequestFactory photonRequestFactory;
    private final LookupHandler requestHandler;
    private final Tracer tracer;

    LookupSearchRequestHandler(String path, LookupHandler handler, String[] languages, String defaultLanguage, OpenTelemetry otel) {
        super(path);
        this.photonRequestFactory = new LookupRequestFactory(Arrays.asList(languages), defaultLanguage);
        this.requestHandler = handler;
        this.tracer = otel.getTracer(LookupSearchRequestHandler.class.getName());
    }

    LookupSearchRequestHandler(String path, LookupHandler handler, String[] languages, String defaultLanguage) {
        this(path, handler, languages, defaultLanguage, OpenTelemetry.noop());
    }

    public String handle(Request request, Response response) throws IOException {
        Span validateRequestSpan = tracer.spanBuilder("validateRequest").startSpan();
        // We need to initialize this as the compiler does not recognise that `halt` will exit the method
        LookupRequest lookupRequest = null;
        try (Scope scope = validateRequestSpan.makeCurrent()){
            lookupRequest = photonRequestFactory.create(request);
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
        PhotonResult result;
        try (Scope scope = querySpan.makeCurrent()){
            result = requestHandler.lookup(lookupRequest.placeId(), querySpan);
        } catch (Exception e) {
            querySpan.recordException(e);
            throw e;
        } finally {
            querySpan.end();
        }

        Span postProcessSpan = tracer.spanBuilder("postProcess").startSpan();
        String output;
        try (Scope scope = postProcessSpan.makeCurrent()){
            List<PhotonResult> results = new ArrayList<>();
            results.add(result);
            output = new GeocodeJsonFormatter(false, lookupRequest.language()).convert(results, null);
        } catch (Exception e) {
            postProcessSpan.recordException(e);
            throw e;
        } finally {
            postProcessSpan.end();
        }

        return output;
    }
}
