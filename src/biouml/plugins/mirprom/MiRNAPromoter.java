package biouml.plugins.mirprom;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.bsa.StrandType;

public class MiRNAPromoter extends DataElementSupport
{
    private String miRName;
    private String chr;
    private int pos = 0;
    private int strand = StrandType.STRAND_NOT_KNOWN;
    private List<String> cells = new ArrayList<>();

    public MiRNAPromoter(int promoterId, DataCollection<?> origin, String miRname, String chr, int pos, int strand)
    {
        super( String.valueOf( promoterId ), origin );
        this.miRName = miRname;
        this.chr = chr;
        this.pos = pos;
        this.strand = strand;
    }

    public String getMiRName()
    {
        return miRName;
    }
    public void setMiRName(String miRName)
    {
        this.miRName = miRName;
    }


    public String getChr()
    {
        return chr;
    }

    public int getPos()
    {
        return pos;
    }
    
    public String getLocation()
    {
        return chr + ":" + pos + ":" + (strand== StrandType.STRAND_PLUS ? "+" : "-");
    }

    public int getStrand()
    {
        return strand;
    }
    
    public String[] getCells()
    {
        return cells.toArray( new String[0] );
    }
    
    void addCell(String cell)
    {
        cells.add( cell );
    }
    
    public String getCellsString()
    {
        return String.join( ", ", getCells() );
    }
}
