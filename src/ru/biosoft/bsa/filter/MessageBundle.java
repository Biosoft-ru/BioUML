package ru.biosoft.bsa.filter;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }


    private static String[] taxonFilterTypes =
    {
        "vertebrata",
        "insecta",
        "viridiplantae",
        "fungi",
        "nematoda",
        "bacteria",
    };

    private final static String [] taxonFilterFullNames =
    {
        "taxon/eukaryota/animalia/metazoa/chordata/vertebrata",
        "taxon/eukaryota/animalia/metazoa/arthropoda/insecta",
        "taxon/eukaryota/viridiplantae",
        "taxon/eukaryota/fungi",
        "taxon/eukaryota/animalia/metazoa/nematoda",
        "taxon/bacteria",
    };


    private Object[][] contents =
    {
        //----- general constants ------------------------------------/
        {"PN_NAME_FILTER",      "name"},
        {"PD_NAME_FILTER",      "Name filter"},

        {"PN_MATRIXID_FILTER",      "matrixID"},
        {"PD_MATRIXID_FILTER",      "Matrix ID"},

        {"PN_SITEID_FILTER",      "siteID"},
        {"PD_SITEID_FILTER",      "Site ID"},

        {"PN_MATRIX_FILTER",    "matrix filter"},
        {"PD_MATRIX_FILTER",    "Matrix filter"},

        //----- region filter constants ------------------------------/
        { "DISPLAY_NAME",                 "Region" },
        { "SHORT_DESCRIPTION",            "Part of the map to display" },

        { "ENABLED_NAME",                 "Enabled" },
        { "ENABLED_DESCRIPTION",          "Display or not this map" },

        { "TOTALLENGTH_NAME",             "Length" },
        { "TOTALLENGTH_DESCRIPTION",      "Total length of the map" },

        { "TOTALSITENUMBER_NAME",         "Site number" },
        { "TOTALSITENUMBER_DESCRIPTION",  "Total site number in the map" },

        { "REGIONFROM_NAME",             "Region from" },
        { "REGIONFROM_DESCRIPTION",      "Start of the region" },

        { "REGIONTO_NAME",            "Region to" },
        { "REGIONTO_DESCRIPTION",     "End of the region" },

        { "REGIONSITENUMBER_NAME",        "Sites in region" },
        { "REGIONSITENUMBER_DESCRIPTION", "Site number in the region" },

        {"PN_REGION_FILTER",              "Region filter" },
        {"PD_REGION_FILTER",              "Region filter..." },
/*
        {"PN_ENABLED",                    "Enable" },
        {"PD_ENABLED",                    "Enable/disable region" },

        {"PN_REGION_START",               "Start" },
        {"PD_REGION_START",               "Start of region" },

        {"PN_REGION_END",                 "End" },
        {"PD_REGION_END",                 "End of region" },
*/

        //----- site filter constants --------------------------------/
        {"PN_SITE_FILTER",              "site filter"},
        {"PD_SITE_FILTER",              "Site filter"},

        {"PN_TYPE_FILTER",              "type"},
        {"PD_TYPE_FILTER",              "Site type filter"},

        {"PN_BASIS_FILTER",             "basis"},
        {"PD_BASIS_FILTER",             "Site basis filter"},

        {"PN_STRAND_FILTER",            "strand"},
        {"PD_STRAND_FILTER",            "Site strand filter"},

        {"PN_CUTOFF_FILTER",            "cutoff"},
        {"PD_CUTOFF_FILTER",            "weight cutoff for sites"},

        {"PN_CORE_CUTOFF_FILTER",       "core cutoff"},
        {"PD_CORE_CUTOFF_FILTER",       "Core weight cutoff for sites"},

        //----- transcription factor filter constants ----------------/
        {"PN_TRANSCRIPTION_FACTOR_FILTER",          "transcription factor filter" },
        {"PD_TRANSCRIPTION_FACTOR_FILTER",          "Transcription factor filter" },

        {"PN_COMPOSITE_FILTER",                     "Composite filter" },
        {"PD_COMPOSITE_FILTER",                     "Composite filter" },

        {"TAXON_FILTER_TYPES",                      taxonFilterTypes},
        {"TAXON_FILTER_TYPES_FULL_NAMES",           taxonFilterFullNames},

        {"PN_TAXON_FILTER",                         "taxon"},
        {"PD_TAXON_FILTER",                         "Taxon filter"},

        {"PN_SPECIES_FILTER",                       "species"},
        {"PD_SPECIES_FILTER",                       "Species filter"},

        {"PN_CLASSIFICATION_FILTER",                "class"},
        {"PD_CLASSIFICATION_FILTER",                "Classification filter"},

        {"PN_DOMAIN_FILTER",                        "DNA binding domain"},
        {"PD_DOMAIN_FILTER",                        "DNA binding domain filter"},

        {"PN_FACTOR_NAME_FILTER",                   "factor name"},
        {"PD_FACTOR_NAME_FILTER",                   "Transcription factor name filter"},

        {"PN_POSITIVE_TISSUE_SPECIFICITY_FILTER",   "+ tissue specificity"},
        {"PD_POSITIVE_TISSUE_SPECIFICITY_FILTER",   "Positive tissue specifilty filter"},

        {"PN_NEGATIVE_TISSUE_SPECIFICITY_FILTER",   "- tissue specificity"},
        {"PD_NEGATIVE_TISSUE_SPECIFICITY_FILTER",   "Negative tissue specifilty filter"},
    };
}
