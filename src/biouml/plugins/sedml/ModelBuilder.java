package biouml.plugins.sedml;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;
import org.jdom.JDOMException;
import org.jdom.input.DOMBuilder;
import org.jdom.output.DOMOutputter;
import org.jlibsedml.ComputeChange;
import org.jlibsedml.Model;
import org.w3c.dom.Document;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.util.TextUtil2;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Node;
import biouml.plugins.cellml.CellMLDiagramType;
import biouml.plugins.cellml.CellMLModelReader;
import biouml.plugins.cellml.CellMLModelWriter;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbml.SbmlDiagramType;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.sbml.SbmlModelWriter;
import biouml.plugins.sbml.converters.SBGNConverterNew;
import biouml.plugins.sbml.converters.SbmlConverter;
import biouml.plugins.sedml.analyses.DownloadModel;
import biouml.plugins.sedml.analyses.DownloadModel.Parameters;
import biouml.plugins.state.analyses.ChangeDiagram;
import biouml.plugins.state.analyses.ChangeDiagramParameters;
import biouml.plugins.state.analyses.StateChange;
import biouml.standard.state.DiagramStateUtility;
import biouml.standard.state.State;
import biouml.standard.state.StatePropertyChangeUndo;

import com.developmentontheedge.beans.DynamicProperty;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class ModelBuilder extends WorkflowBuilder
{
    private static final Logger log = Logger.getLogger( ModelBuilder.class.getName() );

    private Model sedmlModel;
    private DataElementPath modelCollectionPath;

    private Node inputModelNode;
    private Node outputModelNode;

    private Diagram inputDiagram;
    private Diagram outputDiagram;

    private Map<String, Node> availableModelNodes;
    private Map<String, Diagram> availableModels;

    public ModelBuilder(Compartment parent, WorkflowSemanticController controller)
    {
        super( parent, controller );
    }

    public void setModelCollectionPath(DataElementPath modelCollectionPath)
    {
        this.modelCollectionPath = modelCollectionPath;
    }

    public void setInputModelNode(Node inputModelNode)
    {
        this.inputModelNode = inputModelNode;
    }

    public void setInputDiagram(Diagram diagram)
    {
        this.inputDiagram = diagram;
    }

    public void setSedmlModel(Model model)
    {
        this.sedmlModel = model;
    }

    public void setAvailableModelNodes(Map<String, Node> availableModelNodes)
    {
        this.availableModelNodes = availableModelNodes;
    }

    public void setAvailableModels(Map<String, Diagram> availableModels)
    {
        this.availableModels = availableModels;
    }

    public Node getOutputModelNode()
    {
        return outputModelNode;
    }

    public Diagram getOutputDiagram()
    {
        return outputDiagram;
    }

    @Override
    public void build()
    {
        if(inputModelNode == null)
        {
            buildInputModelNode();
            buildInputDiagram();
        }

        if( !sedmlModel.getListOfChanges().isEmpty() )
        {
            buildOutputDiagram();

            ComputeChangesBuilder builder = new ComputeChangesBuilder( parent, controller );
            List<ComputeChange> computeChanges = StreamEx.of( sedmlModel.getListOfChanges() ).select( ComputeChange.class ).toList();
            builder.setChanges( computeChanges );
            builder.setModel( outputDiagram );
            builder.setModelNode( inputModelNode );
            builder.setReferencedModelNodes( availableModelNodes );
            builder.setReferencedModels( availableModels );
            builder.build();
            Compartment analysisNode = builder.getChangeDiagramNode();

            State state = buildState();
            ChangeDiagramParameters parameters = (ChangeDiagramParameters)AnalysisDPSUtils.readParametersFromAttributes( analysisNode.getAttributes() );
            fillChangeDiagramParameters(parameters, state);

            String analysisName = AnalysisMethodRegistry.getAnalysisMethod( ChangeDiagram.class ).getName();
            AnalysisDPSUtils.writeParametersToNodeAttributes( analysisName, parameters, analysisNode.getAttributes() );

            String title = getTitleForSedmlElement( sedmlModel );
            outputModelNode = addDataElementNode( title, "$Output folder$/" + title );
            addDirectedEdge( parent, (Node)analysisNode.get( "outputDiagram" ), outputModelNode );
        }
        else
        {
            outputModelNode = inputModelNode;
            outputDiagram = inputDiagram;
        }
    }

    private static boolean isExternalSource(String source)
    {
        return source.startsWith( "urn:" ) || source.startsWith( "http://" ) || source.startsWith( "ftp://" );
    }

    private void buildInputDiagram()
    {
        String source = sedmlModel.getSource();
        if( isExternalSource( source ) )
        {
            FolderVectorCollection origin = new FolderVectorCollection( "parent", null );
            FunctionJobControl jobControl = new FunctionJobControl( log );
            try
            {
                inputDiagram = DownloadModel.downloadModel( source, "model", origin, jobControl, log );
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
        }
        else
        {
            String modelName = getDiagramNameFromSource( source );
            DataElementPath modelPath = modelCollectionPath.getChildPath( modelName );
            inputDiagram = modelPath.getDataElement( Diagram.class );
        }
        inputDiagram = convertToSBMLDiagram(inputDiagram);
    }

    private void buildInputModelNode()
    {
        String source = sedmlModel.getSource();
        if( isExternalSource( source ) )
        {
            DownloadModel analysis = AnalysisMethodRegistry.getAnalysisMethod( DownloadModel.class );
            Parameters parameters = analysis.getParameters();
            parameters.setSource( source );
            Compartment analysisNode = addAnalysis( analysis );
            if( sedmlModel.getListOfChanges().isEmpty() )
            {
                String title = getTitleForSedmlElement( sedmlModel );
                inputModelNode = addDataElementNode( title, "$Output folder$/" + title );
                addDirectedEdge( parent, (Node)analysisNode.get( "outputPath" ), inputModelNode );
            }
            else
                inputModelNode = (Node)analysisNode.get( "outputPath" );
        }
        else
        {
            if( modelCollectionPath == null || !modelCollectionPath.exists() )
                throw new IllegalArgumentException( "Can not resolve model id=" + sedmlModel.getId() + " source=" + source );
            String modelName = getDiagramNameFromSource( source );
            DataElementPath modelPath = modelCollectionPath.getChildPath( modelName );
            String title = getTitleForSedmlElement( sedmlModel );
            inputModelNode = addDataElementNode( title, modelPath.toString() );
        }
    }

    private void fillChangeDiagramParameters(ChangeDiagramParameters parameters, State state)
    {
        Stream<StateChange> stateChanges = StreamEx.of(state.getStateUndoManager().getEditsFlat()).select( StatePropertyChangeUndo.class )
            .filter( undo -> undo.getPropertyName().startsWith( "role/vars/" ) )
            .filter( undo -> undo.getSource() instanceof DiagramElement )
            .map( undo -> {
                StateChange stateChange = new StateChange();
                stateChange.setElementId( ( (DiagramElement)undo.getSource() ).getCompleteNameInDiagram() );
                stateChange.setElementProperty( undo.getPropertyName() );
                stateChange.setPropertyValue( TextUtil2.toString( undo.getNewValue() ) );
                return stateChange;
            });
        parameters.setChanges( Stream.concat( Stream.of( parameters.getChanges() ), stateChanges ).toArray( StateChange[]::new ) );
    }

    private Diagram convertToSBMLDiagram(Diagram diagram)
    {
        DiagramType type = diagram.getType();
        try
        {
            if( type instanceof SbgnDiagramType )
            {
                Diagram result = SBGNConverterNew.restore( diagram );
                result.getAttributes().add( new DynamicProperty( SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME, Diagram.class, diagram ) );
                return result;
            }
            else if( ! ( type instanceof SbmlDiagramType || type instanceof CellMLDiagramType ) )
            {
                Diagram result = diagram.clone( diagram.getOrigin(), diagram.getName() );
                return new SbmlConverter().convert( result, null );
            }
            else
                return diagram;
        }
        catch( Exception e )
        {
            throw new RuntimeException( "Can not convert diagram type : " + type.toString() + " to SBML", e );
        }
    }

    private static String getDiagramNameFromSource(String source)
    {
        String name;
        if( source.contains( "/" ) )
            name = source.substring( source.lastIndexOf( "/" ) + 1 );
        else if( source.contains( "\\" ) )
            name = source.substring( source.lastIndexOf( "\\" ) + 1 );
        else
            name = source;
        if( name.endsWith( ".xml" ) ) //model is already imported in BioUML, it does not have file extension in its name
            name = name.substring( 0, name.length() - 4 );
        return name;
    }

    private State buildState()
    {
        try
        {
            return DiagramStateUtility.createState(inputDiagram, outputDiagram, "State of " + sedmlModel.getId());
        }
        catch( Exception e )
        {
            throw new RuntimeException("Can not create state for " + sedmlModel.getId() + " model changes", e);
        }
    }

    private void buildOutputDiagram()
    {
        try
        {
            Document document = createDocument(inputDiagram);
            org.jdom.Document modelDOM = w3cdomToJdomDocument(document);
            SedmlUtils.applySedMlChanges(sedmlModel, modelDOM);
            Document changedDocument = jdomToW3cdomDocument(modelDOM);
            outputDiagram = createDiagram(sedmlModel.getLanguage(), sedmlModel.getId(), changedDocument);
        }
        catch( Exception e )
        {
            throw new RuntimeException("Can not apply changes for " + sedmlModel.getId(), e);
        }
    }

    private static org.jdom.Document w3cdomToJdomDocument(Document doc)
    {
        return (new DOMBuilder()).build(doc);
    }

    private static Document jdomToW3cdomDocument(org.jdom.Document doc) throws JDOMException
    {
        return (new DOMOutputter()).output(doc);
    }

    /**
     * Creates Diagram by DOM Document
     * Supports SBML and CellML so far
     * @param language language
     * @param name name of the Diagram
     * @param document document to create diagram by
     * @return created Diagram
     * @throws Exception in case of failure
     */
    protected Diagram createDiagram(String language, String name, Document document) throws Exception
    {
        Diagram diagram = null;
        if(language.toLowerCase().contains("sbml"))
            diagram = SbmlModelFactory.readDiagram(document, name, null, name, null);
        else if(language.toLowerCase().contains("cellml"))
            diagram = (new CellMLModelReader(name, document)).read(null);
        else
            throw new Exception("Unknown language: "+language);
        if(diagram == null) throw new Exception("Unable to read diagram");
        return diagram;
    }

    /**
     * Creates DOM Document by Diagram
     * Supports SBML and CellML so far (CellML writer is not completed yet)
     * @param diagram Diagram
     * @return created Document
     * @throws Exception in case of failure
     */
    protected Document createDocument(Diagram diagram) throws Exception
    {
        DiagramType type = diagram.getType();
        Document document = null;
        if(type instanceof SbmlDiagramType)
        {
            SbmlModelWriter writer = SbmlModelFactory.getWriter( diagram );
            writer.setWriteBioUMLAnnotation( false );
            document = SbmlModelFactory.createDOM(diagram, writer );
        }
        else if(type instanceof CellMLDiagramType)
        {
            document = CellMLModelWriter.createDOM(diagram);
        }
        else
        {
            throw new Exception("");
        }
        if(document == null) throw new Exception("Unable to create diagram document");
        return document;
    }
}
