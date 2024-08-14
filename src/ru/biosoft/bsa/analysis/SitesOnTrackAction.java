package ru.biosoft.bsa.analysis;

import java.util.List;
import java.util.Properties;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.ProjectAsLists;
import ru.biosoft.bsa.project.TrackInfo;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.table.TableDataCollection;

@SuppressWarnings ( "serial" )
public class SitesOnTrackAction extends BackgroundDynamicAction
{
    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, Object properties) throws Exception
    {
        return new AbstractJobControl(log) {
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    Properties props = ((DataCollection<?>)model).getInfo().getProperties();
                    DataElementPath resultPath = ((DataCollection<?>)model).getCompletePath().getParentPath();
                    DataElementPath trackPath = DataElementPath.create(props.getProperty(SiteSearchReport.TRACK_PROPERTY));
                    FilteredTrack track = trackPath.getDataElement(FilteredTrack.class);
                    Track sitesTrack = DataElementPath.create(track.getSourcePath()).getDataElement(Track.class);
                    SiteSearchTrackInfo siteSearchTrackInfo = new SiteSearchTrackInfo(sitesTrack);
                    Track promotersTrack = siteSearchTrackInfo.getIntervals();
                    if(promotersTrack == null)
                        promotersTrack = siteSearchTrackInfo.getSequencesDC().cast( Track.class );
                    AnnotatedSequence sequence = DataElementPath.create(promotersTrack).getChildPath(selectedItems.get(0).getName())
                            .getDataElement(AnnotatedSequence.class);
                    Project project = ProjectAsLists.createProjectByMap(resultPath.getDataCollection(), sequence.getName(), sequence);
                    TrackInfo trackInfo = new TrackInfo(track);
                    trackInfo.setTitle("Sites");
                    project.addTrack(trackInfo);
                    CollectionFactoryUtils.save(project);
                    setPreparedness(100);
                    resultsAreReady(new Object[]{project});
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }
            }
        };
    }
    
    @Override
    public boolean isApplicable(Object model)
    {
        try
        {
            if(!(model instanceof TableDataCollection)) return false;
            Properties props = ((DataCollection<?>)model).getInfo().getProperties();
            DataElementPath trackPath = DataElementPath.create(props.getProperty(SiteSearchReport.TRACK_PROPERTY));
            if(trackPath == null) return false;
            trackPath.getDataElement(FilteredTrack.class);
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }

}
