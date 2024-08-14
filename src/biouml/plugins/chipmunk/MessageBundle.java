package biouml.plugins.chipmunk;

import ru.biosoft.util.ConstantResourceBundle;

/**
 * @author lan
 */
public class MessageBundle extends ConstantResourceBundle
{
    public static final String CN_CHIPMUNK_ANALYSIS = "ChIPMunk analysis";
    public static final String CD_CHIPMUNK_ANALYSIS = "Motif search";
    public static final String CN_CHIPHORDE_ANALYSIS = "ChIPHorde analysis";
    public static final String CD_CHIPHORDE_ANALYSIS = "Multiple motif search";
    public static final String PN_INPUT_SEQUENCES = "Input sequences";
    public static final String PD_INPUT_SEQUENCES = "Collection containing input reads.";
    public static final String PN_THREAD_COUNT = "Number of threads";
    public static final String PD_THREAD_COUNT = "Number of concurrent threads when processing";
    public static final String PN_STEP_LIMIT = "Step limit";
    public static final String PD_STEP_LIMIT = "Step limit";
    public static final String PN_TRY_LIMIT = "Try limit";
    public static final String PD_TRY_LIMIT = "Try limit";
    public static final String PN_GC_PERCENT = "GC percent";
    public static final String PD_GC_PERCENT = "Fraction of GC nucleotides (0..1)";
    public static final String PN_START_LENGTH = "Start length";
    public static final String PD_START_LENGTH = "Start length of the matrix";
    public static final String PN_STOP_LENGTH = "Stop length";
    public static final String PD_STOP_LENGTH = "Stop length of the matrix (less or equal than start length)";
    public static final String PN_ZOOPS_FACTOR = "ZOOPS factor";
    public static final String PD_ZOOPS_FACTOR = "Zero-or-one-occurence-per-sequence factor";
    public static final String PN_OUTPUT_LIBRARY = "Output matrix library";
    public static final String PD_OUTPUT_LIBRARY = "Path to the matrix library to put matrix into (will be created if not specified)";
    public static final String PN_MATRIX_NAME = "Matrix name";
    public static final String PD_MATRIX_NAME = "Name of created matrix";
    public static final String PN_MATRIX_NAME_PREFIX = "Matrix name prefix";
    public static final String PD_MATRIX_NAME_PREFIX = "Prefix for the matrix name. It will be followed by number.";
    public static final String PN_N_MOTIFS = "Motifs count limit";
    public static final String PD_N_MOTIFS = "Maximum number of motifs to discover";
    public static final String PN_HORDE_MODE = "Filtering mode";
    public static final String PD_HORDE_MODE = "Whether to mask polyN (\"Mask\") or to drop entire sequence (\"Filter\")";
    public static final String PN_USE_PROFILES = "Use peak profiles";
    public static final String PD_USE_PROFILES = "Whether to use peak profiles (if available)";
}
