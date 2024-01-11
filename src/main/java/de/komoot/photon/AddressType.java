package de.komoot.photon;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * List of address ranks available to Photon.
 * <p>
 * The different types correspond to the address parts available in GeocodeJSON. This type also defines
 * the mapping toward Nominatim's address ranks.
 */
public enum AddressType {
    HOUSE("house"),
    STREET("street"),
    LOCALITY("locality"),
    DISTRICT("district"),
    CITY("city"),
    COUNTY("county"),
    STATE("state"),
    COUNTRY("country");

    private final String name;

    AddressType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static List<String> getNames() {
        return Arrays.stream(AddressType.values()).map(AddressType::getName).collect(Collectors.toList());
    }
}
