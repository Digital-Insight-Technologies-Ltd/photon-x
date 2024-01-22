package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.searcher.TagFilter;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Collection of query parameters for a search request.
 */
@Getter
public class PhotonRequest {
    private final String query;
    private final String language;
    private int limit = 15;
    private Point locationForBias = null;
    private double scaleForBias = 0.2;
    private int zoomForBias = 14;
    private Envelope bbox = null;
    private Boolean debug = false;
    private final List<TagFilter> osmTagFilters = new ArrayList<>(1);
    private Set<String> layerFilters = new HashSet<>(1);

    public PhotonRequest(String query, String language) {
        this.query = query;
        this.language = language;
    }

    PhotonRequest addOsmTagFilter(TagFilter filter) {
        osmTagFilters.add(filter);
        return this;
    }

    PhotonRequest setLayerFilter(Set<String> filters) {
        layerFilters = filters;
        return this;
    }

    PhotonRequest setLimit(Integer limit) {
        if (limit != null) {
            this.limit = Integer.max(Integer.min(limit, 50), 1);
        }
        return this;
    }

    PhotonRequest setLocationForBias(Point locationForBias) {
        if (locationForBias != null) {
            this.locationForBias = locationForBias;
        }
        return this;
    }

    PhotonRequest setScale(Double scale) {
        if (scale != null) {
            this.scaleForBias = Double.max(Double.min(scale, 1.0), 0.0);
        }
        return this;
    }

    PhotonRequest setZoom(Integer zoom) {
        if (zoom != null) {
            this.zoomForBias = Integer.max(Integer.min(zoom, 18), 0);
        }
        return this;
    }

    PhotonRequest setBbox(Envelope bbox) {
        if (bbox != null) {
            this.bbox = bbox;
        }
        return this;
    }

    PhotonRequest enableDebug() {
        this.debug = true;
        return this;
    }
}
