package biouml.plugins.gtrd.master.sites;

import java.beans.PropertyDescriptor;
import java.util.Comparator;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import biouml.plugins.gtrd.master.utils.SizeOf;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.bean.StaticDescriptor;

public class GenomeLocation implements Site, SizeOf
{
    protected int id;
    protected String chr;
    protected int from, to;//1-based both inclusive
    
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getChr()
    {
        return chr;
    }
    public void setChr(String chr)
    {
        this.chr = chr;
    }

    @Override
    public int getFrom()
    {
        return from;
    }
    public void setFrom(int from)
    {
        this.from = from;
    }

    @Override
    public int getTo()
    {
        return to;
    }
    public void setTo(int to)
    {
        this.to = to;
    }
    
    public boolean hasSummit() { return false; }
    //summit is the most "important" position
    //measured as offset(zero-based) from getFrom() position
    //to get absolute summit position use getSummit() + getFrom()
    public int getSummit() { return (to-from+1)/2; }
    

    //ru.biosoft.bsa.Site implementation
    protected BigBedTrack<?> origin;
    
    public String getStableId()
    {
        return String.valueOf(id);
    }
    
    @Override
    public String getName()
    {
        return getStableId();
    }
    
    @Override
    public BigBedTrack<?> getOrigin()
    {
        return origin;
    }
    
    public void setOrigin(BigBedTrack<?> origin)
    {
        this.origin = origin;
    }

    @Override
    public String getType()
    {
        return SiteType.TYPE_UNSURE;
    }

    @Override
    public int getBasis()
    {
        return Basis.BASIS_ANNOTATED;
    }

    @Override
    public int getStart()
    {
        return getStrand() == StrandType.STRAND_MINUS ? to : from;
    }

    @Override
    public int getLength()
    {
        return to - from + 1;
    }

    @Override
    public Interval getInterval()
    {
        return new Interval( from, to );
    }

    @Override
    public int getPrecision()
    {
        return PRECISION_NOT_KNOWN;
    }

    @Override
    public int getStrand()
    {
        return StrandType.STRAND_NOT_APPLICABLE;
    }

    @Override
    public Sequence getSequence()
    {
        Sequence chrSeq = origin.getChromosomeSequence( chr );
        return new SequenceRegion( chrSeq, getStart(), getLength(),  chrSeq.getStart(), getStrand() == StrandType.STRAND_MINUS, false );
    }

    @Override
    public Sequence getOriginalSequence()
    {
        return origin.getChromosomeSequence( chr );
    }

    @Override
    public String getComment()
    {
        return "";
    }

    @Override
    public double getScore()
    {
        return 0;
    }

    protected static final PropertyDescriptor STABLE_ID_PD = StaticDescriptor.createReadOnly( "stableId", "Stable id" );
    protected static final PropertyDescriptor SUMMIT_PD = StaticDescriptor.createReadOnly( "summit", "Summit" );
    @Override
    public DynamicPropertySet getProperties()
    {
        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        dps.add(new DynamicProperty( STABLE_ID_PD, String.class, getStableId() ));
        if(hasSummit())
            dps.add( new DynamicProperty(SUMMIT_PD, Integer.class, getSummit()) );
        return dps;
    }
    
    public static final Comparator<GenomeLocation> ORDER_BY_LOCATION = new Comparator<GenomeLocation>() {

        @Override
        public int compare(GenomeLocation o1, GenomeLocation o2)
        {
            int res = o1.chr.compareTo( o2.chr );
            if(res != 0)
                return res;
            res = Integer.compare( o1.from, o2.from );
            if(res != 0)
                return res;
            res = Integer.compare( o2.to, o1.to );//longer sites comes first
            return res;
        }
        
    };

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        GenomeLocation other = (GenomeLocation)obj;
        if( id != other.id )
            return false;
        return getStableId().equals( other.getStableId() );
    }

    //Memory usage
    @Override
    public long _fieldsSize()
    {
        return 4 //id
             + 8 //chr
             + 4 //from
             + 4 //to
             + 8 //origin
             ;
    }
    
    @Override
    public long _childsSize()
    {
        //return SizeOfUtils.sizeOfString(chr);
        //will not count chr since it should be interned
        return 0;
    }
}


