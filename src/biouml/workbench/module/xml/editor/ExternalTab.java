package biouml.workbench.module.xml.editor;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import biouml.model.CollectionDescription;
import biouml.workbench.module.xml.XmlModule;

public class ExternalTab extends JPanel
{
    protected MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    protected List<CollectionDescription> externalTypesList = new ArrayList<>();

    protected RichTableControl rtControl;

    public ExternalTab(List<CollectionDescription> externalTypes)
    {
        if( externalTypes != null )
        {
            for( CollectionDescription cd : externalTypes )
            {
                externalTypesList.add(cd.clone());
            }
        }

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        rtControl = new RichTableControl(externalTypesList, CollectionDescription.class, null);
        add(rtControl, BorderLayout.CENTER);
    }

    public String validateForm()
    {
        StringBuilder result = new StringBuilder("<html>");
        for( CollectionDescription cd : externalTypesList )
        {
            if( cd.getModuleName() == null || cd.getModuleName().length() == 0 || cd.getSectionName() == null
                    || cd.getSectionName().length() == 0 )
            {
                result.append(messageBundle.getResourceString("EXTERNAL_TAB_VALUES_ERROR")).append("<br>");
                break;
            }
        }
        if( result.length() > 6 )
        {
            return result.toString();
        }
        return null;
    }

    public void applyChanges(XmlModule module)
    {
        module.setExternalType(externalTypesList);
    }
}
