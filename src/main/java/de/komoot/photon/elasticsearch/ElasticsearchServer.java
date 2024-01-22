package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import de.komoot.photon.searcher.LookupHandler;
import de.komoot.photon.searcher.ReverseHandler;
import de.komoot.photon.searcher.SearchHandler;
import de.komoot.photon.logging.PhotonLogger;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ElasticsearchServer {
    private final List<Header> headers = new ArrayList<>(){};
    private final RestClientBuilder restClientBuilder;
    public final String serverUrl;
    private final JsonpMapper jsonpMapper = new JacksonJsonpMapper();
    public ElasticsearchClient esClient;

    public ElasticsearchServer(String serverUrl) {
        this.serverUrl = serverUrl;
        this.restClientBuilder = RestClient.builder(HttpHost.create(serverUrl));
    }

    public ElasticsearchServer apiKey(String apiKey) {
        this.headers.add(new BasicHeader("Authorization", String.format("ApiKey %s", apiKey)));
        return this;
    }

    public ElasticsearchServer start() {
        if (!headers.isEmpty()) { restClientBuilder.setDefaultHeaders(headers.toArray(Header[]::new)); }

        ElasticsearchTransport transport = new RestClientTransport(restClientBuilder.build(), jsonpMapper);

        esClient = new ElasticsearchClient(transport);

        PhotonLogger.logger.info("Started elastic search client connected to " + serverUrl);

        return this;
    }

    public ElasticsearchServer waitForReady() throws IOException {
        esClient.cluster().health(fn -> fn.waitForStatus(HealthStatus.Yellow));
        return this;
    }

    public SearchHandler createSearchHandler(String[] languages) {
        return new ElasticsearchSearchHandler(esClient, languages);
    }

    public ReverseHandler createReverseHandler() {
        return new ElasticsearchReverseHandler(esClient);
    }

    public LookupHandler createLookupHandler() {
        return new ElasticsearchLookupHandler(esClient);
    }
}
