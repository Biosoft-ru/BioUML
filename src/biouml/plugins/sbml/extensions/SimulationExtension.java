package biouml.plugins.sbml.extensions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.XmlStream;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Module;
import biouml.model.dynamics.Variable;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.type.BaseSupport;

public class SimulationExtension extends SbmlExtensionSupport
{
    public static final String SIMULATION_ELEMENT = "simulation";
    public static final String PARAMETERS_ELEMENT = "parameters";
    public static final String ATOL_ELEMENT = "atol";
    public static final String RTOL_ELEMENT = "rtol";
    public static final String RESULT_ELEMENT = "result";
    public static final String VARIABLEVALUE_ELEMENT = "variableValue";
    public static final String TIMEVALUE_ELEMENT = "timeValue";

    public static final String ID_ATTR = "id";
    public static final String TITLE_ATTR = "title";
    public static final String DESCRIPTION_ATTR = "description";
    public static final String SIMULATORNAME_ATTR = "simulatorName";
    public static final String SIMULATORVERSION_ATTR = "simulatorVersion";
    public static final String EXPERIMENTID_ATTR = "experimentID";
    public static final String VERSION_ATTR = "version";
    public static final String ALGORITHM_ATTR = "algorithm";
    public static final String SOLVER_ATTR = "solver";
    public static final String INITIALTIME_ATTR = "initialTime";
    public static final String COMPLETIONTIME_ATTR = "completionTime";
    public static final String TIMEINCREMENT_ATTR = "timeIncrement";
    public static final String ATOL_ATTR = "atol";
    public static final String RTOL_ATTR = "rtol";
    public static final String VALUE_ATTR = "value";
    public static final String TIME_ATTR = "time";
    public static final String INITIALVALUE_ATTR = "initialValue";

    public static final String DIAGRAM_SIMULATIONS_PROPERTY = "simulations";
    public static final String VERSION = "0.8.0";

    @Override
    public void readElement(Element element, DiagramElement specie, @Nonnull Diagram diagram)
    {
        if( !element.getNodeName().equals(SIMULATION_ELEMENT) )
            return;

        String id = element.getAttribute(ID_ATTR);
        String title = element.getAttribute(TITLE_ATTR);

        if( id.isEmpty() || title.isEmpty() )
            return;

        Simulation simulation = new Simulation(null, id);

        Element parameters = getElement(element, PARAMETERS_ELEMENT);
        if( parameters != null )
        {
            readParameters(parameters, simulation);
        }

        Element result = getElement(element, RESULT_ELEMENT);
        if( result != null )
        {
            SimulationResult sr = readResult(result, id, diagram);
            sr.setTitle(title);
            sr.setDescription(element.getAttribute(DESCRIPTION_ATTR));
            sr.setSimulatorName(element.getAttribute(SIMULATORNAME_ATTR));
            simulation.setResult(sr);
        }

        simulation.setExperimentID(element.getAttribute(EXPERIMENTID_ATTR));
        simulation.setVersion(element.getAttribute(VERSION_ATTR));

        Object object = diagram.getAttributes().getValue(DIAGRAM_SIMULATIONS_PROPERTY);
        if( object != null && ( object instanceof Simulation[] ) )
        {
            //add experiment to current list
            Simulation[] oldSimulations = (Simulation[])object;
            Simulation[] newSimulations = new Simulation[oldSimulations.length + 1];
            System.arraycopy(oldSimulations, 0, newSimulations, 0, oldSimulations.length);
            newSimulations[oldSimulations.length] = simulation;
            diagram.getAttributes().getProperty(DIAGRAM_SIMULATIONS_PROPERTY).setValue(newSimulations);
        }
        else
        {
            //create new experiments list
            Simulation[] newSimulations = new Simulation[1];
            newSimulations[0] = simulation;
            try
            {
                diagram.getAttributes().add(new DynamicProperty(DIAGRAM_SIMULATIONS_PROPERTY, Simulation[].class, newSimulations));
            }
            catch( Exception e )
            {
            }
        }
    }

