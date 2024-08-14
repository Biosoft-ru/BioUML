
/**
 * Author:  Igor V. Tyazhev  (champ@developmentontheedge.com)
 *
 * Created: 27.02.2001
 *
 * Description:
 *
 * Copyright (C) 2000, 2001 DevelopmentOnTheEdge.com. All rights reserved.
 */

/**
* SequenceViewOptions message bundle
*/

package ru.biosoft.bsa.view;

import java.util.ListResourceBundle;

public class SequenceViewOptionsMessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private static final String[] viewTypes = {"Sequence", "Ruler", "Sequence and ruler"};

    private Object[][] contents =
    {
        {"VIEW_TYPES",                viewTypes},

        { "DISPLAY_NAME",             "Sequence options" },
        { "SHORT_DESCRIPTION",        "A set of properties for tuning seqiunce view" },

        { "DENSITY_NAME",             "pixel/nucleotide" },
        { "DENSITY_DESCRIPTION",      "Density of nucleotides in number of pixels per nucleotide" },

        { "FONT_NAME",                "Font" },
        { "FONT_DESCRIPTION",         "Font used for painting nucletides letters" },

        { "TYPE_NAME",                "Type" },
        { "TYPE_DESCRIPTION",         "Representation of sequence (0 - sequence, 1 - ruler, 2 - both)" },

        { "RULEROPTIONS_NAME",        "Ruler view options" },
        { "RULEROPTIONS_DESCRIPTION", "Options of the ruler (if used)" },
    };
}// end of class MessagesBundle
