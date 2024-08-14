package biouml.plugins.cellml;

import java.util.Properties;

import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.ModuleType;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;

/**
 * Module type that process CellML 1.0 and 1.1 biochemical models.
 *
 * While CellML stores all information in diagram files, so DATA and METADATA directory
 * absents and category issues are unsupported.
 */
public class CellMLModuleType extends DataElementSupport implements ModuleType
{
    public static final String VERSION = "CellML 1.1";

    public CellMLModuleType()
    {
        super("CellML", null);
    }

    @Override
    @SuppressWarnings ( "unchecked" )
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        return new Class[]
        {
            CellMLDiagramType.class
        };
    }
    
    @Override
    public String[] getXmlDiagramTypes()
    {
        return null;
    }

    @Override
    public String getVersion()
    {
        return VERSION;
    }

    @Override
    public boolean isCategorySupported()
    {
        return false;
    }

    @Override
    public String getCategory(Class<? extends DataElement> c)
    {
        throw new UnsupportedOperationException("Category issues are not supported for CellML module.");
    }

    @Override
    public boolean canCreateEmptyModule()
    {
        return true;
    }

    @Override
    public Module createModule(Repository parent, String name) throws Exception
    {
        // Create Module data collection (root)
        Properties primary = new Properties();
        primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName());

        Properties transformed = new Properties();
        transformed.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, Module.class.getName());
        transformed.setProperty(Module.TYPE_PROPERTY,  CellMLModuleType.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.cellml");

        Module module = (Module)CollectionFactoryUtils.createDerivedCollection(parent, name, primary, transformed, name);
        LocalRepository moduleLR = (LocalRepository)module.getPrimaryCollection();

        CollectionFactoryUtils.createTransformedFileCollection(moduleLR,     Module.DIAGRAM, "xml", CellMLDiagramTransformer.class);
        return module;
    }
}
