
package ru.biosoft.bsa.view.colorscheme;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents =
    {
        //----- ColorSchemeEditor constants --------------------------/
        {"PN_OPEN_COLOR_SCHEME",        "Open" },
        {"PD_OPEN_COLOR_SCHEME",        "Open color scheme" },
        
        {"PN_SAVE_COLOR_SCHEME",        "Save as"},
        {"PD_SAVE_COLOR_SCHEME",        "Save color scheme as ..."},

        {"PN_COLOR_SCHEME",             "Scheme"},
        {"PD_COLOR_SCHEME",             "Scheme ..."},

        {"PN_DEFAULT_BRUSH",             "default color"},
        {"PD_DEFAULT_BRUSH",             "This color is used when the color scheme can not be applied to the site, " +
                                         "for example site weight color scheme can not be applied if the site has not weight."},


        //----- SiteWeightColorScheme constants ----------------------/
        {"CN_WEIGHT_COLOR_SCHEME",      "Weight color scheme"},
        {"CD_WEIGHT_COLOR_SCHEME",      "Weight color scheme ..."},

        {"PN_COLOR1",                   "0.0 weight color"},
        {"PD_COLOR1",                   "The color to be used for sites with weight 0.0"},

        {"PN_COLOR2",                   "1.0 weight color"},
        {"PD_COLOR2",                   "The color to be used for sites with weight 1.0"},

        {"PN_COEFFICIENT",              "contrast"},
        {"PD_COEFFICIENT",              "contrast"},

        //----- ColorScheme constants -----------------------/
        {"PN_NAME",   "Name"},
        {"PD_NAME",   "Name..."},
        
        //----- CompositeColorScheme constants -----------------------/
        {"CN_COMPOSITE_COLOR_SCHEME",   "Composite scheme"},
        {"CD_COMPOSITE_COLOR_SCHEME",   "Composite scheme ..."},

        {"PN_SCHEMES",                  "schemes"},
        {"PD_SCHEMES",                  "schemes ..."},

        //----- KeyBasedSiteColorScheme constants --------------------/
        {"CN_KEY_BASED_COLOR_SCHEME",   "Key based color scheme"},
        {"CD_KEY_BASED_COLOR_SCHEME",   "Key based color scheme ..."},

        {"PN_KEY_COLOR_GROUP",          "first group"},
        {"PD_KEY_COLOR_GROUP",          "first group ..."},

        {"CN_KEY_COLOR_GROUP",          "Key color group"},
        {"CD_KEY_COLOR_GROUP",          "Key color group ..."},

        {"PN_GROUPS",                   "groups"},
        {"PD_GROUPS",                   "groups array ..."},

        {"PN_BRUSH",                    "brush"},
        {"PD_BRUSH",                    "brush ..."},

        {"CN_COLOR_SCHEME_EDITOR",      "Color scheme editor" },
        {"CD_COLOR_SCHEME_EDITOR",      "Color scheme editor" },
        
        {"PN_MIN_VALUE", "Min value"},
        {"PD_MIN_VALUE", "Minimal value for weight"},
        
        {"PN_MAX_VALUE", "Max value"},
        {"PD_MAX_VALUE", "Maximum value for weight"},
        
        {"PN_WEIGHT_PROPERTY", "Weight property"},
        {"PD_WEIGHT_PROPERTY", "Site property name to be used as weight"},

    };
}
