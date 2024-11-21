package biouml.plugins.virtualcell.simulation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.Role;
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

/**
 * @author Damag
 */
@PropertyName ( "Virtual cell simulation engine" )
@PropertyDescription ( "Virtual cell simulation engine." )
public class VirtualCellSimulationEngine extends SimulationEngine implements PropertyChangeListener
{

    private DataElementPath resultPath;
    private double timeIncrement;
    private double timeCompletion;

    public double getTimeIncrement()
    {
        return timeIncrement;
    }

    public void setTimeIncrement(double timeIncrement)
    {
        this.timeIncrement = timeIncrement;
    }

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
    }

    @Override
    public Model createModel() throws Exception
    {
        VirtualCellModel model = new VirtualCellModel();
        for (Node node: diagram.recursiveStream().select( Node.class ))
        {
            Role role = node.getRole();
            if (role instanceof TableCollectionPoolProperties)
            {
                MapPool pool = new MapPool(node.getName());
                DataElementPath path = ( (TableCollectionPoolProperties)role ).getPath();
                if (path != null)
                    pool.load( path.getDataElement( TableDataCollection.class ), "Value" );

            }
            else if (role instanceof TranslationProperties)
            {
                TranslationAgent translationAgent = new TranslationAgent( node.getName(),  new UniformSpan( 0, timeCompletion, timeIncrement ) );
                model.addAgent( translationAgent );
            }
            else if (role instanceof ProteinDegradationProperties)
            {
                
            }
            else if (role instanceof PopulationProperties)
            {
                
            }
        }
        return null;
    }

    @Override
    public String simulate(Model model, ResultListener[] resultListeners) throws Exception
    {
        Span span = new UniformSpan( 0, timeCompletion, timeIncrement );
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
    public boolean hasVariablesToPlot()
    {
        return false;
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
}