    protected void readParameters(Element element, Simulation simulation)
    {
        Parameters parameters = new Parameters(null, "");

        parameters.setAlgorithm(element.getAttribute(ALGORITHM_ATTR));
        parameters.setSolver(element.getAttribute(SOLVER_ATTR));
        try
        {
            if(element.hasAttribute(INITIALTIME_ATTR))
            {
                parameters.setInitialTime(Double.parseDouble(element.getAttribute(INITIALTIME_ATTR)));
            }
            if(element.hasAttribute(COMPLETIONTIME_ATTR))
            {
                parameters.setCompletionTime(Double.parseDouble(element.getAttribute(COMPLETIONTIME_ATTR)));
            }
            if(element.hasAttribute(TIMEINCREMENT_ATTR))
            {
                parameters.setTimeIncrement(Double.parseDouble(element.getAttribute(TIMEINCREMENT_ATTR)));
            }
            if(element.hasAttribute(TIMEINCREMENT_ATTR))
            {
                parameters.setAtol(Double.parseDouble(element.getAttribute(ATOL_ATTR)));
            }
            if(element.hasAttribute(TIMEINCREMENT_ATTR))
            {
                parameters.setRtol(Double.parseDouble(element.getAttribute(RTOL_ATTR)));
            }
        }
        catch( Exception e )
        {
            //can't transform string to double
        }

        NodeList atols = element.getElementsByTagName(ATOL_ELEMENT);
        List<String> atolList = new ArrayList<>();
        int atolsLength = atols.getLength();
        for( int i = 0; i < atolsLength; i++ )
        {
            atolList.add(readAtolRtol((Element)atols.item(i)));
        }
        if( atolList.size() > 0 )
        {
            parameters.setAtolList(atolList.toArray(new String[atolList.size()]));
        }

        NodeList rtols = element.getElementsByTagName(RTOL_ELEMENT);
        List<String> rtolList = new ArrayList<>();
        int rtolsLength = rtols.getLength();
        for( int i = 0; i < rtolsLength; i++ )
        {
            rtolList.add(readAtolRtol((Element)rtols.item(i)));
        }
        if( rtolList.size() > 0 )
        {
            parameters.setRtolList(rtolList.toArray(new String[rtolList.size()]));
        }

        simulation.setParameters(parameters);
    }

    protected String readAtolRtol(Element element)
    {
        String id = element.getAttribute(ID_ATTR);
        String title = element.getAttribute(TITLE_ATTR);
        String value = element.getAttribute(VALUE_ATTR);

        return id + ":" + title + ":" + value;
    }

    protected SimulationResult readResult(Element element, String simulationName, @Nonnull Diagram diagram)
    {
        DataCollection<?> parent = Module.getModule(diagram);
        try
        {
            parent = (DataCollection<?>)parent.get(Module.SIMULATION);
            parent = (DataCollection<?>)parent.get(Module.RESULT);
        }
        catch( Exception e )
        {
        }
        SimulationResult result = new SimulationResult(parent, simulationName);

        NodeList variables = element.getElementsByTagName(VARIABLEVALUE_ELEMENT);
        Map<String, Integer> variableMap = new HashMap<>();
        List<double[]> valuesList = new ArrayList<>();
        int length = variables.getLength();
        for( int i = 0; i < length; i++ )
        {
            Element variableElement = (Element)variables.item(i);
            if( result.getTimes() == null )
            {
                result.setTimes(readTimes(variableElement));
            }

            String id = variableElement.getAttribute(ID_ATTR);
            String title = variableElement.getAttribute(TITLE_ATTR);
            String initialValue = variableElement.getAttribute(INITIALVALUE_ATTR);

            variableMap.put(id, i);
            Variable var = new Variable(id, null, null);
            var.setInitialValue(Double.parseDouble(initialValue));
            result.addInitialValue(var);

            valuesList.add(readValues(variableElement));
        }
        result.setVariableMap(variableMap);
        double[][] values = new double[result.getTimes().length][valuesList.size()];
        for( int i = 0; i < result.getTimes().length; i++ )
        {
            for( int j = 0; j < valuesList.size(); j++ )
            {
                values[i][j] = valuesList.get(j)[i];
            }
        }
        result.setValues(values);

        return result;
    }

