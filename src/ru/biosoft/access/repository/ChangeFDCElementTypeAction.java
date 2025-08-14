package ru.biosoft.access.repository;

import com.developmentontheedge.application.Application;

import ru.biosoft.access.BeanRegistry;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.FDCBeanProvider.FileInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.file.GenericFileDataCollection;
import ru.biosoft.util.PropertiesDialog;

@SuppressWarnings ( "serial" )
public class ChangeFDCElementTypeAction extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        FileInfo fileInfo = (FileInfo) BeanRegistry.getBean( "properties/fdc/" + de.getCompletePath().toString(), null );
        PropertiesDialog propertiesDialog = new PropertiesDialog( Application.getApplicationFrame(), "Change element type", fileInfo );
        if(propertiesDialog.doModal())
        {
            BeanRegistry.saveBean( "properties/fdc/" + de.getCompletePath().toString(), fileInfo, null );
        }
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return DataCollectionUtils.checkPrimaryElementType( de.getOrigin(), GenericFileDataCollection.class )
                && !DataCollectionUtils.checkPrimaryElementType( de, GenericFileDataCollection.class );
    }
}
