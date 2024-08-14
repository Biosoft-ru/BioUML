package ru.biosoft.bsa.analysis;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.util.bean.StaticDescriptor;

public class GenomeCoverage extends AnalysisMethodSupport<GenomeCoverageParameters>
{
    private PropertyDescriptor PROFILE_PD = StaticDescriptor.create("profile");
    private PropertyDescriptor MAX_PROFILE_HEIGHT_PD = StaticDescriptor.create( "maxProfileHeight" );

    public GenomeCoverage(DataCollection<?> origin, String name)
    {
        super(origin, name, new GenomeCoverageParameters());
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final Track inputTrack = parameters.getInputTrack().getDataElement(Track.class);
        Track regionsTrack = parameters.getRegionsTrack().getDataElement(Track.class);

        final SqlTrack result = SqlTrack.createTrack(parameters.getOutputTrack(), inputTrack);

        jobControl.forCollection(DataCollectionUtils.asCollection(regionsTrack.getAllSites(), Site.class), new Iteration<Site>()
        {
            @Override
            public boolean run(Site region)
            {
                try
                {
                    region = new SiteImpl(null, region.getName(), region.getType(), region.getBasis(), region.getStart(), region
                            .getLength(), region.getPrecision(), region.getStrand(), region.getOriginalSequence(), region.getProperties());
                    result.addSite(computeCoverage(region, inputTrack));
                    return true;
                }
                catch( Exception e )
                {
                    throw ExceptionRegistry.translateException(e);
                }
            }
        });
        if(jobControl.isStopped())
            return null;
        result.finalizeAddition();
        CollectionFactoryUtils.save(result);
        return result;
    }

    private Site computeCoverage(Site region, Track track) throws Exception
    {
        double[] profile = new double[region.getLength()];

        int regionFrom = region.getFrom();
        int regionTo = region.getTo();
        if( parameters.getSiteLength() != -1 )
        {
            regionFrom -= ( parameters.getSiteLength() - 1 );
            regionTo += ( parameters.getSiteLength() - 1 );
        }
        DataCollection<Site> sites = track.getSites(DataElementPath.create(region.getSequence()).toString(), regionFrom, regionTo);

        for( Site site : sites )
        {
            int from = site.getFrom();
            int to = site.getTo();

            if( parameters.getSiteLength() != -1 )
            {
                if( site.getStrand() == StrandType.STRAND_MINUS )
                    from = to - parameters.getSiteLength() + 1;
                else
                    to = from + parameters.getSiteLength() - 1;
            }

            from -= region.getFrom();
            to -= region.getFrom();

            if( from < 0 )
                from = 0;
            if( to >= region.getLength() )
                to = region.getLength() - 1;

            for( int pos = from; pos <= to; pos++ )
                profile[pos]++;
        }

        double max = 0;
        for(double x : profile)
            if(x > max)
                max = x;

        region.getProperties().add(new DynamicProperty(PROFILE_PD, double[].class, profile));
        region.getProperties().add( new DynamicProperty( MAX_PROFILE_HEIGHT_PD, Double.class, max ) );

        return region;
    }
}
