package biouml.plugins.ensembl.analysis;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import ru.biosoft.bsa.Site;
import biouml.plugins.ensembl.analysis.SiteData.Location;

/**
 * @author lan
 *
 */
public class StructureSiteAggregator extends SiteAggregator
{

    @Override
    public String getName()
    {
        return "Structure";
    }

    @Override
    public Object aggregate(Site gene, int fivePrimeSize, int threePrimeSize, List<SiteData> siteData)
    {
        if(siteData == null) return "";
        Map<Location, Long> counts = StreamEx.of( siteData ).map( SiteData::getLocation )
                .collect( Collectors.groupingBy( x -> x, Collectors.counting() ) );
        return EntryStream.of( locationNames ).mapValues( counts::get ).nonNullValues()
                .mapKeyValue( (type, count) -> count > 1 ? type + " x " + count : type ).joining( "; " );
    }
}
