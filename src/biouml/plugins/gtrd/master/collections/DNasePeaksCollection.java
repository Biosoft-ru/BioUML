package biouml.plugins.gtrd.master.collections;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.Experiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToDNasePeak;
import biouml.plugins.gtrd.master.sites.dnase.DNasePeak;
import ru.biosoft.access.core.DataCollection;

public class DNasePeaksCollection<P extends DNasePeak> extends PeaksCollection<DNaseExperiment, P>
{
    public DNasePeaksCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
    }
    
    @Override
    protected String getNameForFile(String fileName)
    {
        String[] parts = fileName.split( "[_.]" );
        String id = parts[0];
        String rep = parts[1];
        if(rep.startsWith( "rep" ))
            rep = rep.substring( "rep".length() );
        return id + "_" + rep + ".bb";
    }
   
    static final Pattern NAME_PATTERN = Pattern.compile( "(DPEAKS[0-9]+)_([0-9]+)[.]bb" );
    
    @Override
    protected String getPeakIdFromName(String name)
    {
        Matcher matcher = NAME_PATTERN.matcher( name );
        if(!matcher.matches())
            return null;
        String peakId = matcher.group( 1 );
        return peakId;
    }
  
    private String getRepIdFromName(String name)
    {
        Matcher matcher = NAME_PATTERN.matcher( name );
        if(!matcher.matches())
            return null;
        String repId = matcher.group( 2 );
        return repId;
    }
    
    @Override
    protected Properties createBBTrackProperties(String name, Experiment exp)
    {
        Properties props = super.createBBTrackProperties( name, exp );
        props.setProperty( BedEntryToDNasePeak.PROP_REPLICATE, getRepIdFromName( name ) );
        return props;
    }
    
    @Override
    protected Map<String, DNaseExperiment> getExperimentsCollectionFromMetadata(Metadata meta)
    {
        return meta.dnaseExperiments;
    }
    
    
}
