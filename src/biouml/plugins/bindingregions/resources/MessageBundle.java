
package biouml.plugins.bindingregions.resources;

import ru.biosoft.util.ConstantResourceBundle;

/**
 * @author yura
 *
 */
public class MessageBundle extends ConstantResourceBundle
{
    public static final String CELL_LINE_COLUMN = "nameOfCellLine";
    public static final String CELL_LINE_SYNONYM_COLUMN = "newNameOfCellLine";
    public static final String TF_NAME_COLUMN = "tfName";
    public static final String GENE_ID_COLUMN = "Gene_ID";

    public static final String CN_BINDING_REGIONS = "Analysis of Binding Regions";
    public static final String CD_BINDING_REGIONS = "Analysis of Binding Regions";
    
    public static final String PN_MODE = "MODE";
    public static final String PD_MODE = "MODE defines the concrete session of given analysis.";
    
    public static final String PN_DB_SELECTOR = "Sequences collection";
    public static final String PD_DB_SELECTOR = "Select a source of nucleotide sequences";
    
    public static final String PN_CHIPSEQ_FOLDER = "Folder containing ChIP-seq tracks";
    public static final String PD_CHIPSEQ_FOLDER = "Folder containing ChIP-seq tracks";
    
    public static final String PN_CELLLINE_SYNONYMS_TABLE = "Cell line synonyms table";
    public static final String PD_CELLLINE_SYNONYMS_TABLE = "Cell line synonyms table (must contain the '"+CELL_LINE_SYNONYM_COLUMN+"' column)";
    
    public static final String PN_CHROMOSOME_GAPS_TABLE = "Chromosome gaps table";
    public static final String PD_CHROMOSOME_GAPS_TABLE = "Table containing chromosome gaps information (output of \"Gathering genome statistics\" analysis)";
    
    public static final String PN_SPECIES = "Species";
    public static final String PD_SPECIES = "Select a taxonomical species";

    public static final String PN_TF_NAMES_TABLE = "TF names table";
    public static final String PD_TF_NAMES_TABLE = "TF names table (TF class as a row name and TF title in '" + TF_NAME_COLUMN + "' column)";

    // 24.03.22
    //public static final String PN_TRACK_PATH = "Input track";
    public static final String PN_TRACK_PATH = "Path to input track";

    public static final String PD_TRACK_PATH_MERGED = "Select input  track (it has to contain the merged binding regions; it can be the result of 'Merge binding regions for cell-lines' analysis)";
    public static final String PD_TRACK_PATH_PEAK_FINDER = "Select input track (it can be ChIP-seq track from GTRD; thus, it can be the result of MACS or SISSRs peak-finder)";
    public static final String PD_TRACK_PATH_CIS_MODULES = "Select input track (it has to contain cis-reulatory modules; in particular, it can be the result of 'Cis-module identification' analysis)";
    public static final String PD_TRACK_PATH_MERGED_OR_NOT = "Select input track (it must contain ChIP-Seq peaks or merged binding regions depending on input parameter 'Is ChIP-Seq peaks'";
    public static final String PD_TRACK_PATH_SITE_PREDICTIONS = "Select input  track (it has to contain the predicted sites)";
    
    // 12.04.22
//    public static final String PN_TRACK_PATH_WITH_RA_SCORES = "Input track";
//    public static final String PD_TRACK_PATH_WITH_RA_SCORES = "Select input track (it must contain RA-scores)";
    public static final String PN_TRACK_PATH_WITH_RA_SCORES_ = "Input track";
    public static final String PD_TRACK_PATH_WITH_RA_SCORES_ = "Select input track (it must contain meta-clusters with RA-scores)";

    public static final String PN_TRACK_PATH_SECOND = "Second input track";
    public static final String PD_TRACK_PATH_SECOND_SITE_PREDICTIONS = "Select second input  track (it has to contain the predicted sites of second transcription factor)";
    
    public static final String PN_PATH_TO_FILTER_TRACK = "Path to filter track";
    public static final String PD_PATH_TO_FILTER_TRACK = "Path to filter track (can be contain histone modifications)";
    
