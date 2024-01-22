package de.komoot.photon;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import de.komoot.photon.elasticsearch.ElasticsearchServer;
import org.tinylog.Logger;

import static spark.Spark.*;

public class App {
    public static void main(String[] rawArgs) throws Exception {
        CommandLineArgs args = parseCommandLine(rawArgs);

        final ElasticsearchServer esServer = new ElasticsearchServer(args.getServerUrl())
                .apiKey(args.getApiKey())
                .start()
                .waitForReady();

        startApi(args, esServer);
    }


    private static CommandLineArgs parseCommandLine(String[] rawArgs) {
        CommandLineArgs args = new CommandLineArgs();
        final JCommander jCommander = new JCommander(args);
        try {
            jCommander.parse(rawArgs);

            if (args.getServerUrl() == null) { throw new ParameterException("serverUrl is a required parameter"); }
            if (args.getApiKey() == null) { throw new ParameterException("apiKey is a required parameter"); }

        } catch (ParameterException e) {
            Logger.error("could not start photon: " + e.getMessage());
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
    private static void startApi(CommandLineArgs args, ElasticsearchServer server){
        port(args.getListenPort());
        ipAddress(args.getListenIp());

        before((request, response) -> response.type("application/json; charset=UTF-8"));

        // setup search API
        String[] languages = new String[]{"en", "de", "fr", "it"};
        get("api", new SearchRequestHandler("api", server.createSearchHandler(languages), languages, args.getDefaultLanguage()));
        get("api/", new SearchRequestHandler("api/", server.createSearchHandler(languages), languages, args.getDefaultLanguage()));
        get("reverse", new ReverseSearchRequestHandler("reverse", server.createReverseHandler(), languages, args.getDefaultLanguage()));
        get("reverse/", new ReverseSearchRequestHandler("reverse/", server.createReverseHandler(), languages, args.getDefaultLanguage()));
        get("lookup", new LookupSearchRequestHandler("lookup", server.createLookupHandler(), languages, args.getDefaultLanguage()));
        get("lookup/", new LookupSearchRequestHandler("lookup/", server.createLookupHandler(), languages, args.getDefaultLanguage()));
    }
}
