package ru.biosoft.bsa;

/**
 * The site types according to <br>
 *
 * <a href="http://www.ebi.ac.uk/embl/Documentation/FT_definitions/feature_table.html">
 * The DDBJ-EMBL-GenBank Feature Table Definition</a> (Version 3.0 Dec 15 2000).
 *
 * <p>The rule how constant name is generated from the Feature Key:
 * <ul>
 * <li>prefix <code>TYPE_</code> is added before Feature Key</li>
 * <li>all leters are capitalised </li>
 * <li>symbols '<code>'</code>' and '<code>-</code>' are changed by '<code>_</code>'</li>
 * </ul>
 */
public interface SiteType
{
    public static final String TYPE_10_signal              = "-10_signal";
    public static final String TYPE_35_signal              = "-35_signal";
    public static final String TYPE_3_CLIP_clip            = "3'clip"  ;

    public static final String TYPE_3_UTR                  = "3'UTR";
    public static final String TYPE_5_CLIP                 = "5'clip";
    public static final String TYPE_5_UTR                  = "5'UTR";
    public static final String TYPE_ATTENUATOR             = "attenuator";
    public static final String TYPE_C_REGION               = "C_region";
    public static final String TYPE_CAAT_SIGNAL            = "CAAT_signal";
    public static final String TYPE_CDS                    = "CDS";
    public static final String TYPE_CONFLICT               = "conflict";
    public static final String TYPE_D_SEGMENT              = "D_segment";
    public static final String TYPE_D_LOOP                 = "D-loop";
    public static final String TYPE_ENHANCER               = "enhancer";
    public static final String TYPE_EXON                   = "exon";
    public static final String TYPE_GC_SIGNAL              = "GC_signal";
    public static final String TYPE_GENE                   = "gene";
    public static final String TYPE_IDNA                   = "iDNA";
    public static final String TYPE_IMMUNOGLOBULIN_RELATED = "immunoglobulin_related";
    public static final String TYPE_INTRON                 = "intron";
    public static final String TYPE_J_SEGMENT              = "J_segment";
    public static final String TYPE_LTR                    = "LTR";
    public static final String TYPE_MAT_PEPTIDE            = "mat_peptide";
    public static final String TYPE_MISC_BINDING           = "misc_binding";
    public static final String TYPE_MISC_DIFFERENCE        = "misc_difference";
    public static final String TYPE_MISC_FEATURE           = "misc_feature";
    public static final String TYPE_MISC_RECOMB            = "misc_recomb";
    public static final String TYPE_MISC_RNA               = "misc_RNA";
    public static final String TYPE_MISC_SIGNAL            = "misc_signal";
    public static final String TYPE_MISC_STRUCTURE         = "misc_structure";
    public static final String TYPE_MODIFIED_BASE          = "modified_base";
    public static final String TYPE_MRNA                   = "mRNA";
    public static final String TYPE_N_REGION               = "N_region";
    public static final String TYPE_OLD_SEQUENCE           = "old_sequence";
    public static final String TYPE_POLYA_SIGNAL           = "polyA_signal";
    public static final String TYPE_POLYA_SITE             = "polyA_site";
    public static final String TYPE_PRECURSOR_RNA          = "precursor_RNA";
    public static final String TYPE_PRIM_TRANSCRIPT        = "prim_transcript";
    public static final String TYPE_PRIMER_BIND            = "primer_bind";
    public static final String TYPE_PROMOTER               = "promoter";
    public static final String TYPE_PROTEIN_BIND           = "protein_bind";
    public static final String TYPE_RBS                    = "RBS    ";
    public static final String TYPE_REP_ORIGIN             = "rep_origin";
    public static final String TYPE_REPEAT_REGION          = "repeat_region";
    public static final String TYPE_REPEAT_UNIT            = "repeat_unit";
    public static final String TYPE_RRNA                   = "rRNA";
    public static final String TYPE_S_REGION               = "S_region";
    public static final String TYPE_SATELLITE              = "satellite";
    public static final String TYPE_SCRNA                  = "scRNA";
    public static final String TYPE_SIG_PEPTIDE            = "sig_peptide";
    public static final String TYPE_SNRNA                  = "snRNA";
    public static final String TYPE_STEM_LOOP              = "stem_loop";
    public static final String TYPE_STS                    = "STS";
    public static final String TYPE_TATA_SIGNAL            = "TATA_signal";
    public static final String TYPE_TERMINATOR             = "terminator";
    public static final String TYPE_TRANSCRIPTION_FACTOR   = "TF binding site";
    public static final String TYPE_TRANSCRIPTION_START    = "Transcription start site";
    public static final String TYPE_TRANSIT_PEPTIDE        = "transit_peptide";
    public static final String TYPE_TRNA                   = "tRNA";
    public static final String TYPE_UNSURE                 = "unsure";
    public static final String TYPE_V_REGION               = "V_region";
    public static final String TYPE_V_SEGMENT              = "V_segment";
    public static final String TYPE_VARIATION              = "variation";
    public static final String TYPE_INSERTION              = "insertion";
    public static final String TYPE_DELETION               = "deletion";
}
