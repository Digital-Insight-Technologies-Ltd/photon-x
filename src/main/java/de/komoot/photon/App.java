package de.komoot.photon;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import de.komoot.photon.elasticsearch.ElasticsearchServer;
import de.komoot.photon.elasticsearch.IndexSettings;
import de.komoot.photon.utils.CorsFilter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import static spark.Spark.*;


@Slf4j
public class App {

    public static void main(String[] rawArgs) throws Exception {
        CommandLineArgs args = parseCommandLine(rawArgs);

        final ElasticsearchServer esServer = new ElasticsearchServer(args.getServerUrl())
                .apiKey(args.getApiKey())
                .start();

        log.info("Make sure that the ES cluster is ready, this might take some time.");
        esServer.waitForReady();

        // Working on an existing installation.
        // Update the index settings in case there are any changes.
        if (args.isRefreshIndexSettings()) {
            log.info("Refreshing index settings.");
            esServer.updateSettings(IndexSettings.buildSettings(args.getSynonymFile()));
            esServer.waitForReady();
        }

        log.info("ES cluster is now ready.");

        startApi(args, esServer);
    }


    private static CommandLineArgs parseCommandLine(String[] rawArgs) {
        CommandLineArgs args = new CommandLineArgs();
        final JCommander jCommander = new JCommander(args);
        try {
            jCommander.parse(rawArgs);

            // Cors arguments are mutually exclusive.
            if (args.isCorsAnyOrigin() && args.getCorsOrigin() != null) {
                throw new ParameterException("Use only one cors configuration type");
            }

            if (args.getServerUrl() == null) {
                throw new ParameterException("serverUrl is a required parameter");
            }
        } catch (ParameterException e) {
            log.warn("could not start photon: " + e.getMessage());
            jCommander.usage();
            System.exit(1);
        }

        // show help
        if (args.isUsage()) {
            jCommander.usage();
            System.exit(1);
        }

        return args;
    }

    /**
     * Start API server to accept search requests via http.
     */
    private static void startApi(CommandLineArgs args, ElasticsearchServer server) throws IOException {
        // Get database properties and ensure that the version is compatible.
        DatabaseProperties dbProperties = server.loadDbProperties();

        port(args.getListenPort());
        ipAddress(args.getListenIp());

        String allowedOrigin = args.isCorsAnyOrigin() ? "*" : args.getCorsOrigin();
        if (allowedOrigin != null) {
            CorsFilter.enableCORS(allowedOrigin, "get", "*");
        } else {
            before((request, response) -> {
                response.type("application/json; charset=UTF-8"); // in the other case set by enableCors
            });
        }

        // setup search API
        String[] langs = dbProperties.getLanguages();
        get("api", new SearchRequestHandler("api", server.createSearchHandler(langs), langs, args.getDefaultLanguage()));
        get("api/", new SearchRequestHandler("api/", server.createSearchHandler(langs), langs, args.getDefaultLanguage()));
        get("reverse", new ReverseSearchRequestHandler("reverse", server.createReverseHandler(), dbProperties.getLanguages(), args.getDefaultLanguage()));
        get("reverse/", new ReverseSearchRequestHandler("reverse/", server.createReverseHandler(), dbProperties.getLanguages(), args.getDefaultLanguage()));
        get("lookup", new LookupSearchRequestHandler("lookup", server.createLookupHandler(), dbProperties.getLanguages(), args.getDefaultLanguage()));
        get("lookup/", new LookupSearchRequestHandler("lookup/", server.createLookupHandler(), dbProperties.getLanguages(), args.getDefaultLanguage()));
    }
}