    public static final String PN_MATRIX_PATH = "Matrix";
    public static final String PD_MATRIX_PATH = "Path to frequency matrix";
    
    public static final String PN_TYPE_OF_MATRIX = "Type of matrix";
    public static final String PD_TYPE_OF_MATRIX = "Select type of matrix";
    
    public static final String PN_FILTRATION_MATRIX_PATH = "Filtration matrix";
    public static final String PD_FILTRATION_MATRIX_PATH = "Path to filtration matrix";
    
    public static final String PN_TF_CLASS = "TF class";
    public static final String PD_TF_CLASS = "TF class in Wingender classification (like '2.1.1.1.4')";
    
    public static final String PN_MIXTURE_COMPONENTS_NUMBER = "Number of mixture components";
    public static final String PD_MIXTURE_COMPONENTS_NUMBER = "Define number of mixture components (it must be > 1";
    
    public static final String PN_MAX_ITERATIONS = "Maximal number of iterations";
    public static final String PD_MAX_ITERATIONS = "Maximal number of iterations in algorithm";
    
    public static final String PN_BEST_SITES_PERCENTAGE = "% of best sites";
    public static final String PD_BEST_SITES_PERCENTAGE = "Best sites percentage";
    
    public static final String PN_INITIAL_AS_CONSENSUS = "Initial matrix as consensus";
    public static final String PD_INITIAL_AS_CONSENSUS = "Check this if you want to specify an initial matrix as consensus";
    
    public static final String PN_CONSENSUS = "Consensus";
    public static final String PD_CONSENSUS = "Specify the consensus";
    
    public static final String PN_ARE_BOTH_STRANDS = "Are both strands?";
    public static final String PD_ARE_BOTH_STRANDS = "Do analize both strands of sequences? (if 'no', then only strand (+) will be analyzed)";
    
    public static final String PN_INITIAL_MATRIX = "Matrix path";
    public static final String PD_INITIAL_MATRIX = "Specify the path to the initial matrix";
    
    public static final String PN_MAX_NUMBER_OF_CREATING_MATRICES = "Max number of creating matrices";
    public static final String PD_MAX_NUMBER_OF_CREATING_MATRICES = "How many matrices it is necessary to construct";
    
    public static final String PN_NUMBER_OF_MATRICES = "Number of matrices";
    public static final String PD_NUMBER_OF_MATRICES = "How many matrices will be constructed";
    
    public static final String PN_NUMBER_GROUPS = "Number of groups";
    public static final String PD_NUMBER_GROUPS = "How many groups will be created";
    
    public static final String PN_MIN_OVERLAPS = "Minimal number of overlaps";
    public static final String PD_MIN_OVERLAPS = "Minimal number of overlaps";
    
    public static final String PN_CIS_MODULE_TYPE_2 = "Type-2 cis-modules";
    public static final String PD_CIS_MODULE_TYPE_2 = "Check this if you want to identify type-2 cis-modules";
    
    public static final String PN_OLIG_LENGTH = "Length of oligs";
    public static final String PD_OLIG_LENGTH = "Length of oligonucleotides";
    
    public static final String PN_IS_IPS_MODEL = "Is IPS model?";
    public static final String PD_IS_IPS_MODEL = "Is IPS model? Otherwise multiplicative IPS model will be used";
    
    public static final String PN_AROUND_SUMMIT = "Is around summit";
    public static final String PD_AROUND_SUMMIT = "Is around summit";
    
    public static final String PN_MIN_REGION_LENGTH = "Length of each sequence";
    public static final String PD_MIN_REGION_LENGTH = "Length of each sequence";

    public static final String PN_LENGTH_OF_EACH_SEQUENCE = "Length of each sequence";
    public static final String PD_LENGTH_OF_EACH_SEQUENCE = "Length of each sequence";
    
    public static final String PN_NUMBER_OF_SEQUENCES = "Number of sequences";
    public static final String PD_NUMBER_OF_SEQUENCES = "Number of sequences in sequence sample";

