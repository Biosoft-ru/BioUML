package ru.biosoft.access.subaction;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

public class RemoveSelectionAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object object)
    {
        if( object instanceof DataCollection && ( (DataCollection<?>)object ).isMutable() )
        {
            return true;
        }
        return false;
    }

    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, Object properties) throws Exception
    {
        return new AbstractJobControl(log) {
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    Iterator<DataElement> iter = selectedItems.iterator();
                    while( iter.hasNext() )
                    {
                        try
                        {
                            DataElement de = iter.next();
                            de.getOrigin().remove(de.getName());
                        }
                        catch( Exception e )
                        {
                            log.log( Level.SEVERE, e.getMessage(), e );
                        }
                    }
                    setPreparedness(100);
                    resultsAreReady(new Object[]{model});
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }
            }
        };
    }

    @Override
    public String getConfirmationMessage(Object model, List<DataElement> selectedItems)
    {
        return "Do you really want to remove the selection from " + ( (DataCollection<?>)model ).getName();
    }
}
