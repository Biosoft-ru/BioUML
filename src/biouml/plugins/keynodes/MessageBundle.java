package biouml.plugins.keynodes;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch( Throwable th )
        {

        }
        return key;
    }

    private final static Object[][] contents = {
        {"CN_CLASS", "Parameters"},
        {"CD_CLASS", "Parameters"},
        {"PN_MOLECULES_COLLECTION", "Molecules collection"},
        {"PD_MOLECULES_COLLECTION", "Input the collection of molecules/genes"},
        {"PN_KEYNODE_OUTPUT_COLLECTION", "Output collection"},
        {"PD_KEYNODE_OUTPUT_COLLECTION", "Output collection."},
        {"PN_KEYNODE_OUTPUT_NAME", "Output name"},
        {"PD_KEYNODE_OUTPUT_NAME", "Output name."},
        {"PN_MAX_RADIUS", "Max radius"},
        {"PD_MAX_RADIUS", "Maximal search radius"},
        {"PN_DIRECTION", "Search direction"},
        {"PD_DIRECTION", "Direction to perform search in (either upstream, downstream reactions or both directions)"},
        {"PN_PENALTY", "Penalty"},
        {"PD_PENALTY", "Penalty value for false positives"},
        {"PN_MIN_HITS", "Minimal hits"},
        {"PD_MIN_HITS", "Minimal hits from input molecules collection"},
        {"PN_SCORE_CUTOFF", "Score cutoff"},
        {"PD_SCORE_CUTOFF", "Molecules with Score lower than specified will be excluded from the result"},
        {"PN_CALCULATING_FDR", "Calculate FDR"},
        {"PD_CALCULATING_FDR", "If true, analysis will calculate False Discovery Rate"},
        {"PN_FDR_CUTOFF", "FDR cutoff"},
        {"PD_FDR_CUTOFF", "Molecules with FDR higher than specified will be excluded from the result"},
        {"PN_ZSCORE_CUTOFF", "Z-score cutoff"},
        {"PD_ZSCORE_CUTOFF", "Molecules with Z-score lower than specified will be excluded from the result"},
        {"PN_WEIGHT_COLUMN", "Weighting column"},
        {"PD_WEIGHT_COLUMN", "Column to replace weights in search graph"},
        {"PN_CONTEXT_SET", "Context genes"},
        {"PD_CONTEXT_SET", "Drug target search will be attracted towards context genes by decreasing the cost for close edges"},
        {"PN_CONTEXT_WEIGHT_COLUMN", "Context weighting column"},
        {"PD_CONTEXT_WEIGHT_COLUMN", "Attraction strength"},
        {"PN_DECAY_FACTOR", "Decay factor"},
        {"PD_DECAY_FACTOR", "Decay factor to decrease attraction with distance increase"},
        {"PN_ISOFORM_FACTOR", "Normalize multi-forms"},
        {"PD_ISOFORM_FACTOR", "Normalize weights of multiple forms"},

        /* Matrix list to molecules */
        {"PN_KEYNODES_MATRIX_COLLECTION", "Matrices collection"},
        {"PD_KEYNODES_MATRIX_COLLECTION", "Site search summary table or any other collection to get matrices from"},
        {"PN_KEYNODES_MATRIX_LIBRARY", "Matrices library"},
        {"PD_KEYNODES_MATRIX_LIBRARY", "TRANSFAC matrix library which contains listed matrices"},
        {"PN_KEYNODES_MATRICES", "List of matrices"},
        {"PD_KEYNODES_MATRICES", "Matrices to be used"},
        {"PN_KEYNODES_ALL_MATRICES", "Use all matrices"},
        {"PD_KEYNODES_ALL_MATRICES", "Uncheck to manually select matrices"},
        {"PN_KEYNODES_TRANSPATH_COLLECTION", "TRANSPATH collection"},
        {"PD_KEYNODES_TRANSPATH_COLLECTION", "Module containing TRANSPATH database"},
        {"PN_KEYNODES_ORTHOLEVEL", "Ortholevel"},
        {"PD_KEYNODES_ORTHOLEVEL", "Include ortholog information"},
        {"PN_KEYNODES_OUTPUTNAME", "Output path"},
        {"PD_KEYNODES_OUTPUTNAME", "Table to store output to"},
        {"PN_IS_INPUT_LIMITED", "Limit input size"},
        {"PD_IS_INPUT_LIMITED", "Limit size of input list"},
        {"PN_INPUT_SIZE", "Input size"},
        {"PD_INPUT_SIZE", "Size of input list"},
        
        /* KeyNode visualization parameters*/
        {"PN_VIZKEYNODES_RESULT", "Analysis result"},
        {"PD_VIZKEYNODES_RESULT", "Result or target or effector search analysis"},
        {"PN_VIZKEYNODES_DIAGRAM", "Diagram path"},
        {"PD_VIZKEYNODES_DIAGRAM", "Diagram path"},
        {"PN_VIZKEYNODES_COLUMN", "Rank column"},
        {"PD_VIZKEYNODES_COLUMN", "The result will be sorted by selected column in descending order and top ranking molecules will be taken"},
        {"PN_VIZKEYNODES_NUMTOP", "Number of top ranking molecules"},
        {"PD_VIZKEYNODES_NUMTOP", "Number of top ranking molecules"},
        {"PN_VIZKEYNODES_LOWEST", "Take lowest-ranking"},
        {"PD_VIZKEYNODES_LOWEST", "Use molecules with lowest values in given column instead"},
        {"PN_VIZKEYNODES_SCORE", "Score"},
        {"PD_VIZKEYNODES_SCORE", "Number of top ranking molecules"},
        {"PN_VIZKEYNODES_SEPARATE", "Separate diagrams"},
        {"PD_VIZKEYNODES_SEPARATE", "Create an individual diagram for each top ranking molecule"},
        
        /* DiagramExtensionAnalysis constants */
        {"PN_INPUT_DIAGRAM", "Diagram"},
        {"PD_INPUT_DIAGRAM", "Diagram to enrich"},
        {"PN_OUTPUT_DIAGRAM", "Output name"},
        {"PD_OUTPUT_DIAGRAM", "Output diagram name"},
        {"PN_ITERATION", "Steps"},
        {"PD_ITERATION", "Number of extension steps"},
        {"PN_REACTIONS_ONLY", "Add only reactions"},
        {"PD_REACTIONS_ONLY", "Do not add other reaction participants not presented on diagram"},
        
        /* Shortest path clustering constants*/
        {"PN_FULL_PATH", "Display intermediate molecules"},
        {"PD_FULL_PATH", "Output the diagram with the direct reactions and all intermediate molecules"},
    };
}