    public static final String PN_WINDOW_SIZE = "Window size";
    public static final String PD_WINDOW_SIZE = "Window size";
    
    public static final String PN_MATRIX_LIBRARY = "Matrix library";
    public static final String PD_MATRIX_LIBRARY = "Library containing matrices";

    public static final String PN_SNP_TRACK = "SNP track";
    public static final String PD_SNP_TRACK = "Track containing SNPs (sites must have 'AltAllele' and 'RefAllele' properties)";
    
    public static final String PN_SNP_REGION_LENGTH = "SNP region length";
    public static final String PD_SNP_REGION_LENGTH = "Length of each reference region";

    public static final String PN_OUTPUT_OPTIONS = "The output options";
    public static final String PD_OUTPUT_OPTIONS = "Select the output options";
    
    public static final String PN_OUTPUT_PATH = "Path to output folder";
    public static final String PD_OUTPUT_PATH = "Output folder will be created under this location when it doesn't exist";

    public static final String PN_OUTPUT_TABLE_PATH = "Output table path";
    public static final String PD_OUTPUT_TABLE_PATH = "Path to the output table";

    public static final String PN_OUTPUT_CHART_TABLE_PATH = "Output table";
    public static final String PD_OUTPUT_CHART_TABLE_PATH = "Table with charts will be created in this location";
    
    public static final String PN_OUTPUT_MATRIX_LIBRARY_PATH = "Output matrix library";
    public static final String PD_OUTPUT_MATRIX_LIBRARY_PATH = "Matrix library to store the resulting matrices (will be created if not exists)";
    
    public static final String PN_OUTPUT_MATRIX_BASE_NAME = "Matrix name prefix";
    public static final String PD_OUTPUT_MATRIX_BASE_NAME = "All matrices will be created with given name prefix";

    public static final String PN_MATRIX_NAME = "Matrix name";
    public static final String PD_MATRIX_NAME = "Define matrix name";

    public static final String PN_SITE_MODEL_TYPE = "Type of site model";
    public static final String PD_SITE_MODEL_TYPE = "Select type of site model that will be used for matrix derivation";
    public static final String PD_SITE_MODEL_TYPE_GENERAL = "Select type of site model";
    
    public static final String PN_SITE_MODEL_TYPES = "Types of site models";
    public static final String PD_SITE_MODEL_TYPES = "Select site models for comparative analysis";
    
    public static final String PN_CHIPSEQ_FOLDER1 = "First folder containing ChIP-seq tracks";
    public static final String PD_CHIPSEQ_FOLDER1 = "First folder containing ChIP-seq tracks (created by the 1-st peak finder)";
    
    public static final String PN_CHIPSEQ_FOLDER2 = "Second folder containing ChIP-seq tracks";
    public static final String PD_CHIPSEQ_FOLDER2 = "Second folder containing ChIP-seq tracks (created by the 2-nd peak finder)";
    
    public static final String PN_PATH_TO_COLLECTION_OF_FOLDERS = "Path to collection of folders";
    public static final String PD_PATH_TO_COLLECTION_OF_FOLDERS = "Path to collection of folders that contain tables with AUCs";
    
    public static final String PN_REVISED = "Are revised";
    public static final String PD_REVISED = "Do consider the revised matrices for analysis?";
    
    public static final String PN_MINIMAL_SIZE = "Minimal size";
    public static final String PD_MINIMAL_SIZE = "AUC will be missed if it was calculated on sequence set with size less than 'Minimal size'";
    public static final String PD_MINIMAL_SIZE_CIS_MODULE = "Cis-regulatory module will be missed if its size is less than this threshold";
    
    public static final String PN_MAX_NUMBER_OF_MISSING_TFCLASSES = "Maximal number of missing TF-classes";
    public static final String PD_MAX_NUMBER_OF_MISSING_TFCLASSES = "Maximal number of TF-classes that can be absent in cis-regulatory modules";
    
    public static final String PN_CIS_MODULE_PATTERN = "Pattern of cis-regulatory module";
    public static final String PD_CIS_MODULE_PATTERN = "Select TF-classes";
    
