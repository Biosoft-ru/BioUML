package biouml.plugins.modelreduction;

import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Event;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

public class ApplyEvents extends AnalysisMethodSupport<ApplyEventsParameters> implements ResultListener
{
    protected static final Logger log = Logger.getLogger(ApplyEvents.class.getName());
    protected double step;

    public ApplyEvents(DataCollection<?> origin, String name)
    {
        super(origin, name, new ApplyEventsParameters());
    }

    @Override
    public void setParameters(AnalysisParameters params) throws IllegalArgumentException
    {
        try
        {
            parameters = (ApplyEventsParameters)params;
        }
        catch( Exception ex )
        {
            throw new IllegalArgumentException("Wrong parameters");
        }
    }

    @Override
    public ApplyEventsParameters getParameters()
    {
        return parameters;
    }

    @Override
    public SimulationResult justAnalyzeAndPut() throws Exception
    {
        SimulationEngine engine = parameters.getEngineWrapper().getEngine();
        Diagram initialDiagram = engine.getDiagram();
        try
        {
            TableDataCollection table = parameters.getTablePath().getDataElement(TableDataCollection.class);
            SimulationResult result = new SimulationResult(parameters.getResultPath().getParentCollection(),
                    parameters.getResultPath().getName());
            Diagram processedDiagram = addEvents(initialDiagram, table);
            engine.setDiagram(processedDiagram);
            step = 100.0 / ( engine.getCompletionTime() - engine.getInitialTime() );
            engine.simulate(result);
            result.getOrigin().put(result);
            return result;
        }
        finally
        {
            engine.setDiagram(initialDiagram);
        }
    }

    public Diagram addEvents(Diagram diagram, TableDataCollection table)
    {
        Diagram result = diagram.clone(null, diagram.getName());
        String parameterName = "W";

        Map<Integer, Double> dayToTime = new HashMap<>();
        for( RowDataElement rde : table )
        {
            double duration = Double.parseDouble(rde.getValueAsString("Duration"));
            double intensity = Double.parseDouble(rde.getValueAsString("Intensity"));
            
            String startString = rde.getValueAsString("Start");
            String[] starts = startString.split(",");
            for( String start : starts )
            {
                if( start.contains("-") )
                {
                    String[] range = start.split("-");
                    for( int i = Integer.parseInt(range[0]); i <= Integer.parseInt(range[1]); i++ )
                        addEvents(i, duration, intensity, dayToTime, parameterName, result);
                }
                else
                {
                    addEvents(Integer.parseInt(start), duration, intensity, dayToTime, parameterName, result);
                }
                
            }
        }
        return result;
    }
    
    public void addEvents(int day, double duration, double intensity, Map<Integer, Double> dayToTime, String parameterName, Diagram diagram)
    {
        double startOfDay = ( day - 1 ) * 1440;
        double dayTime = 0;
        if( dayToTime.containsKey(day) )
            dayTime = dayToTime.get(day);
        createEvents(diagram, parameterName, startOfDay + dayTime, duration, intensity);
        dayTime += duration;
        dayToTime.put(day, dayTime);
    }

    private void createEvents(Diagram d, String parameterName, double start, double duration, double intensity)
    {
        String name = DefaultSemanticController.generateUniqueNodeName(d, "activity_start");
        Node evStart = new Node(d, new Stub(null, name, Type.MATH_EVENT));
        Event event = new Event(evStart);
        evStart.setRole(event);
        event.setTrigger("time >= " + start);
        event.setTriggerInitialValue(false);
        event.clearAssignments(false);
        event.addEventAssignment(new Assignment(parameterName, String.valueOf(intensity)), false);
        event.addEventAssignment(new Assignment("t_start", "time"), false);
        d.put(evStart);

        name = DefaultSemanticController.generateUniqueNodeName(d, "activity_end");
        Node evEnd = new Node(d, new Stub(null, name, Type.MATH_EVENT));
        event = new Event(evStart);
        event.setTrigger("time >= " + ( start + duration ));
        event.clearAssignments(false);
        event.addEventAssignment(new Assignment(parameterName, "0"), false);
        event.setPriority("-1");
        evEnd.setRole(event);
        d.put(evEnd);
    }

    @Override
    public void start(Object model)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void add(double t, double[] y) throws Exception
    {
        jobControl.setPreparedness((int)(t * step));
    }
}
