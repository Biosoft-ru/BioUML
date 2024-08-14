package biouml.plugins.gtrd.master.collections;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToWHotspot2FootprintCluster;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToWMACS2FootprintCluster;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster.Design;
import biouml.plugins.gtrd.master.sites.dnase.FootprintCluster;
import biouml.plugins.gtrd.master.sites.dnase.WellingtonHotspot2FootprintCluster;
import biouml.plugins.gtrd.master.sites.dnase.WellingtonMACS2FootprintCluster;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.track.big.BedEntryConverter;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class FootprintClustersCollection extends ClustersCollection<FootprintCluster>
{
    
    public FootprintClustersCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
    }


    //Footprint_clusters_HOTSPOT2_10.bb
    private static final Pattern NAME_PATTERN = Pattern.compile( "Footprint_clusters_([^_]*)_([0-9]+)[.]bb" );
    
    @Override
    protected BigBedTrack<FootprintCluster> doGet(String name) throws Exception
    {
        if(!name.startsWith( "Footprint" ))
        {
            return super.doGet( name );
        }
        
        //Obsolete naming style, will be removed in the future
        Matcher matcher = NAME_PATTERN.matcher( name );
        if(!matcher.matches())
            return null;

        String expType = Design.DNASE_SEQ.label;
        String peakCaller = matcher.group( 1 ).toLowerCase();
        String cellId = matcher.group( 2 );
        peakCaller = "wellington_" + peakCaller;
        
        return doGet(name, expType, cellId, peakCaller);
    }
    

    @Override
    protected BedEntryConverter<FootprintCluster> getConverter(String expType, String peakCaller, CellLine cell,
            BigBedTrack<FootprintCluster> result)
    {
        Design design = Design.getByLabel( expType );
        BedEntryConverter<? extends DNaseCluster> converter;
        switch( peakCaller )
        {
            case WellingtonMACS2FootprintCluster.PEAK_CALLER:
                converter = new BedEntryToWMACS2FootprintCluster( result, cell, design );
                break;
            case WellingtonHotspot2FootprintCluster.PEAK_CALLER:
                converter = new BedEntryToWHotspot2FootprintCluster( result, cell, design );
                break;
            default:
                throw new AssertionError( "Unsupported peak caller: " + peakCaller );
        }
        return (BedEntryConverter<FootprintCluster>)converter;
    }

}
