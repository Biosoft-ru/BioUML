package ru.biosoft.access.biohub;

/**
 * Relation type enum class
 */
public class RelationType
{
    public static String[] allTypes()
    {
        return new String[]{SEMANTIC, REACTANT, PRODUCT, MODIFIER, NOTE_LINK};
    }
    
    public static final String SEMANTIC = "semantic";
    public static final String REACTANT = "reactant";
    public static final String PRODUCT = "product";
    public static final String MODIFIER = "modifier";
    public static final String NOTE_LINK = "noteLink";
}
