package biouml.standard.filter;

import java.util.ListResourceBundle;

/**
 *
 * @pending enhance short descriptions.
 */
public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents =
    {
        {"PN_DIAGRAM_FILTER",       "Diagram filter"},
        {"PD_DIAGRAM_FILTER",       "Common purpose diagram filter allowing to filter Biopolimers " +
                                    "by species, cell types and inducers."},

        {"PN_SPECIES_FILTER",       "Species filter"},
        {"PD_SPECIES_FILTER",       "Species filter."},

        {"PN_CELL_TYPE_FILTER",     "Cell type filter"},
        {"PD_CELL_TYPE_FILTER",     "Cell type filter."},

        {"PN_INDUCER_FILTER",       "Inducer filter"},
        {"PD_INDUCER_FILTER",       "Inducer filter."},
/*
        {"PN_HIGHLIGHTER",      "Highlighter"},
        {"PD_HIGHLIGHTER",      "Highlighter color to mark up the diagram elements satisfying the filter conditions."},

        {"PN_ACTION",           "Filter mode"},
        {"PD_ACTION",           "Filter mode allows user specify how the filter will be applied to diagram elements. " +
                                "In hiding mode diagram elements not satisfying to filter conditions will be hided. " +
                                "In highlighter mode diagram elements satisfying to filter conditions will be marked up."},
*/
    };
}

