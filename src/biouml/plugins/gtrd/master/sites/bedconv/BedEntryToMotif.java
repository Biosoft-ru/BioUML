package biouml.plugins.gtrd.master.sites.bedconv;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import biouml.plugins.gtrd.master.sites.PWMMotif;
import biouml.plugins.gtrd.master.utils.StringPool;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bigbed.BedEntry;
import ru.biosoft.bigbed.ChromInfo;
import ru.biosoft.bsa.track.big.BedEntryConverter;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToMotif implements BedEntryConverter<PWMMotif>
{
    public static final String SITE_MODEL_COLLECTION = PROP_PREFIX + "SiteModelCollection"; 
    private DataElementPath siteModelPath;
    private BigBedTrack<?> origin;
    public BedEntryToMotif(BigBedTrack<?> origin, Properties props)
    {
    	this.origin = origin;
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
        
        ChromInfo chrInfo = origin.getChromInfo(e.chrId);
        String chrName = origin.internalToExternal(chrInfo.name);
        m.setChr( StringPool.get( chrName ) );
        m.setFrom( e.start + 1 );
        m.setTo( e.end );
        
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
	public BedEntry toBedEntry(PWMMotif m) {
		String rest = m.getId() + "\t" + m.getScore() + "\t" + (m.isForwardStrand() ? "+" : "-");

		ChromInfo chrInfo = origin.getChromInfo(m.getChr());
		BedEntry e = new BedEntry(chrInfo.id, m.getFrom() - 1, m.getTo());
		e.data = rest.getBytes(StandardCharsets.UTF_8);
		return e;
	}
    
}
