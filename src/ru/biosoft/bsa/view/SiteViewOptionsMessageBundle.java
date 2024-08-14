
/**
 * Author:  Igor V. Tyazhev  (champ@developmentontheedge.com)
 *
 * Created: 27.02.2001
 *
 * Description:
 *
 * Copyright (C) 2000, 2001 DevelopmentOnTheEdge.com. All rights reserved.
 */

package ru.biosoft.bsa.view;

import java.util.ListResourceBundle;

public class SiteViewOptionsMessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    protected String[] siteDisplayTypes = {
                                            "ID",
                                            "Type",
                                            "Title",
                                            "TF name (if applicable)",
                                            "Strand"
    };

    private Object[][] contents =
    {
        { "SITE_DISPLAY_NAME_TYPES",   siteDisplayTypes },

        { "DISPLAY_NAME",              "Site options" },
        { "SHORT_DESCRIPTION",         "Properties of site view" },

        { "FONT_NAME",                 "Font" },
        { "FONT_DESCRIPTION",          "Font used for painting site label, sequence and position" },
/*
        { "STATICFILTEROPTIONS_NAME",                 "Static filter" },
        { "STATICFILTEROPTIONS_DESCRIPTION",          "Filter for sites, used while building map" },

        { "DYNAMICFILTEROPTIONS_NAME",                 "Dynamic filter" },
        { "DYNAMICFILTEROPTIONS_DESCRIPTION",          "Filter, used dynamicaly, without rebuilding map" },
*/
        { "DISPLAYNAME_NAME",          "Name" },
        { "DISPLAYNAME_DESCRIPTION",   "Display name of site view" },

        { "PENWIDTH_NAME",             "Pen width" },
        { "PENWIDTH_DESCRIPTION",      "Width of the pen used for site painting" },

        { "LAYOUTALGORITHM_NAME",           "Layout algorithm" },
        { "LAYOUTALGORITHM_DESCRIPTION",    "Layout algorithm used to optimeze locations of the sites" },

        { "INTERVAL_NAME",             "Interval" },
        { "INTERVAL_DESCRIPTION",      "Interval between site marker and label, sequence and positions" },

        { "BOXHEIGHT_NAME",            "Height" },
        { "BOXHEIGHT_DESCRIPTION",     "Height of the site if it painted as box" },

        { "SHOWTITLE_NAME",            "Title visible" },
        { "SHOWTITLE_DESCRIPTION",     "Should be title of the site displayed?" },

        { "SHOWPOSITIONS_NAME",        "Positions visible" },
        { "SHOWPOSITIONS_DESCRIPTION", "Should be positions of the site displayed?" },

        { "SHOWSEQUENCE_NAME",         "Sequence visible" },
        { "SHOWSEQUENCE_DESCRIPTION",  "Should be sequence of the site displayed?" },

        { "SHOWSTRAND_NAME",           "Strand visible" },
        { "SHOWSTRAND_DESCRIPTION",    "Should be strand of the site displayed?" },

        { "SHOWBOX_NAME",              "As box" },
        { "SHOWBOX_DESCRIPTION",       "Should be site displayed as box?" },
    };
}// end of class MessagesBundle
