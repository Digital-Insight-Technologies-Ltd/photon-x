package de.komoot.photon;

import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import de.komoot.photon.searcher.HealthHandler;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.SemanticAttributes;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

import java.io.IOException;

public class HealthCheckHandler extends RouteImpl {
    private final HealthHandler handler;
    private final JsonpMapper jsonpMapper = new JacksonJsonpMapper();
    private final OpenTelemetry otel;

    HealthCheckHandler(String path, HealthHandler handler, OpenTelemetry otel) {
        super(path);
        this.handler = handler;
        this.otel = otel;
    }

    @Override
    public String handle(Request request, Response response) throws IOException {
        var tracer = otel.getTracer("PhotonApi");

        var enquiryId = request.headers("Xapien-Enquiry-Id");
        var deploymentStage = request.headers("Xapien-Deployment-Stage");

        Span mainSpan = tracer.spanBuilder("Health")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        mainSpan.setAttribute("client.address", request.ip())
                .setAttribute("http.route", "/health")
                .setAttribute("http.request.method", "GET")
                .setAttribute("server.address", request.host())
                .setAttribute("server.port", request.port())
                .setAttribute("url.path", "/health")
                .setAttribute("url.query", request.queryString())
                .setAttribute("url.full", request.url())
                .setAttribute("url.scheme", request.scheme())
                .setAttribute("user_agent.original", request.userAgent())
                .setAttribute("labels.enquiry_id", enquiryId)
                .setAttribute("deployment.environment", deploymentStage);

        String output;

        try (Scope scope = mainSpan.makeCurrent()) {
            HealthResponse report = handler.health();

            if (report.status() != HealthStatus.Green) {
                response.status(503);
                mainSpan.setStatus(StatusCode.ERROR);
                mainSpan.setAttribute("http.response.status_code", 503);
            } else {
                response.status(200);
                mainSpan.setStatus(StatusCode.OK);
                mainSpan.setAttribute("http.response.status_code", 200);
            }

            output = JsonpUtils.toJsonString(report, jsonpMapper);

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
