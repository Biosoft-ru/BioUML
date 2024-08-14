package ru.biosoft.bsastats;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import one.util.streamex.StreamEx;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName ( "Cut adapters" )
@PropertyDescription ( "Cut adapter from read ends." )
public class CutAdapters extends TaskProcessor
{
    private Adapter[] adapters = new Adapter[] {new Adapter()};
    
    Map<String, AdapterAligner> ADAPTER_ALIGNERS = StreamEx
            .<AdapterAligner> of( new HammingAdapterAligner(), new NWAdapterAligner() )
            .mapToEntry( AdapterAligner::getName, Function.identity() ).toCustomMap( LinkedHashMap::new );
    
    private AdapterAligner adapterAligner;

    public CutAdapters()
    {
        setAdapterAligner( ADAPTER_ALIGNERS.values().iterator().next() );
    }
    
    @PropertyName ( "Adapters" )
    @PropertyDescription ( "Adapters" )
    public Adapter[] getAdapters()
    {
        return adapters;
    }

    public void setAdapters(Adapter[] adapters)
    {
        Adapter[] oldValue = this.adapters;
        this.adapters = adapters;
        if(oldValue != null)
            for(Adapter a : oldValue)
                a.setParent( null );
        if(adapters != null)
            for(Adapter a : adapters)
                a.setParent( this );
        firePropertyChange( "adapters", oldValue, adapters );
    }
    
    @PropertyName("Adapter aligner")
    @PropertyDescription("Adapter aligner algorithm")
    public String getAdapterAlignerName()
    {
        return adapterAligner.getName();
    }
    
    public void setAdapterAlignerName(String name)
    {
        setAdapterAligner( ADAPTER_ALIGNERS.get( name ) );
    }

    public AdapterAligner getAdapterAligner()
    {
        return adapterAligner;
    }

    public void setAdapterAligner(AdapterAligner adapterAligner)
    {
        AdapterAligner oldValue = this.adapterAligner;
        this.adapterAligner = adapterAligner;
        if(oldValue != null)
            oldValue.setParent( null );
        if(adapterAligner != null)
            adapterAligner.setParent( this );
        firePropertyChange( "*", null, null );
    }

    @Override
    public Task process(Task task)
    {
        AdapterMatch bestMatch = null;
        boolean bestReversed = false;
        for( Adapter adapter : adapters )
        {
            boolean reverse = "From 5' end".equals( adapter.getType() );
            byte[] readSequence = task.getSequence();
            byte[] adapterSequence = adapter.getSequence().getBytes();
            if( reverse )
            {
                ArrayUtils.reverse( readSequence = readSequence.clone() );
                ArrayUtils.reverse( adapterSequence = adapterSequence.clone() );
            }

            AdapterMatch match = adapterAligner.alignAdapter( adapterSequence, readSequence );
            if(bestMatch == null || match.getScore() > bestMatch.getScore())
            {
                bestMatch = match;
                bestReversed = reverse;
            }
            
        }
        if( bestMatch == null || bestMatch.getOffset() >= task.getSequence().length )
            return task;

        int from = 0;
        int to = bestMatch.getOffset();
        if( bestReversed )
        {
            from = task.getSequence().length - bestMatch.getOffset();
            to = task.getSequence().length;
        }
        return new Task( Arrays.copyOfRange( task.getSequence(), from, to ), Arrays.copyOfRange( task.getQuality(), from, to ), task.getData() );
    }

    public static class Adapter extends OptionEx implements JSONBean
    {
        static final String[] TYPES = new String[] {"From 5' end", "From 3' end"};
        private String sequence;

        private String type = TYPES[0];

        public Adapter()
        {
        }

        public Adapter(String str)
        {
            String[] fields = str.split( ": ", 2 );
            type = fields[0];
            sequence = fields[1];
        }

        @PropertyName ( "Adapter" )
        @PropertyDescription ( "Adapter sequence" )
        public String getSequence()
        {
            return sequence;
        }
        public void setSequence(String sequence)
        {
            Object oldValue = this.sequence;
            this.sequence = sequence;
            firePropertyChange( "sequence", oldValue, sequence );
        }

        @PropertyName ( "Remove from" )
        @PropertyDescription ( "Remove from specified read end" )
        public String getType()
        {
            return type;
        }
        public void setType(String type)
        {
            Object oldValue = this.type;
            this.type = type;
            firePropertyChange( "type", oldValue, type );
        }

        @Override
        public String toString()
        {
            return type + ": " + sequence;
        }
    }

    public static class AdapterBeanInfo extends BeanInfoEx2<Adapter>
    {
        public AdapterBeanInfo()
        {
            super( Adapter.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "sequence" ).add();
            property( "type" ).tags( Adapter.TYPES ).add();
        }
    }
}
