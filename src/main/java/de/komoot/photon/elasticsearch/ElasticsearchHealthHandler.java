package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import de.komoot.photon.searcher.HealthHandler;

import java.io.IOException;

public class ElasticsearchHealthHandler implements HealthHandler {
    private final ElasticsearchClient client;

    public ElasticsearchHealthHandler(ElasticsearchClient client) {
        this.client = client;
    }

    public HealthResponse health() throws IOException {
        return client.cluster().health();
    }
}
