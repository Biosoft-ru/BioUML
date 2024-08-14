package biouml.plugins.agentmodeling;

import java.util.Arrays;
import java.util.logging.Level;

import biouml.model.Diagram;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.BasicStatCollector;
import biouml.plugins.agentmodeling.MortalAgent;
import biouml.plugins.agentmodeling.Scheduler;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.CompositeModelPreprocessor;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;

public class AgentPopulation
{
    private int size = 1;
    private double timeIncrement = 1;
    private double completionTime = 43825;
    private Diagram diagram;
    private AgentBasedModel model;
    private BasicStatCollector collector;
    
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

    public AgentPopulation(Diagram diagram, int size)
    {
        this.diagram = diagram;
        this.size = size;
    }
    
    public void generatePopulation() throws Exception
    {
        CompositeModelPreprocessor cmp = new CompositeModelPreprocessor();
        diagram = cmp.preprocess(diagram);
        model = new AgentBasedModel();

        for( int i = 0; i < size; i++ )
            model.addAgent(generateAgent(diagram, diagram.getName() + "_" + i));
    }

    public void simulate() throws Exception
    {
        Scheduler scheduler = new Scheduler();
        collector = new BasicStatCollector();
        collector.setShowPlot(false);
        scheduler.addStatisticsCollector(collector);
        scheduler.start(model, new UniformSpan(0, completionTime, timeIncrement), null, null);
        System.out.println("Simulation finished");
    }
    
    public double[] getTimes()
    {
        
        return Arrays.copyOf(DoubleStreamEx.of(collector.getAgentsNumber().keySet()).toArray(), (int)(completionTime/timeIncrement));
    }
    
    public double[] getSizeDynamic()
    {
        return Arrays.copyOf(IntStreamEx.of(collector.getAgentsNumber().values()).asDoubleStream().toArray(), (int)(completionTime/timeIncrement));
    }
    
    public double[] getX()
    {
        double[] result = new double[40];
        for (int i=0; i<40; i++)
            result[i] = i;
        return result;
    }
    

    public double[] getVals()
    {
        double[] result = new double[40];
        for (int i=0; i<40; i++)
            result[i] = i*i/40;
        return result;
    }
    
    private MortalAgent generateAgent(Diagram diagram, String name) throws Exception
    {
        SimulationEngine engine = new JavaSimulationEngine();
        engine.setDiagram(diagram);
        engine.setLogLevel( Level.SEVERE );
        engine.setCompletionTime(completionTime);
        engine.setTimeIncrement(timeIncrement);
        MortalAgent agent = new MortalAgent(engine, name);
        return agent;
    }

}
