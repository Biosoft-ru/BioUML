package biouml.plugins.virtualcell.diagram;

import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.util.ModelXmlReader;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.XmlUtil;

public class VirtualCellModelReader extends ModelXmlReader
{
    public VirtualCellModelReader(Diagram diagram)
    {
        super( diagram );
    }

    @Override
    public EModel readModel(Element element)
    {
        EModel model = null;
        try
        {
            String className = element.getAttribute( MODEL_CLASS_ATTR );
            Class<? extends EModel> clazz = ClassLoading.loadSubClass( className, EModel.class );
            model = clazz.getConstructor( DiagramElement.class ).newInstance( diagram );
            diagram.setRole( model );
            readProcesses( element, model );
            readPools( element, model );
        }
        catch( Throwable t )
        {
            error( "ERROR_EXECUTABLE_MODEL", new String[] {diagram.getName(), t.getMessage()}, t );
        }
        if( model != null )
            model.setPropagationEnabled( true );
        return model;
    }

    protected void readProcesses(Element modelElement, EModel emodel)
    {
        Element processesElement = XmlUtil.findElementByTagName( modelElement, "processes" );
        if( processesElement == null )
            return;

        for( Element processElement : XmlUtil.elements( processesElement ) )
        {
            String name = processElement.getAttribute( "name" );
            String tag = processElement.getTagName();

            Node node = emodel.getParent().findNode( name );

            switch( tag )
            {
                case "translation":
                    TranslationProperties translationproperties = new TranslationProperties( name );
                    String translationRates = processElement.getAttribute( "translation_rates" );
                    if( !translationRates.isEmpty()  )
                        translationproperties.setTranslationRates( DataElementPath.create( translationRates ) );
                    translationproperties.setDiagramElement( node );
                    node.setRole( translationproperties );
                    break;
                case "protein_degradation":
                    ProteinDegradationProperties proteinDegradationProperties = new ProteinDegradationProperties( name );
                    String proteindegradationRates = processElement.getAttribute( "degradation_rates" );
                    if( !proteindegradationRates.isEmpty()  )
                        proteinDegradationProperties.setDegradationRates( DataElementPath.create( proteindegradationRates ) );
                    proteinDegradationProperties.setDiagramElement( node );
                    node.setRole( proteinDegradationProperties );
                    break;
                case "population":
                    PopulationProperties populationProperties = new PopulationProperties( name );
                    String coefficients = processElement.getAttribute( "coefficients" );
                    if( !coefficients.isEmpty()  )
                        populationProperties.setCoeffs( DataElementPath.create( coefficients ) );
                    populationProperties.setDiagramElement( node );
                    node.setRole( populationProperties );
                    break;
                case "metabolism":
                    MetabolismProperties metabolismProperties = new MetabolismProperties( name );
                    String model = processElement.getAttribute( "model" );
                    if( !model.isEmpty()  )
                        metabolismProperties.setDiagramPath( DataElementPath.create( model ) );
                    String table = processElement.getAttribute( "table" );
                    if( !table.isEmpty()  )
                        metabolismProperties.setTablePath( DataElementPath.create( table ) );
                    metabolismProperties.setDiagramElement( node );
                    node.setRole( metabolismProperties );
                    break;
                case "transcription":
                    TranscriptionProperties transcriptionProperties = new TranscriptionProperties( name );
                    String tfs = processElement.getAttribute( "transcriptionFactors" );
                    if( !tfs.isEmpty()  )
                        transcriptionProperties.setTranscriptionFactors( DataElementPath.create( tfs ) );         
                    transcriptionProperties.setDiagramElement( node );
                    
                    String line = processElement.getAttribute( "line" );
                    transcriptionProperties.setLine( line );
                    
                    String modelName = processElement.getAttribute( "model" );
                    transcriptionProperties.setModel( modelName );
                    node.setRole( transcriptionProperties );
                    break;
            }
        }

    }

    protected void readPools(Element modelElement, EModel emodel)
    {
        Element poolsElement = XmlUtil.findElementByTagName( modelElement, "pools" );
        if( poolsElement == null )
            return;
        for( Element poolElement : XmlUtil.elements( poolsElement, "pool" ) )
        {
            String name = poolElement.getAttribute( "name" );
            Node node = emodel.getParent().findNode( name );
            TableCollectionPoolProperties role = new TableCollectionPoolProperties( name );
            role.setDiagramElement( node );
            String path = poolElement.getAttribute( "path" );
            if( !path.isEmpty() )
                role.setPath( DataElementPath.create( path ) );
            
            String isSaved = poolElement.getAttribute( "should_be_saved");
            if (isSaved.equals( "true" ))
                role.setShouldBeSaved( true );
            
            String step = poolElement.getAttribute( "save_step");
            if (!step.isEmpty())
                role.setSaveStep( Double.parseDouble( step ) );
            node.setRole( role );
        }
    }

}