    public static final String PN_PATH_TO_TABLE_WITH_GENE = "Path to table with genes";
    public static final String PD_PATH_TO_TABLE_WITH_GENE = "Path to table with genes";
    
    public static final String PN_PATH_TO_TABLE_WITH_GENE1 = "Path to table1 with 1-st gene set";
    public static final String PD_PATH_TO_TABLE_WITH_GENE1 = "Path to table with 1-st gene set (table must contain the '" + GENE_ID_COLUMN + "' column)";
    
    public static final String PN_PATH_TO_TABLE_WITH_GENE2 = "Path to table2 with 2-nd gene set";
    public static final String PD_PATH_TO_TABLE_WITH_GENE2 = "Path to table with 2-nd gene set (table must contain the '" + GENE_ID_COLUMN + "' column)";
    
    public static final String PN_PATH_TO_TABLE_WITH_COEFFICIENTS = "Path to table with coefficients"; 
    public static final String PD_PATH_TO_TABLE_WITH_COEFFICIENTS = "Path to table with regression coefficients";
    
    public static final String PN_PATH_TO_FILE = "Path to file"; 
    public static final String PD_PATH_TO_FILE = "Path to file";
    
    public static final String PN_PATH_TO_TEXT_FILE_WITH_CIS_BP_INFORMATION = "Path to text file with CIS-BP information"; 
    public static final String PD_PATH_TO_TEXT_FILE_WITH_CIS_BP_INFORMATION = "Path to text file with CIS-BP information";
    
    public static final String PN_LENGTH_OF_BEST_OLIGONUCLEOTIDES = "Length of best oligonucleotides"; 
    public static final String PD_LENGTH_OF_BEST_OLIGONUCLEOTIDES = "Length of best oligonucleotides";
    
    public static final String PN_NUMBER_OF_BEST_OLIGONUCLEOTIDES = "Number of best oligonucleotides"; 
    public static final String PD_NUMBER_OF_BEST_OLIGONUCLEOTIDES = "Number of best oligonucleotides";
    
    public static final String PN_TYPE_OF_INITIAL_MATRICES = "Type of initial matrices";
    public static final String PD_TYPE_OF_INITIAL_MATRICES = "Select type of initial matrices"; 
    
    public static final String PN_PATH_TO_FOLDER_WITH_CISBP_MATRICES = "Path to folder with CIS-BP Matrices";
    public static final String PD_PATH_TO_FOLDER_WITH_CISBP_MATRICES = "Path to folder with CIS-BP Matrices"; 

    
    public static final String PN_MAX_DISTANCE = "Maximal distance between cis-modules and genes";
    public static final String PD_MAX_DISTANCE = "Maximal distance between cis-regulatory modules and genes";
    
    public static final String PN_SCORE_THRESHOLD = "Score threshold";
    public static final String PD_SCORE_THRESHOLD = "Score threshold";
    
    public static final String PN_CHROMOSOME = "Chromosome";
    public static final String PD_CHROMOSOME = "Chromosome";
    
    public static final String PN_START_POSITION = "Start position";
    public static final String PD_START_POSITION = "Start position of chromosome fragment";
    
    public static final String PN_FINISH_POSITION = "Finish position";
    public static final String PD_FINISH_POSITION = "Finish position of chromosome fragment";
    
    public static final String PN_MATRICES_AND_THRESHOLDS = "Matrices and thresholds";
    public static final String PD_MATRICES_AND_THRESHOLDS = "Define matrices and thresholds";
    
    public static final String PN_SITE_NAME = "Site name";
    public static final String PD_SITE_NAME = "Name of predicted sites";
    
    public static final String PN_ALL_MATRICES = "Are also all matrices";
    public static final String PD_ALL_MATRICES = "Would you like to consider also all matrices simultaneously?";

    public static final String PN_IS_CHIP_SEQ_PEAKS = "Is ChIP-Seq peaks";
    public static final String PD_IS_CHIP_SEQ_PEAKS = "Is ChIP-Seq peaks (or merged binding regions)";
    
