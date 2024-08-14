package biouml.plugins.ensembl.analysis.mutationeffect;

public enum MutationType
{
    SYNONYNYMOUS_SNV, //a single nucleotide change that do not cause an amino acid change
    NONSYNONYMOUS_SNV, //a single nucleotide change that cause an amino acid change
    STOP_GAIN, //a nonsynonymous SNV, frameshift insertion/deletion, nonframeshift insertion/deletion or block substitution that lead to the immediate creation of stop codon at the variant site. For frameshift mutations, the creation of stop codon downstream of the variant will not be counted as "stopgain"!
    NONSENSE_MEDIATED_DECAY,//special case of STOP_GAIN that leads to nonsense mediated decay 
    STOP_LOSS, //a nonsynonymous SNV, frameshift insertion/deletion, nonframeshift insertion/deletion or block substitution that lead to the immediate elimination of stop codon at the variant site
    NONFRAMESHIFT_INSERTION, //an insertion of 3 or multiples of 3 nucleotides that do not cause frameshift changes in protein coding sequence
    NONFRAMESHIFT_DELETION, //a deletion of 3 or mutliples of 3 nucleotides that do not cause frameshift changes in protein coding sequence
    NONFRAMESHIFT_BLOCK_SUBSTITUTION, //a block substitution of one or more nucleotides that do not cause frameshift changes in protein coding sequence
    FRAMESHIFT_INSERTION, //an insertion of one or more nucleotides that cause frameshift changes in protein coding sequence
    FRAMESHIFT_DELETION, //a deletion of one or more nucleotides that cause frameshift changes in protein coding sequence
    FRAMESHIFT_BLOCK_SUBSTITUTION, //a block substitution of one or more nucleotides that cause frameshift changes in protein coding sequence
    NOTHING; // nothing changed at all
}