    protected double[] readTimes(Element element)
    {
        NodeList tElements = element.getElementsByTagName(TIMEVALUE_ELEMENT);
        return XmlStream.elements( tElements ).map( e -> e.getAttribute(TIME_ATTR) )
            .mapToDouble( Double::parseDouble ).toArray();
    }

    protected double[] readValues(Element element)
    {
        NodeList vElements = element.getElementsByTagName(TIMEVALUE_ELEMENT);
        return XmlStream.elements( vElements ).map( e -> e.getAttribute(VALUE_ATTR) )
                .mapToDouble( Double::parseDouble ).toArray();
    }

    @Override
    public Element[] writeElement(DiagramElement specie, Document document)
    {
        if( ! ( specie instanceof Diagram ) )
        {
            return null;
        }

        Object object = ( (Diagram)specie ).getAttributes().getValue(DIAGRAM_SIMULATIONS_PROPERTY);
        if( object != null && ( object instanceof Simulation[] ) )
        {
            Element[] elements = new Element[ ( (Simulation[])object ).length];
            for( int i = 0; i < ( (Simulation[])object ).length; i++ )
            {
                Element element = document.createElement(SIMULATION_ELEMENT);
                Simulation simulation = ( (Simulation[])object )[i];

                element.setAttribute(ID_ATTR, simulation.getName());

                SimulationResult sr = simulation.getResult();
                if( sr != null )
                {
                    element.setAttribute(TITLE_ATTR, sr.getTitle());

                    String description = sr.getDescription();
                    if( description != null )
                    {
                        element.setAttribute(DESCRIPTION_ATTR, description);
                    }
                    String sName = sr.getSimulatorName();
                    if( sName != null )
                    {
                        element.setAttribute(SIMULATORNAME_ATTR, sName);
                    }

                    element.setAttribute(SIMULATORVERSION_ATTR, VERSION);
                }

                String experimentId = simulation.getExperimentID();
                if( experimentId != null )
                {
                    element.setAttribute(EXPERIMENTID_ATTR, experimentId);
                }
                String version = simulation.getVersion();
                if( version != null )
                {
                    element.setAttribute(VERSION_ATTR, version);
                }

                if( simulation.getParameters() != null )
                {
                    Element p = writeParameters(simulation.getParameters(), document);
                    element.appendChild(p);
                }

                if( simulation.getResult() != null )
                {
                    Element p = writeResult(simulation.getResult(), document);
                    element.appendChild(p);
                }

                elements[i] = element;
            }
            return elements;
        }
        return null;
    }

    protected Element writeParameters(Parameters parameters, Document document)
    {
        Element result = document.createElement(PARAMETERS_ELEMENT);

        String param = parameters.getAlgorithm();
        if( param != null )
        {
            result.setAttribute(ALGORITHM_ATTR, param);
        }
        param = parameters.getSolver();
        if( param != null )
        {
            result.setAttribute(SOLVER_ATTR, param);
        }

        result.setAttribute(INITIALTIME_ATTR, String.valueOf(parameters.getInitialTime()));
        result.setAttribute(COMPLETIONTIME_ATTR, String.valueOf(parameters.getCompletionTime()));
        result.setAttribute(TIMEINCREMENT_ATTR, String.valueOf(parameters.getTimeIncrement()));
        result.setAttribute(ATOL_ATTR, String.valueOf(parameters.getAtol()));
        result.setAttribute(RTOL_ATTR, String.valueOf(parameters.getRtol()));

        if( parameters.getAtolList() != null )
        {
            for( String el : parameters.getAtolList() )
            {
                result.appendChild(writeAtolRtol(el, document, ATOL_ELEMENT));
            }
        }
        if( parameters.getRtolList() != null )
        {
            for( String el : parameters.getRtolList() )
            {
                result.appendChild(writeAtolRtol(el, document, RTOL_ELEMENT));
            }
        }

        return result;
    }

