package de.komoot.photon;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.LookupRequest;
import de.komoot.photon.query.LookupRequestFactory;
import de.komoot.photon.searcher.GeocodeJsonFormatter;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.LookupHandler;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Span;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.SemanticAttributes;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenTelemetry otel;

    LookupSearchRequestHandler(String path, LookupHandler handler, String[] languages, String defaultLanguage, OpenTelemetry otel) {
        super(path);
        this.photonRequestFactory = new LookupRequestFactory(Arrays.asList(languages), defaultLanguage);
        this.requestHandler = handler;
        this.otel = otel;
    }

    LookupSearchRequestHandler(String path, LookupHandler handler, String[] languages, String defaultLanguage) {
        this(path, handler, languages, defaultLanguage, OpenTelemetry.noop());
    }

    public String handle(Request request, Response response) throws IOException {
        var tracer = otel.getTracer("PhotonApi");

        var enquiryId = request.headers("Xapien-Enquiry-Id");
        var deploymentStage = request.headers("Xapien-Deployment-Stage");

        Span mainSpan = tracer.spanBuilder("Lookup")
                .setAttribute(SemanticAttributes.HTTP_ROUTE, "/lookup")
                .setAttribute(SemanticAttributes.HTTP_REQUEST_METHOD, "GET")
                .setAttribute(SemanticAttributes.URL_FULL, request.url())
                .setAttribute(SemanticAttributes.URL_QUERY, request.queryString())
                .setAttribute("enquiry_id", enquiryId)
                .setAttribute("deployment_stage", deploymentStage)
                .startSpan();

        String output;
        try (Scope scope = mainSpan.makeCurrent()){
            Span validateRequestSpan = tracer.spanBuilder("validateRequest")
                    .setParent(Context.current().with(mainSpan))
                    .startSpan();

            // We need to initialize this as the compiler does not recognise that `halt` will exit the method
            LookupRequest lookupRequest = null;
            try {
                lookupRequest = photonRequestFactory.create(request);
            } catch (BadRequestException e) {
                validateRequestSpan.recordException(e);
                halt(e.getHttpStatus(), objectMapper.createObjectNode().put("message", e.getMessage()).toString());
            } catch (Exception e) {
                validateRequestSpan.recordException(e);
                validateRequestSpan.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                validateRequestSpan.end();
            }

            Span querySpan = tracer.spanBuilder("query")
                    .setParent(Context.current().with(mainSpan))
                    .startSpan();

            PhotonResult result;
            try {
                result = requestHandler.lookup(lookupRequest.placeId(), tracer, querySpan);
            } catch (Exception e) {
                querySpan.recordException(e);
                querySpan.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                querySpan.end();
            }

            Span postProcessSpan = tracer.spanBuilder("postProcess")
                    .setParent(Context.current().with(mainSpan))
                    .startSpan();

            try {
                List<PhotonResult> results = new ArrayList<>();
                results.add(result);
                output = new GeocodeJsonFormatter(false, lookupRequest.language()).convert(results, null);
            } catch (Exception e) {
                postProcessSpan.recordException(e);
                postProcessSpan.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                postProcessSpan.end();
            }
        } catch (Exception e) {
            mainSpan.recordException(e);
            mainSpan.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            mainSpan.end();
        }
        return output;
    }
}
