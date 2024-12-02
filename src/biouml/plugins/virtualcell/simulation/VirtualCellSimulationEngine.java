package biouml.plugins.virtualcell.simulation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.virtualcell.diagram.PopulationProperties;
import biouml.plugins.virtualcell.diagram.ProteinDegradationProperties;
import biouml.plugins.virtualcell.diagram.TableCollectionPoolProperties;
import biouml.plugins.virtualcell.diagram.TranslationProperties;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;

/**
 * @author Damag
 */
@PropertyName ( "Virtual cell simulation engine" )
@PropertyDescription ( "Virtual cell simulation engine." )
public class VirtualCellSimulationEngine extends SimulationEngine implements PropertyChangeListener
{

    private DataElementPath resultPath;
    private double timeIncrement = 1;
    private double timeCompletion = 100;

    @PropertyName ( "Time increment" )
    public double getTimeIncrement()
    {
        return timeIncrement;
    }

    public void setTimeIncrement(double timeIncrement)
    {
        this.timeIncrement = timeIncrement;
    }

    @PropertyName ( "Completion time" )
    public double getTimeCompletion()
    {
        return timeCompletion;
    }

    public void setTimeCompletion(double timeCompletion)
    {
        this.timeCompletion = timeCompletion;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( doNotAffectSimulation( evt ) )
            return;
        diagramModified = true;
    }

    public VirtualCellSimulationEngine()
    {
        diagram = null;
        simulatorType = "VirtualCell";
        this.simulator = new VirtualCellSimulator();
    }

    @Override
    public Model createModel() throws Exception
    {
        Map<String, MapPool> createdPools = new HashMap<>();

        VirtualCellModel model = new VirtualCellModel();
        for( Node node : diagram.recursiveStream().select( Node.class ) )
        {
            Role role = node.getRole();
            if( role instanceof TableCollectionPoolProperties )
            {
                TableCollectionPoolProperties properties = (TableCollectionPoolProperties)role;
                MapPool pool = new MapPool( node.getName() );
                pool.setSaved( properties.isShouldBeSaved() );
                pool.setSaveStep( properties.getSaveStep() );
                DataElementPath path = properties.getPath();
                if( path != null )
                    pool.load( path.getDataElement( TableDataCollection.class ), "Value" );
                model.addPool( pool );
                createdPools.put( node.getName(), pool );
            }
        }

        for( Node node : diagram.recursiveStream().select( Node.class ) )
        {
            Role role = node.getRole();
            if( role instanceof TranslationProperties )
            {
                TranslationAgent translationAgent = new TranslationAgent( node.getName(),
                        new UniformSpan( 0, timeCompletion, timeIncrement ) );
                
                TableDataCollection parameters = ((TranslationProperties)role).getTranslationRates().getDataElement( TableDataCollection.class );
                MapPool parametersPool = new MapPool("rates");
                parametersPool.load( parameters, "Value" );
                translationAgent.addParametersPool( "rates", parametersPool );
                
                for( Node otherNode : node.edges().filter( e -> e.getOutput().equals( node ) ).map( e -> e.getInput() ) )
                {
                    if( otherNode.getRole() instanceof TableCollectionPoolProperties )
                    {
                        MapPool pool = createdPools.get( otherNode.getName() );
                        translationAgent.addInpuPool( "RNA", pool );
                        translationAgent.initPoolVariables( pool );
                    }
                }
                for( Node otherNode : node.edges().filter( e -> e.getInput().equals( node ) ).map( e -> e.getOutput() ) )
                {
                    if( otherNode.getRole() instanceof TableCollectionPoolProperties )
                    {
                        MapPool pool = createdPools.get( otherNode.getName() );
                        translationAgent.addOutputPool( "Protein", pool );
                    }
                }
                model.addAgent( translationAgent );
            }
            else if( role instanceof ProteinDegradationProperties )
            {
                ProteinDegradationAgent proteinDegradationAgent = new ProteinDegradationAgent( node.getName(),
                        new UniformSpan( 0, timeCompletion, timeIncrement ) );
                
                TableDataCollection parameters = ((ProteinDegradationProperties)role).getDegradationRates().getDataElement( TableDataCollection.class );
                MapPool parametersPool = new MapPool("rates");
                parametersPool.load( parameters, "Value" );
                proteinDegradationAgent.addParametersPool( "rates", parametersPool );
                
                for( Node otherNode : node.edges().filter( e -> e.getOutput().equals( node ) ).map( e -> e.getInput() ) )
                {
                    if( otherNode.getRole() instanceof TableCollectionPoolProperties )
                    {
                        MapPool pool = createdPools.get( otherNode.getName() );
                        proteinDegradationAgent.addInpuPool( "Protein", pool );
                        proteinDegradationAgent.initPoolVariables( pool );
                    }
                }
                for( Node otherNode : node.edges().filter( e -> e.getInput().equals( node ) ).map( e -> e.getOutput() ) )
                {
                    if( otherNode.getRole() instanceof TableCollectionPoolProperties )
                    {
                        MapPool pool = createdPools.get( otherNode.getName() );
                        proteinDegradationAgent.addOutputPool( "Protein", pool );
                    }
                }
                model.addAgent( proteinDegradationAgent );
            }
            else if( role instanceof PopulationProperties )
            {
                PopulationAgent populationAgent = new PopulationAgent( node.getName(),
                        new UniformSpan( 0, timeCompletion, timeIncrement ) );
                
                TableDataCollection parameters = ((PopulationProperties)role).getCoeffs().getDataElement( TableDataCollection.class );
                MapPool parametersPool = new MapPool("coeffs");
                parametersPool.load( parameters, "Value" );
                populationAgent.addParametersPool( "coeffs", parametersPool );
                
                for( Node otherNode : node.edges().filter( e -> e.getOutput().equals( node ) ).map( e -> e.getInput() ) )
                {
                    if( otherNode.getRole() instanceof TableCollectionPoolProperties )
                    {
                        MapPool pool = createdPools.get( otherNode.getName() );
                        populationAgent.addInpuPool( "Protein", pool );
                        populationAgent.initPoolVariables( pool );
                    }
                }
                for( Node otherNode : node.edges().filter( e -> e.getInput().equals( node ) ).map( e -> e.getOutput() ) )
                {
                    if( otherNode.getRole() instanceof TableCollectionPoolProperties )
                    {
                        MapPool pool = createdPools.get( otherNode.getName() );
                        populationAgent.addOutputPool( "Population", pool );
                    }
                }
                model.addAgent( populationAgent );
            }
        }
        return model;
    }

