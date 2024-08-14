package ru.biosoft.access.generic;

import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.exception.DataElementExistsException;
import ru.biosoft.access.repository.AbstractElementAction;

import com.developmentontheedge.application.Application;

public class CreateFolderAction extends AbstractElementAction
{
    protected ResourceBundle messageBundle = MessageBundle.getBundle(MessageBundle.class.getName());

    @Override
    protected void performAction(DataElement de) throws Exception
    {
        DataCollection dc = (DataCollection)de;
        String message = messageBundle.getString("FOLDER_NAME_INPUT");
        String name = JOptionPane.showInputDialog(Application.getApplicationFrame(), message);
        if( name != null && !name.isEmpty() )
        {
            DataElementPath path = DataElementPath.create(dc, name);
            if( path.exists() )
            {
                throw new DataElementExistsException(path);
            }
            DataCollectionUtils.createSubCollection(path, false);
        }
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return de instanceof FolderCollection;
    }
}
