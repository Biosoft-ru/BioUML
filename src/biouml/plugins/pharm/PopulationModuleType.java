package biouml.plugins.pharm;

import java.util.Properties;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.ModuleType;
import biouml.model.util.DiagramXmlTransformer;
import biouml.standard.StandardModuleType;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;

/**
 * 
 * @author Ilya
 *
 */
public class PopulationModuleType extends DataElementSupport implements ModuleType
{
    public static final String VERSION = "0.86";

    public PopulationModuleType()
    {
        super("Pharm", null);
    }

    @Override
    @SuppressWarnings ( "unchecked" )
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        return StandardModuleType.getGeneralPurposeTypes().append(PopulationModelDiagramType.class).toArray(Class[]::new);
    }

    @Override
    public String[] getXmlDiagramTypes()
    {
        return new String[0];
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
        if (c == Diagram.class)
            return Module.DIAGRAM;
        
        throw new UnsupportedOperationException("Category issues are not supported for Pharm module.");
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
        transformed.setProperty(Module.TYPE_PROPERTY,  PopulationModuleType.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.pharm");

        Module module = (Module)CollectionFactoryUtils.createDerivedCollection(parent, name, primary, transformed, name);
        LocalRepository moduleLR = (LocalRepository)module.getPrimaryCollection();

        CollectionFactoryUtils.createTransformedFileCollection(moduleLR,     Module.DIAGRAM, "", DiagramXmlTransformer.class);
        
        return module;
    }
}
