package ru.biosoft.analysis.gui;

import java.util.ListResourceBundle;


public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private final static Object[][] contents = {
            {"CN_CLASS", "Parameters"},
            {"CD_CLASS", "Parameters"},
            {"PN_METHOD_NAME", "Method name"},
            {"PD_METHOD_NAME", "Method name"},
            {"PN_METHOD_DESCRIPTION", "Method description"},
            {"PD_METHOD_DESCRIPTION", "Method description"},
            {"PN_METHOD_SHORT_DESCRIPTION", "Short description"},
            {"PD_METHOD_SHORT_DESCRIPTION", "Short description"},


            //Import
            {"PN_IMPORT_FILE", "File to import"},
            {"PD_IMPORT_FILE", "Specify file from your computer"},
            {"PN_IMPORT_RESULT_PATH", "Target"},
            {"PD_IMPORT_RESULT_PATH", "Path in repository to put imported file to"},
            {"PN_IMPORT_PROPERTIES", "Properties"},
            {"PD_IMPORT_PROPERTIES", "Additional importer parameters"},

            {"PN_EXPERIMENTS_COUNT", "Number of experiments"},
            {"PD_EXPERIMENTS_COUNT", "Number of experiments"},
            {"PN_EXPERIMENTS", "Experiments"},
            {"PD_EXPERIMENTS", "Experiments"},

            //Microarray analysis common parameters
            {"PN_OUTPUT_TABLE", "Output table"},
            {"PD_OUTPUT_TABLE", "Output table"},
            {"PN_OUTPUT_HISTOGRAM", "Output histogram"},
            {"PD_OUTPUT_HISTOGRAM", "If specified, histogram will be created showing fold-change distribution"},
            {"PN_EXPERIMENT", "Experiment"},
            {"PD_EXPERIMENT", "Table with experimental(test) data"},
            {"PN_CONTROL", "Control"},
            {"PD_CONTROL", "Table with control data"},
            {"PN_PVALUE", "P-value threshold"},
            {"PD_PVALUE", "Only results with a P-value below this threshold will appear in the output"},
            {"PN_THRESHOLD", "Value boundaries"},
            {"PD_THRESHOLD", "All values outside these boundaries will be treated as outliers"},
            {"PN_LOWER_BOUNDARY", "Lower boundary"},
            {"PD_LOWER_BOUNDARY", "Input values below this threshold will be ignored"},
            {"PN_UPPER_BOUNDARY", "Upper boundary"},
            {"PD_UPPER_BOUNDARY", "Input values above this threshold will be ignored"},
            {"PN_CALCULATING_FDR", "Calculate FDR"},
            {"PD_CALCULATING_FDR", "If checked, False Discovery Rate will be calculated"},

            //GeneModelBuildingParameters
            {"PN_REGULATOR_LIST", "Regulator list"},
            {"PD_REGULATOR_LIST", "Table with regulators ID"},
            {"PN_TARGET_LIST", "Target list"},
            {"PD_TARGET_LIST", "Table with regulators ID"},

            //SDEModelBuildingParameters
            {"PN_REGULATORS_LIMIT", "Regulators limit"},
            {"PD_REGULATORS_LIMIT", "Regulators number limit"},

            //LinearShiftedModelBuildingParameters
            {"PN_SMOOTHING_TARGET", "Target smothing type"},
            {"PD_SMOOTHING_TARGET", "Smoothing type for target gene profile"},
            {"PN_SMOOTHING_REGULATOR", "Regulator smothing type"},
            {"PD_SMOOTHING_REGULATOR", "Smoothing type for regulator gene profile"},
            {"PN_SHIFT", "Maximum shift"},
            {"PD_SHIFT", "Maximum shift between regulator and target"},
            {"PN_SHIFT_DEFINITION", "Shift definition"},
            {"PD_SHIFT_DEFINITION", "Shift definition"},
            {"PN_DEGRADATION", "Include degradation"},
            {"PD_DEGRADATION", "Include protein degradation in gene regulation model"},
            {"PN_ERROR", "Error"},
            {"PD_ERROR", "Spline error"},
            //nonlinear model
            {"PN_OPT_METHOD", "Optimization method"},
            {"PD_OPT_METHOD", "Optimization method"},
            {"PN_NUMBER_OF_BEST", "Best regulator quantity"},
            {"PD_NUMBER_OF_BEST", "Number of best regulators for each target in output"},

            //optimization parameters
            {"PN_SRES_PARS", "SRES parameters"},
            {"PD_SRES_PARS", "SRES parameters"},
            {"PN_GLB_PARS", "GLB parameters"},
            {"PD_GLB_PARS", "GLB parameters"},
            {"PN_ASA_PARS", "ASA parameters"},
            {"PD_ASA_PARS", "ASA parameters"},

            {"PN_NUM_OF_ITERATION", "Number of iterations"},
            {"PD_NUM_OF_ITERATION", "Number of iterations"},
            {"PN_SRES_SURVIVAL_SIZE", "Survival size"},
            {"PD_SRES_SURVIVAL_SIZE", "Survival size"},
            {"PN_DELTA", "Delta"},
            {"PD_DELTA", "Delta"},

            //Kernel smoothing
            {"PN_KERNELPARS", "Kernel type"},
            {"PD_KERNELPARS", "Smoothing kernel type"},
            {"PN_KERNEL_FRAME", "Kernal frame"},
            {"PD_KERNEL_FRAME", "Kernel window frame for approximation with Nadarya-Watson averaging"},
            //Spline smoothing
            {"PN_SPLINEPARS", "Spline type"},
            {"PD_SPLINEPARS", "Smoothing spline type"},
            {"PN_SPLINE_ERROR", "Spline error"},
            {"PD_SPLINE_ERROR", "Spline error"},

            //Up and down identification
            {"PN_UPDOWN_OUTPUT_TYPE", "Output type"},
            {"PD_UPDOWN_OUTPUT_TYPE", "Type for output data: 'Up-regulated', 'Down-regulated' or 'Up- and down-regulated'"},
            {"PN_UPDOWN_METHOD", "Statistical test"},
            {"PD_UPDOWN_METHOD", "Method for statistical calculation: 'Student', 'Wilcoxon', 'Lehman-Rosenblatt' and 'Kolmogorov-Smirnov'"},

            //FoldChange
            {"PN_FOLD_CHANGE_TYPE", "Data handling"},
            {"PD_FOLD_CHANGE_TYPE", "Type of fold-change: average none, average control, average experiment, average all and one to one"},
            {"PN_INPUT_LOGARITHM_BASE", "Input logarithm base"},
            {"PD_INPUT_LOGARITHM_BASE", "Logarithm base of input data"},
            {"PN_OUTPUT_LOGARITHM_BASE", "Result logarithm base"},
            {"PD_OUTPUT_LOGARITHM_BASE", "Logarithm base of result"},

            //Hypergeometric analysis
            {"PN_HYPER_DETAILED", "Detailed output"},
            {"PD_HYPER_DETAILED", "If checked, results will comprise detailed information, otherwise only scores"},
            {"PN_HYPER_BV", "Boundary value"},
            {"PD_HYPER_BV",
                    "Experiment values will be compared to this parameter. If experiment is to be compared with control than this value should be set to 1."},
            {"PN_HYPER_MATCHING_COLLECTION", "Matching Collection (Optional)"},
            {"PD_HYPER_MATCHING_COLLECTION", "If selected, experiment IDs will be matched to elements of this collection, and result will contain meta-scores for those elements"},
            {"PN_HYPER_NEW_KEY_SOURCE", "New key source"},
            {"PD_HYPER_NEW_KEY_SOURCE", "Field from matchingCollection that will be source of the new key"},
            {"PN_HYPER_AVERAGE_CONTROL", "Average control"},
            {"PD_HYPER_AVERAGE_CONTROL", "If true than we weill compare experiments with mean values of control"},

            //Meta analysis
            {"PN_INPUT_COLLECTION", "Input collection"},
            {"PD_INPUT_COLLECTION", "Input data collection"},
            {"PN_META_INPUT_TABLES", "Tables"},
            {"PD_META_INPUT_TABLES", "Analyses results for meta-analysis"},

            //Regression analysis
            {"PN_REGRESSION_POWER", "Regression power"},
            {"PD_REGRESSION_POWER", "Regression power"},

            //Annotate
            {"PN_ANNOTATION_COLLECTION", "Annotation source"},
            {"PD_ANNOTATION_COLLECTION", "Data collection with annotations"},
            {"PN_ANNOTATION_COLUMNS", "Annotation columns"},
            {"PD_ANNOTATION_COLUMNS", "Names for annotation columns"},
            {"PN_ANNOTATION_SPECIES", "Species"},
            {"PD_ANNOTATION_SPECIES", "Species to be used during matching (if applicable)"},
            {"PN_ANNOTATION_REPLACE_DUPLICATES", "Remove duplicate annotations"},
            {"PD_ANNOTATION_REPLACE_DUPLICATES", "If input table was already annotated by this source, old annotation columns will be removed from the result"},
            
            //Cluster analysis
            {"PN_CLUSTER_COUNT", "Cluster number"},
            {"PD_CLUSTER_COUNT", "Number of clusters in result"},
            {"PN_CLUSTER_METHOD", "Cluster algorithm"},
            {"PD_CLUSTER_METHOD", "Cluster algorithm"},

            //CRCluster analysis
            {"PN_CRC_CHAIN_COUNT", "Cluster process number"},
            {"PD_CRC_CHAIN_COUNT", "Number of independent clustering processes to be launched"},
            {"PN_CRC_CYCLE_COUNT", "Cycles per clustering process"},
            {"PD_CRC_CYCLE_COUNT", "The number of cycles to be executed for each process"},
            {"PN_CRC_CUTOFF", "Probability threshold"},
            {"PD_CRC_CUTOFF", "Threshold probability for a gene to be assigned to current cluster"},
            {"PN_CRC_MAXIMUM_SHIFT", "Maximum allowed shift"},
            {"PD_CRC_MAXIMUM_SHIFT", "Gene expression profile will be considered within the accuracy to that time shift"},
            {"PN_CRC_ALLOW_INVERSION", "Allow inversion"},
            {"PD_CRC_ALLOW_INVERSION", "If true, gene expression profile will be considered within the accuracy to inversion"},

            //Join Table
            {"PN_JOIN_TYPE", "Join type"},
            {"PD_JOIN_TYPE", "Type of join"},
            {"PN_MERGE_COLUMNS", "Merge columns with the same names"},
            {"PD_MERGE_COLUMNS", "Merge columns with the same names"},
            {"PN_JOIN_AGGREGATOR", "Aggregator for numbers"},
            {"PD_JOIN_AGGREGATOR", "Function to be used for numerical columns when several rows are merged into a single one if merge columns option is selected"},
            

            //Venn diagrams
            {"PN_VENN_SIMPLE_PICTURE", "Simple picture"},
            {"PD_VENN_SIMPLE_PICTURE", "All circles has equal radius"},
            {"PN_VENN_OUTPUT", "Output path"},
            {"PD_VENN_OUTPUT", "Where to store output picture"},
            {"PN_LEFT_TABLE", "Left table"},
            {"PD_LEFT_TABLE", "Left table (optional)"},
            {"PN_RIGHT_TABLE", "Right table"},
            {"PD_RIGHT_TABLE", "Right table (optional)"},
            {"PN_CENTER_TABLE", "Center table"},
            {"PD_CENTER_TABLE", "Center table (optional)"},

            //Correlation analsyis
            {"PN_CORR_DATA_SOURCE", "Data source"},
            {"PD_CORR_DATA_SOURCE", "Which data will be used for correlation: rows or columns"},
            {"PN_CORR_RESULT_TYPE", "Result type"},
            {"PD_CORR_RESULT_TYPE", "Show results as correlation matrix or table of triplets (id1, id2, corr)"},
            {"PN_CORR_TYPE", "Correlation type"},
            {"PD_CORR_TYPE", "Correlation formula to be used"},
            {"PN_CORR_CUTOFF", "Correlation cutoff"},
            {"PD_CORR_CUTOFF", "Cutoff for absolute value of correlation (should be between 0 and 1)"},

            // Affymetrix normalization
            {"PN_CELNORM_CELCOLLECTION", "CEL collection"},
            {"PD_CELNORM_CELCOLLECTION", "CEL collection"},
            {"PN_CELNORM_OUTPUTCOLLECTION", "Output collection"},
            {"PD_CELNORM_OUTPUTCOLLECTION", "Output collection"},
            {"PN_CELNORM_OUTPUTNAME", "Output name"},
            {"PD_CELNORM_OUTPUTNAME", "Output name (optional)"},
            {"PN_CELNORM_CELLIST", "CEL files"},
            {"PD_CELNORM_CELLIST", "List of CEL files to be normalized"},
            {"PN_CELNORM_METHOD", "Method"},
            {"PD_CELNORM_METHOD", "Normalization method. Note, in the case of arrays of new design the normalization is always done with RMA method."},
            {"PN_CELNORM_BG_CORRECTION", "Background correction"},
            {"PD_CELNORM_BG_CORRECTION", "Background correction"},
            {"PN_CELNORM_NORM_METHOD", "Normalization method"},
            {"PD_CELNORM_NORM_METHOD", "Normalization method"},
            {"PN_CELNORM_PM_CORRECTION", "PM correction"},
            {"PD_CELNORM_PM_CORRECTION", "PM correction"},
            {"PN_CELNORM_SUMMARIZATION", "Summarization"},
            {"PD_CELNORM_SUMMARIZATION", "Summarization"},
            {"PN_CELNORM_CDF", "CDF version"},
            {"PD_CELNORM_CDF", "Specific CDF version for normalization, use 'none' for default one"},

            // Convert table
            {"PN_CONVERTER_SOURCE", "Input table"},
            {"PD_CONVERTER_SOURCE", "Data set to be converted"},
            {"PN_CONVERTER_SOURCE_TYPE", "Input type"},
            {"PD_CONVERTER_SOURCE_TYPE", "Type of references in input table"},
            {"PN_CONVERTER_TARGET_TYPE", "Output type"},
            {"PD_CONVERTER_TARGET_TYPE",
                    "Type of references in output table. This list is created depending on input type and available converters."},
            {"PN_SPECIES", "Species"}, {"PD_SPECIES", "Species used during conversion"},
            {"PN_CONVERTER_AGGREGATOR", "Aggregator for numbers"},
            {"PD_CONVERTER_AGGREGATOR", "Function to be used for numerical columns when several rows are merged into a single one"},
            {"PN_CONVERTER_OUTPUT", "Output table"}, {"PD_CONVERTER_OUTPUT", "Path to store result to"},
            {"PN_CONVERTER_UNMATCHED", "Unmatched rows"}, {"PD_CONVERTER_UNMATCHED", "Path to store unmatched rows of the table"},
            {"PN_CONVERTER_COLUMN_NAME", "Name of main column"},
            {"PD_CONVERTER_COLUMN_NAME", "If specified then target value will be selected based on aggregator rule"},
            
            // Agilent normalization parameters
            {"PN_AGNORM_LIST", "Agilent files"},
            {"PD_AGNORM_LIST", "List of Agilent files to be normalized"},
            {"PN_DUAL_CHANNEL", "Dual channel"},
            {"PD_DUAL_CHANNEL", "Dual channel Agilent microarray"},
            
            // Illumina normalization parameters
            {"PN_ILLNORM_LIST", "Illumina files"},
            {"PD_ILLNORM_LIST", "List of Illumina files to be normalized"},
            
            // Split table by columns
            {"PN_SLICE_COLUMNS", "Columns"},
            {"PD_SLICE_COLUMNS", "Columns to be copied to the new table"},
            
            //Multiple join
            {"PN_MULTIJOIN_INPUT", "Input tables"},
            {"PD_MULTIJOIN_INPUT", "Input tables"},
    };
}
