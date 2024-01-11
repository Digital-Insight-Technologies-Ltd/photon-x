package de.komoot.photon;

import lombok.extern.slf4j.Slf4j;

/**
 * Class collecting database global properties.
 *
 * The server is responsible for making the data persistent throughout the Photon database.
 */
@Slf4j
public class DatabaseProperties {


    private String[] languages = null;

    /**
     * Return the list of languages for which the database is configured.
     *
     * @return
     */
    public String[] getLanguages() {
        if (languages == null) {
            return new String[]{"en", "de", "fr", "it"};
        }

        return languages;
    }

    /**
     * Replace the language list with the given list.
     *
     * @param languages Array of two-letter language codes.
     * @return This object for chaining.
     */
    public DatabaseProperties setLanguages(String[] languages) {
        this.languages = languages;
        return this;
    }
}
