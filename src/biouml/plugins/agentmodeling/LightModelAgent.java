package biouml.plugins.agentmodeling;

import java.util.logging.Level;

import java.util.ArrayList;
import java.util.Map;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Simulator;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.standard.simulation.ResultListener;

/**
 * SimulateableAgent with interchangeable simulator
 * i.e. simulator is set from outside at each step and is initialized again 
 * @author Ilya
 *
 */
public class LightModelAgent extends SimulationAgent
{
    protected JavaBaseModel model;
    protected ArrayList<ResultListener> listeners = new ArrayList<>();
    protected double[] currentValues;
    protected double[] yValues;
    private double[] updatedValues;
    protected boolean updatedFromOutside = false;
    protected Map<String, Integer> allVariables;
    private int nextSpanIndex = 1;
    private String error;

    //TEST issues
    //        public void writeResults() throws Exception
    //        {
    //            String path = "C:/Users/Damag/results/" + name + "_result.txt";
    //            try (BufferedWriter bw = ApplicationUtils.utfAppender( path ))
    //            {
    //                bw.append( DoubleStreamEx.of( currentValues ).prepend( currentTime ).joining( "\t" ) + "\n" );
    //            }
    //        }

    public LightModelAgent(SimulationEngine engine) throws Exception
    {
        this( engine, engine.getDiagram().getName() );
    }

    public LightModelAgent(SimulationEngine engine, String name) throws Exception
    {
        this( engine.createModel(), new UniformSpan( engine.getInitialTime(), engine.getCompletionTime(), engine.getTimeIncrement() ), name,
                engine.getListeners() );
        this.allVariables = engine.getVarIndexMapping();
    }

    public LightModelAgent(Model model, Span span, String name, ResultListener ... listeners) throws Exception
    {
        super( name, span );
        this.model = (JavaBaseModel)model;

        if( !model.isInit() )
            this.model.init();

        currentValues = model.getCurrentValues();
        updatedValues = currentValues.clone();

        for( ResultListener listener : listeners )
            this.listeners.add( listener );
    }

    public void setMapping(Map<String, Integer> mapping)
    {
        this.allVariables = mapping;
    }

    @Override
    public void init() throws Exception
    {
        super.init();

        if( !model.isInit() )
            this.model.init();
        currentValues = model.getCurrentValues();
        yValues = model.getInitialValues().clone();
        updatedValues = currentValues.clone();
        nextSpanIndex = 1;
    }

    @Override
    public void iterate()
    {

    }

    public void iterate(Simulator simulator)
    {
        try
        {
            if( isAlive )
            {
                if( updatedFromOutside )
                {
                    model.setCurrentValues( currentValues );
                    yValues = model.getY();
                }
                simulator.init( model, yValues, span, listeners.toArray( new ResultListener[listeners.size()] ),
                        new FunctionJobControl( log ) );
                while( simulator.getProfile().getTime() < span.getTime( nextSpanIndex ) && isAlive )
                    isAlive = simulator.doStep();
                nextSpanIndex++;
                currentValues = model.getCurrentValues();
                currentTime = simulator.getProfile().getTime();
                error = simulator.getProfile().getErrorMessage();
                updatedFromOutside = false;
                this.yValues = model.getY();
                //                this.writeResults();
                if( nextSpanIndex >= span.getLength() )
                    isAlive = false;
            }
        }
        catch( Exception ex )
        {
            log.log( Level.SEVERE, ex.getMessage() );
            currentTime = Double.POSITIVE_INFINITY;
        }
    }

    @Override
    public void addVariable(String name) throws Exception
    {
        if( !allVariables.containsKey( name ) )
            throw new Exception( "Unknown variable " + name + " in agent " + this.name );
    }

    @Override
    public void addVariable(String name, double value) throws Exception
    {
        if( !allVariables.containsKey( name ) )
            throw new Exception( "Unknown variable " + name + " in agent " + this.name );
        int index = allVariables.get( name );
        this.model.getCurrentValues()[index] = value;
    }

    @Override
    public boolean containsVariable(String name) throws Exception
    {
        return allVariables.containsKey( name );
    }

    @Override
    public double getCurrentValue(String name) throws Exception
    {
        Integer index = this.allVariables.get( name );
        if( index == null )
            throw new Exception( "Unknown variable " + name + " in agent " + this.name );
        return currentValues[index];
    }

    @Override
    public void setCurrentValue(String name, double value) throws Exception
    {
        Integer index = this.allVariables.get( name );
        if( index == null )
            throw new Exception( "Unknown variable " + name + " in agent " + this.name );
        if( value != currentValues[index] )
        {
            currentValues[index] = value;
            updatedFromOutside = true;
        }
    }

    @Override
    public void setCurrentValueUpdate(String name, double update) throws Exception
    {
        Integer index = this.allVariables.get( name );
        if( index != null )
            currentValues[index] += update;
        updatedFromOutside = true;
    }

    @Override
    public double[] getCurrentValues() throws Exception
    {
        return currentValues;
    }

    @Override
    public String[] getVariableNames()
    {
        return null;
    }

    @Override
    public double[] getUpdatedValues() throws Exception
    {
        return updatedValues;
    }

    @Override
    protected void setUpdated() throws Exception
    {
        System.arraycopy( currentValues, 0, updatedValues, 0, currentValues.length );
    }

    public SimulationEngine getEngine()
    {
        return null;
    }

    public Model getModel()
    {
        return this.model;
    }

    @Override
    public void applyChanges() throws Exception
    {
        model.setCurrentValues( currentValues );
        currentValues = model.getCurrentValues();
    }

    @Override
    public void setCurrentValues(double[] values) throws Exception
    {
        this.currentValues = values;
        applyChanges();
    }

    @Override
    public String getErrorMessage()
    {
        return error;
    }

    public Map<String, Integer> getMapping()
    {
        return this.allVariables;
    }


    public LightModelAgent clone(String name) throws Exception
    {
        Span newSpan = span.getRestrictedSpan( currentTime, completionTime );
        LightModelAgent result = new LightModelAgent( model.clone(), newSpan, name,
                this.listeners.toArray( new ResultListener[listeners.size()] ) );
        result.setMapping( allVariables );
        return result;
    }
}
