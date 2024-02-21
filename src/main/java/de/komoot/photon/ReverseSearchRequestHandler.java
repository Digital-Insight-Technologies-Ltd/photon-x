package de.komoot.photon;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.ReverseRequestFactory;
import de.komoot.photon.searcher.GeocodeJsonFormatter;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.ReverseHandler;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Span;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.SemanticAttributes;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenTelemetry otel;

    ReverseSearchRequestHandler(String path, ReverseHandler handler, String[] languages, String defaultLanguage, OpenTelemetry otel) {
        super(path);
        this.reverseRequestFactory = new ReverseRequestFactory(Arrays.asList(languages), defaultLanguage);
        this.requestHandler = handler;
        this.otel = otel;
    }

    @Override
    public String handle(Request request, Response response) throws IOException {
        var tracer = otel.getTracer("PhotonApi");

        var enquiryId = request.headers("Xapien-Enquiry-Id");
        var deploymentStage = request.headers("Xapien-Deployment-Stage");

        Span mainSpan = tracer.spanBuilder("Reverse")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        mainSpan.setAttribute("http.request.method", "GET")
                .setAttribute("url.full", request.url())
                .setAttribute("user_agent.original", request.userAgent())
                .setAttribute("labels.enquiry_id", enquiryId)
                .setAttribute("deployment.environment", deploymentStage);

        String output;
        try (Scope scope = mainSpan.makeCurrent()) {
            Span validateRequestSpan = tracer.spanBuilder("validateRequest")
                    .setParent(Context.current().with(mainSpan))
                    .startSpan();

            // We need to initialize this as the compiler does not recognise that `halt` will exit the method
            ReverseRequest photonRequest = null;
            try {
                photonRequest = reverseRequestFactory.create(request);
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

            List<PhotonResult> results;
            try {
                results = requestHandler.reverse(photonRequest, tracer, querySpan);
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
                postProcessSpan.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                postProcessSpan.end();
            }
            mainSpan.setStatus(StatusCode.OK);
            mainSpan.setAttribute("http.response.status_code", 200);
        } catch (Exception e) {
            mainSpan.recordException(e);
            mainSpan.setStatus(StatusCode.ERROR);
            mainSpan.setAttribute("http.response.status_code", 500);
            throw e;
        } finally {
            mainSpan.end();
        }

        return output;
    }
}
