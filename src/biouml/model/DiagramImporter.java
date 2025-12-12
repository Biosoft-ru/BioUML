package biouml.model;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import biouml.standard.StandardModuleType;

import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
//import ru.biosoft.access.DataElementRegistry;
//import ru.biosoft.exception.ExceptionRegistry;

/** General interface to import diagram, for example from SBML or CellML file. */
public abstract class DiagramImporter implements DataElementImporter
{
    public static final String DATABASE_TYPE = "moduleType";
    protected Class<? extends ModuleType> moduleType = null;
    protected Map<String, String> newPaths;
    
    public void setNewPaths(Map<String, String> newPaths)
    {
        this.newPaths = newPaths;
    }
    
    /**
     * Returns true if the specified file can be imported.
     * @param file - file with data that will be imported to the diagram
     */
    public abstract int accept(File file);

    /**
     * Imports diagram from the specified file into the specified module.
     * @param module - module where the imported diagram will be located
     * @param file - data file to be imported (for example file with SBML or CellML model)
     * @param diagramName - name of the diagram in the module.
     */
    public abstract ru.biosoft.access.core.DataElement doImport(Module module, File file, String diagramName) throws Exception;

    /**
     * Returns true if the specified file can be imported into specified ru.biosoft.access.core.DataCollection.
     * @param parent - parent DataCollection (or Module) to import data to
     * @param file - file with data that will be imported to the diagram
     * if file is null then method should check parent only and return true if check passed
     */
    @Override
    public int accept(DataCollection parent, File file)
    {
        if( ! ( parent instanceof Module ) && ! ( parent instanceof Diagram )
                && ( !parent.getName().equalsIgnoreCase(Module.DIAGRAM) || !parent.isMutable() ) )
            return ACCEPT_UNSUPPORTED;
        Module module = Module.optModule(parent);
        if( module == null )
            return ACCEPT_UNSUPPORTED;
        if( !moduleType.isInstance(module.getType()) )
            return ACCEPT_UNSUPPORTED;
        return file == null ? ACCEPT_HIGH_PRIORITY : accept(file);
    }

    /**
     * Imports diagram from the specified file into the specified module.
     * @param parent - parent DataCollection (or Module) to import data to
     * @param file - data file to be imported (for example file with SBML or CellML model)
     * @param diagramName - name of the diagram in the module.
     */
    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String diagramName, FunctionJobControl jobControl, Logger log) throws Exception
    {
        Module module = Module.optModule(parent);
        if( jobControl == null )
        {
            return doImport(module, file, diagramName);
        }
        jobControl.functionStarted();
        DataElement de = doImport(module, file, diagramName);
        if( jobControl.getStatus() != JobControl.TERMINATED_BY_REQUEST && jobControl.getStatus() != JobControl.TERMINATED_BY_ERROR )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
        return de;
    }

    @Override
    public Object getProperties(DataCollection parent, File file, String elementName)
    {
        return null;
    }

    /**
     * Initializes importer
     * @param properties - properties from <export> block in plugin.xml
     * @return true if everything is ok and importer can be registered; false otherwise
     */
    @Override
    public boolean init(Properties properties)
    {
        String moduleTypeName = properties.getProperty(DATABASE_TYPE);
        moduleType = null;
        if( moduleTypeName != null )
        {
            try
            {
//                moduleType = ClassLoading.loadSubClass( moduleTypeName, properties.getProperty(DataElementRegistry.PLUGIN_ID), ModuleType.class );
            }
            catch( Exception e )
            {
//                ExceptionRegistry.log(e);
                return false;
            }
        }
        else
        {
            moduleType = StandardModuleType.class;
        }
        return true;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return Diagram.class;
    }
}