    @Override
    public String simulate(Model model, ResultListener[] resultListeners) throws Exception
    {
        Span span = new UniformSpan( 0, timeCompletion, timeIncrement );
        ( (VirtualCellSimulator)simulator ).setResultPath( resultPath );
        simulator.init( model, null, span, resultListeners, jobControl );
        simulator.start( model, span, resultListeners, jobControl );
        return null;
    }

    @Override
    public void setDiagram(Diagram diagram)
    {
        if( this.diagram != null && this.diagram == diagram )
            return;

        this.diagram = diagram;
        this.originalDiagram = diagram;
    }

    @Override
    public String getEngineDescription()
    {
        return "Virtual cell simulation engine";
    }

    @Override
    public Object getSolver()
    {
        return simulator;
    }

    @Override
    public void setSolver(Object solver)
    {
        this.simulator = (Simulator)solver;
    }

    @Override
    public String getVariableCodeName(String varName)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getVariableCodeName(String diagramName, String varName)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Integer> getVarIndexMapping()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Integer> getVarPathIndexMapping()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public DataElementPath getResultPath()
    {
        return resultPath;
    }

    public void setResultPath(DataElementPath resultPath)
    {
        this.resultPath = resultPath;
    }

    @PropertyName ( "Show plot" )
    public boolean getNeedToShowPlot()
    {
        return false;
    }

    @Override
    public boolean hasVariablesToPlot()
    {
        return false;
    }

    @Override
    public PlotInfo[] getPlots()
    {
        return new PlotInfo[] {};
    }

    @Override
    public List<String> getIncorrectPlotVariables()
    {
        return new ArrayList<String>();
    }

    @Override
    public ResultListener[] getListeners()
    {
        return new ResultListener[0];
    }

    @Override
    public SimulationResult generateSimulationResult()
    {
        return null;
    }

    @Override
    public String[] getVariableNames()
    {
        return new String[0];
    }
}
