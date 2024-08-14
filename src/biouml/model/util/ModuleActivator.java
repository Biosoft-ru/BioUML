package biouml.model.util;

import java.util.jar.JarFile;

import javax.swing.JDialog;

import java.util.logging.Logger;

import ru.biosoft.access.CreateDataCollectionController;
import ru.biosoft.access.Repository;
import biouml.model.Module;

/**
 * This class is used to activate the module during the process of module instalation.
 *
 * Examples of module activation actions are: <ul>
 * <li> request module key if module is not free </li>
 * <li> copy some additional data in repository.
 *   <br>For example, we distribute only Java code for GeneNet module,
 *   while GeneNet data is copied by module activator from MGL server.</li>
 * </ul>
 *
 * Constants {@link CREATE_DATA_COLLECTION_BEFORE}, {@link CREATE_DATA_COLLECTION_AFTER} and
 * {@link CREATE_DATA_COLLECTION_NEVER} indicates when corresponding data collection should be
 * created.
 *
 * @pending refine comments
 * @pending rename to ModucleActivator
 */
public interface ModuleActivator
{
    /**
     * This consatant indicates that first the data collection corresponding to module
     * packed in bmd file will be created, after that <code>createModule</code> will be called.
     */
    public static final int CREATE_DATA_COLLECTION_BEFORE = 1;

    /**
     * This consatant indicates that first <code>createModule</code> will be called
     * and after that data collection corresponding to module packed in bmd file
     * will be created.
     */
    public static final int CREATE_DATA_COLLECTION_AFTER = 2;

    /**
     * This consatant indicates that <code>createModule</code> completely
     * responsible for creation of data collection corresponding to module packed in bmd file.
     */
    public static final int CREATE_DATA_COLLECTION_NEVER = 3;

    /**
     * Creates the module with initialised internal structure from moduleFile .
     */
    public Module createModule(JDialog owner, Repository parent, JarFile moduleFile, String classpath, Logger log,  CreateDataCollectionController controller) throws Exception;

    /**
     * Indicates when module data collection should be created by {@link ModulePackager}.
     */
    public int whenCreateDataCollection();
}