    public static final String PN_GENOME_FRAGMENT_TYPE = "Genome fragment type";
    public static final String PD_GENOME_FRAGMENT_TYPE = "Select type of genome fragments in which sites will be predicted";
    
    public static final String PN_PATH_TO_OUTPUT_TRACK = "Path to output track";
    public static final String PD_PATH_TO_OUTPUT_TRACK = "Path to output track";
    
    public static final String PN_TRACKS_FOLDER = "Folder containing tracks";
    public static final String PD_TRACKS_FOLDER = "Folder containing tracks with filters (such as histone modifications)";
    
    public static final String PN_TRACK_NAMES = "Tracks for filtration";
    public static final String PD_TRACK_NAMES = "Select tracks for filtration (they can contain histone modifications";
    
    public static final String PN_ARE_TRACKS_SHUFFLED = "Are tracks shuffled";
    public static final String PD_ARE_TRACKS_SHUFFLED = "Do shuffle the filter tracks?";
    
    public static final String PN_IS_FILTER_TRACK_SHUFFLED = "Is filfter track shuffled";
    public static final String PD_IS_FILTER_TRACK_SHUFFLED = "Do shuffle the filter track?";
    
    public static final String PN_MAX_LENGTH_OF_FILTERS = "Maximal length of filters";
    public static final String PD_MAX_LENGTH_OF_FILTERS = "Maximal length of filters";
    
    public static final String PN_MAX_DISTANCE_TO_EDGE = "Maximal distance to edge";
    public static final String PD_MAX_DISTANCE_TO_EDGE = "Maximal distance to edge";
    
    public static final String PN_FILES_FOLDER = "Folder containing files";
    public static final String PD_FILES_FOLDER = "Folder containing files";
    
    public static final String PN_MRNA_AND_RIBO_SEQ_FEATURE_NAMES = "mRNA and Ribo-Seq features";
    public static final String PD_MRNA_AND_RIBO_SEQ_FEATURE_NAMES = "Select mRNA and Ribo-Seq features";
    
    public static final String PN_REGRESSION_TYPE = "Regression type";
    public static final String PD_REGRESSION_TYPE = "Select regression type";
    
    public static final String PN_CLASSIFICATION_TYPE = "Classification type";
    public static final String PD_CLASSIFICATION_TYPE = "Select classification type";

    public static final String PN_CLUSTERIZATION_TYPE = "Clusterization type";
    public static final String PD_CLUSTERIZATION_TYPE = "Select clusterization type";
    
    public static final String PN_REGRESSION_MODE = "Regression mode";
    public static final String PD_REGRESSION_MODE = "Select regression mode";
    
    public static final String PN_CLASSIFICATION_MODE = "Classification mode";
    public static final String PD_CLASSIFICATION_MODE = "Select classification mode";
    
    public static final String PN_NUMBER_OF_CLUSTERS = "Number of clusters";
    public static final String PD_NUMBER_OF_CLUSTERS = "Number of clusters";
    
    public static final String PN_NUMBER_OF_PRINCIPAL_COMPONENTS = "Number of principal components";
    public static final String PD_NUMBER_OF_PRINCIPAL_COMPONENTS = "Number of principal components";
    
    public static final String PN_PRINCIPAL_COMPONENT_SORTING_TYPE = "Principal component sorting type";
    public static final String PD_PRINCIPAL_COMPONENT_SORTING_TYPE = "Select type of principal component sorting";

    public static final String PN_CLUSTERING_QUALITY_INDICES_NAMES = "Clustering quality indices names";
    public static final String PD_CLUSTERING_QUALITY_INDICES_NAMES = "Select clustering quality indices";
    
    public static final String PN_TRANSFORMATION_TYPE = "Type of data transformation";
    public static final String PD_TRANSFORMATION_TYPE = "Type of data transformation";

    public static final String PN_DISTANCE_TYPE = "Distance type";
    public static final String PD_DISTANCE_TYPE = "Select distance type";
    
