package biouml.plugins.simulation.document;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.Variable;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.simulation.ResultListener;
import biouml.standard.type.BaseSupport;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollection;

/**
 * @author axec
 *
 */
public class InteractiveSimulation extends BaseSupport implements ResultListener
{
    private Diagram diagram;
    private VectorDataCollection<InputParameter> inputParameters;
    private SimulationEngine engine;
    private Model model;
    private Map<String, Integer> parameterMapping; //TODO: upgrade to use with composite models
    private double[] inputValues;
    private Set<String> outputNames;
    private Map<String, double[]> simulatedValues;
    private int spanLength;
    private int spanIndex;
    public InteractiveSimulationDocument doc;//TODO: make listener?
    private static final Logger log = Logger.getLogger( InteractiveSimulation.class.getName() );
    private double[] x;

    private PlotsInfo plots;
    
    public InteractiveSimulation(DataCollection parent, String name, Diagram diagram) throws Exception
    {
        super( parent, name );
        this.inputParameters = new VectorDataCollection<InputParameter>( "inputParameters" );
        engine = DiagramUtility.getEngine( diagram );
        engine = engine.clone();
        engine.setDiagram( diagram );
        engine.setLogLevel( Level.SEVERE );
        EModel emodel = diagram.getRole( EModel.class );
        emodel.detectVariableTypes(); //it may take a while
        for( Variable var : emodel.getVariables() )
        {
            if( !var.getName().startsWith( "$$" ) && !var.getType().equals( Variable.TYPE_CALCULATED ) )
                inputParameters.put( new InputParameter( var.getName(), var.getTitle(), var.getType(), var.getInitialValue() ) );
        }

        this.diagram = diagram;
        this.plots = DiagramUtility.getPlotsInfo( diagram );
        if (plots == null)
            plots = new PlotsInfo(emodel);
        plots = plots.clone( emodel );
        
        model = engine.createModel();
        if( model == null )
        {
            log.info( "Model was not generated!" );
            return;
        }
        model.init();

        inputValues = model.getCurrentValues();
        parameterMapping = engine.getVarPathIndexMapping();
    }
    
    public void doSimulation()
    {
        updateSpan();
        try
        {            
            if( model == null )
                model = engine.createModel();

            model.init();
            model.setCurrentValues( inputValues );

            engine.simulate( model, new ResultListener[] {this} );

            if( doc != null )
            doc.propertyChange( null );//TODO: change to listener?
        }
        catch( Exception ex )
        {
            log.log( java.util.logging.Level.SEVERE, "Error during simulation " + ex.getMessage(), ex );
        }
    }

    public void updateValue(InputParameter parameter)
    {
        int index = parameterMapping.get( parameter.getName() );
        inputValues[index] = parameter.getValue();
    }

    public void updateValues()
    {
        for( InputParameter p : inputParameters )
        {
            if (!parameterMapping.containsKey( p.getName() ))
                continue;
            int index = parameterMapping.get( p.getName() );
            inputValues[index] = p.getValue();
        }
    }

    public DataCollection<InputParameter> getInputParameters()
    {
        return inputParameters;
    }

    public Diagram getDiagram()
    {
        return diagram;
    }

    public SimulationEngine getEngine()
    {
        return engine;
    }

    @Override
    public void start(Object model)
    {
        simulatedValues = new HashMap<>();
        for( String name : outputNames )
            simulatedValues.put( name, new double[spanLength] );
        simulatedValues.put( "time", x );
        spanIndex = 0;
    }

    @Override
    public void add(double t, double[] y) throws Exception
    {
        for( String name : outputNames )
        {
            int index = this.parameterMapping.get( name );
            simulatedValues.get( name )[spanIndex] = y[index];
        }
        spanIndex++;
    }

    public void setOutputNames(Set<String> names)
    {
        this.outputNames = names;
    }

    public Map<String, double[]> getResult()
    {
        return simulatedValues;
    }
    
    public PlotsInfo getPlots()
    {
        return plots;
    }

    public void saveParametersToDiagram()
    {
        EModel emodel = diagram.getRole( EModel.class );
        for( InputParameter parameter : this.inputParameters )
        {
            Variable var = emodel.getVariable( parameter.getName() );
            if( var == null )
                log.info( "Can not find variable " + parameter.getName() + " in diagram " + diagram.getName() );
            else
                var.setInitialValue( parameter.getValue() );
            parameter.setDefaultValue( parameter.getValue() );
        }
        try
        {
            diagram.save();
        }
        catch( Exception ex )
        {
            log.log( java.util.logging.Level.SEVERE, "Error during diagram saving " + ex.getMessage(), ex );
        }
    }

    public void resetParameters()
    {
        EModel emodel = diagram.getRole( EModel.class );
        for( InputParameter inputParameter : inputParameters )
        {
            Variable var = emodel.getVariable( inputParameter.getName() );
            double value = var.getInitialValue();
            inputParameter.setValue( value );
            inputParameter.setValueStep( value / 10 );
            
            int index = parameterMapping.get( inputParameter.getName() );
            inputValues[index] = inputParameter.getValue();
        }       
    }
    
    public void resetParameter(InputParameter parameter)
    {
        EModel emodel = diagram.getRole( EModel.class );
        Variable var = emodel.getVariable( parameter.getName() );
        double value = var.getInitialValue();
        parameter.setValue( value );
        parameter.setValueStep( value / 10 );
        
        int index = parameterMapping.get( parameter.getName() );
        inputValues[index] = parameter.getValue();
    }

    public InputParameter getParameter(String parameterName)
    {
        return inputParameters.get( parameterName );
    }
    
    public void updatePlots()
    {
        doc.updatePlots();
    }
    
    public void updateSpan()
    {
        ArraySpan span = new ArraySpan( 0, engine.getCompletionTime(), engine.getTimeIncrement() );
        spanLength = span.getLength();
        x = span.getTimes();
    }
    
    public void recompile() throws Exception
    {
        this.diagram = diagram.getCompletePath().getDataElement( Diagram.class );
        model = engine.createModel();
        model.init(); 
        
        this.inputParameters = new VectorDataCollection<InputParameter>( "inputParameters" );
        engine.setDiagram( diagram );
        engine.setLogLevel( Level.SEVERE );
        EModel emodel = diagram.getRole( EModel.class );
        emodel.detectVariableTypes(); //it may take a while
        for( Variable var : emodel.getVariables() )
        {
            if( !var.getName().startsWith( "$$" ) && !var.getType().equals( Variable.TYPE_CALCULATED ) )
                inputParameters.put( new InputParameter( var.getName(), var.getTitle(), var.getType(), var.getInitialValue() ) );
        }

        this.plots = DiagramUtility.getPlotsInfo( diagram ).clone( emodel );
        if (plots == null)
            plots = new PlotsInfo(emodel);
        
        model = engine.createModel();
        model.init();

        inputValues = model.getCurrentValues();
        parameterMapping = engine.getVarPathIndexMapping();
    }
}
