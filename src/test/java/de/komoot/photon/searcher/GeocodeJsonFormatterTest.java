package de.komoot.photon.searcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.komoot.photon.Constants;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GeocodeJsonFormatterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Test
    public void testConvertToGeojson() throws JsonProcessingException {
        GeocodeJsonFormatter formatter = new GeocodeJsonFormatter(false, "en");
        List<PhotonResult> allResults = new ArrayList<>();
        allResults.add(createDummyResult("99999", "Park Foo", "leisure", "park"));
        allResults.add(createDummyResult("88888", "Bar Park", "leisure", "park"));

        String geojsonString = formatter.convert(allResults, null);
        JsonNode jsonObj = objectMapper.readTree(geojsonString);
        assertEquals("FeatureCollection", jsonObj.get("type").asText());
        ArrayNode features = (ArrayNode) jsonObj.get("features");
        assertEquals(2, features.size());
        for (int i = 0; i < features.size(); i++) {
            JsonNode feature = features.get(i);
            assertEquals("Feature", feature.get("type").asText());
            assertEquals("Point", feature.get("geometry").get("type").asText());
            assertEquals("leisure", feature.get("properties").get(Constants.OSM_KEY).asText());
            assertEquals("park", feature.get("properties").get(Constants.OSM_VALUE).asText());
        }
    }
    
    private PhotonResult createDummyResult(String postCode, String name, String osmKey, String osmValue) {
        return new MockPhotonResult()
                .put(Constants.POSTCODE, postCode)
                .putLocalized(Constants.NAME, "en", name)
                .put(Constants.OSM_KEY, osmKey)
                .put(Constants.OSM_VALUE, osmValue);
    }

}
