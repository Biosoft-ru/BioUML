package biouml.plugins.agentmodeling;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Simulator;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.standard.simulation.ResultListener;
import one.util.streamex.EntryStream;

public class ModelAgent extends SimulationAgent
{
    protected Simulator simulator;
    protected Model model;
    protected ArrayList<ResultListener> listeners = new ArrayList<>();
    private SimulationEngine engine = null;
    protected double[] currentValues;
    private double[] updatedValues;
    protected boolean updatedFromOutside = false;
    protected Map<String, Integer> variableToIndex = new HashMap<>();
    protected int nextSpanIndex = 1;
    protected int recalculateStepIndex = -1; //if != -1 than simulation step may be changed by the model itself.
    private double oldStep;

    public ModelAgent(SimulationEngine engine) throws Exception
    {
        this( engine, engine.getDiagram().getName() );
    }

    public ModelAgent(SimulationEngine engine, String name) throws Exception
    {
        this( engine.createModel(), engine.getSimulator(),
                new UniformSpan( engine.getInitialTime(), engine.getCompletionTime(), engine.getTimeIncrement() ), name,
                engine.getListeners() );
        this.engine = engine;
    }

    public ModelAgent(Model model, Simulator simulator, Span span, String name, ResultListener ... listeners) throws Exception
    {
        super( name, span );
        this.model = model;
        this.simulator = simulator;

        if( !model.isInit() )
            this.model.init();

        this.simulator.init( model, model.getInitialValues(), span, listeners, new FunctionJobControl( log ) );

        currentValues = model.getCurrentValues();

        updatedValues = currentValues.clone();

        for( ResultListener listener : listeners )
            this.listeners.add( listener );
    }

    @Override
    public void init() throws Exception
    {
        super.init();

        if( !model.isInit() )
            this.model.init();
        this.simulator.init( model, model.getInitialValues(), span, listeners.toArray( new ResultListener[listeners.size()] ),
                new FunctionJobControl( log ) );
        currentValues = model.getCurrentValues();
        updatedValues = currentValues.clone();
        nextSpanIndex = 1;
        if( recalculateStepIndex != -1 )
            recalculateSpan();
    }

    public void setInitialValues(double[] initialValues) throws Exception
    {
        this.simulator.init( model, initialValues, span, listeners.toArray( new ResultListener[listeners.size()] ),
                new FunctionJobControl( log ) );
    }

    protected void recalculateSpan() throws Exception
    {
        double newStep = this.getCurrentValues()[recalculateStepIndex];
        if( newStep == oldStep )
            return;
        this.span = new UniformSpan( this.currentTime, this.completionTime, newStep );
        simulator.init( model, simulator.getProfile().getX(), span, listeners.toArray( new ResultListener[listeners.size()] ),
                new FunctionJobControl( log ) );
        //        simulator.setInitialValues( currentValues );
        this.nextSpanIndex = 1;
        this.oldStep = newStep;
    }

    public void setRecalculateStep(int index)
    {
        recalculateStepIndex = index;
    }

    @Override
    public void iterate()
    {
        try
        {
            if( isAlive )
            {
                if( updatedFromOutside )
                    simulator.setInitialValues( currentValues );

                if( recalculateStepIndex != -1 )
                    recalculateSpan();

                while( simulator.getProfile().getTime() < span.getTime( nextSpanIndex ) && isAlive )
                    isAlive = simulator.doStep();

                nextSpanIndex++;
                currentValues = model.getCurrentValues();
                currentTime = simulator.getProfile().getTime();
                updatedFromOutside = false;

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



    public void addPort(String name, int index) throws Exception
    {
        try
        {
            variableToIndex.put( name, index );
        }
        catch( Exception ex )
        {
            throw new Exception( "Unknown variable " + name + " in agent " + this.name );
        }
    }

    @Override
    public void addVariable(String name) throws Exception
    {
        if( engine == null || !engine.getVarIndexMapping().containsKey( name ) )
            throw new Exception( "Unknown variable " + name + " in agent " + this.name );
        addPort( name, engine.getVarIndexMapping().get( name ) );
    }

    @Override
    public void addVariable(String name, double value) throws Exception
    {
        if( engine == null || !engine.getVarIndexMapping().containsKey( name ) )
            throw new Exception( "Unknown variable " + name + " in agent " + this.name );
        int index = engine.getVarIndexMapping().get( name );
        addPort( name, index );
        this.model.getCurrentValues()[index] = value;
    }

    @Override
    public boolean containsVariable(String name) throws Exception
    {
        return variableToIndex.containsKey( name );
    }

    @Override
    public double getCurrentValue(String name) throws Exception
    {
        Integer index = this.variableToIndex.get( name );
        if( index == null )
            throw new Exception( "Unknown variable " + name + " in agent " + this.name );
        return currentValues[index];
    }

    @Override
    public void setCurrentValue(String name, double value) throws Exception
    {
        Integer index = this.variableToIndex.get( name );
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
        Integer index = this.variableToIndex.get( name );
        if( index != null )
            currentValues[index] += update;
        updatedFromOutside = true;
    }

    public void addListener(ResultListener listener)
    {
        try
        {
            listeners.add( listener );
            this.simulator.init( model, model.getInitialValues(), span, listeners.toArray( new ResultListener[listeners.size()] ),
                    new FunctionJobControl( log ) );
        }
        catch( Exception ex )
        {
        }
    }

    @Override
    public double[] getCurrentValues() throws Exception
    {
        return currentValues;
    }

    @Override
    public String[] getVariableNames()
    {
        // TODO Auto-generated method stub
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
        return this.engine;
    }

    public Model getModel()
    {
        return this.model;
    }

    @Override
    public void applyChanges() throws Exception
    {
        simulator.setInitialValues( currentValues );
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
        return simulator.getProfile().getErrorMessage();
    }

    @Override
    public void die()
    {
        super.die();

        try
        {
            if( engine != null )
                engine.checkVariables( model.getCurrentState(), EntryStream.of( engine.getVarIndexMapping() ).invert().toMap() );
        }
        catch( Exception ex )
        {

        }
    }

    @Override
    public ModelAgent clone(String name) throws Exception
    {
        Span newSpan = span.getRestrictedSpan( currentTime, completionTime );
        //TODO: clone ismulator options
        Simulator newSimulator = this.simulator.getClass().newInstance();
        ModelAgent result = new ModelAgent( model.clone(), newSimulator, newSpan, name,
                this.listeners.toArray( new ResultListener[listeners.size()] ) );
        return result;
    }
}
