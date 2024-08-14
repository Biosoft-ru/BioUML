package ru.biosoft.bsa;

import ru.biosoft.access.core.DataElementPath;

public class Const
{
    public final static String COLORSCHEMES_NAME      = "colorschemes";
    
    public final static DataElementPath BSA_GENOME_BROWSER_PATH = DataElementPath.create("databases/Utils/Genome browser");
    public final static DataElementPath FULL_COLORSCHEMES      = BSA_GENOME_BROWSER_PATH.getChildPath(COLORSCHEMES_NAME);

    public final static String DECIMAL_FORMAT_PATTERN = "#.####";

    public final static String TRANSFAC_FACTORS_RELATIVE = "../factor";
    public final static String TRANSFAC_MATRICES_RELATIVE = "../matrix";
    public final static String TRANSFAC_CLASSIFICATIONS_RELATIVE = "../classifications";
    
    public final static String TRANSFAC_FACTORS = "factor";
    public static final String CLASSIFICATION_PATH_PROPERTY = "classifications";
    public static final String DEFAULT_PROFILE = "databases/HOCOMOCO v10/Data/PWM_HUMAN_mono_pval=0.0001";
    public static final String LAST_PROFILE_PREFERENCE = "LastProfileUsed";

    public static final String TAXON_CLASSIFICATION = "taxon";
    public static final String TRANSCRIPTION_FACTOR_CLASSIFICATION = "transcription_factor";
    public static final String DNA_BINDING_DOMAIN_CLASSIFICATION = "DNA_binding_domain";

    final static public String NUMBER_PROPERTY      = "number";

    final static public String LEVEL_PROPERTY       = "level";

    final static public String CLASSNAME_PROPERTY   = "class-name";

    final static public String DESCRIPTION_PROPERTY = "description";
}