    public static final String PN_ADD_NUMBERS_OF_OVERLAPS = "Do add numbers of overlaps";
    public static final String PD_ADD_NUMBERS_OF_OVERLAPS = "Do add numbers of overlaps to the set of peak characteristics?";

    public static final String PN_DATA_SET = "Data set name";
    public static final String PD_DATA_SET = "Select data set";
    
    public static final String PN_PATH_TO_FOLDER_WITH_DATA_SETS = "Path to folder with special data sets";
    public static final String PD_PATH_TO_FOLDER_WITH_DATA_SETS = "Path to folder with special data sets";
    
    public static final String PN_PATH_TO_FOLDER_WITH_FILES = "Path to folder with files";
    public static final String PD_PATH_TO_FOLDER_WITH_FILES = "Path to folder with input files";

    // 21.04.22
    public static final String PN_PATH_TO_INPUT_FOLDER = "Path to input folder";
    public static final String PD_PATH_TO_INPUT_FOLDER = "Path to input folder that contais tracks with meta-clusters";
    
    public static final String PN_PATH_TO_TABLE_WITH_SAMPLE1 = "Path to table with 1-st sample";
    public static final String PD_PATH_TO_TABLE_WITH_SAMPLE1 = "Path to table with 1-st sample";
    
    public static final String PN_PATH_TO_TABLE_WITH_SAMPLE2 = "Path to table with 2-nd sample";
    public static final String PD_PATH_TO_TABLE_WITH_SAMPLE2 = "Path to table with 2-nd sample";

    public static final String PN_START_CODON_TYPE = "Start codon type";
    public static final String PD_START_CODON_TYPE = "Start codon type";
    
    public static final String PN_START_CODON_ORDER = "Order of start codon";
    public static final String PD_START_CODON_ORDER = "Order of start codon (>=1)";

    public static final String PN_LEFT_BOUNDARY = "Left boundary of each mRNA fragment";
    public static final String PD_LEFT_BOUNDARY = "Left boundary of each mRNA fragment with respect to start (or stop) codon";
    
    public static final String PN_RIGHT_BOUNDARY = "Right boundary of each mRNA fragment";
    public static final String PD_RIGHT_BOUNDARY = "Right boundary of each mRNA fragment with respect to start (or stop) codon";
    
    public static final String PN_GENE_TYPE = "Gene type";
    public static final String PD_GENE_TYPE = "Select gene type";
    
    public static final String PN_GENE_NAME = "Gene name";
    public static final String PD_GENE_NAME = "Select gene name";
    
    public static final String PN_FLANKS_LENGTH = "Length of each flank of gene";
    public static final String PD_FLANKS_LENGTH = "Length of each flank of gene";
    
    public static final String PN_IS_LONG_OLIG = "Is long oligonucleotide (N)8-(N)12-NGG";
    public static final String PD_IS_LONG_OLIG = "Otherwise short olig (N)12-NGG";
    
    public static final String PN_P_VALUE_THRESHOLD = "P-value threshold";
    public static final String PD_P_VALUE_THRESHOLD = "P-value threshold";
    
    public static final String PN_THRESHOLD_FOR_NUMBER_OF_SITES = "Threshold for number of sites";
    public static final String PD_THRESHOLD_FOR_NUMBER_OF_SITES = "Threshold for number of sites";
    
    public static final String PN_DO_ADD_INTERCEPT_TO_REGRESSION = "Do add intercept to regression";
    public static final String PD_DO_ADD_INTERCEPT_TO_REGRESSION = "Do consider the intercept as additional regression variable?";
    
    public static final String PN_PATH_TO_TABLE_WITH_DATA_MATRIX = "Path to table with data matrix";
    public static final String PD_PATH_TO_TABLE_WITH_DATA_MATRIX = "Path to table with data matrix";
    
    public static final String PN_PATH_TO_DATA_MATRIX = "Path to data matrix";
    public static final String PD_PATH_TO_DATA_MATRIX = "Path to table or file with data matrix";


    public static final String PN_PATH_TO_TABLE_WITH_DATA = "Path to table with input data";
    public static final String PD_PATH_TO_TABLE_WITH_DATA = "Path to table with input data";

