package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.komoot.photon.query.PhotonRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ElasticSearchHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private PhotonRequest photonRequest(String q) {
        return new PhotonRequest(q, "default");
    }

    private ElasticsearchSearchHandler elasticsearchSearchHandler() {
        ElasticsearchClient client = Mockito.mock(ElasticsearchClient.class);
        String[] languages = new String[]{"en", "it", "fr", "de"};
        return new ElasticsearchSearchHandler(client, languages);
    }

    @Test
    public void testDumpQueryReturnsValidJson() throws Exception {
        ElasticsearchSearchHandler handler = elasticsearchSearchHandler();
        PhotonRequest request = photonRequest("London");
        String debugInfo = handler.dumpQuery(request);

        mapper.readTree(debugInfo);
    }
}
