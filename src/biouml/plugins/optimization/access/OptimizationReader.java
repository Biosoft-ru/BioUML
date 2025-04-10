package biouml.plugins.optimization.access;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import biouml.model.Diagram;
import biouml.model.util.DiagramXmlReader;
import biouml.plugins.optimization.MessageBundle;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationConstraint;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.OptimizationMethodRegistry;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.optimization.ParameterConnection;
import biouml.plugins.simulation.SimulationTaskParameters;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.OptimizationMethodParameters;
import ru.biosoft.analysis.optimization.OptimizationMethodParameters.StateInfo;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.analysis.optimization.methods.SRESOptMethod;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.XmlUtil;

public class OptimizationReader extends OptimizationSupport
{
    public OptimizationReader(String name, InputStream stream)
    {
        log = Logger.getLogger(OptimizationReader.class.getName());
        this.name = name;
        this.stream = stream;
    }

    protected String name;
    protected InputStream stream;

    public Optimization read(DataCollection origin) throws Exception
    {
        optimization = null;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = null;

        try
        {
            doc = builder.parse(stream);
        }
        catch( SAXException e )
        {
            log.log(Level.SEVERE, "Parse optimization \"" + name + "\" error: " + e.getMessage());
            return null;
        }
        catch( IOException e )
        {
            log.log(Level.SEVERE, "Read optimization \"" + name + "\" error: " + e.getMessage());
            return null;
        }
        return read(origin, doc);
    }

    //TODO: simulation parameters serialization
    public Optimization read(DataCollection origin, Document document)
    {
        Element root = document.getDocumentElement();

        if( OPTIMIZATION_ELEMENT.equals(root.getTagName()) )
        {
            Diagram diagram = null;

            boolean newStyle = root.hasAttribute(OPTIMIZED_DIAGRAM_PATH_ATTR);
            if( newStyle )
            {
                String path = processPath( root.getAttribute( OPTIMIZED_DIAGRAM_PATH_ATTR ) );
                diagram = DataElementPath.create( path ).optDataElement( Diagram.class );
            }

            Element methodElement = getElement(root, METHOD_ELEMENT);
            Element experimentsElement = getElement(root, LIST_OF_EXPERIMENTS_ELEMENT);
            Element constraintsElement = getElement(root, LIST_OF_CONSTRAINTS_ELEMENT);
            Element fParamsElement = getElement(root, LIST_OF_FITTING_PARAMETERS_ELEMENT);
            Element sParamsElement = getElement(root, LIST_OF_SIMULATION_PARAMETERS_ELEMENT);
            Element stateInfosElement = getElement(root, LIST_OF_STATE_INFOS_ELEMENT);

            OptimizationMethod optMethod = readOptimizationMethod(methodElement);

            if( !newStyle )
            {
                DataElementPath diagramPath = ( (OptimizationMethodParameters)optMethod.getParameters() ).getDiagramPath();
                diagram = diagramPath.optDataElement(Diagram.class);
            }

            optimization = Optimization.createOptimization(name, origin, diagram);
            optimization.setOptimizationDiagramPathQuiet( DataElementPath.create( processPath( root.getAttribute( DIAGRAM_PATH_ATTR ) ) ) );
            optimization.setOptimizationMethod(optMethod);

            List<OptimizationExperiment> experiments = readOptimizationExperiments(experimentsElement);
            List<OptimizationConstraint> constraints = readOptimizationConstraints(constraintsElement, experiments);
            List<Parameter> fParams = readFittingParameters(fParamsElement);
            List<StateInfo> stateInfos = readStateInfos(stateInfosElement);

            optimization.setDescription(optMethod.getDescription());

            OptimizationParameters optParameters = ( optimization.getParameters() );
            optParameters.setOptimizationExperiments(experiments);
            optParameters.setOptimizationConstraints(constraints);
            optParameters.setFittingParameters(fParams);
            optParameters.setStateInfos(stateInfos);
//            optimization.initDiagramParameters();

            readSimulationTaskParameters(sParamsElement);

            return optimization;
        }
        return null;
    }

    protected OptimizationMethod readOptimizationMethod(Element methodElement)
    {
        String methodName = methodElement.getAttribute(NAME_ATTR);
        OptimizationMethod method = OptimizationMethodRegistry.getOptimizationMethod( methodName );

        if( method == null ) //test case
            method = new SRESOptMethod(null, methodName);

        method = method.clone(method.getOrigin(), method.getName());

        Element mParams = getElement(methodElement, LIST_OF_METHOD_PARAMETERS_ELEMENT);
        fillDPS(mParams, method.getParameters());

        return method;
    }

    protected ArrayList<OptimizationExperiment> readOptimizationExperiments(Element expElement)
    {
        ArrayList<OptimizationExperiment> optExperiments = new ArrayList<>();

        for( Element e : XmlUtil.elements(expElement, EXPERIMENT_ELEMENT) )
        {
            String name = e.getAttribute(NAME_ATTR);

            DynamicPropertySet dps = DiagramXmlReader.readDPS(e, null);
            boolean oldVersion = false;
            if(dps.isEmpty()) //old version of optimization serialization//
            {
            	dps = fillProperties(e);
                oldVersion = true;
            }
            String val = processPath( dps.getProperty( "filePath" ).getValue().toString() );
            DataElementPath path = DataElementPath.create( val );
            OptimizationExperiment optExp = new OptimizationExperiment(name, path);
            dps.remove("filePath");

            if (oldVersion)
            {
            	ComponentModel cModel = ComponentFactory.getModel(optExp);
            	fillComponentModelParameters(dps, cModel);
            }
            else
                DPSUtils.readBeanFromDPS(optExp, dps, "");

            Element paramConnectionsElement = getElement(e, LIST_OF_PARAMETER_CONNECTIONS_ELEMENT);
            if( paramConnectionsElement != null )
            {
                ArrayList<ParameterConnection> paramConnectionsMap = readParameterConnections(paramConnectionsElement, optExp);
                optExp.setParameterConnections(paramConnectionsMap);
                optExp.setDiagram(optimization.getDiagram());
            }

            optExperiments.add(optExp);
        }
        return optExperiments;
    }

