package biouml.plugins.gtrd.master.sites.bedconv;

import org.jetbrains.bio.big.BedEntry;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster.Design;
import ru.biosoft.bsa.track.big.BedEntryConverter;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil2;

public abstract class BedEntryToDNaseCluster<T extends DNaseCluster> implements BedEntryConverter<T>
{
    public static final String PROP_PEAK_CALLER = PROP_PREFIX + "peakCaller";
    public static final String PROP_CELL_ID = PROP_PREFIX + "cellId";
    
    private BigBedTrack<?> origin;
    private CellLine cell;
    private Design desigin;
    
    public BedEntryToDNaseCluster(BigBedTrack<?> origin, CellLine cell, Design design)
    {
        this.origin = origin;
        this.cell = cell;
        this.desigin = design;
    }
    
    protected abstract T createCluster();
    protected abstract void parseRestString(T cluster, String[] parts);

    @Override
    public T fromBedEntry(BedEntry e)
    {
        T c = createCluster();
        c.setOrigin( origin );
        c.setCell( cell );
        c.setDesign( desigin );
        
        c.setChr( e.getChrom() );
        c.setFrom( e.getStart()+1 );
        c.setTo( e.getEnd() );
        
        String[] parts = TextUtil2.split( e.getRest(), '\t' );
        parseRestString(c, parts);
        
        return c;
    }

    protected abstract String getRestString(T cluster);

    @Override
    public BedEntry toBedEntry(T c)
    {
        return new BedEntry( c.getChr(), c.getFrom()-1, c.getTo(), getRestString( c ) );
    }

}
