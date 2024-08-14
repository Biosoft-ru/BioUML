package biouml.standard.type;

/**
 * Defines protein properties constants.
 */
public interface ProteinProperties
{
    // state properties
    public static final String STATE_ACTIVE = "active";
    public static final String STATE_INACTIVE = "inactive";
    public static final String STATE_UNKNOWN = "unknown";

    // modefication properties
    public static final String MODIFICATION_NONE = "none";
    public static final String MODIFICATION_PHOSPHORYLATED = "phosphorylated";
    public static final String MODIFICATION_FATTY_ACYLATION = "fatty_acylation";
    public static final String MODIFICATION_PRENYLATION = "prenylation";
    public static final String MODIFICATION_CHOLESTEROLATION = "cholesterolation";
    public static final String MODIFICATION_UBIQUITINATION = "ubiquitination";
    public static final String MODIFICATION_SUMOLATION = "sumolation";
    public static final String MODIFICATION_GLYCATION = "glycation";
    public static final String MODIFICATION_GPI_ANCHOR = "gpi_anchor";

    public static final String MODIFICATION_UNKNOWN = "unknown";

    // composition properties
    public static final String MONOMER = "monomer";
    public static final String HOMODIMER = "homodimer";
    public static final String HETERODIMER = "heterodimer";
    public static final String MULTIMER = "multimer";
    public static final String COMPLEX = "complex";
    public static final String UNKNOWN = "unknown";

}
