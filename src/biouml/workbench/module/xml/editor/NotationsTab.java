package biouml.workbench.module.xml.editor;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import biouml.workbench.module.xml.XmlModule;
import biouml.workbench.module.xml.XmlModuleConstants;
import biouml.workbench.module.xml.XmlModule.DiagramTypeDescription;

public class NotationsTab extends JPanel
{
    protected MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    protected List<DiagramTypeDescription> dtList = new ArrayList<>();

    protected RichTableControl rtControl;

    public NotationsTab(List<DiagramTypeDescription> diagramTypes)
    {
        if( diagramTypes != null )
        {
            for( DiagramTypeDescription dt : diagramTypes )
            {
                dtList.add(dt.clone());
            }
        }

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        rtControl = new RichTableControl(dtList, DiagramTypeDescription.class, null);
        add(rtControl, BorderLayout.CENTER);
    }

    public String validateForm()
    {
        StringBuilder result = new StringBuilder("<html>");
        if( dtList.size() == 0 )
        {
            result.append(messageBundle.getResourceString("NOTATIONS_TAB_LIST_ERROR")).append("<br>");
        }
        for( DiagramTypeDescription dt : dtList )
        {
            if( dt.getName() == null || dt.getName().length() == 0 || dt.getType() == null || dt.getType().length() == 0 )
            {
                result.append(messageBundle.getResourceString("NOTATIONS_TAB_VALUES_ERROR")).append("<br>");
                break;
            }
            if( dt.getType().equals(XmlModuleConstants.GRAPHIC_NOTATION_TYPE_JAVA)
                    && ( dt.getClassName() == null || dt.getClassName().length() == 0 ) )
            {
                result.append(messageBundle.getResourceString("NOTATIONS_TAB_CLASS_ERROR")).append("<br>");
                break;
            }
            else if( dt.getType().equals(XmlModuleConstants.GRAPHIC_NOTATION_TYPE_XML)
                    && ( dt.getPath() == null || dt.getPath().length() == 0 ) )
            {
                result.append(messageBundle.getResourceString("NOTATIONS_TAB_PATH_ERROR")).append("<br>");
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
        module.setDiagramTypes(dtList);
    }
}
