package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.komoot.photon.Constants;
import de.komoot.photon.searcher.LookupHandler;
import de.komoot.photon.searcher.PhotonResult;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;

import java.io.IOException;

public class ElasticsearchLookupHandler implements LookupHandler {
    private final ElasticsearchClient client;
    public ElasticsearchLookupHandler(ElasticsearchClient client) {
        this.client = client;
    }

    public PhotonResult lookup(String placeId, Tracer tracer, Span parentSpan) throws IOException {
        Span sendQuerySpan = tracer.spanBuilder("sendQuery")
                .setParent(Context.current().with(parentSpan))
                .startSpan();

        GetRequest request = new GetRequest.Builder().index(Constants.PHOTON_INDEX).id(placeId).build();

        GetResponse<ObjectNode> response;
        try {
            response = client.get(request, ObjectNode.class);
            if (!response.found()) {
                return null;
            }
        } catch (Exception e) {
            sendQuerySpan.recordException(e);
            sendQuerySpan.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            sendQuerySpan.end();
        }

        return new ElasticResult(response.source(), response.id());
    }
}
