package ru.biosoft.plugins.javascript;

import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.AbstractElementAction;

import com.developmentontheedge.application.Application;

public class AddJSAction extends AbstractElementAction
{
    protected ResourceBundle messageBundle = MessageBundle.getBundle(MessageBundle.class.getName());

    @Override
    protected void performAction(DataElement de) throws Exception
    {
        DataCollection<JSElement> dc = (DataCollection<JSElement>)de;
        String message = messageBundle.getString("JAVASCRIPT_ADD_ELEMENT");
        String name = JOptionPane.showInputDialog(Application.getApplicationFrame(), message);
        if( name == null )
            return;
        JSElement jsElement = new JSElement(dc, name, "");
        dc.put(jsElement);
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return (de instanceof DataCollection) && DataCollectionUtils.isAcceptable((DataCollection)de, JSElement.class);
    }
}
