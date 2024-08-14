
package ru.biosoft.bsa.view;

import java.util.logging.Level;
import java.util.ListResourceBundle;

import javax.swing.Action;

import java.util.logging.Logger;

public class MessageBundle extends ListResourceBundle
{
    private Logger log = Logger.getLogger(MessageBundle.class.getName());
    
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private final static String[] viewTypes = {"Sequence", "Ruler", "Sequence and ruler"};
    private final static String[] siteDisplayTypes = {"name", "type", "matrix ID", "TF name"};
    private final static Object[][] contents =
    {
        //LinkedMapViewOptions constants.
        { "CN_LINKED_MAP_VO",                     "Linked maps" },
        { "CD_LINKED_MAP_VO",                     "A set of properties for tuning linked map view" },

        { "PN_LINKED_MAP_VO_USEXSCALE",           "Fit to width" },
        { "PD_LINKED_MAP_VO_USEXSCALE",           "Fit map to width of window" },

        { "PN_LINKED_MAP_VO_USEYSCALE",           "Fit to height" },
        { "PD_LINKED_MAP_VO_USEYSCALE",           "Fit map to height of window" },

        { "PN_LINKED_MAP_VO_OVERVIEWPANEOPTIONS", "Overview pane options" },
        { "PD_LINKED_MAP_VO_OVERVIEWPANEOPTIONS", "Options of overview pane" },

        { "PN_LINKED_MAP_VO_NORMALPANEOPTIONS",   "Normal pane options" },
        { "PD_LINKED_MAP_VO_NORMALPANEOPTIONS",   "Options of normal pane" },

        { "PN_LINKED_MAP_VO_DETAILEDPANEOPTIONS", "Detailed pane options" },
        { "PD_LINKED_MAP_VO_DETAILEDPANEOPTIONS", "Options of detailed pane" },

        
        //MapViewOptions constants
        { "CN_MAP_VIEW_OPTIONS",                         "Map options" },
        { "CD_MAP_VIEW_OPTIONS",                         "A set of properties for tuning map view" },

        { "PN_MAP_VIEW_OPTIONS_FONT",                   "Title font" },
        { "PD_MAP_VIEW_OPTIONS_FONT",                   "Font for region title" },
        
        { "PN_MAP_VIEW_OPTIONS_COLORSCHEME",            "Color scheme" },
        { "PD_MAP_VIEW_OPTIONS_COLORSCHEME",             "Scheme to color the sites" },

        { "PN_MAP_VIEW_OPTIONS_MAXWIDTH",               "Max width" },
        { "PD_MAP_VIEW_OPTIONS_MAXWIDTH",                "Maximal width of single map (in nucleotides)" },

        { "PN_MAP_VIEW_OPTIONS_INTERVAL",               "Interval" },
        { "PD_MAP_VIEW_OPTIONS_INTERVAL",                "Vertical interval between two maps" },

        { "PN_MAP_VIEW_OPTIONS_SEQUENCEVIEWOPTIONS",    "Sequence options" },
        { "PD_MAP_VIEW_OPTIONS_SEQUENCEVIEWOPTIONS",     "Options of the sequence view in the map" },

        { "PN_MAP_VIEW_OPTIONS_REGION",                 "Linked region size" },
        { "PD_MAP_VIEW_OPTIONS_REGION",                  "Size of linked region" },

        { "PN_MAP_VIEW_OPTIONS_REGIONCOLOR",            "Linked region color" },
        { "PD_MAP_VIEW_OPTIONS_REGIONCOLOR",             "Color of linked region" },

        //SequenceViewOptions constants
        {"VIEW_TYPES",                viewTypes},

        { "CN_SEQUENCE_VIEW_OPTIONS",        "Sequence options" },
        { "CD_SEQUENCE_VIEW_OPTIONS",        "A set of properties for tuning seqiunce view" },

        { "PN_SEQUENCE_VIEW_OPTIONS_DENSITY",             "pixel/nucleotide" },
        { "PD_SEQUENCE_VIEW_OPTIONS_DENSITY",      "Density of nucleotides in number of pixels per nucleotide" },

        { "PN_SEQUENCE_VIEW_OPTIONS_FONT",                "Font" },
        { "PD_SEQUENCE_VIEW_OPTIONS_FONT",         "Font used for painting nucletides letters" },

        { "PN_SEQUENCE_VIEW_OPTIONS_TYPE",                "Type" },
        { "PD_SEQUENCE_VIEW_OPTIONS_TYPE",         "Representation of sequence (0 - sequence, 1 - ruler, 2 - both)" },

        { "PN_SEQUENCE_VIEW_OPTIONS_RULEROPTIONS",        "Ruler view options" },
        { "PD_SEQUENCE_VIEW_OPTIONS_RULEROPTIONS", "Options of the ruler (if used)" },

        //SiteViewOptions constants
        { "SITE_DISPLAY_NAME_TYPES",   siteDisplayTypes },

        { "PN_SITE_VIEW_OPTIONS_SHOW_ALIGNMENT", "Show alignment" },
        { "PD_SITE_VIEW_OPTIONS_SHOW_ALIGNMENT", "Show mismatched bases, gaps and insertions" },
        
        { "PN_SITE_VIEW_OPTIONS_SHOW_PHRED_QUAL", "Show phred qualities" },
        { "PD_SITE_VIEW_OPTIONS_SHOW_PHRED_QUAL", "Show phred qualities" },
        
        { "PN_SITE_VIEW_OPTIONS_SHOW_CONTIG", "Show contig" },
        { "PD_SITE_VIEW_OPTIONS_SHOW_CONTIG", "Show contig" },
        
        { "PN_SITE_VIEW_OPTIONS_SHOW_ONLY_MISMATCHES_IN_CONTIG", "Show only mismatches in contig" },
        { "PD_SITE_VIEW_OPTIONS_SHOW_ONLY_MISMATCHES_IN_CONTIG", "Show only mismatches in contig" },
        
        { "PN_SITE_VIEW_OPTIONS_PHRED_QUAL_COLOR", "Phred quality color"},
        { "PD_SITE_VIEW_OPTIONS_PHRED_QUAL_COLOR", "Color for phred quality chart"},

        { "PN_SITE_VIEW_OPTIONS_PROFILE_VIEW", "Always show profile" },
        { "PD_SITE_VIEW_OPTIONS_PROFILE_VIEW", "Selected, sites density is shown even if <"+TrackViewBuilder.SITE_COUNT_LIMIT+" sites appear in the view" },

        { "PN_SITE_VIEW_OPTIONS_MAX_PROFILE_HEIGHT", "Max profile height" },
        { "PD_SITE_VIEW_OPTIONS_MAX_PROFILE_HEIGHT", "Profile will be rescaled if this height is exceeded" },
        
        { "PN_SITE_VIEW_OPTIONS_SHOW_PASSING_THROUGH_INTRONS", "Show passing through introns" },
        { "PD_SITE_VIEW_OPTIONS_SHOW_PASSING_THROUGH_INTRONS", "Show alignment if only N part of cigar visible" },
        
        
        { "PN_TRACK_DISPLAY_MODE", "Track display mode" },
        { "PD_TRACK_DISPLAY_MODE", "How to display the track" },

        //Project constants.
        { "CN_PROJECT",                    "Project" },
        { "CD_PROJECT",                    "Project" },

        { "PN_PROJECT_NAME",               "Name" },
        { "PD_PROJECT_NAME",               "Project name" },
        
        //--- Actions ---------------------------------------------------/
        
        { TracksViewPart.ADD_ACTION    + Action.SMALL_ICON           , "add.gif"},
        { TracksViewPart.ADD_ACTION    + Action.NAME                 , "Add"},
        { TracksViewPart.ADD_ACTION    + Action.SHORT_DESCRIPTION    , "Add track"},
        { TracksViewPart.ADD_ACTION    + Action.ACTION_COMMAND_KEY   , "cmd-add-track"},
        
        { TracksViewPart.REMOVE_ACTION    + Action.SMALL_ICON           , "remove.gif"},
        { TracksViewPart.REMOVE_ACTION    + Action.NAME                 , "Remove"},
        { TracksViewPart.REMOVE_ACTION    + Action.SHORT_DESCRIPTION    , "Remove track"},
        { TracksViewPart.REMOVE_ACTION    + Action.ACTION_COMMAND_KEY   , "cmd-remove-track"},
        
//        { ProfileEditorViewPart.ADD_ACTION    + Action.SMALL_ICON           , "add.gif"},
//        { ProfileEditorViewPart.ADD_ACTION    + Action.NAME                 , "Add"},
//        { ProfileEditorViewPart.ADD_ACTION    + Action.SHORT_DESCRIPTION    , "Add selected matrices"},
//        { ProfileEditorViewPart.ADD_ACTION    + Action.ACTION_COMMAND_KEY   , "cmd-add-matrices"},
//
        //Messages
        {"REMOVE_REGION_CONFIRM_MESSAGE", "Do you really want to remove region \"{0}\" from project?"},
        {"REMOVE_REGION_CONFIRM_TITLE", "Remove confirmation"},
        {"REMOVE_TRACK_CONFIRM_MESSAGE", "Do you really want to remove track \"{0}\" from project?"},
        {"REMOVE_TRACK_CONFIRM_TITLE", "Remove confirmation"},
        
        //Dialog constants
        {"LOAD_TRACK_DATABASE_LABEL", "Databases: "},
        { "CN_TRACK_LINE",                     "Track" },
        { "CD_TRACK_LINE",                     "Track" },
        { "PN_TRACK_LINE_NAME",                "Name" },
        { "PD_TRACK_LINE_NAME",                "Complete track name" },
        { "PN_TRACK_LINE_ADD",                 "Add" },
        { "PD_TRACK_LINE_ADD",                 "Add track to project" },
        
        { "CN_PROFILE_EDITOR", "Options" },
        { "CD_PROFILE_EDITOR", "Options" },
        { "PN_MATRIX_LIBRARY", "Matrix library" },
        { "PD_MATRIX_LIBRARY", "Collection of matrices" },
        { "PN_INITIAL_CUTOFFS", "Initial cutoffs" },
        { "PD_INITIAL_CUTOFFS", "Initial cutoffs" },
        { "PN_CUTOFF_TEMPLATE", "Template profile" },
        { "PD_CUTOFF_TEMPLATE", "Use given profile as template" },
        
        { "PN_FILTER", "Filter library" },
        { "PD_FILTER", "Specify constraints to filter out some matrices from the view"},
    };
    
    /**
     * Returns string from the resource bundle for the specified key.
     * If the sting is absent the key string is returned instead and
     * the message is printed in <code>java.util.logging.Logger</code> for the component.
     */
    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}
