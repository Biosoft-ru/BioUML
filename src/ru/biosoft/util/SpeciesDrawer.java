package ru.biosoft.util;

/**
 * Optional marker interface for CustomImageLoader implementations
 * that can parse species information from the imageId passed to loadImage().
 */
public interface SpeciesDrawer {
    /**
     * Extract species latin name from the imageId string.
     * Returns null if no species parameter is present.
     */
    String getSpecies(String imageId);
}
