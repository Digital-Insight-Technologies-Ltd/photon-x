package de.komoot.photon;

/**
 * Command Line Arguments parsed by {@link com.beust.jcommander.JCommander} and used to start photon.
 */

import com.beust.jcommander.Parameter;
import lombok.Data;

@Data
public class CommandLineArgs {

    @Parameter(names = "-cluster", description = "name of elasticsearch cluster to put the server into (default is 'photon')")
    private String cluster = "photon";

    @Parameter(names = "-server-url", description = "the server url to connect to the elasticsearch cluster")
    private String serverUrl = System.getenv("ELASTIC_CLUSTER_SERVER_URL");

    @Parameter(names = "-api-key", description = "the api key required to connect to the elasticsearch cluster")
    private String apiKey = System.getenv("ELASTIC_CLUSTER_API_KEY");

    @Parameter(names = "-refresh-index-settings", description = "refresh the index settings on start")
    private boolean refreshIndexSettings = false;

    @Parameter(names = "-default-language", description = "language to return results in when no explicit language is chosen by the user")
    private String defaultLanguage = "default";

    @Parameter(names = "-synonym-file", description = "file with synonym and classification terms")
    private String synonymFile = null;

    @Parameter(names = "-listen-port", description = "listen to port (default 2322)")
    private int listenPort = 2322;

    @Parameter(names = "-listen-ip", description = "listen to address (default '0.0.0.0')")
    private String listenIp = "0.0.0.0";

    @Parameter(names = "-cors-any", description = "enable cross-site resource sharing for any origin ((default CORS not supported)")
    private boolean corsAnyOrigin = false;
    
    @Parameter(names = "-cors-origin", description = "enable cross-site resource sharing for the specified origin (default CORS not supported)")
    private String corsOrigin = null;

    @Parameter(names = "-h", description = "show help / usage")
    private boolean usage = false;

}

