package ru.biosoft.table.access;

import java.text.MessageFormat;

import javax.swing.JOptionPane;

import com.developmentontheedge.application.Application;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.AbstractElementAction;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.table.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class NewTableDocumentAction extends AbstractElementAction
{
    private static MessageBundle messageBundle = new MessageBundle();

    public static final String DATA_COLLECTION = "transformed data collection";

    @Override
    protected void performAction(DataElement de) throws Exception
    {
        DataCollection parent = (DataCollection)de;
        
        String tableName = "";
        while( 0 == tableName.length()  )
        {
            tableName = JOptionPane.showInputDialog(messageBundle.getResourceString("INFO_TABLE_NAME_ENTERING"));
            if( null == tableName )
                break;
        }
        if( null != tableName )
        {
            if( parent.contains(tableName) )
            {
                String message = MessageFormat.format(messageBundle.getResourceString("WARN_TABLE_EXISTENCE"),
                        new Object[] {tableName});
                int res = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message);
                if( res != JOptionPane.YES_OPTION )
                    return;
            }
            TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection(parent, tableName);
            CollectionFactoryUtils.save(tdc);
            DocumentManager.getDocumentManager().openDocument(tdc);
        }
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        if(!(de instanceof DataCollection)) return false;
        DataCollection dc = ( (DataCollection)de );
        return DataCollectionUtils.isAcceptable(dc, TableDataCollection.class);
    }
}
