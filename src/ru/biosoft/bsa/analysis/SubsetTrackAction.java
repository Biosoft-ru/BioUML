package ru.biosoft.bsa.analysis;

import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModelTransformedCollection;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;

import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

@SuppressWarnings ( "serial" )
public class SubsetTrackAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object object)
    {
        if(!(object instanceof Track)) return false;
        try
        {
            ((Track)object).getAllSites();
        }
        catch(Exception e)
        {
            return false;
        }
        return true;
    }
    
    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, final Object properties) throws Exception
    {
        return new AbstractJobControl(log){
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    Track source = (Track)model;
                    DataCollection<Site> allSites = source.getAllSites();
                    DataElementPath path = (DataElementPath)((DynamicPropertySet)properties).getValue("target");
                    SqlTrack result;
                    try
                    {
                        result = SqlTrack.createTrack( path, source, source.getClass() );
                    }
                    catch( Exception e )
                    {
                        throw new Exception("Unable to create resulting track "+path);
                    }
                    for(DataElement item: selectedItems)
                    {
                        result.addSite(allSites.get(item.getName()));
                    }
                    result.finalizeAddition();
                    path.save(result);
                    setPreparedness(100);
                    resultsAreReady(new Object[]{result});
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }
            }
        };
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        DataElementPath sourcePath = DataElementPath.create((DataElement)model);
        return getTargetProperties(SiteModelTransformedCollection.class, DataElementPath.create(sourcePath.optParentCollection(), sourcePath.getName() + " subset"));
    }
}
