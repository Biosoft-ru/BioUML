package biouml.plugins.sbml;

import java.io.File;
import java.util.logging.Level;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.security.Permission;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Diagram;
import biouml.model.DiagramImporter;
import biouml.model.Module;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbml.converters.SBGNConverterNew;
import biouml.standard.type.DiagramInfo;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.application.ApplicationUtils;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

public class SbmlImporter extends DiagramImporter
{   
    protected static final Logger log = Logger.getLogger(SbmlImporter.class.getName());
    private SbmlImportProperties properties;   
 
    /**
     * Get maximum access priority for current type
     */
    protected int getAcceptPriority()
    {
        //HIGH priority can be used for specific extensions such as CellDesigner importer
        //+1 added to not confuse with tab-separated file
        return ACCEPT_MEDIUM_PRIORITY + 1;
    }

    /**
     * Returns true if the specified file can be imported into specified ru.biosoft.access.core.DataCollection.
     * @param parent - parent DataCollection (or Module) to import data to
     * @param file - file with data that will be imported to the diagram
     * if file is null then method should check parent only and return true if check passed
     */
    @Override
    public int accept(DataCollection parent, File file)
    {
        if( DataCollectionUtils.isAcceptable(parent, Diagram.class) )
            return file == null ? getAcceptPriority() : accept(file);
        if( ! ( parent instanceof Module ) && ! ( parent instanceof Diagram )
                && ( !parent.getName().equalsIgnoreCase(Module.DIAGRAM) || !parent.isMutable() ) )
            return ACCEPT_UNSUPPORTED;
        Module module = Module.optModule(parent);
        if( module == null )
            return ACCEPT_UNSUPPORTED;
        if( !moduleType.isInstance(module.getType()) )
            return ACCEPT_UNSUPPORTED;
        return file == null ? getAcceptPriority() : accept(file);
    }

    @Override
    public int accept(File file)
    {
        try
        {
            String header = ApplicationUtils.readAsString(file, 2000);

            int iXml = header.indexOf("<?xml");
            if(iXml == -1)
            {
                return ACCEPT_UNSUPPORTED;
            }

            if( !header.substring(iXml, iXml + 100 > header.length() ? header.length() : iXml + 100).matches(
                    "(\\s)*<\\?xml(\\s)*version(\\s)*=(.|\\s)*") )
            {
                return ACCEPT_UNSUPPORTED;
            }

            int start = header.indexOf("<sbml");
            int end = header.indexOf(">", start);

            if( start == -1 || end == -1 )
                return ACCEPT_UNSUPPORTED;

            String str = header.substring(start, end);
            if( checkSBMLVersion(str, log) )
            {
                return getAcceptPriority();
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "accept error :", t);
        }

        return ACCEPT_UNSUPPORTED;
    }


    /**
     * Check SBML level and version
     */
    public static boolean checkSBMLVersion(String str, Logger logger)
    {
        // search for " level" instead of "level" to avoid confusions
        // with with namespace specification
        int level = getAttrValue(str, " level");
        int version = getAttrValue(str, " version");

        String format = "L" + level + "V" + version;
        List<String> formats = Arrays.asList(SbmlConstants.SBML_SUPPORTED_FORMATS);
        if( formats.contains(format) )
        {
            logger.info("Autodetect, SBML level " + level + " version " + version);
            return true;
        }
        return false;
    }

    /**
     * Get value of XML attribute
     */
    protected static int getAttrValue(String str, String attr)
    {
        int ind = str.indexOf(attr);
        if( ind != -1 )
        {
            if( ( ind = str.indexOf("\"", ind + attr.length()) ) != -1 )
            {
                str = str.substring(ind + 1).trim();
                if( ( ind = str.indexOf("\"") ) != -1 )
                    return Integer.parseInt(str.substring(0, ind));
            }
        }
        return -1;
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
        DataCollection<?> actualParent = null;
        parent = DataCollectionUtils.fetchPrimaryCollection(parent, Permission.WRITE);
        if( parent instanceof FolderCollection && parent.isAcceptable( Diagram.class ) )
        {
            actualParent = parent;
        }
        else
        {
            Module module = ( parent instanceof Module ) ? (Module)parent : Module.optModule(parent);
            if( module != null )
                actualParent = (DataCollection<?>)module.get( Module.DIAGRAM );
        }
        if( jobControl == null )
        {
            return doImport(actualParent, file, diagramName);
        }
        jobControl.functionStarted();
        DataElement de = doImport(actualParent, file, diagramName);
        if( jobControl.getStatus() != JobControl.TERMINATED_BY_REQUEST && jobControl.getStatus() != JobControl.TERMINATED_BY_ERROR )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
        return de;
    }


