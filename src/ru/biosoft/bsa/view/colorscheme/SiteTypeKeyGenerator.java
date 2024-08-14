package ru.biosoft.bsa.view.colorscheme;

import java.awt.Color;
import java.util.HashMap;

import ru.biosoft.bsa.Site;

/**
 * SiteTypeKeyGenerator - keys are generating in dependency on site type.
 *
 * For "pretty" legend view site types are grouped in some hierarchy.
 */
public class SiteTypeKeyGenerator extends SiteToKeyGenerator
{
    private static HashMap<String, String> typeMap;
    private static void initTypeMap()
    {
        typeMap = new HashMap<>();
        for(int i=0; i<typeMapping.length; i++)
            typeMap.put(typeMapping[i].type, typeMapping[i].key);
    }

    /** Always return <code>true</code>. */
    @Override
    public boolean isSuitable(Site site)
    {
        return true;
    }

    /**
     * Get key for site
     *
     * @param site a site
     * @return key
     */
    @Override
    public String getKey(Site site)
    {
        if(typeMap == null)
            initTypeMap();

        String key = typeMap.get(site.getType());
        if(key == null)
            key = site.getType();

        return key;
    }

    ////////////////////////////////////////
    //
    //

    /** This class associates hierarchy node and color with site type. */
    public static class SiteTypeColor
    {
        /** Site type. @see SiteType */
        public String type;

        /** Hierarchy string. */
        public String key;

        public Color color;

        public SiteTypeColor(Color color, String type, String key)
        {
            this.type   = type;
            this.key    = key;
            this.color  = color;
        }
    }

