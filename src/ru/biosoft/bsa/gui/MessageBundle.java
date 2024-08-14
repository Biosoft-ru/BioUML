package ru.biosoft.bsa.gui;

public class MessageBundle extends biouml.workbench.resources.MessageBundle
{
    public MessageBundle()
    {
        setParent(new biouml.workbench.resources.MessageBundle());
    }
    
    @Override
    protected Object[][] getContents() { return contents; }
    
    @Override
    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch (Throwable th)
        {

        }
        return key;
    }
    
    private final static Object[][] contents =
    {
        {"EXPORT_TRACK_DIALOG_TITLE", "Export track"},
        {"EXPORT_TRACK_DIALOG_NO_TRACKS", "No tracks to export"},
        {"EXPORT_TRACK_DIALOG_NO_EXPORTERS", "No suitable exporters registered"},
        {"TRACK_DIALOG_FILE", "File:"},
        {"TRACK_DIALOG_TRACK", "Track:"},
        {"TRACK_DIALOG_SEQUENCE", "Sequence:"},
        {"TRACK_DIALOG_FORMAT", "Format: "},
        {"TRACK_DIALOG_INFO", "Messages:"},
        {"TRACK_DIALOG_CLOSE", "Close"},
        {"TRACK_DIALOG_CANCEL", "Cancel"},
        {"EXPORT_TRACK_DIALOG_EXPORT", "Export"},
        {"EXPORT_TRACK_DIALOG_NO_EXPORTERS", "There are no suitable formats for this track to export."},
        {
                "EXPORT_TRACK_DIALOG_NO_EXPORTER",
                "Ops, the diagram can not be exported in this format.\n" + "format={0}, diagramType={1}.\n"
                        + "Please report to info@biouml.org about this error."},
        {"EXPORT_TRACK_DIALOG_SUCCESS", "Track {0} was successfuly exported in {1} format.\n" + "File: {2}\ntime: {3} ms."},
        {"EXPORT_TRACK_DIALOG_ERROR", "Could not export diagram {0} in format {1}, error: {2}."},
        {"EXPORT_TRACK_DIALOG_PREFERENCES_DIR_PN", "Export dialog, directory"},
        {"EXPORT_TRACK_DIALOG_PREFERENCES_DIR_PD", "Default directory for file chooser specify file to export track."},
        {"EXPORT_TRACK_VISIBLE_RANGE", "Visible range"},
        {"EXPORT_TRACK_WHOLE_SEQUENCE", "Whole sequence"},
        {"EXPORT_TRACK_CUSTOM_RANGE", "Custom range"},
        {"EXPORT_TRACK_EXPORT_STARTED", "Export started"},
        {"EXPORT_TRACK_EXPORT_CANCELLED", "Export cancelled"},
        {"EXPORT_TRACK_FAILED", "Export failed: {0}"},
        {"EXPORT_TRACK_INVALID_RANGE", "Invalid range specified: export aborted"},
        
        {"PN_SITESEARCH_SEQCOLLECTION", "Sequences collection"},
        {"PD_SITESEARCH_SEQCOLLECTION", "Collection of sequences"},
        {"PN_SITESEARCH_SEQLIST", "Sequence(s)"},
        {"PD_SITESEARCH_SEQLIST", "List of sequences to search sites in"},
        {"PN_SITESEARCH_PROFILE", "Profile"},
        {"PD_SITESEARCH_PROFILE", "Predefined set of site models"},
        {"PN_SITESEARCH_TRACK", "Track"},
        {"PD_SITESEARCH_TRACK", "Filtering track"},
        {"PN_SITESEARCH_OUTPUTCOLLECTION", "Output collection"},
        {"PD_SITESEARCH_OUTPUTCOLLECTION", "Output collection"},
        {"PN_SITESEARCH_OUTPUTNAME", "Output name"},
        {"PD_SITESEARCH_OUTPUTNAME", "Output name"},
        
        {"PN_IPSMOTIFDISCOVERY_SEQCOLLECTION", "Input Sequences"},
        {"PD_IPSMOTIFDISCOVERY_SEQCOLLECTION", "Input Sequences"},
        {"PN_IPSMOTIFDISCOVERY_INITIAL_MATRICES", "Initial matrices"},
        {"PD_IPSMOTIFDISCOVERY_INITIAL_MATRICES", "Frequecy matrices that will be used as initial approximation"},
        {"PN_IPSMOTIFDISCOVERY_MAX_ITERATIONS", "Max iterations"},
        {"PD_IPSMOTIFDISCOVERY_MAX_ITERATIONS", "Maximum number of iterations"},
        {"PN_IPSMOTIFDISCOVERY_MIN_CLUSTER_SIZE", "Min number of sequences"},
        {"PD_IPSMOTIFDISCOVERY_MIN_CLUSTER_SIZE", "Minimal number of sequences to make a matrix"},
        {"PN_IPSMOTIFDISCOVERY_WINDOW_SIZE", "Window size"},
        {"PD_IPSMOTIFDISCOVERY_WINDOW_SIZE", "Size of window around binding site for calculation of local background"},
        {"PN_IPSMOTIFDISCOVERY_CRIT_IPS", "Critical IPS threshold"},
        {"PD_IPSMOTIFDISCOVERY_CRIT_IPS", "Critical IPS threshold"},
        {"PN_IPSMOTIFDISCOVERY_OUTPUT_MATRIX_LIB", "Output matrix collection"},
        {"PD_IPSMOTIFDISCOVERY_OUTPUT_MATRIX_LIB", "Collection where constructed matrices will be saved"},
        {"PN_IPSMOTIFDISCOVERY_EXTEND", "Search for sub motifs"},
        {"PD_IPSMOTIFDISCOVERY_EXTEND", "Whether to search for sub motifs"},
        
        { "CN_CLASS", "Parameters"},
        { "CD_CLASS", "Parameters"},
        { "PN_METHOD_NAME",                        "Method"},
        { "PD_METHOD_NAME",                        "Method"},

        { "PN_METHOD_DESCRIPTION",                 "Method description"},
        { "PD_METHOD_DESCRIPTION",                 "Method description"},

        {"PN_GENESET_SOURCECOLLECTION", "Table collection"},
        {"PD_GENESET_SOURCECOLLECTION", "Data collection which contains source table"},
        {"PN_GENESET_SOURCE", "Table"},
        {"PD_GENESET_SOURCE", "Table of source genes"},
        {"PN_GENESET_OUTPUTNAME", "Output name"},
        {"PD_GENESET_OUTPUTNAME", "Output name"},
        {"PN_GENESET_FROM", "From"},
        {"PD_GENESET_FROM", "From position (relative to gene start)"},
        {"PN_GENESET_TO", "To"},
        {"PD_GENESET_TO", "To position (relative to gene start)"},
        {"PN_GENESET_OUTPUTPATH", "Output path"},
        {"PD_GENESET_OUTPUTPATH", "Output path"},
        {"PN_GENESET_OUTPUT_TRACK", "Output track"},
        {"PD_GENESET_OUTPUT_TRACK", "Path to output track"},
        {"PN_SNP_OUTPUT_TABLE", "Output SNP table"},
        {"PD_SNP_OUTPUT_TABLE", "Path to output table with additional annotation"},
        {"PN_SNP_OUTPUT_GENES", "Output genes table"},
        {"PD_SNP_OUTPUT_GENES", "Path to output table containing all the genes matched to SNPs"},
        {"PN_SNP_5PRIME", "5' region size"},
        {"PD_SNP_5PRIME", "Include 5' region (promoter) of given size in bp"},
        {"PN_SNP_3PRIME", "3' region size"},
        {"PD_SNP_3PRIME", "Include 3' region of given size in bp"},
        {"PN_SNP_OUTPUT_NON_MATCHED", "Output non-matched"},
        {"PD_SNP_OUTPUT_NON_MATCHED", "Whether to include in output table and track SNP's which were not matched to any gene"},
        {"PN_SNP_NUMERIC_COLUMN", "Column to copy"},
        {"PD_SNP_NUMERIC_COLUMN", "Name of additional column to be copied to created genes table"},
        {"PN_SNP_AGGREGATION_TYPE", "Aggregator"},
        {"PD_SNP_AGGREGATION_TYPE", "Operation to perform on column values if several SNP's matched to single gene ('minimum','maximum','sum','average')"},
        
        {"PN_GENESET_YESSET", "Yes set"},
        {"PD_GENESET_YESSET", "Set of genes expressed in experiment (test set)"},
        {"PN_GENESET_NOSET", "No set"},
        {"PD_GENESET_NOSET", "Set of background genes (control set)"},
        {"PN_GENESET_TRACKOUTPUTCOLLECTION", "Tracks output collection"},
        {"PD_GENESET_TRACKOUTPUTCOLLECTION", "Collection to store resulting tracks"},
        {"PN_GENESET_TABLEOUTPUTCOLLECTION", "Table output collection"},
        {"PD_GENESET_TABLEOUTPUTCOLLECTION", "Collection to store resulting table"},
        {"PN_GENESET_PREFIX", "Output prefix"},
        {"PD_GENESET_PREFIX", "Name prefix for all output data"},
        {"PN_GENESET_OPTIMIZE_CUTOFF", "Optimize cutoffs"},
        {"PD_GENESET_OPTIMIZE_CUTOFF", "Matrix cutoffs from profile will be tuned to maximize p-value"},
        {"PN_GENESET_OPTIMIZE_WINDOW", "Optimize window"},
        {"PD_GENESET_OPTIMIZE_WINDOW", "From/to positions will be optimized for each matrix to maximize p-value"},
        {"PN_GENESET_DELETE_NON_OPTIMIZED", "Remove non-optimized sites"},
        {"PD_GENESET_DELETE_NON_OPTIMIZED", "Delete preliminary site search result to save drive space"},

        {"PN_SITESEARCH_INPUT_YES_TRACK", "Input yes track"},
        {"PD_SITESEARCH_INPUT_YES_TRACK", "Site search result for genes expressed in experiment"},
        {"PN_SITESEARCH_INPUT_NO_TRACK", "Input no track"},
        {"PD_SITESEARCH_INPUT_NO_TRACK", "Site search result for background genes"},
        {"PN_SITESEARCH_OUTPUT_YES_TRACK", "Output yes track"},
        {"PD_SITESEARCH_OUTPUT_YES_TRACK", "Filtered site search result for expressed genes"},
        {"PN_SITESEARCH_OUTPUT_NO_TRACK", "Output no track"},
        {"PD_SITESEARCH_OUTPUT_NO_TRACK", "Filtered site search result for background genes"},
        {"PN_SITESEARCH_SEQDATABASE", "Sequences source"},
        {"PD_SITESEARCH_SEQDATABASE", "Select database to get sequences from or 'Custom' to specify sequences location manually"},

        {"PN_SITESEARCH_PVALUE_CUTOFF", "P-value cutoff"},
        {"PD_SITESEARCH_PVALUE_CUTOFF", "Matrix will be removed from result if p-value cannot be optimized to values lower than specified cutoff"},
        {"PD_GENESET_PVALUE_CUTOFF", "Matrix will be removed from result if p-value cannot be optimized to values lower than specified cutoff (parameters ignored if both cutoff and window optimizations are switched off)"},

        {"PN_SITESEARCH_OPTIMIZATION_TYPE", "Optimization type"},
        {"PD_SITESEARCH_OPTIMIZATION_TYPE", "Whether to optimize only matrix cutoffs, promoter windows or both"},
        
        {"PN_SUMMARY_YESTRACK", "Yes track"},
        {"PD_SUMMARY_YESTRACK", "Site search result for genes expressed in experiment"},
        {"PN_SUMMARY_NOTRACK", "No track"},
        {"PD_SUMMARY_NOTRACK", "Site search result for background genes"},
        {"PN_SUMMARY_OUTPUTNAME", "Output name"},
        {"PD_SUMMARY_OUTPUTNAME", "Output name"},
        {"PN_SUMMARY_OUTPROFILE", "Output profile"},
        {"PD_SUMMARY_OUTPROFILE", "Output profile"},
        {"PN_SUMMARY_OVERREPRESENTED_ONLY", "Overrepresented matrices in summary"},
        {"PD_SUMMARY_OVERREPRESENTED_ONLY", "Remove underrepresented matrices from the output (not applicable if no-set is not specified)"},
        
        
        {"PN_MACS_NOLAMBDA", "Use fixed lambda"},
        {"PD_MACS_NOLAMBDA", "Use fixed background lambda as local lambda for every peak region"},
        {"PN_MACS_BW", "Band width"},
        {"PD_MACS_BW", "Band width"},
        {"PN_MACS_SHIFTSIZE", "Shift size"},
        {"PD_MACS_SHIFTSIZE", "The arbitrary shift size in bp. Used in no-model mode."},
        {"PN_MACS_NOMODEL", "No model"},
        {"PD_MACS_NOMODEL", "Do not build the shifting model. In this mode shift size parameter is used."},
        {"PN_MACS_GSIZE", "Genome size"},
        {"PD_MACS_GSIZE", "Effective genome size"},
        {"PN_MACS_MFOLD", "Enrichment ratio"},
        {"PD_MACS_MFOLD", "High-confidence enrichment ratio against background"},
        {"PN_MACS_MFOLD_LOWER", "MFOLD lower"},
        {"PD_MACS_MFOLD_LOWER", "Lower boundary of high-confidence enrichment ratio"},
        {"PN_MACS_MFOLD_UPPER", "MFOLD upper"},
        {"PD_MACS_MFOLD_UPPER", "Upper boundary of high-confidence enrichment ratio"},
        {"PN_MACS_TSIZE", "Tag size"},
        {"PD_MACS_TSIZE", "Tag size"},
        {"PN_MACS14_TSIZE", "Tag size (0 = autodetect)"},
        {"PD_MACS14_TSIZE", "Length of the tag in bp. If 0, it will be calculated based on average read length over several first reads."},
        {"PN_MACS_PVALUE", "P-value"},
        {"PD_MACS_PVALUE", "P-value cutoff for peak detection"},
        {"PN_MACS_FUTURE_FDR", "Future FDR"},
        {"PD_MACS_FUTURE_FDR", "Adopt the new peak detection method as new standard. The default method only considers the peak location in the 1k, 5k, or 10kb regions of the control data. In contrast, the new method also considers the 5k or 10k regions of the test data to calculate the local bias."},
        {"PN_MACS_LAMBDA_SET", "Lambda set"},
        {"PD_MACS_LAMBDA_SET", "Three levels of nearby region in basepairs to calculate dynamic lambda"},
        {"PN_MACS_CONTROL", "Control track"},
        {"PD_MACS_CONTROL", "Control track (can be omitted)"},
        {"PN_MACS_LAMBDA1", "lambda 1"},
        {"PD_MACS_LAMBDA1", "lambda 1"},
        {"PN_MACS_LAMBDA2", "lambda 2"},
        {"PD_MACS_LAMBDA2", "lambda 2"},
        {"PN_MACS_LAMBDA3", "lambda 3"},
        {"PD_MACS_LAMBDA3", "lambda 3"},
        {"PN_MACS_KEEP_DUP", "Keep duplicates"},
        {"PD_MACS_KEEP_DUP", "How many tags will be kept in the same location (auto, all or number). Auto means value will be calculated based on binomal distribution using 1e-5 as p-value cutoff"},
        {"PN_MACS_TO_SMALL", "Scale to small"},
        {"PD_MACS_TO_SMALL", "Whether to scale larger dataset down to smaller one."},
        {"PN_MACS_S_LOCAL", "Small region for dynamic lambda"},
        {"PD_MACS_S_LOCAL", "The small nearby region in basepairs to calculate dynamic lambda. This is used to capture the bias near the peak summit region. Invalid if there is no control data."},
        {"PN_MACS_L_LOCAL", "Large region for dynamic lambda"},
        {"PD_MACS_L_LOCAL", "The large nearby region in basepairs to calculate dynamic lambda. This is used to capture the bias near the peak summit region. Invalid if there is no control data."},
        {"PN_MACS_AUTO_OFF", "No auto pair process"},
        {"PD_MACS_AUTO_OFF", "Whether to turn off the auto pair model process. If true, then when MACS failed to build paired model, it will exit with error message. Otherwise it will use the nomodel settings"},
        {"PN_MACS_COMPUTE_PEAK_PROFILE", "Compute peak profile"},
        {"PD_MACS_COMPUTE_PEAK_PROFILE", "Compute peak profile"},

        {"PN_SPECIES", "Species"},
        {"PD_SPECIES", "Taxonomical species"},
        
        {"PN_SEQUENCES_INPUT_SEQUENCES", "Input sequences"},
        {"PD_SEQUENCES_INPUT_SEQUENCES", "Folder containing input sequences"},
        {"PN_SEQUENCES_OUTPUT_TRACK", "Output track"},
        {"PD_SEQUENCES_OUTPUT_TRACK", "Output track"},
        
        {"NEW_PROJECT_DIALOG_TITLE", "Select tracks to add to genome browser"},
        
        {"CN_TRACK", "Track"},
        {"CD_TRACK", "Track"},
        {"PN_TRACK_NAME", "Track"},
        {"PD_TRACK_NAME", "Track"},
        {"PN_TRACK_SELECTED", " "},
        {"PD_TRACK_SELECTED", "Add this track to the project"},
};
}