    @Override
    public DataElement doImport(Module module, File file, String diagramName) throws Exception
    {
        DataCollection<?> origin = (DataCollection<?>)module.get( Module.DIAGRAM );
        return doImport(origin, file, diagramName);
    }

    /**
     * @todo Play with diagramName and diagram.getName() -
     * there is something bad
     *
     * @todo Implement clone() method of EModel
     */
    protected DataElement doImport(DataCollection<?> origin, File file, String diagramName) throws Exception
    {
        if( properties != null )
            diagramName = properties.getDiagramName();

        Diagram diagram = SbmlModelFactory.readDiagram(file, origin, diagramName, newPaths);

        Object sbgnDiagramObj = diagram.getAttributes().getValue(SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME);
        Diagram sbgnDiagram = null;
        if( sbgnDiagramObj instanceof Diagram )
        {
            sbgnDiagram = (Diagram)sbgnDiagramObj;
            ( (DiagramInfo)sbgnDiagram.getKernel() ).setDatabaseReferences( ( (DiagramInfo)diagram.getKernel() ).getDatabaseReferences());
            ( (DiagramInfo)sbgnDiagram.getKernel() )
                    .setLiteratureReferences( ( (DiagramInfo)diagram.getKernel() ).getLiteratureReferences());
        }
        else
        {
            sbgnDiagram = SBGNConverterNew.convert(diagram);
        }
        sbgnDiagram.getAttributes()
                .add(new DynamicProperty(SbmlDiagramTransformer.BASE_DIAGRAM_TYPE, String.class, diagram.getType().getClass().getName()));
        diagram = sbgnDiagram;

        diagram.getViewOptions().setAutoLayout(properties == null || properties.enableAutoLayout);
        ((DataCollection)origin).put( diagram );
        return diagram;
    }

    @Override
    public SbmlImportProperties getProperties(DataCollection parent, File file, String elementName)
    {
        properties = new SbmlImportProperties(elementName);
        return properties;
    }

    @PropertyName ( "Import properties" )
    @PropertyDescription ( "Import properties." )
    public static class SbmlImportProperties extends Option
    {
        protected boolean enableAutoLayout = true;
        protected String diagramName;

        public SbmlImportProperties(String name)
        {
            this.diagramName = name;
        }
        @PropertyName ( "Enable Autolayout" )
        @PropertyDescription ( "Enable automatic edges layout. Is not recommended for large diagrams." )
        public boolean isEnableAutoLayout()
        {
            return enableAutoLayout;
        }
        public void setEnableAutoLayout(boolean enableAutoLayout)
        {
            this.enableAutoLayout = enableAutoLayout;
        }
        @PropertyName ( "Diagram name" )
        @PropertyDescription ( "Diagram name." )
        public String getDiagramName()
        {
            return diagramName;
        }
        public void setDiagramName(String diagramName)
        {
            this.diagramName = diagramName;
        }
    }

    public static class SbmlImportPropertiesBeanInfo extends BeanInfoEx2<SbmlImportProperties>
    {
        public SbmlImportPropertiesBeanInfo()
        {
            super(SbmlImportProperties.class, MessageBundle.class.getName());
        }
        @Override
        public void initProperties() throws Exception
        {
            add("diagramName");
            add("enableAutoLayout");
        }
    }

    protected boolean isComposite(Diagram diagram)
    {
        Object packageNames = diagram.getAttributes().getValue( SbmlConstants.PACKAGES_ATTR );
        return packageNames instanceof String[] && StreamEx.of((String[])packageNames).has(SbmlConstants.COMP_PACKAGE);

    }
}
