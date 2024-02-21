package de.komoot.photon.searcher;

import co.elastic.clients.elasticsearch.cluster.HealthResponse;

import java.io.IOException;


public interface HealthHandler {
    HealthResponse health() throws IOException;
}
