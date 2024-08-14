package biouml.plugins.ensembl.type;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteImpl;

@SuppressWarnings ( "serial" )
public class Exon extends SiteImpl
{
    private int startPhase, endPhase;
    private int exonId;

    public Exon(DataCollection<?> parent, int exonId, Sequence chromosome, int from, int to, boolean forwardStrand, int startPhase,
            int endPhase)
    {
        super(parent, String.valueOf(exonId), TYPE_EXON, BASIS_ANNOTATED, forwardStrand ? from : to, to - from + 1, forwardStrand
                ? STRAND_PLUS : STRAND_MINUS, chromosome);
        this.exonId = exonId;
        this.startPhase = startPhase;
        this.endPhase = endPhase;
    }
 
    /**
     * The Ensembl phase convention can be thought of as
     * "the number of bases of the first codon which are
     * on the previous exon".  It is therefore 0, 1 or 2
     * (or -1 if the exon is non-coding).  In ascii art,
     * with alternate codons represented by ### and +++:
     *        Previous Exon   Intron   This Exon
     *     ...-------------            -------------...
     *     5'                    Phase                3'
     *     ...#+++###+++###          0 +++###+++###+...
     *     ...+++###+++###+          1 ++###+++###++...
     *     ...++###+++###++          2 +###+++###+++...
     */
    
    public int getStartPhase()
    {
        return startPhase;
    }
    
    public int getEndPhase()
    {
        return endPhase;
    }
    
    public int getId()
    {
        return exonId;
    }

}
