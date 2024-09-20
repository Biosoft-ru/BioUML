package biouml.plugins.physicell.javacode;


import javax.swing.JOptionPane;

import com.developmentontheedge.application.Application;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.AbstractElementAction;

@SuppressWarnings ( "serial" )
public class AddJavaElementAction extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        DataCollection<?> dc = de.cast( ru.biosoft.access.core.DataCollection.class );
        String name = JOptionPane.showInputDialog( Application.getApplicationFrame(), "Create Java code" );
        if( name == null )
            return;
        JavaElement element = new JavaElement( dc, name, "" );
        CollectionFactoryUtils.save( element );
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return true;//( de instanceof DataCollection ) && DataCollectionUtils.isAcceptable( (DataCollection<?>)de, JavaDataElement.class );
    }
}


