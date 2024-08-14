package biouml.plugins.keynodes.customhub;

import java.util.Properties;

import biouml.model.Module;
import biouml.standard.StandardModuleType;
import biouml.standard.type.Compartment;
import biouml.standard.type.Complex;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.Substance;
import biouml.standard.type.access.ReactionTransformer;
import biouml.standard.type.access.TitleIndex;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.DefaultQuerySystem;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.security.SecurityManager;

public class UserHubModuleType extends StandardModuleType
{
    @Override
    public Module createModule(Repository parent, String name) throws Exception
    {
        Module module = initModule( parent, name );
        Repository dataDC = initDataSubCollection( module );

        initFileCollection( dataDC, "compartment", "CMP0000", Compartment.class );
        initFileCollection( dataDC, "complex", "CPX000000", Complex.class );
        initFileCollection( dataDC, "substance", "SBS000000", Substance.class );
        initFileCollection( dataDC, "protein", "PRT000000", Protein.class );
        initFileCollection( dataDC, "reaction", "RCT000000", Reaction.class, ReactionTransformer.class );

        return module;
    }

    private void initFileCollection(Repository dataDC, String name, String idFormat, Class<? extends DataElement> deType) throws Exception
    {
        initFileCollection( dataDC, name, idFormat, deType, null );
    }
    private void initFileCollection(Repository dataDC, String name, String idFormat, Class<? extends DataElement> deType,
            Class<?> transformerClass) throws Exception
    {
        if( transformerClass != null )
            createDataCollection( name, dataDC, deType, transformerClass );
        else
            createDataCollection( name, dataDC, deType );
        DataCollection<?> dc = dataDC.get( name );
        dc = getElement( dc, TransformedDataCollection.class );

        dc.getInfo().getProperties().setProperty( DataCollectionConfigConstants.ID_FORMAT, idFormat );
        dc.getInfo().getProperties().setProperty( QuerySystem.QUERY_SYSTEM_CLASS, DefaultQuerySystem.class.getName() );
        dc.getInfo().getProperties().setProperty( QuerySystem.INDEX_LIST, "title" );
        dc.getInfo().getProperties().setProperty( TitleIndex.INDEX_TITLE, TitleIndex.class.getName() );
        dataDC.put( dc );
    }

    /**
     * Creates 'Data' sub collection (repository) in the module
     * @param module Module to create Data collection in
     * @return 'Data' repository
     */
    private Repository initDataSubCollection(Module module) throws Exception
    {
        LocalRepository moduleLR = (LocalRepository)module.getPrimaryCollection();
        Properties dataProperties = new Properties();
        dataProperties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, Module.DATA );
        dataProperties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
        dataProperties.setProperty( LocalRepository.UNPROTECTED_PROPERTY, "true" );
        DataCollection<?> dataDC = moduleLR.createDataCollection( Module.DATA, dataProperties, Module.DATA, null, null );
        Repository dataDCRepo = getElement( dataDC, Repository.class );
        moduleLR.put( dataDCRepo );
        return dataDCRepo;
    }

    /**
     * Creates 'Module' data collection (root)
     * @param parent parent repository
     * @param name name of new module
     * @return created module
     */
    private Module initModule(Repository parent, String name) throws Exception
    {
        Properties primary = new Properties();
        primary.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        primary.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );

        Properties transformed = new Properties();
        transformed.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, UserHubModule.class.getName() );
        transformed.setProperty( Module.TYPE_PROPERTY, UserHubModuleType.class.getName() );

        DataCollection<?> dc = CollectionFactoryUtils.createDerivedCollection( parent, name, primary, transformed, name );
        Module module = getElement( dc, Module.class );
        String pluginsProperty = module.getInfo().getProperties().getProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY );
        if( pluginsProperty == null )
            pluginsProperty = "";
        if( !pluginsProperty.isEmpty() )
            pluginsProperty += ";";
        pluginsProperty += "biouml.plugins.keynodes";
        module.getInfo().getProperties().setProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY, pluginsProperty );
        return module;
    }

    @SuppressWarnings ( "unchecked" )
    private <T> T getElement(DataCollection<?> dc, Class<? extends T> clazz) throws Exception
    {
        if( clazz.isInstance( dc ) )
            return (T)dc;
        else
        {
            return (T)SecurityManager.runPrivileged( () -> {
                return DataCollectionUtils.fetchPrimaryCollectionPrivileged( dc );
            } );
        }
    }
}
