package biouml.standard;

import java.util.jar.JarFile;

import javax.swing.JDialog;

import java.util.logging.Logger;

import ru.biosoft.access.CreateDataCollectionController;
import ru.biosoft.access.Repository;
import biouml.model.Module;
import biouml.model.util.ModuleActivator;
import biouml.model.util.ModulePackager;

public class StandardModuleActivator implements ModuleActivator
{
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
    public Module createModule(JDialog parentWindow, Repository parent, JarFile moduleFile, String classpath, Logger log, CreateDataCollectionController controller) throws Exception
    {
        throw new UnsupportedOperationException("Creation of Standard module is not implemented.");
    }
}
