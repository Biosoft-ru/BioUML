package biouml.plugins.sedml;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.Properties;

import javax.annotation.Nonnull;

import java.util.logging.Logger;
import org.jlibsedml.Libsedml;
import org.jlibsedml.SEDMLDocument;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.util.ExProperties;
import biouml.model.Diagram;
import biouml.workbench.graph.DiagramToGraphTransformer;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.application.ApplicationUtils;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class SedmlImporter implements DataElementImporter
{
    protected static final Logger log = Logger.getLogger(SedmlImporter.class.getName());

    protected ImportProperties properties;

    @Override
    public int accept(DataCollection parent, File file)
    {
        if( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable(parent, getResultType()) )
            return ACCEPT_UNSUPPORTED;
        return ( file == null || isSedMLFile(file) ) ? ACCEPT_HIGH_PRIORITY : ACCEPT_UNSUPPORTED;
    }

    private boolean isSedMLFile(File file)
    {
        try
        {
            String header = ApplicationUtils.readAsString(file, 1000);
            if( header.indexOf("<?xml") == -1 )
                return false;
            if( header.indexOf("<sedML") == -1 )
                return false;
            return true;
        }
        catch( IOException e )
        {
        }
        return false;
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String elementName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();

        if( parent.contains(elementName) )
            parent.remove(elementName);

        try
        {
            Diagram diagram = createSedMlWorkflow(parent, file, elementName, jobControl);
            if( diagram != null )
            {
                parent.put(diagram);
                String plugins = ExProperties.getPluginsString(parent.getInfo().getProperties(), "biouml.plugins.sedml");
                parent.getInfo().writeProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, plugins);
            }
            if( jobControl != null )
            {
                jobControl.setPreparedness(100);
                jobControl.functionFinished();
                jobControl.resultsAreReady( new Object[] {diagram} );
            }
            return diagram;
        }
        catch( Exception e )
        {
            if( jobControl != null )
                jobControl.functionTerminatedByError(e);
        }
        return null;
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

    @Override
    public ImportProperties getProperties(DataCollection parent, File file, String elementName)
    {
        try
        {
            properties = new ImportProperties(file);
            return properties;
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error while attempting to create Import properties: " + ex.getMessage());
            return null;
        }
    }

    private Diagram createSedMlWorkflow(DataCollection parent, File file, String diagramName, FunctionJobControl jobControl)
            throws Exception
    {
        SEDMLDocument sedml = Libsedml.readDocument( file );
        DataElementPath workflowPath = DataElementPath.create( parent, diagramName );
        SedmlToWorkflowConverter converter = new SedmlToWorkflowConverter( workflowPath, sedml, properties.getModelCollectionPath() );
        Diagram workflow = converter.convertToWorkflow( jobControl );
        layoutDiagram( workflow );
        return workflow;
    }


    private void layoutDiagram(Diagram diagram)
    {
        log.info("Layouting diagram " + diagram.getName());
        HierarchicLayouter layouter = new HierarchicLayouter();
        layouter.setHoistNodes(true);
        layouter.getSubgraphLayouter().layerDeltaY = 50;
        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);
        pathwayLayouter.doLayout(graph, null);
        DiagramToGraphTransformer.applyLayout(graph, diagram);
        diagram.setView(null);
    }

    public static class ImportProperties extends Option
    {
        private DataElementPath modelCollectionPath;

        public ImportProperties(File file) throws IOException
        {
        }

        public DataElementPath getModelCollectionPath()
        {
            return modelCollectionPath;
        }

        public void setModelCollectionPath(DataElementPath modelCollectionPath)
        {
            Object oldValue = this.modelCollectionPath;
            this.modelCollectionPath = modelCollectionPath;
            firePropertyChange("modelCollectionPath", oldValue, modelCollectionPath);
        }
    }

    public static class ImportPropertiesBeanInfo extends BeanInfoEx
    {
        public ImportPropertiesBeanInfo()
        {
            super(ImportProperties.class, MessageBundle.class.getName());
            beanDescriptor.setDisplayName(MessageBundle.PN_IMPORT_PROPERTIES);
            beanDescriptor.setShortDescription(MessageBundle.PD_IMPORT_PROPERTIES);
        }
        @Override
        public void initProperties() throws Exception
        {
            PropertyDescriptorEx pde = DataElementPathEditor.registerInputChild("modelCollectionPath", beanClass, Diagram.class, true);
            add(pde, MessageBundle.PN_INPUT_MODELS, MessageBundle.PD_INPUT_MODELS);
        }
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return Diagram.class;
    }
}
