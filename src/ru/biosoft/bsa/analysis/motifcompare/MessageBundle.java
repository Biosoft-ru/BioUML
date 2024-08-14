package ru.biosoft.bsa.analysis.motifcompare;

import ru.biosoft.util.ConstantResourceBundle;

public class MessageBundle extends ConstantResourceBundle
{
    public static final String CN_MOTIF_COMPARE = "Compare site models";
    public static final String CD_MOTIF_COMPARE = "Compare site models with ROC curve";
    
    public static final String PN_SITE_MODELS = "Site models";
    public static final String PD_SITE_MODELS = "Site models";
    
    public static final String PN_SEQUENCES = "Sequences";
    public static final String PD_SEQUENCES = "Sequences containing motifs";
    
    public static final String PN_BACKGROUND_SEQUENCES = "Background sequences";
    public static final String PD_BACKGROUND_SEQUENCE = "Background sequences";
    
    public static final String PN_NUMBER_OF_PERMUTATIONS = "Number of permutations";
    public static final String PD_NUMBER_OF_PERMUTATIONS = "Number of permutations of the sequences to perform when computing false positive rates";
    
    public static final String PN_SEED = "Seed";
    public static final String PD_SEED = "Seed for random number generator";
    
    public static final String PN_OUTPUT = "Output table";
    public static final String PD_OUTPUT = "Output table (will be created if not exists)";
    
    public static final String PN_MODEL_FDR = "Model FDR";
    public static final String PD_MODEL_FDR = "Set models threshold to match specified FDR";

}
