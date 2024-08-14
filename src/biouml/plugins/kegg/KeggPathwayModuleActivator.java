package biouml.plugins.kegg;

import java.text.MessageFormat;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import javax.swing.JDialog;

import biouml.model.Module;
import biouml.model.util.ModuleActivator;
import biouml.model.util.ModulePackager;
import biouml.plugins.kegg.type.access.CompoundTransformer;
import biouml.plugins.kegg.type.access.EnzymeTransformer;
import biouml.plugins.kegg.type.access.ReactionTransformer;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.CreateDataCollectionController;
import ru.biosoft.access.FileEntryCollection2;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
public class KeggPathwayModuleActivator implements ModuleActivator
{
    private static MessageBundle messageBundle = new MessageBundle();

    /**
     * Indicates when module data collection should be created by {@link ModulePackager}.
     */
    @Override
    public int whenCreateDataCollection()
    {
        return CREATE_DATA_COLLECTION_NEVER;
    }

    /**
     * Creates the module with initialised internal structure from moduleFile .
     */
    @Override
    public Module createModule(JDialog parentWindow, Repository parent, JarFile moduleFile, String classpath, Logger log,  CreateDataCollectionController controller) throws Exception
    {
        Module module = null;
        String moduleName = ModulePackager.getModuleAttribute(moduleFile, ModulePackager.MF_DATABASE_NAME);

        // Create primary data collection (root)
        log(log, "CREATING_DATABASE", moduleName);
        Properties props = new Properties();
        props.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, moduleName );
        props.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName());
        log(log, "CREATING_DATABASE_SUCCESS", moduleName);

        log(log, "CREATING_DATABASE_TYPE", moduleName);
        String moduleTypeClassName = ModulePackager.getModuleAttribute(moduleFile, ModulePackager.MF_DATABASE_TYPE_CLASS);
        Properties moduleProps = new Properties();
        moduleProps.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, moduleName );
        moduleProps.setProperty(Module.TYPE_PROPERTY, moduleTypeClassName);
        moduleProps.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, Module.class.getName() );
        moduleProps.setProperty( DataCollectionConfigConstants.NEXT_CONFIG, DataCollectionConfigConstants.DEFAULT_REPOSITORY );
        moduleProps.setProperty( DataCollectionConfigConstants.CLASSPATH_JAR_PROPERTY, classpath );
        moduleProps.setProperty(DataCollectionConfigConstants.NODE_IMAGE, "kegg.gif");
        module = (Module)CollectionFactoryUtils.createDerivedCollection(parent, moduleName, props, moduleProps, moduleName);
        Repository repository = (Repository)module.getPrimaryCollection();
        log(log, "CREATING_DATABASE_TYPE_SUCCESS", moduleName);

        // create data
        log(log, "CREATING_ITEM", Module.DATA);
        Repository dataDC = CollectionFactoryUtils.createLocalRepository(repository, Module.DATA);
        log(log, "CREATING_ITEM_SUCCESS", Module.DATA);

        // fill data
        createDataCollection("compound", "compound",    "ENTRY", "///", "ENTRY", dataDC, CompoundTransformer.class, FileEntryCollection2.class, controller, log);
        createDataCollection("enzyme",   "enzyme",      "ENTRY", "///", "ENTRY", dataDC, EnzymeTransformer.class,   FileEntryCollection2.class, controller, log);
        createDataCollection("reaction", "reaction.lst",     "",    "",      "", dataDC, ReactionTransformer.class, FileEntryCollection2.class, controller, log);

        // create diagrams
        log(log, "CREATING_ITEM", Module.DIAGRAM);
//        CollectionFactory.createTransformedFileCollection(parent, Module.DIAGRAM, "", DiagramXmlTransformer.class);
        log(log, "CREATING_ITEM_SUCCESS", Module.DIAGRAM);

        /////////////////////////////////////////////////////////////
        // importing diagrams from KEGG format into BioUML format

        return module;
    }

    protected void createDataCollection(String name, String fileName,
                                        String entryStart, String entryEnd, String entryId,
                                        Repository dataDC,
                                        Class transformer, Class collection,
                                        CreateDataCollectionController controller,
                                        Logger log) throws Exception
    {
        log(log, "IMPORTING_ITEM", name);

        DataCollection dc = CollectionFactoryUtils.createTransformedCollection(
                                dataDC, name, transformer, null,
                                null, null, // name+".gif", name+"_lib.gif",
                                "", entryStart, entryId, entryEnd, null);

        log(log, "IMPORTING_ITEM_SUCCESS", name);
    }

    protected void log(Logger log, String param1)
    {
        String message = messageBundle.getString(param1);
        log.info(message);
    }

    protected void log(Logger log, String param1, String param2)
    {
        String message = MessageFormat.format(messageBundle.getString(param1), new Object[]{param2});
        log.info(message);
    }

    protected void log(Logger log, String param1, String param2, String param3)
    {
        String message = MessageFormat.format(messageBundle.getString(param1), new Object[]{param2, param3});
        log.info(message);
    }
}

