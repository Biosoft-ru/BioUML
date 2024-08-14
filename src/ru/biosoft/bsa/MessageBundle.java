package ru.biosoft.bsa;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    public final static String[] strandTypes     = {"?", "x", "+", "-", "+/-"};
    private final static String[] precisionTypes  = {"exactly", "cutted from left", "cutted from right", "cutted from both sides", "not known"};
    private final static String[] basisTypes      = {"user defined", "annotated", "predicted", "annotated & user defined"};
    private final static String[] browserTypes    = {"user defined", "annotated", "predicted", "annotated & user defined"};

    private final static Object[][] contents =
    {
        // BindingElement constants
        {"CN_BINDING_ELEMENT"            , "binding factor"},
        {"CD_BINDING_ELEMENT"            , "Binding factor..."},

        {"PN_BINDING_ELEMENT_NAME"       , "factor name"},
        {"PD_BINDING_ELEMENT_NAME"       , "Name of factor"},

        // MapAsVectorCollection constants
        {"CN_MAP_AS_VECTOR"                  , "map set"},
        {"CD_MAP_AS_VECTOR"                  , "A map set"},

        {"PN_MAP_AS_VECTOR_NAME"             , "Name"},
        {"PD_MAP_AS_VECTOR_NAME"             , "Sequence identifier"},
        {"PN_MAP_AS_VECTOR_SEQUENCE"         , "Sequence"},
        {"PD_MAP_AS_VECTOR_SEQUENCE"         , "Sequence details"},
        {"PN_MAP_AS_VECTOR_ATTR"             , "Attributes"},
        {"PD_MAP_AS_VECTOR_ATTR"             , "Sequence attributes"},

        // Sequence constants
        {"PN_SEQUENCE_LENGTH"            , "Length"},
        {"PD_SEQUENCE_LENGTH"            , "Sequence length"},

        {"PN_SEQUENCE_NAME"            , "Sequence (chromosome) name"},
        {"PD_SEQUENCE_NAME"            , "Name of the sequence (chromosome)"},

        {"PN_SEQUENCE_CIRCULAR"          , "Circular"},
        {"PD_SEQUENCE_CIRCULAR"          , "Is sequence circular"},

        // SiteImpl constants
        {"STRAND_TYPES"                  , strandTypes},
        {"PRECISION_TYPES"               , precisionTypes},
        {"BASIS_TYPES"                   , basisTypes},
        {"BROWSER_TYPES"                 , browserTypes},
        {"CN_SITE"                  , "site"},
        {"CD_SITE"                  , "A site of sequence"},

        {"PN_SITE_NAME"             , "SiteID"},
        {"PD_SITE_NAME"             , "ID of site"},

        {"PN_SITE_TYPE"             , "Type"},
        {"PD_SITE_TYPE"             , "Type of site"},

        {"PN_SITE_SCORE"             , "Score"},
        {"PD_SITE_SCORE"             , "Site score"},

        {"PN_MODEL_NAME"             , "Model"},
        {"PD_MODEL_NAME"             , "Name of the model used to predict the site"},

        {"PN_SITE_LENGTH"           , "Length"},
        {"PD_SITE_LENGTH"           , "Site length"},

        {"PN_SITE_FROM"             , "From"},
        {"PD_SITE_FROM"             , "Left position of site in sequence"},

        {"PN_SITE_TO"               , "To"},
        {"PD_SITE_TO"               , "Right position of site in sequence"},
        
        {"PN_SITE_STRAND"           , "Strand"},
        {"PD_SITE_STRAND"           , "Strand of site"},

        {"PN_SITE_ATTR"             , "Properties"},
        {"PD_SITE_ATTR"             , "Site properties"},
        
        //Region constants.
        { "CN_REGION",                     "Region" },
        { "CD_REGION",                     "Region" },

        { "PN_REGION_DB",                  "DB" },
        { "PD_REGION_DB",                  "Complete sequence name" },

        { "PN_REGION_TITLE",               "Sequence" },
        { "PD_REGION_TITLE",               "Sequence display name" },
        
        { "PN_REGION_STRAND",                     "Strand" },
        { "PD_REGION_STRAND",                     "Strand" },

        { "PN_REGION_FROM",                "From" },
        { "PD_REGION_FROM",                "Start position" },
        
        { "PN_REGION_TO",                  "To" },
        { "PD_REGION_TO",                  "End position" },
        
        { "PN_REGION_ORDER",               "Order" },
        { "PD_REGION_ORDER",               "Region order" },
        
        { "PN_REGION_VISIBLE",             "Visible" },
        { "PD_REGION_VISIBLE",             "Indicates if region should be visible" },
        
        //TrackInfo constants.
        { "CN_TRACK_INFO",                     "Track" },
        { "CD_TRACK_INFO",                     "Track" },

        { "PN_TRACK_INFO_DB",                  "DB" },
        { "PD_TRACK_INFO_DB",                  "Complete track name" },

        { "PN_TRACK_INFO_TITLE",               "Track" },
        { "PD_TRACK_INFO_TITLE",               "Track display name" },
        
        { "PN_TRACK_INFO_GROUP",               "Group" },
        { "PD_TRACK_INFO_GROUP",               "Track group name" },
        
        { "PN_TRACK_INFO_ORDER",               "Order" },
        { "PD_TRACK_INFO_ORDER",               "Track order" },
        
        { "PN_TRACK_INFO_VISIBLE",             "Visible" },
        { "PD_TRACK_INFO_VISIBLE",             "Indicates if track should be visible" },

        { "PN_TRACK",                  "Track" },
        { "PD_TRACK",                  "Complete track name" },
        { "PN_FILTER",                  "Filter" },
        { "PD_FILTER",                  "Filter" },
        
        //TrackImportProperties
        { "PN_TRACKIMPORT_PROPERTIES", "Properties for track import"},
        { "PD_TRACKIMPORT_PROPERTIES", "Properties for track import"},
        { "PN_TRACKIMPORT_SEQCOLLECTION", "Sequence collection"},
        { "PD_TRACKIMPORT_SEQCOLLECTION", "Specify path to folder containing sequences if 'Custom' sequences source is selected"},
        { "PN_TRACKIMPORT_GENOME_ID", "Genome ID string"},
        { "PD_TRACKIMPORT_GENOME_ID", "Something like 'hg18' or 'mm6'"},
        { "PN_SITESEARCH_SEQDATABASE", "Sequences source"},
        { "PD_SITESEARCH_SEQDATABASE", "Select database to get sequences from or 'Custom' to specify sequences location manually"},
        
        { "PN_EXPORT_INCLUDE_HEADER", "Include 'track' header"},
        { "PD_EXPORT_INCLUDE_HEADER", "Track name will be added to exported file"},
        { "PN_EXPORT_PREPEND_CHR_PREFIX", "Prepend 'chr' prefix to chromosome name"},
        { "PD_EXPORT_PREPEND_CHR_PREFIX", "'chr' prefix will be prepended to chromosome name"},
        
        { "PN_FASTA_NUCLEOTIDES_PER_LINE", "Nucleotides per line"},
        { "PD_FASTA_NUCLEOTIDES_PER_LINE", "Number of nucleotides in each row (0 = no limit)"},
        { "PN_FASTA_NUCLEOTIDES_PER_SECTION", "Nucleotides per section"},
        { "PD_FASTA_NUCLEOTIDES_PER_SECTION", "Number of nucleotides per section (0 = no sections). Sections are separated by white space."},
        
        {"PN_SAMBAMTRACKIMPORT_CREATE_INDEX","Create index"},
        {"PD_SAMBAMTRACKIMPORT_CREATE_INDEX","Create index (can take a long time)"},
    };
}
