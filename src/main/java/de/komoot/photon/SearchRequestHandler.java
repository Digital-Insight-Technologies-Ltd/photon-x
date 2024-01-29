package de.komoot.photon;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.PhotonRequestFactory;
import de.komoot.photon.searcher.GeocodeJsonFormatter;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.SearchHandler;
import de.komoot.photon.searcher.StreetDupesRemover;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Span;

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
 * Created by Sachin Dole on 2/12/2015.
 */
public class SearchRequestHandler extends RouteImpl {
    private final PhotonRequestFactory photonRequestFactory;
    private final SearchHandler requestHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenTelemetry otel;

    SearchRequestHandler(String path, SearchHandler handler, String[] languages, String defaultLanguage, OpenTelemetry otel) {
        super(path);
        this.photonRequestFactory = new PhotonRequestFactory(Arrays.asList(languages), defaultLanguage);
        this.requestHandler = handler;
        this.otel = otel;
    }

    @Override
    public String handle(Request request, Response response) throws IOException {
        var tracer = otel.getTracer("searchHandler");
        Span mainSpan = tracer.spanBuilder("search")
                .setAttribute(SemanticAttributes.HTTP_ROUTE, "api")
                .setAttribute(SemanticAttributes.HTTP_REQUEST_METHOD, "GET")
                .startSpan();

        String output;
        try (Scope scope = mainSpan.makeCurrent()){
            Span validateRequestSpan = tracer.spanBuilder("validateRequest")
                    .startSpan();

            // We need to initialize this as the compiler does not recognise that `halt` will exit the method
            PhotonRequest photonRequest = null;
            try {
                photonRequest = photonRequestFactory.create(request);
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
                    .startSpan();

            List<PhotonResult> results;
            try {
                results = requestHandler.search(photonRequest, tracer, querySpan);
            } catch (Exception e) {
                querySpan.recordException(e);
                querySpan.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                querySpan.end();
            }

            Span postProcessSpan = tracer.spanBuilder("postProcess")
                    .startSpan();

            try {
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