package biouml.workbench.module.xml.editor;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import biouml.workbench.module.xml.XmlModule;
import biouml.workbench.module.xml.XmlModule.InternalType;

public class TypesTab extends JPanel
{
    protected MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    protected List<InternalType> typesList = new ArrayList<>();

    protected RichTableControl rtControl;

    public TypesTab(List<InternalType> internalTypes)
    {
        if( internalTypes != null )
        {
            for( InternalType it : internalTypes )
            {
                typesList.add(it.clone());
            }
        }

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        rtControl = new RichTableControl(typesList, InternalType.class, new String[]{"name", "section", "typeClass"});
        add(rtControl, BorderLayout.CENTER);
    }

    public String validateForm()
    {
        StringBuilder result = new StringBuilder("<html>");
        for( InternalType it : typesList )
        {
            if( it.getName() == null || it.getName().length() == 0 || it.getSection() == null || it.getSection().length() == 0
                    || it.getTypeClass() == null || it.getTypeClass().length() == 0 || it.getTypeTransformer() == null
                    || it.getTypeTransformer().length() == 0 )
            {
                result.append(messageBundle.getResourceString("TYPES_TAB_VALUES_ERROR")).append("<br>");
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
        module.setInternalTypes(typesList);
    }
}
