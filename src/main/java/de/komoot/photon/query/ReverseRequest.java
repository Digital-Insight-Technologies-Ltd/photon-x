package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.searcher.TagFilter;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * @author svantulden
 */
@Getter
@AllArgsConstructor
public class ReverseRequest implements Serializable {
    private Point location;
    private String language;
    private Double radius;
    private Integer limit;
    private String queryStringFilter;
    private Boolean locationDistanceSort;
    private Set<String> layerFilters;
    private Boolean debug;
    private final List<TagFilter> osmTagFilters = new ArrayList<>(1);

    ReverseRequest addOsmTagFilter(TagFilter filter) {
        osmTagFilters.add(filter);
        return this;
    }
}
