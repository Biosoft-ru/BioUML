package ru.biosoft.bsa.analysis.motifquality;

import ru.biosoft.util.ConstantResourceBundle;

public class MessageBundle extends ConstantResourceBundle
{
    public static final String CN_MOTIF_QUALITY_ANALYSIS = "Motif quality analysis";
    public static final String CD_MOTIF_QUALITY_ANALYSIS = "Motif quality analysis";
    
    public static final String PN_SEQUENCES = "Sequences";
    public static final String PD_SEQUENCES = "Track or sequence with motifs";
    
    public static final String PN_SITE_MODEL = "Site model";
    public static final String PD_SITE_MODEL = "Site model";
    
    public static final String PN_SEED = "Seed";
    public static final String PD_SEED = "Seed for random number generator";
    
    public static final String PN_OUTPUT = "Output path";
    public static final String PD_OUTPUT = "Path to store results";
    
    public static final String PN_NUMBER_OF_POINTS = "Number of points";
    public static final String PD_NUMBER_OF_POINTS = "Number of points where sensitivity and FDR will be computed";
    
    public static final String PN_SHUFFLES_COUNT = "Shuffle count";
    public static final String PD_SHUFFLES_COUNT = "How many times seqeunce letters will be shuffled for FDR estimation";
    
}
