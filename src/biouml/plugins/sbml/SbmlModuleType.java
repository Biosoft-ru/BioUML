package biouml.plugins.sbml;

import java.util.Properties;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.ModuleType;
import biouml.plugins.sbgn.SbgnCompositeDiagramType;
import biouml.plugins.sbgn.SbgnDiagramType;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.FileCollection;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.TransformedDataCollection;

/**
 * Module type that process SBML diagrams. While SBML stores all information in diagram files, so DATA and METADATA directory
 * absents and category issues are unsupported.
 */
public class SbmlModuleType extends DataElementSupport implements ModuleType
{
    public static final String VERSION = "SBML level 1";

    public SbmlModuleType()
    {
        super("SBML", null);
    }

    @Override
    @SuppressWarnings ( "unchecked" )
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        return new Class[] {SbgnDiagramType.class, SbgnCompositeDiagramType.class};
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
        
        throw new UnsupportedOperationException("Category issues are not supported for SBML module.");
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
        transformed.setProperty(Module.TYPE_PROPERTY, SbmlModuleType.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.sbml");

        Module module = (Module)CollectionFactoryUtils.createDerivedCollection(parent, name, primary, transformed, name);
        LocalRepository moduleLR = (LocalRepository)module.getPrimaryCollection();

        primary = new Properties();
        primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, Module.DIAGRAM);
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, FileCollection.class.getName());
        primary.setProperty(FileCollection.FILE_FILTER, "xml");
        primary.setProperty(FileCollection.FILE_SUFFIX, ".xml");

        Properties derived = new Properties();
        derived.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, Module.DIAGRAM);
        derived.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, TransformedDataCollection.class.getName());
        derived.setProperty(DataCollectionConfigConstants.TRANSFORMER_CLASS, SbmlDiagramTransformer.class.getName());

        CollectionFactoryUtils.createDerivedCollection(moduleLR, Module.DIAGRAM, primary, derived, Module.DIAGRAM);
        //

        // 3. launch scripts for MMD base creation

        // 4. launch scripts for plot/simulation results creation in DB


        return module;
    }
}
