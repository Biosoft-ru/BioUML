package biouml.plugins.agentmodeling;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import biouml.model.Diagram;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.MortalAgent;
import biouml.plugins.agentmodeling.Scheduler;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.EulerSimple;
import biouml.standard.diagram.CompositeModelPreprocessor;
import biouml.standard.simulation.ResultListener;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

public class AgentPopulation2
{
    private int size = 1;
    private double timeIncrement = 1;
    private double completionTime = 43825;
    private Diagram diagram;
    private AgentBasedModel agentmodel;
    private ClassificationStatCollector collector;
    private BasicStatCollector collector2;
    private Map<String, Integer> varToIndex;

    public void addClassification(Classification classification) throws Exception
    {
        collector.addClassification(classification);
        for (ModelAgent agent: StreamEx.of(agentmodel.getAgents()).select(ModelAgent.class))
            agent.addPort(classification.getVariableName(), varToIndex.get(classification.getVariableName()));
    }

    public void setTimeIncrement(double t)
    {
        this.timeIncrement = t;
    }

    public void setCompletionTime(double t)
    {
        this.completionTime = t;
    }

    public void setSize(int size)
    {
        this.size = size;
    }

    public AgentPopulation2(Diagram diagram, int size) throws Exception
    {
        this.diagram = diagram;
        this.size = size;
        collector = new ClassificationStatCollector();
        collector.setStepUpdate( false );
        collector2 = new BasicStatCollector();
        collector2.setShowPlot(false);
    }
    
    public void generatePopulation() throws Exception
    {
        diagram = new CompositeModelPreprocessor().preprocess(diagram);
        agentmodel = new AgentBasedModel();
        
        SimulationEngine engine = new JavaSimulationEngine();
        engine.setSolver(new EulerSimple());
        Simulator simulator = engine.getSimulator();
        engine.setDiagram(diagram);        
        Model model = engine.createModel();
        
        varToIndex = engine.getVarIndexMapping();
        
        int deathIndex = varToIndex.get("Death");
        int divisionIndex = -1;
        if( varToIndex.containsKey( "Division" ) )
            divisionIndex = varToIndex.get( "Division" );
        for( int i = 0; i < size; i++ )
        {
            MortalAgent agent = new MortalAgent( model.getClass().newInstance(), deathIndex, simulator.getClass().newInstance(),
                    new UniformSpan( 0, completionTime, timeIncrement ), diagram.getName() + "_" + i, new ResultListener[0] );
            agent.setDivideIndex( divisionIndex );
            agentmodel.addAgent( agent );
        }
    }

    public void simulate() throws Exception
    {
        Scheduler scheduler = new Scheduler();
        scheduler.addStatisticsCollector(collector2);
        scheduler.addStatisticsCollector(collector);
        scheduler.start(agentmodel, new UniformSpan(0, completionTime, timeIncrement), null, null);
        System.out.println("Simulation finished");
    }
    
    public double[] getDynamicDouble(String value)
    {
        return IntStreamEx.of(collector.getDynamic(value)).asDoubleStream().toArray();
    }
    
    public double[] getTimes()
    {
        return collector.getTimes();
    }
    
    public double[] getSizeDynamicFullDouble()
    {
        return Arrays.copyOf(IntStreamEx.of(collector.getSizeDynamic()).asDoubleStream().toArray(), (int)(completionTime/timeIncrement));
    }
    
    public double[] getTimesFull()
    {
        return Arrays.copyOf(collector.getTimes(), (int)(completionTime/timeIncrement));
    }
    
}
