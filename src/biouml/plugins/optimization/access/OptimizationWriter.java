package biouml.plugins.optimization.access;

import java.io.OutputStream;
import java.util.logging.Level;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.analysis.optimization.OptimizationMethodParameters.StateInfo;
import biouml.model.util.DiagramXmlWriter;
import biouml.plugins.optimization.MessageBundle;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationConstraint;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.ParameterConnection;
import biouml.plugins.simulation.SimulationTaskParameters;

public class OptimizationWriter extends OptimizationSupport
{
    private Document doc;
    protected OutputStream stream;

    public OptimizationWriter(OutputStream stream)
    {
        log = Logger.getLogger(OptimizationWriter.class.getName());
        this.stream = stream;
    }

    public void write(Optimization sourceOpt) throws Exception
    {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        write(sourceOpt, transformerFactory.newTransformer());
    }

    public void write(Optimization sourceOpt, Transformer transformer) throws Exception
    {
        if( sourceOpt == null )
        {
            String msg = MessageBundle.getMessage("ERROR_OPTIMIZATION_WRITING");
            Exception e = new NullPointerException(msg);
            log.log(Level.SEVERE, msg, e);
            throw e;
        }

        optimization = sourceOpt;

        buildDocument();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(stream);
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.MEDIA_TYPE, "text/xml");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.transform(source, result);
    }

    protected Document buildDocument() throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.newDocument();
        doc.appendChild(createOptimization());
        return doc;
    }

    protected Element createOptimization()
    {
        Element element = doc.createElement(OPTIMIZATION_ELEMENT);
        element.setAttribute(ID_ATTR, optimization.getName());
        element.setAttribute( DIAGRAM_PATH_ATTR, processPath( optimization.getOptimizationDiagramPath().toString() ) );
        
        String optimizedDiagramPath = processPath( DataElementPath.create( optimization.getDiagram() ).toString() );
        element.setAttribute( OPTIMIZED_DIAGRAM_PATH_ATTR, optimizedDiagramPath );

        element.appendChild(createMethodElement(optimization));
        element.appendChild(createListOfExperiments(optimization));
        element.appendChild(createListOfConstraints(optimization));
        Element simulationTaskElement = createSimulationTaskParameters(optimization);
        if( simulationTaskElement != null )
            element.appendChild(simulationTaskElement);
        element.appendChild(createListOfFittingParameters(optimization));
        element.appendChild(createListOfStateInfos(optimization));

        return element;
    }

    private String processPath(String path)
    {
        if( newPaths == null )
            return path;
        if( newPaths.containsKey( path ) )
            path = newPaths.get( path );
        return path;
    }

    protected Element createMethodElement(Optimization optimization)
    {
        OptimizationMethod<?> method = optimization.getOptimizationMethod();
        Element element = doc.createElement(METHOD_ELEMENT);

        element.setAttribute(NAME_ATTR, method.getName());
        element.appendChild(createListOfMethodParameters(method));

        return element;
    }

    protected Element createListOfMethodParameters(OptimizationMethod<?> method)
    {
        Element element = doc.createElement(LIST_OF_METHOD_PARAMETERS_ELEMENT);
        serializeBean(method.getParameters(), element);
        return element;
    }

    protected Element createListOfExperiments(Optimization optimization)
    {
        Element list = doc.createElement(LIST_OF_EXPERIMENTS_ELEMENT);
        List<OptimizationExperiment> experiments = optimization.getParameters().getOptimizationExperiments();

        for( OptimizationExperiment exp : experiments )
        {
            Element element = doc.createElement(EXPERIMENT_ELEMENT);
            element.setAttribute(NAME_ATTR, exp.getName());
            serializeBean(exp, element);
            element.appendChild(createParameterConnectionsElement(exp));
            list.appendChild(element);
        }
        return list;
    }

    protected Element createParameterConnectionsElement(OptimizationExperiment exp)
    {
        Element list = doc.createElement(LIST_OF_PARAMETER_CONNECTIONS_ELEMENT);
        List<ParameterConnection> paramConnections = exp.getParameterConnections();

        for( ParameterConnection connection : paramConnections )
        {
            Element element = doc.createElement(PARAMETER_CONNECTION_ELEMENT);
            serializeBean(connection, element);
            list.appendChild(element);
        }
        return list;
    }

    protected Element createListOfConstraints(Optimization optimization)
    {
        Element list = doc.createElement(LIST_OF_CONSTRAINTS_ELEMENT);
        List<OptimizationConstraint> constraints = optimization.getParameters().getOptimizationConstraints();

        for( int i = 0; i < constraints.size(); ++i )
        {
            Element element = doc.createElement(CONSTRAINT_ELEMENT);
            serializeBean(constraints.get(i), element);
            list.appendChild(element);
        }
        return list;
    }

    protected Element createSimulationTaskParameters(Optimization optimization)
    {
        Element list = doc.createElement( LIST_OF_SIMULATION_PARAMETERS_ELEMENT );
        Map<String, SimulationTaskParameters> taskParameters = optimization.getParameters().getSimulationTaskParameters();

        for( Map.Entry<String, SimulationTaskParameters> entry : taskParameters.entrySet() )
        {
            Element element = doc.createElement( SIMULATION_PARAMETERS_ELEMENT );
            element.setAttribute( NAME_ATTR, entry.getKey() );
            serializeBean( entry.getValue().getParametersBean(), element );
            list.appendChild( element );
        }
        return list;
    }

    protected Element createListOfFittingParameters(Optimization optimization)
    {
        Element list = doc.createElement(LIST_OF_FITTING_PARAMETERS_ELEMENT);
        List<Parameter> params = optimization.getParameters().getFittingParameters();

        for( int i = 0; i < params.size(); ++i )
        {
            Element element = doc.createElement(PARAMETER_ELEMENT);
            serializeBean(params.get(i), element);
            list.appendChild(element);
        }
        return list;
    }

    protected Element createListOfStateInfos(Optimization optimization)
    {
        Element list = doc.createElement(LIST_OF_STATE_INFOS_ELEMENT);
        List<StateInfo> infos = optimization.getParameters().getStateInfos();

        if( infos != null )
            for( int i = 0; i < infos.size(); ++i )
            {
                StateInfo info = infos.get(i);
                Element infoElement = doc.createElement(STATE_INFO_ELEMENT);
                infoElement.setAttribute(PATH_ATTR, info.getPath());
                infoElement.appendChild(createListOfStateResults(info));
                list.appendChild(infoElement);
            }
        return list;
    }

    protected Element createListOfStateResults(StateInfo info)
    {
        Element list = doc.createElement(LIST_OF_STATE_RESULTS_ELEMENT);
        for( String result : info.getResults() )
        {
            Element element = doc.createElement(STATE_RESULT_ELEMENT);
            element.setAttribute(PATH_ATTR, result);
            list.appendChild(element);
        }
        return list;
    }

    protected void serializeBean(Object bean, Element root)
    {
    	DynamicPropertySet dps = new DynamicPropertySetAsMap();
    	DPSUtils.writeBeanToDPS(bean, dps, "");
        processDPS( dps );
    	DiagramXmlWriter.serializeDPS(doc, root, dps, null);
    }
}
