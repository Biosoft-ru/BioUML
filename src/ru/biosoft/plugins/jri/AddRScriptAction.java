package ru.biosoft.plugins.jri;

import javax.swing.JOptionPane;

import com.developmentontheedge.application.Application;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.AbstractElementAction;

@SuppressWarnings ( "serial" )
public class AddRScriptAction extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        DataCollection<?> dc = de.cast( ru.biosoft.access.core.DataCollection.class );
        String name = JOptionPane.showInputDialog(Application.getApplicationFrame(), "Create R script");
        if( name == null )
            return;
        RElement rElement2 = new RElement(dc, name, "");
        CollectionFactoryUtils.save( rElement2 );
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return (de instanceof DataCollection) && DataCollectionUtils.isAcceptable((DataCollection<?>)de, RElement.class);
    }
}