    protected Element writeAtolRtol(String value, Document document, String type)
    {
        Element result = document.createElement(type);

        String[] params = TextUtil2.split( value, ':' );
        if( params.length < 3 )
            return null;

        result.setAttribute(ID_ATTR, params[0]);
        result.setAttribute(TITLE_ATTR, params[1]);
        result.setAttribute(VALUE_ATTR, params[2]);

        return result;
    }

    protected Element writeResult(SimulationResult result, Document document)
    {
        Element element = document.createElement(RESULT_ELEMENT);

        double[][] values = result.getValues();
        Map<String, Integer> variablesMap = result.getVariableMap();

        for( Entry<String, Integer> entry : variablesMap.entrySet() )
        {
            String varId = entry.getKey();
            Element variable = document.createElement(VARIABLEVALUE_ELEMENT);
            variable.setAttribute(ID_ATTR, varId);
            variable.setAttribute(TITLE_ATTR, varId);
            variable.setAttribute(INITIALVALUE_ATTR, getInitialValue(result, varId));

            for( int j = 0; j < values.length; j++ )
            {
                Element timeElement = document.createElement(TIMEVALUE_ELEMENT);

                timeElement.setAttribute(TIME_ATTR, String.valueOf(result.getTimes()[j]));
                timeElement.setAttribute(VALUE_ATTR, String.valueOf(values[j][entry.getValue()]));

                variable.appendChild(timeElement);
            }

            element.appendChild(variable);
        }

        return element;
    }

    protected String getInitialValue(SimulationResult result, String varName)
    {
        for( Variable v : result.getInitialValues() )
        {
            if( v.getName().equals(varName) )
            {
                return String.valueOf(v.getInitialValue());
            }
        }
        return null;
    }

    @SuppressWarnings ( "serial" )
    public static class Simulation extends BaseSupport
    {
        public Simulation(DataCollection<?> parent, String name)
        {
            super(parent, name);
        }

        protected String experimentID;
        protected String version;
        protected Parameters parameters;
        protected SimulationResult result;

        public Parameters getParameters()
        {
            return parameters;
        }
        public void setParameters(Parameters parameters)
        {
            this.parameters = parameters;
        }
        public String getExperimentID()
        {
            return experimentID;
        }
        public void setExperimentID(String experimentID)
        {
            this.experimentID = experimentID;
        }
        public String getVersion()
        {
            return version;
        }
        public void setVersion(String version)
        {
            this.version = version;
        }
        public SimulationResult getResult()
        {
            return result;
        }
        public void setResult(SimulationResult result)
        {
            this.result = result;
        }
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends BaseSupport
    {
        public Parameters(DataCollection<?> parent, String name)
        {
            super(parent, name);
        }

        protected String algorithm;
        protected String solver;
        protected double initialTime;
        protected double completionTime;
        protected double timeIncrement;
        protected double atol;
        protected double rtol;
        protected String[] atolList;
        protected String[] rtolList;

        public String getAlgorithm()
        {
            return algorithm;
        }
        public void setAlgorithm(String algorithm)
        {
            this.algorithm = algorithm;
        }
        public double getAtol()
        {
            return atol;
        }
        public void setAtol(double atol)
        {
            this.atol = atol;
        }
        public String[] getAtolList()
        {
            return atolList;
        }
        public void setAtolList(String[] atolList)
        {
            this.atolList = atolList;
        }
        public double getCompletionTime()
        {
            return completionTime;
        }
        public void setCompletionTime(double completionTime)
        {
            this.completionTime = completionTime;
        }
        public double getInitialTime()
        {
            return initialTime;
        }
        public void setInitialTime(double initialTime)
        {
            this.initialTime = initialTime;
        }
        public double getRtol()
        {
            return rtol;
        }
        public void setRtol(double rtol)
        {
            this.rtol = rtol;
        }
        public String[] getRtolList()
        {
            return rtolList;
        }
        public void setRtolList(String[] rtolList)
        {
            this.rtolList = rtolList;
        }
        public String getSolver()
        {
            return solver;
        }
        public void setSolver(String solver)
        {
            this.solver = solver;
        }
        public double getTimeIncrement()
        {
            return timeIncrement;
        }
        public void setTimeIncrement(double timeIncrement)
        {
            this.timeIncrement = timeIncrement;
        }
    }
}
