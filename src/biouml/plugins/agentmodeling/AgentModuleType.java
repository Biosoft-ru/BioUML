package biouml.plugins.agentmodeling;

import java.util.Properties;

import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.util.DiagramXmlTransformer;
import biouml.standard.StandardModuleType;
import biouml.standard.type.Cell;
import biouml.standard.type.Compartment;
import biouml.standard.type.Concept;
import biouml.standard.type.RNA;
import biouml.standard.type.SemanticRelation;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollectionConfigConstants;

public class AgentModuleType extends StandardModuleType
{
 
    @Override
    @SuppressWarnings ( "unchecked" )
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        return getGeneralPurposeTypes().toArray( Class[]::new );
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
        transformed.setProperty(Module.TYPE_PROPERTY, StandardModuleType.class.getName());

        Module module = (Module)CollectionFactoryUtils.createDerivedCollection(parent, name, primary, transformed, name);
        LocalRepository moduleLR = (LocalRepository)module.getPrimaryCollection();

        // init Data data collection
        Repository dataDC = CollectionFactoryUtils.createLocalRepository(moduleLR, Module.DATA);

        // init data collections

        createDataCollection("cell", dataDC, Cell.class);
        createDataCollection("compartment", dataDC, Compartment.class);
        createDataCollection("concept", dataDC, Concept.class);
        createDataCollection("rna", dataDC, RNA.class);
        createDataCollection("relation", dataDC, SemanticRelation.class);

        // create file collections
        CollectionFactoryUtils.createTransformedFileCollection(moduleLR, Module.DIAGRAM, "", DiagramXmlTransformer.class);

        return module;
    }
}
