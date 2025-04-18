package biouml.plugins.gtrd.analysis.nosql;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import biouml.plugins.gtrd.master.sites.GenomeLocation;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToGEMChipSeqPeak;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToMACS2ChipSeqPeak;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToPICSChipSeqPeak;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToPeak;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToSISSRSChipSeqPeak;
import biouml.plugins.gtrd.master.sites.chipseq.GEMPeak;
import biouml.plugins.gtrd.master.sites.chipseq.MACS2ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.chipseq.PICSPeak;
import biouml.plugins.gtrd.master.sites.chipseq.SISSRSPeak;
import ru.biosoft.bigbed.BedEntry;
import ru.biosoft.bigbed.ChromInfo;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.track.big.BedEntryConverter;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryWithStableIdConverter implements BedEntryConverter<Site>
{
    public static final String PROP_STABLE_ID_COLUMN = PROP_PREFIX + "StableIdColumn";
    private int stableIdCol = -1;//0 - is the first column after chr\tstart\tend, -1 means last column
    
    private BigBedTrack<?> origin;
    private Properties props;
    public BedEntryWithStableIdConverter(BigBedTrack<?> origin, Properties props)
    {
        this.origin = origin;
        this.props = props;

        String stableIdColStr = props.getProperty( PROP_STABLE_ID_COLUMN );
        if(stableIdColStr != null)
            stableIdCol = Integer.parseInt( stableIdColStr );
    }
    

    @Override
    public Site fromBedEntry(BedEntry e)
    {
        String[] parts = e.getRest().split( "\t" );
        String stableId = stableIdCol == -1 ? parts[parts.length-1] : parts[stableIdCol];
        String[] idParts = stableId.split( "[.]" );
        String siteType = idParts[0];
        if(siteType.equals( "p" ))//peaks
        {
            String expId = idParts[1];
            if(!expId.startsWith( "EXP" ))
                throw new UnsupportedOperationException();
            props.setProperty( BedEntryToPeak.PROP_EXPERIMENT_ID, expId );
            
            String peakCaller = idParts[2];
            String id = idParts[3];
            BedEntryConverter<? extends GenomeLocation> converter;
            switch(peakCaller)
            {
                case MACS2ChIPSeqPeak.PEAK_CALLER: converter = new BedEntryToMACS2ChipSeqPeak( origin, props ); break;
                case SISSRSPeak.PEAK_CALLER: converter = new BedEntryToSISSRSChipSeqPeak( origin, props ); break;
                case GEMPeak.PEAK_CALLER: converter = new BedEntryToGEMChipSeqPeak( origin, props ); break;
                case PICSPeak.PEAK_CALLER: converter = new BedEntryToPICSChipSeqPeak( origin, props );break;
                default:
                    throw new AssertionError();
            }
            
            //replace stable id with numeric id
            parts[stableIdCol==-1?parts.length-1:stableIdCol] = id;
            BedEntry e2 = new BedEntry( e.chrId, e.start, e.end);
            e2.data = String.join( "\t", parts ).getBytes(StandardCharsets.UTF_8);
            
            return converter.fromBedEntry( e2 );
        }else if(siteType.equals( "ms" ))//meta cluster
        {
        	ChromInfo chrInfo = origin.getChromInfo(e.chrId);
        	String chrName = origin.internalToExternal(chrInfo.name);
            Sequence seq = origin.getChromosomeSequence( chrName );
            SiteImpl s = new SiteImpl( null, stableId, e.start + 1, e.end - e.start, StrandType.STRAND_NOT_KNOWN, seq );
            s.setType( parts[parts.length-2] );//TF name
            return s;
        }else
            throw new UnsupportedOperationException();
    }

    @Override
    public BedEntry toBedEntry(Site s)
    {
        throw new UnsupportedOperationException();
    }

}
