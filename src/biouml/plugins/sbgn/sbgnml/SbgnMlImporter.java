package biouml.plugins.sbgn.sbgnml;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.application.ApplicationUtils;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

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
import biouml.plugins.sbgn.SbgnDiagramViewOptions;

public class SbgnMlImporter extends DiagramImporter
{
    protected static final Logger log = Logger.getLogger( SbgnMlImporter.class.getName() );

    protected int getAcceptPriority()
    {
        //HIGH priority can be used for specific extensions such as CellDesigner importer
        //+1 added to not confuse with tab-separated file
        return ACCEPT_MEDIUM_PRIORITY;
    }

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
            if( iXml == -1 )
            {
                return ACCEPT_UNSUPPORTED;
            }

            if( !header.substring(iXml, iXml + 100 > header.length() ? header.length() : iXml + 100)
                    .matches("(\\s)*<\\?xml(\\s)*version(\\s)*=(.|\\s)*") )
            {
                return ACCEPT_UNSUPPORTED;
            }
            int start = header.indexOf("<sbgn");
            int end = header.indexOf(">", start);

            if( start == -1 || end == -1 )
                return ACCEPT_UNSUPPORTED;
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, "accept error :", t );
        }

        return getAcceptPriority();
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String diagramName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {

        if( properties != null )
            diagramName = properties.getDiagramName();

        DataCollection<?> actualParent = null;
        parent = DataCollectionUtils.fetchPrimaryCollection(parent, Permission.WRITE);
        if( parent instanceof FolderCollection && parent.isAcceptable(Diagram.class) )
        {
            actualParent = parent;
        }
        else
        {
            Module module = ( parent instanceof Module ) ? (Module)parent : Module.optModule(parent);
            if( module != null )
                actualParent = (DataCollection<?>)module.get(Module.DIAGRAM);
        }

        if( jobControl == null )
            return doImport(actualParent, file, diagramName);

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
        DataCollection<?> origin = (DataCollection<?>)module.get(Module.DIAGRAM);
        return doImport(origin, file, diagramName);
    }


    protected DataElement doImport(DataCollection<?> origin, File file, String diagramName) throws Exception
    {
        Diagram diagram = new SbgnMlReader().read(origin, file, diagramName);

        if( properties != null )
        {
            SbgnDiagramViewOptions viewOptions = (SbgnDiagramViewOptions)diagram.getViewOptions();
            viewOptions.setAutoLayout(properties.isEnableAutoLayout());

            if( SbgnMlImportProperties.BLACK_AND_WHITE_SCHEME.equals(properties.getDefaultScheme()) )
                SbgnMlReader.setBlackAndWhite(viewOptions);
        }
        diagram.save();
        return diagram;
    }

    private SbgnMlImportProperties properties;

    @Override
    public SbgnMlImportProperties getProperties(DataCollection parent, File file, String elementName)
    {
        properties = new SbgnMlImportProperties(elementName);
        return properties;
    }

    @PropertyName ( "Import properties" )
    @PropertyDescription ( "Import properties." )
    public static class SbgnMlImportProperties extends Option
    {
        public static final String BIOUML_SCHEME = "BioUML scheme";
        public static final String BLACK_AND_WHITE_SCHEME = "Black & white scheme";

        public String[] getAvailableSchemes()
        {
            return new String[] {BIOUML_SCHEME, BLACK_AND_WHITE_SCHEME};
        }

        protected boolean enableAutoLayout = true;
        protected String diagramName;
        protected String defaultScheme = BLACK_AND_WHITE_SCHEME;

        public SbgnMlImportProperties(String name)
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

        @PropertyName ( "Color scheme" )
        @PropertyDescription ( "Color sheme used by default." )
        public String getDefaultScheme()
        {
            return defaultScheme;
        }
        public void setDefaultScheme(String defaultScheme)
        {
            this.defaultScheme = defaultScheme;
        }
    }

    public static class SbgnMlImportPropertiesBeanInfo extends BeanInfoEx2<SbgnMlImportProperties>
    {
        public SbgnMlImportPropertiesBeanInfo()
        {
            super(SbgnMlImportProperties.class);
        }
        @Override
        public void initProperties() throws Exception
        {
            add("diagramName");
            add("enableAutoLayout");
            property("defaultScheme").tags(bean -> StreamEx.of(bean.getAvailableSchemes())).add();
        }
    }


}
