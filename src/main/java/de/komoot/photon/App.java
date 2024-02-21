package de.komoot.photon;

import de.komoot.photon.elasticsearch.ElasticsearchServer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

import static spark.Spark.*;

public class App {
    private final static String serverUrl = System.getenv("ELASTIC_CLUSTER_SERVER_URL");
    private final static String apiKey = System.getenv("ELASTIC_CLUSTER_API_KEY");
    private final static String defaultLanguage = System.getenv("PHOTON_DEFAULT_LANGUAGE");
    private final static String[] languages = new String[]{"en", "de", "fr", "it"};
    private final static OpenTelemetry otel = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();

    public static void main(String[] rawArgs) throws Exception {
        if (serverUrl == null) {
            throw new RuntimeException("Required environment variable: ELASTIC_CLUSTER_SERVER_URL");
        }

        if (apiKey == null) {
            throw new RuntimeException("Required environment variable: ELASTIC_CLUSTER_API_KEY");
        }

        ElasticsearchServer server = new ElasticsearchServer(serverUrl, otel)
                .apiKey(apiKey)
                .start()
                .waitForReady();

        port(2322);
        ipAddress("0.0.0.0");
        before((request, response) -> response.type("application/json; charset=UTF-8"));
        get("api", new SearchRequestHandler("api", server.createSearchHandler(languages), languages, defaultLanguage, otel));
        get("reverse", new ReverseSearchRequestHandler("reverse", server.createReverseHandler(), languages, defaultLanguage, otel));
        get("lookup", new LookupSearchRequestHandler("lookup", server.createLookupHandler(), languages, defaultLanguage, otel));
        get("health", new HealthCheckHandler("health", server.createHealthHandler(), otel));
    }

}