    public static final String PN_PATH_TO_FOLDER_WITH_SAVED_MODEL = "Path to folder with saved model";
    public static final String PD_PATH_TO_FOLDER_WITH_SAVED_MODEL = "Path to folder with saved model";

    public static final String PN_PERCENTAGE_OF_DATA_FOR_TRAINING = "Percentage of data for training";
    public static final String PD_PERCENTAGE_OF_DATA_FOR_TRAINING = "Percentage of input data set (this part of input data set will be used as training set)";
    
    public static final String PN_VARIABLE_NAMES = "Variable names";
    public static final String PD_VARIABLE_NAMES = "Select variable names";
    
    public static final String PN_RESPONSE_NAME = "Response name";
    public static final String PD_RESPONSE_NAME = "Select response name";
    
    public static final String PN_DATA_MATRIX_EXTENSIONS = "Data matrix extensions";
    public static final String PD_DATA_MATRIX_EXTENSIONS = "Select data matrix extensions";
    
    public static final String PN_CLASSIFIER_NAME = "Classifier name";
    public static final String PD_CLASSIFIER_NAME = "Select classifier name: classifier consists of names of classes (samples, groups, clusters)";
    
    public static final String PN_PATH_TO_TABLE_SEQUENCE_SAMPLE = "Path to table with sequence sample";
    public static final String PD_PATH_TO_TABLE_SEQUENCE_SAMPLE = "Select path to table with sequence sample";
    
    public static final String PN_NAME_OF_TABLE_COLUMN_WITH_SEQUENCE_SAMPLE = "Name of table column with sequence sample";
    public static final String PD_NAME_OF_TABLE_COLUMN_WITH_SEQUENCE_SAMPLE = "Select name of table column with sequence sample";

    public static final String PN_NAME_OF_TABLE_COLUMN_SAMPLE_NAMES = "Name of table column with sample names";
    public static final String PD_NAME_OF_TABLE_COLUMN_SAMPLE_NAMES = "Name of table column with sample names";
    
    public static final String PN_RESPONSE_NAMES = "Response names";
    public static final String PD_RESPONSE_NAMES = "Select response names";

    public static final String PN_DO_ADD_RESPONSE_TO_CLUSTERIZATION = "Do add response to clusterization?";
    public static final String PD_DO_ADD_RESPONSE_TO_CLUSTERIZATION = "Do add response to clusterization as additional variable?";
    
    public static final String PN_DO_WRITE_MRNA_FEATURES_AND_RIBOSEQ_FEATURE_INTO_TABLE = "Do write mRNA features and Ribo-Seq feature into table?";
    public static final String PD_DO_WRITE_MRNA_FEATURES_AND_RIBOSEQ_FEATURE_INTO_TABLE = "Do write mRNA features and Ribo-Seq feature into table (format : 'TableDataCollection')?";
    
    public static final String PN_ARE_NEAR_START_CODONS = "Are mRNA fragments near start codons?";
    public static final String PD_ARE_NEAR_START_CODONS = "Are mRNA fragments near start codons (or near stop codons)?";
    
    public static final String PN_DO_EXCLUDE_MISSING_DATA = "Do exclude missing data?";
    public static final String PD_DO_EXCLUDE_MISSING_DATA = "Do exclude missing data? (If 'no' then possible missing data will be included as NaN-values";
    
    public static final String PN_SMOOTHING_WINDOW_WIDTH_TYPE = "Smoothing window width type";
    public static final String PD_SMOOTHING_WINDOW_WIDTH_TYPE = "Select the smoothing window width type";

    public static final String PN_GIVEN_SMOOTHING_WINDOW_WIDTH = "Given smoothing window width";
    public static final String PD_GIVEN_SMOOTHING_WINDOW_WIDTH = "Define smoothing window width";

    public static final String PN_INITIAL_MATRIX_APPROXIMATION_TYPE = "Initial matrix approximation type";
    public static final String PD_INITIAL_MATRIX_APPROXIMATION_TYPE = "Select type of initial matrix approximation";

