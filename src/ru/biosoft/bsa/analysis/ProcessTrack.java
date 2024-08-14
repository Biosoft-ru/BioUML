package ru.biosoft.bsa.analysis;

import java.util.logging.Level;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.MergedTrack;
import ru.biosoft.bsa.ResizedTrack;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackRegion;
import ru.biosoft.bsa.TransformedTrack;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.jobcontrol.JobControl;

@ClassIcon("resources/process-track.gif")
public class ProcessTrack extends AnalysisMethodSupport<ProcessTrackParameters>
{
    public ProcessTrack(DataCollection<?> origin, String name)
    {
        super(origin, name, JavaScriptBSA.class, new ProcessTrackParameters());
    }

    @Override
    public Track justAnalyzeAndPut() throws Exception
    {
        Track source = parameters.getSource();
        Track processedTrack = source;
        if(parameters.getEnlargeStart() != 0 || parameters.getEnlargeEnd() != 0)
        {
            processedTrack = new ResizedTrack(processedTrack, parameters.getShrinkMode(), parameters.getEnlargeStart(), parameters.getEnlargeEnd());
        }

        final int minimalSiteSize = parameters.getMinimalSize();
        if(!parameters.isRemoveSmallSites())//enlarge small sites to minimal site size
        {
            processedTrack = new TransformedTrack(processedTrack)
            {
                @Override
                protected Site transformSite(Site s)
                {
                    if(s.getLength() >= minimalSiteSize)
                        return s;
                    int enlarge = (minimalSiteSize - s.getLength()) / 2;
                    int newStart = s.getStart() + ((s.getStrand()==StrandType.STRAND_MINUS)?enlarge:-enlarge);
                    if(newStart < 1)
                        newStart = 1;
                    return new SiteImpl(s.getOrigin(), s.getName(), s.getType(), s.getBasis(), newStart, minimalSiteSize, s.getPrecision(), s.getStrand(), s
                            .getOriginalSequence(), s.getComment(), s.getProperties());
                }
            };
        }
        if(parameters.isMergeOverlapping())
        {
            processedTrack = new MergedTrack(processedTrack);
        }

        if( parameters.isRemoveSmallSites() )
        {
            processedTrack = new FilteredTrack(processedTrack, new FilteredTrack.SiteFilter()
            {
                @Override
                public boolean isAcceptable(Site s)
                {
                    return s.getLength() >= parameters.getMinimalSize();
                }
            });
        }
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
        final WritableTrack result = SqlTrack.createTrack(parameters.getDestPath(), source);
        final Track inputTrack = processedTrack;

        jobControl.forCollection(parameters.getSequences().getChildren(), sequencePath -> {
            try
            {
                TrackRegion tr = new TrackRegion(inputTrack, sequencePath);
                //We should hold the reference to 'dc'(VectorDataCollection) while using iterator!
                //Iterator does not hold reference to DataCollection since it's simple java.util.Vector.Itr.
                //Otherwise garbage collector will finalize 'dc' clearing vector contents (due to explicit vector.clear() in VectorDataCollection.finalize()),
                //subsequently iterator will throw ConcurrentModificationException.
                //See AbstractDataCollection.finalize(), VectorDataCollection.close(), VectorDataCollection.iterator().
                DataCollection<Site> dc = tr.getSites();
                for(Site s: dc)
                {
                    try
                    {
                        result.addSite(s);
                    }
                    catch( Exception e1 )
                    {
                        log.log(Level.SEVERE, "Error while adding site", e1);
                    }
                }
            }
            catch( Exception e2 )
            {
                log.log(Level.SEVERE, "Error while fetching sequence from original track", e2);
            }
            return true;
        });
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
        {
            result.getOrigin().remove(result.getName());
            return null;
        }
        result.finalizeAddition();
        CollectionFactoryUtils.save(result);
        log.info("Track created ("+result.getAllSites().getSize()+" sites)");
        return result;
    }
}
