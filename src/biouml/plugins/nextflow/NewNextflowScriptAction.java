package biouml.plugins.nextflow;

import javax.swing.JOptionPane;

import com.developmentontheedge.application.Application;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.AbstractElementAction;

@SuppressWarnings ( "serial" )
public class NewNextflowScriptAction extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        DataCollection<?> dc = de.cast( ru.biosoft.access.core.DataCollection.class );
        String name = JOptionPane.showInputDialog(Application.getApplicationFrame(), "Create Nextflow script");
        if( name == null )
            return;
        NextflowScript script = new NextflowScript(dc, name, "");
        CollectionFactoryUtils.save( script );
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return (de instanceof DataCollection) && DataCollectionUtils.isAcceptable((DataCollection<?>)de, NextflowScript.class);
    }
}