    /**
     *
     * @pending we can change FT abbrevations to normal expressions,
     * e.g. misc_feature -> miscellanous
     */
    public static final SiteTypeColor[] typeMapping =
    {
        new SiteTypeColor(new Color(255, 128, 128), "misc_feature",             "misc_feature"),

        new SiteTypeColor(new Color(255, 128, 128), "misc_difference",          "misc_difference"),
        new SiteTypeColor(new Color(255, 128, 128), "conflict",                 "misc_difference;conflict"),
        new SiteTypeColor(new Color(255, 128, 128), "unsure",                   "misc_difference;unsure"),
        new SiteTypeColor(new Color(255, 128, 128), "old_sequence",             "misc_difference;old_sequence"),
        new SiteTypeColor(new Color(255, 128, 128), "variation",                "misc_difference;variation"),
        new SiteTypeColor(new Color(255, 128, 128), "modified_base",            "misc_difference;modified_base"),

        new SiteTypeColor(new Color(255, 128, 128), "gene",                     "gene"),
        new SiteTypeColor(new Color(255, 128, 128), "misc_signal",              "gene;misc_signal"),
        new SiteTypeColor(new Color(255, 128, 128), "promoter",                 "gene;promoter"),
        new SiteTypeColor(new Color(255, 128, 128), "CAAT_signal",              "gene;promoter;CAAT_signal"),
        new SiteTypeColor(new Color(255, 128, 128), "TATA_signal",              "gene;promoter;TATA_signal"),
        new SiteTypeColor(new Color(255, 128, 128), "-35_signal",               "gene;promoter;-35_signal"),
        new SiteTypeColor(new Color(255, 128, 128), "-10_signal",               "gene;promoter;-10_signal"),
        new SiteTypeColor(new Color(255, 128, 128), "GC_signal",                "gene;promoter;GC_signal"),
        new SiteTypeColor(new Color(255, 128, 128), "TF binding site",          "gene;promoter;TF binding site"),

        new SiteTypeColor(new Color(255, 128, 128), "RBS",                      "gene;RBS"),
        new SiteTypeColor(new Color(255, 128, 128), "polyA_signal",             "gene;polyA_signal"),
        new SiteTypeColor(new Color(255, 128, 128), "enhancer",                 "gene;enhancer"),
        new SiteTypeColor(new Color(255, 128, 128), "attenuator",               "gene;attenuator"),
        new SiteTypeColor(new Color(255, 128, 128), "terminator",               "gene;terminator"),
        new SiteTypeColor(new Color(255, 128, 128), "rep_origin",               "gene;rep_origin"),

        new SiteTypeColor(new Color(255, 128, 128), "RNA",                      "RNA"),
        new SiteTypeColor(new Color(255, 128, 128), "misc_RNA",                 "RNA;misc_RNA"),
        new SiteTypeColor(new Color(255, 128, 128), "prim_transcript",          "RNA;prim_transcript"),
        new SiteTypeColor(new Color(255, 128, 128), "precursor_RNA",            "RNA;prim_transcript;precursor_RNA"),
        new SiteTypeColor(new Color(255, 128, 128), "mRNA",                     "RNA;prim_transcript;mRNA"),
        new SiteTypeColor(new Color(255, 128, 128), "5'clip",                   "RNA;prim_transcript;5'clip"),
        new SiteTypeColor(new Color(255, 128, 128), "3'clip",                   "RNA;prim_transcript;3'clip"),
        new SiteTypeColor(new Color(255, 128, 128), "5'UTR",                    "RNA;prim_transcript;5'UTR"),
        new SiteTypeColor(new Color(255, 128, 128), "3'UTR",                    "RNA;prim_transcript;3'UTR"),
        new SiteTypeColor(new Color(255, 128, 128), "exon",                     "RNA;prim_transcript;exon"),
        new SiteTypeColor(new Color(255, 128, 128), "CDS",                      "RNA;prim_transcript;CDS"),
        new SiteTypeColor(new Color(255, 128, 128), "sig_peptide",              "RNA;prim_transcript;CDS;sig_peptide"),
        new SiteTypeColor(new Color(255, 128, 128), "transit_peptide",          "RNA;prim_transcript;CDS;transit_peptide"),
        new SiteTypeColor(new Color(255, 128, 128), "mat_peptide",              "RNA;prim_transcript;CDS;mat_peptide"),
        new SiteTypeColor(new Color(255, 128, 128), "intron",                   "RNA;prim_transcript;intron"),
        new SiteTypeColor(new Color(255, 128, 128), "polyA_site",               "RNA;prim_transcript;polyA_site"),
        new SiteTypeColor(new Color(255, 128, 128), "rRNA",                     "RNA;prim_transcript;rRNA"),
        new SiteTypeColor(new Color(255, 128, 128), "tRNA",                     "RNA;prim_transcript;tRNA"),
        new SiteTypeColor(new Color(255, 128, 128), "scRNA",                    "RNA;prim_transcript;scRNA"),
        new SiteTypeColor(new Color(255, 128, 128), "snRNA",                    "RNA;prim_transcript;snRNA"),

        new SiteTypeColor(new Color(255, 128, 128), "immunoglobulin_related",   "immunoglobulin_related"),
        new SiteTypeColor(new Color(255, 128, 128), "C_region",                 "immunoglobulin_related;C_region"),
        new SiteTypeColor(new Color(255, 128, 128), "D_segment",                "immunoglobulin_related;D_segment"),
        new SiteTypeColor(new Color(255, 128, 128), "J_segment",                "immunoglobulin_related;J_segment"),
        new SiteTypeColor(new Color(255, 128, 128), "N_region",                 "immunoglobulin_related;N_region"),
        new SiteTypeColor(new Color(255, 128, 128), "S_region",                 "immunoglobulin_related;S_region"),
        new SiteTypeColor(new Color(255, 128, 128), "V_region",                 "immunoglobulin_related;V_region"),
        new SiteTypeColor(new Color(255, 128, 128), "V_segment",                "immunoglobulin_related;V_segment"),

        new SiteTypeColor(new Color(255, 128, 128), "repeat_region",            "repeat_region"),
        new SiteTypeColor(new Color(255, 128, 128), "repeat_unit",              "repeat_region;repeat_unit"),
        new SiteTypeColor(new Color(255, 128, 128), "LTR",                      "repeat_region;LTR"),
        new SiteTypeColor(new Color(255, 128, 128), "satellite",                "repeat_region;satellite"),

        new SiteTypeColor(new Color(255, 128, 128), "misc_binding",             "misc_binding"),
        new SiteTypeColor(new Color(255, 128, 128), "primer_bind",              "misc_binding;primer_bind"),
        new SiteTypeColor(new Color(255, 128, 128), "protein_bind",             "misc_binding;protein_bind"),
        new SiteTypeColor(new Color(255, 128, 128), "STS",                      "misc_binding;STS"),

        new SiteTypeColor(new Color(255, 128, 128), "misc_recomb",              "misc_recomb"),
        new SiteTypeColor(new Color(255, 128, 128), "iDNA",                     "misc_recomb;iDNA"),

        new SiteTypeColor(new Color(255, 128, 128), "misc_structure",           "misc_structure"),
        new SiteTypeColor(new Color(255, 128, 128), "stem_loop",                "misc_structure;stem_loop"),
        new SiteTypeColor(new Color(255, 128, 128), "D-loop",                   "misc_structure;D-loop"),
    };
}
