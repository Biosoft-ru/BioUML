package ru.biosoft.bsa.analysis;

import java.util.List;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteModelTransformedCollection;
import ru.biosoft.bsa.transformer.SiteModelTransformer;

import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

@SuppressWarnings ( "serial" )
public class SubsetCollectionAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object object)
    {
        return object instanceof DataCollection && SiteModel.class.isAssignableFrom(((DataCollection)object).getDataElementType());
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
                    DataElementPath path = (DataElementPath)((DynamicPropertySet)properties).getValue("target");
                    DataCollection dc;
                    try
                    {
                        dc = SiteModelTransformer.createCollection(path);
                    }
                    catch( Exception e )
                    {
                        throw new Exception("Unable to create resulting collection "+path);
                    }
                    for(DataElement item: selectedItems)
                    {
                        if(item instanceof CloneableDataElement)
                        {
                            CloneableDataElement cde = (CloneableDataElement)item;
                            dc.put(cde.clone(dc, cde.getName()));
                        }
                    }
                    path.save(dc);
                    setPreparedness(100);
                    resultsAreReady(new Object[]{dc});
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