    public static final String PN_ARE_COVARIANCE_MATRICES_EQUAL = "Are covariance matrices equal?";
    public static final String PD_ARE_COVARIANCE_MATRICES_EQUAL = "Are covariance matrices (within classes/groups/clusters) equal?";
    
    public static final String PN_PATH_TO_TABLE_WITH_MRNA_SEQ = "Path to table with mRNA-Seq data";
    public static final String PD_PATH_TO_TABLE_WITH_MRNA_SEQ = "Path to table with mRNA-Seq data";

    public static final String PN_PATH_TO_TABLE_WITH_SAMPLE = "Path to table with sample";
    public static final String PD_PATH_TO_TABLE_WITH_SAMPLE = "Path to table with sample";

    public static final String PN_COLUMN_NAME_WITH_SAMPLE = "Column name with sample";
    public static final String PD_COLUMN_NAME_WITH_SAMPLE = "Select column name with sample";

    public static final String PN_PATH_TO_TABLE_WITH_RIBO_SEQ = "Path to table with Ribo-Seq data";
    public static final String PD_PATH_TO_TABLE_WITH_RIBO_SEQ = "Path to table with Ribo-Seq data";
    
    public static final String PN_COLUMN_NAME_WITH_MRNA_SEQ_READS_NUMBER = "Column name with mRNA-Seq reads number";
    public static final String PD_COLUMN_NAME_WITH_MRNA_SEQ_READS_NUMBER = "Select column name with mRNA-Seq reads number";

    public static final String PN_COLUMN_NAME_WITH_RIBO_SEQ_READS_NUMBER = "Column name with Ribo-Seq reads number";
    public static final String PD_COLUMN_NAME_WITH_RIBO_SEQ_READS_NUMBER = "Select column name with Ribo-Seq reads number";
    
    public static final String PN_COLUMN_NAME_WITH_START_CODON_POSITIONS = "Column name with start codon positions";
    public static final String PD_COLUMN_NAME_WITH_START_CODON_POSITIONS = "Select column name with start codon positions";
    
    public static final String PN_COLUMN_NAME_WITH_TRANSCRIPT_NAMES = "Column name with transcript names";
    public static final String PD_COLUMN_NAME_WITH_TRANSCRIPT_NAMES = "Column name with transcript names";
    
    public static final String PN_MRNA_SEQ_READS_THRESHOLD = "mRNA-Seq reads threshold";
    public static final String PD_MRNA_SEQ_READS_THRESHOLD = "Transcripts for which mRNA-Seq reads number are grater than this threshold will be considered only";
    
    public static final String PN_RIBO_SEQ_READS_THRESHOLD = "Ribo-Seq reads threshold";
    public static final String PD_RIBO_SEQ_READS_THRESHOLD = "Transcripts for which Ribo-Seq reads number are grater than this threshold will be considered only";
    
    public static final String PN_NAME_OF_ALGORITHM = "Name of algorithm";
    public static final String PD_NAME_OF_ALGORITHM = "Select name of algorithm";
    
    public static final String PN_INITIAL_APPROXIMATIONS_FOR_MATRICES = "Initial approximations for matices";
    public static final String PD_INITIAL_APPROXIMATIONS_FOR_MATRICES = "Define initial approximations for matices";
    
    public static final String PN_INITIAL_APPROXIMATION_FOR_MATRIX = "Initial approximation for each matix";
    public static final String PD_INITIAL_APPROXIMATION_FOR_MATRIX = "Define initial approximation for each matix";

    public static final String PN_SEQUENCE_SAMPLE_TYPE = "Sequence sample type";
    public static final String PD_SEQUENCE_SAMPLE_TYPE = "Select sequence sample type";
    
    public static final String PN_TABLE_CONTENT = "Table content";
    public static final String PD_TABLE_CONTENT = "Select table content";
    
    public static final String PN_SPECIFIED_LENGTH_OF_SEQUENCE = "Specified length of sequence";
    public static final String PD_SPECIFIED_LENGTH_OF_SEQUENCE = "Specify sequence length";
}
