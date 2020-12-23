package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import spark.QueryParamsMap;
import spark.Request;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * A factory that creates a {@link PhotonRequest} from a {@link Request web request}
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonRequestFactory {
    private final RequestLanguageResolver languageResolver;
    private static final LocationParamConverter optionalLocationParamConverter = new LocationParamConverter(false);
    private final BoundingBoxParamConverter bboxParamConverter;

    protected static HashSet<String> m_hsRequestQueryParams = new HashSet<>(Arrays.asList("lang", "q", "lon", "lat",
            "limit", "osm_tag", "location_bias_scale", "bbox", "debug"));

    public PhotonRequestFactory(List<String> supportedLanguages, String defaultLanguage) {
        this.languageResolver = new RequestLanguageResolver(supportedLanguages, defaultLanguage);
        this.bboxParamConverter = new BoundingBoxParamConverter();
    }

    public PhotonRequest create(Request webRequest) throws BadRequestException {


        for (String queryParam : webRequest.queryParams())
            if (!m_hsRequestQueryParams.contains(queryParam))
                throw new BadRequestException(400, "unknown query parameter '" + queryParam + "'.  Allowed parameters are: " + m_hsRequestQueryParams);

        String language = languageResolver.resolveRequestedLanguage(webRequest);

        String query = webRequest.queryParams("q");
        if (query == null) throw new BadRequestException(400, "missing search term 'q': /?q=berlin");
        int limit;
        try {
            limit = Integer.valueOf(webRequest.queryParams("limit"));
        } catch (NumberFormatException e) {
            limit = 15;
        }

        Point locationForBias = optionalLocationParamConverter.apply(webRequest);
        Envelope bbox = bboxParamConverter.apply(webRequest);

        // don't use too high default value, see #306
        double scale = 1.6;
        String scaleStr = webRequest.queryParams("location_bias_scale");
        if (scaleStr != null && !scaleStr.isEmpty())
            try {
                scale = Double.parseDouble(scaleStr);
            } catch (Exception nfe) {
                throw new BadRequestException(400, "invalid parameter 'location_bias_scale' must be a number");
            }

        PhotonRequest request = new PhotonRequest(query, limit, bbox, locationForBias, scale, language);

        QueryParamsMap tagFiltersQueryMap = webRequest.queryMap("osm_tag");
        if (new CheckIfFilteredRequest().execute(tagFiltersQueryMap)) {
            request.setUpTagFilters(tagFiltersQueryMap.values());
        }


        return request;
    }

}
