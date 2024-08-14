package ru.biosoft.bsa.analysis;

import java.util.List;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;

import ru.biosoft.jobcontrol.JobControl;

@SuppressWarnings ( "serial" )
public class SiteSearchReportAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object model)
    {
        Track track = getSitesTrack(model);
        if(track == null)
            return false;
        SiteSearchTrackInfo trackInfo = new SiteSearchTrackInfo( track );
        if(trackInfo.getIntervals() != null)
            return true;
        if(trackInfo.getSequencesDC() instanceof Track)
            return true;
        return false;
    }

    @Override
    public JobControl getJobControl(Object model, List<DataElement> selectedItems, Object properties) throws Exception
    {
        DataCollection<DataElement> parent = ( (DataCollection<?>)model ).getCompletePath().getParentCollection();
        List<String> matrices = StreamEx.of( selectedItems ).map( ru.biosoft.access.core.DataElement::getName ).toList();
        return new SiteSearchReport( SiteSearchReport.generateName( parent, matrices ), getSitesTrack( model ), matrices, false, false );
    }
    
    private Track getSitesTrack(Object model)
    {
        if(!(model instanceof TableDataCollection)) return null;
        TableDataCollection table = (TableDataCollection)model;
        String yesTrackPath = table.getInfo().getProperty(SiteSearchReport.YES_TRACK_PROPERTY);
        if(yesTrackPath == null)
            return null;
        return DataElementPath.create(yesTrackPath).optDataElement(Track.class);
    }
}
