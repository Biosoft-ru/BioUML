package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import org.jetbrains.bio.big.BedEntry;

import biouml.plugins.gtrd.master.sites.PWMMotif;
import biouml.plugins.gtrd.master.utils.StringPool;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.track.big.BedEntryConverter;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToMotif implements BedEntryConverter<PWMMotif>
{
    public static final String SITE_MODEL_COLLECTION = PROP_PREFIX + "SiteModelCollection"; 
    private DataElementPath siteModelPath;
    public BedEntryToMotif(BigBedTrack<?> origin, Properties props)
    {
        String siteModelCollection = props.getProperty( SITE_MODEL_COLLECTION );
        String siteModelName = origin.getName();
        if(siteModelName.endsWith( ".bb" ))
            siteModelName = siteModelName.substring( 0, siteModelName.length() - ".bb".length() );
        siteModelPath = DataElementPath.create( siteModelCollection, siteModelName );
    }
    
    @Override
    public PWMMotif fromBedEntry(BedEntry e)
    {
        PWMMotif m = new PWMMotif();
        
        m.setChr( StringPool.get(e.getChrom() ) );
        m.setFrom( e.getStart() + 1 );
        m.setTo( e.getEnd() );
        
        String[] cols = e.getRest().split( "\t" );
        
        String id = cols[0];
        m.setId( Integer.parseInt( id ) );
        
        float score = Float.parseFloat( cols[1] );
        m.setScore( score );
        
        switch(cols[2])
        {
            case "+":
                m.setForwardStrand( true );
                break;
            case "-":
                m.setForwardStrand( false );
                break;
            default:
                throw new IllegalArgumentException();
        }
        
        m.setSiteModelPath( siteModelPath );
        
        return m;
    }

    @Override
    public BedEntry toBedEntry(PWMMotif m)
    {
        String rest = m.getId() + "\t" + m.getScore() + "\t" + (m.isForwardStrand() ? "+" : "-"); 
        return new BedEntry( m.getChr(), m.getFrom() - 1, m.getTo(), rest );
    }
    
}
