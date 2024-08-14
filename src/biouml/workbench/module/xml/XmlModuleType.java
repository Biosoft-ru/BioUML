package biouml.workbench.module.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.Repository;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.standard.StandardModuleType;

public class XmlModuleType extends StandardModuleType
{
    public static final String VERSION = "0.8.0";

    protected static final MessageBundle resources = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    public XmlModuleType()
    {
        super("XML module type");
    }

    @Override
    protected void initCategories()
    {
        //nothing to do
    }

    public void addType(Class<? extends DataElement> c, String path)
    {
        types.put(c, path);
    }

    @Override
    public String getVersion()
    {
        return VERSION;
    }

    protected List<Class<? extends DiagramType>> diagramTypes = new ArrayList<>();
    public void addDiagramType(Class<? extends DiagramType> diagramType)
    {
        diagramTypes.add(diagramType);
    }
    @Override
    @SuppressWarnings ( "unchecked" )
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        if( diagramTypes.size() == 0 )
            return null;

        return diagramTypes.toArray(new Class[diagramTypes.size()]);
    }

    protected List<String> xmlDiagramTypes = new ArrayList<>();
    public void addXmlDiagramType(String xmlDiagramType)
    {
        xmlDiagramTypes.add(xmlDiagramType);
    }
    @Override
    public String[] getXmlDiagramTypes()
    {
        if( xmlDiagramTypes.size() == 0 )
            return null;

        return xmlDiagramTypes.toArray(new String[xmlDiagramTypes.size()]);
    }

    @Override
    public boolean isCategorySupported()
    {
        return true;
    }

    @Override
    public Module createModule(Repository parent, String name) throws Exception
    {
        //nothing to do. XmlModule can't be create from XmlModuleType
        return null;
    }
}