    protected ArrayList<ParameterConnection> readParameterConnections(Element paramConnectionsElement, OptimizationExperiment exp)
    {
        ArrayList<ParameterConnection> paramConnections = new ArrayList<>();

        for( Element e : XmlUtil.elements(paramConnectionsElement, PARAMETER_CONNECTION_ELEMENT) )
        {
            ParameterConnection connection = new ParameterConnection(exp, paramConnections.size());
            connection.setDiagram(optimization.getDiagram());
            fillDPS(e, connection);
            paramConnections.add(connection);
        }

        return paramConnections;
    }

    protected List<OptimizationConstraint> readOptimizationConstraints(Element constrElement, List<OptimizationExperiment> experiments)
    {
        List<OptimizationConstraint> optConstraints = new ArrayList<>();
        for( Element e : XmlUtil.elements(constrElement, CONSTRAINT_ELEMENT) )
        {
            OptimizationConstraint optConstr = new OptimizationConstraint();
            fillDPS(e, optConstr);
            optConstr.setAvailableExperiments( experiments );
            optConstr.setDiagram( optimization.getDiagram() );
            optConstraints.add(optConstr);
        }
        return optConstraints;
    }

    protected void readSimulationTaskParameters(Element sParamsElement)
    {
        Map<String, SimulationTaskParameters> taskParameters = optimization.getParameters().getSimulationTaskParameters();
        for( Element e : XmlUtil.elements( sParamsElement, SIMULATION_PARAMETERS_ELEMENT ) )
        {
            String name = e.getAttribute( NAME_ATTR );
            if( taskParameters.keySet().contains( name ) )
            {
                SimulationTaskParameters stp = taskParameters.get( name );
                fillDPS( e, stp.getParametersBean() );
                stp.setDiagram( optimization.getDiagram() );
            }
        }
    }

    protected ArrayList<Parameter> readFittingParameters(Element fParamsElement)
    {
        ArrayList<Parameter> fParams = new ArrayList<>();
        for( Element e : XmlUtil.elements(fParamsElement, PARAMETER_ELEMENT) )
        {
            Parameter param = new Parameter();
            fillDPS(e, param);
            fParams.add(param);
        }
        return fParams;
    }

    protected ArrayList<StateInfo> readStateInfos(Element stateInfosElement)
    {
        ArrayList<StateInfo> stateInfos = new ArrayList<>();
        for( Element e : XmlUtil.elements(stateInfosElement, STATE_INFO_ELEMENT) )
        {
            String path = e.getAttribute(PATH_ATTR);
            DataElementPath fPath = DataElementPath.create(path);
            if( !fPath.exists() )
                continue;
            Element resultsElement = getElement(e, LIST_OF_STATE_RESULTS_ELEMENT);
            ArrayList<String> results = readStateResults(resultsElement);

            StateInfo info = new StateInfo(path);
            info.setResults(results);

            stateInfos.add(info);
        }
        return stateInfos;
    }

    protected ArrayList<String> readStateResults(Element resultsElement)
    {
        ArrayList<String> results = new ArrayList<>();
        for( Element e : XmlUtil.elements(resultsElement, STATE_RESULT_ELEMENT) )
        {
            results.add(e.getAttribute(PATH_ATTR));
        }
        return results;
    }

    protected void fillDPS(Element element, Object bean)
    {
        DynamicPropertySet dps = DiagramXmlReader.readDPS(element, null);
        if(dps.isEmpty())  //old version of optimization serialization//
        {
        	dps = fillProperties(element);
        	ComponentModel cModel = ComponentFactory.getModel(bean);
        	fillComponentModelParameters(dps, cModel);
        }
        else
        {
            processDPS( dps );
            DPSUtils.readBeanFromDPS(bean, dps, "");
        }
    }

    protected void fillComponentModelParameters(DynamicPropertySet dps, ComponentModel cModel)
    {
        for( int i = 0; i < cModel.getPropertyCount(); ++i )
        {
            Property prop = cModel.getPropertyAt(i);
            DynamicProperty dProp = dps.getProperty(prop.getName());

            if( dProp != null )
            {
                try
                {
                    prop.setValue(dProp.getValue());
                }
                catch( Throwable t )
                {
                    log.log( Level.SEVERE, t.getMessage(), t );
                }
            }
        }
    }

    private DynamicPropertySet fillProperties(Element element)
    {
        DynamicPropertySet dps = new DynamicPropertySetAsMap();

        for( Element e : XmlUtil.elements(element, FIELD_ELEMENT) )
        {
            DynamicProperty property = null;
            try
            {
                property = getProperty(e);
                if( property != null )
                    dps.add(property);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, MessageBundle.format("ERROR_PARSING_PROPERTY", new String[] {e.getTagName()}), t);
            }
        }

        return dps;
    }

    private static DynamicProperty getProperty(Element e) throws Exception
    {
        String name = e.getAttribute(NAME_ATTR);
        String value = e.getAttribute(VALUE_ATTR);
        String type = e.getAttribute(TYPE_ATTR);

        Class<?> propertyType = getPropertyType(type);
        if( propertyType != null )
        {
            return new DynamicProperty(name, name, propertyType, TextUtil2.fromString(propertyType, value));
        }
        return null;
    }

    public String processPath(String s)
    {
        if( newPaths != null && newPaths.containsKey( s ) )
            return newPaths.get( s );
        return s;
    }
}
