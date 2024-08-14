package biouml.standard.type;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;

@ClassIcon ( "resources/rna.gif" )
public class RNA extends Biopolymer
{
    public static final String[] rnaTypes = {"primary transcript", "precursor RNA", "mRNA", "rRNA", "tRNA", "scRNA", "snRNA", "snoRNA", "dsRNA", "other"};
    
    private String rnaType;
    private String gene;

    public RNA(DataCollection origin, String name)
    {
        super( origin, name );
    }

    @Override
    public String getType()
    {
        return TYPE_RNA;
    }

    @PropertyName ( "RNA type" )
    @PropertyDescription ( "RNA type. Possible values are: <ul>" + "<li>primary transript - primary (initial, unprocessed) transcript.</li>"
            + "<li>precursor RNA - any RNA species that is not yet the mature RNA product.</li>"
            + "<li>mRNA - messenger RNA; includes 5'untranslated region (5'UTR)</li>"
            + "<li>rRNA - mature ribosomal RNA; RNA component of the ribonucleoprotein particle "
            + "(ribosome) which assembles amino acids into proteins.</li>"
            + "<li>tRNA - mature transfer RNA, a small RNA molecule (75-85 bases long) "
            + "that mediates the translation of a nucleic acid " + "sequence into an amino acid sequence.</li>"
            + "<li>scRNA - small cytoplasmic RNA; any one of several small " + "cytoplasmic RNA molecules present in the cytoplasm and "
            + "(sometimes) nucleus of a eukaryote.</li>" + "<li>snRNA -  small nuclear RNA molecules involved in pre-mRNA splicing "
            + "and processing.</li>" + "<li>snoRNA - small nucleolar RNA molecules mostly involved in "
            + "rRNA modification and processing.</li>" + "<li>dsRNA - double stranded RNA.</li>" + "<li>other - other RNA types.</li>"
            + "</ul>" )
    public String getRnaType()
    {
        return rnaType;
    }
    public void setRnaType(String rnaType)
    {
        this.rnaType = rnaType;
    }

    @PropertyName ( "Gene ID" )
    @PropertyDescription ( "Identifier of gene (in the given database) encoded this protein or RNA." )
    public String getGene()
    {
        return gene;
    }
    public void setGene(String gene)
    {
        this.gene = gene;
    }
}
