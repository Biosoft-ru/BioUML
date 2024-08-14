package ru.biosoft.bsa.analysis.chipseqprofile;

import ru.biosoft.util.ConstantResourceBundle;

public class MessageBundle extends ConstantResourceBundle
{
    public static final String CN_CHIPSEQ_PROFILE_ANALYSIS = "ChIP-seq profile analysis";
    public static final String CD_CHIPSEQ_PROFILE_ANALYSIS = "Computes probability profiles for ChIP-seq peaks";

    public static final String PN_PEAK_TRACK = "Peak track";
    public static final String PD_PEAK_TRACK = "Track with ChIP-seq peaks";

    public static final String PN_TAG_TRACK = "Tag track";
    public static final String PD_TAG_TRACK = "Track with aligned tags (reads)";

    public static final String PN_FRAGMENT_SIZE = "Fragment size";
    public static final String PD_FRAGMENT_SIZE = "Mean size of DNA fragments used in immunoprecipitation";

    public static final String PN_SIGMA = "Sigma";
    public static final String PD_SIGMA = "Sigma parameter used to model peak shape";

    public static final String PN_ERROR_RATE = "Error rate";
    public static final String PD_ERROR_RATE = "Error rate";

    public static final String PN_PROFILE = "Profile";
    public static final String PD_PROFILE = "Profile";

    public static final String PN_PROFILE_TRACK = "Profiled track";
    public static final String PD_PROFILE_TRACK = "Profiled track";
}
