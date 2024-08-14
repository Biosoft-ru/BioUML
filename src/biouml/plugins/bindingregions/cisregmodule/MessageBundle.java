
package biouml.plugins.bindingregions.cisregmodule;

import ru.biosoft.util.ConstantResourceBundle;

/**
 * @author yura
 */
public class MessageBundle extends ConstantResourceBundle
{
    public static final String CN_CIS_REG_MODULE = "Analysis of Cis-Regulatory Modules";
    public static final String CD_CIS_REG_MODULE = "Analysis of Cis-Regulatory Modules based on ChIP-Seq regions";
    
    public static final String PN_MODE = "MODE";
    public static final String PD_MODE = "MODE defines the concrete session of given analysis. The detailed description of MODE see in panel 'Application Log'";

    public static final String PN_CHIP_SEQ_TRACK_PATH = "ChIP-Seq Regions path";
    public static final String PD_CHIP_SEQ_TRACK_PATH = "ChIP-Seq Regions path";
    
    public static final String PN_SEQUENCES_PATH = "Sequences path";
    public static final String PD_SEQUENCES_PATH = "It is nessecary for determination of Genome Build";
    
    public static final String PN_SPECIE = "Specie";
    public static final String PD_SPECIE = "Specie";
    
    public static final String PN_MINIMAL_NUMBER_OF_OVERLAPS = "Minimal number of overlaps";
    public static final String PD_MINIMAL_NUMBER_OF_OVERLAPS = "Minimally admissible number of overlapped ChIP-Seq regions";
    
    public static final String PN_CIS_REG_MODULE_TABLE = "Table path";
    public static final String PD_CIS_REG_MODULE_TABLE = "Path where to save resulting table (includes proper table name";
}