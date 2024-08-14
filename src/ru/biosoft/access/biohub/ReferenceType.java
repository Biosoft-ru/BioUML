package ru.biosoft.access.biohub;

import ru.biosoft.access.core.DataElementPath;

public interface ReferenceType
{
    public static final int SCORE_HIGH_SPECIFIC = 100;
    public static final int SCORE_ABOVE_MEDIUM_SPECIFIC = 25;
    public static final int SCORE_MEDIUM_SPECIFIC = 10;
    public static final int SCORE_LOW_SPECIFIC = 1;
    public static final int SCORE_NOT_THIS_TYPE = 0;
    public static final String REFERENCE_TYPE_PROPERTY = "referenceType";
    public static final String MATCHING_TYPE_PROPERTY = "matchingType";

    /**
     * Returns an URL for given ID
     */
    public String getURL(String id);
    
    public default DataElementPath getPath(String id) { return null; }
    
    /**
     * Returns whether this id belongs to the current type
     */
    public int getIdScore(String id);
    
    /**
     * Returns name of source database to which this reference belongs
     */
    public String getSource();
    
    /**
     * Returns type of the object referenced by this reference
     */
    public String getObjectType();
    
    /**
     * Returns human-readable display name
     */
    public String getDisplayName();

    /**
     * Returns icon id representing this type
     */
    public String getIconId();
    
    /**
     * Returns reference type identifier
     */
    public String getStableName();
    
    /**
     * Returns HTML description of the type
     */
    public String getDescriptionHTML();
    
    /**
     * Returns sample ID of the type
     */
    public String getSampleID();

    /**
     * Returns MIRIAM ID corresponding to his reference type or null if not known/not applicable
     */
    public String getMiriamId();

    /**
     * Returns species for the most of given IDs if possible.
     * Assumes that IDs belong to current type. 
     */
    public default String predictSpecies(String[] ids)
    {
        return "Unspecified"; //TODO: rework somehow or use constant
    }

    /**
     * Some types can have extended identifier with extra information, that should be removed for correct usage
     * For example, Ensembl gene version: ENSG00000071909.14
     */
    public default String preprocessId(String id)
    {
        return id;
    }
}
