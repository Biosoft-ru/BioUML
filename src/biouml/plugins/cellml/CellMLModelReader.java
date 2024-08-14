package biouml.plugins.cellml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graph.ForceDirectedLayouter;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.model.VariableResolver;
import ru.biosoft.math.parser.Parser;
import ru.biosoft.math.xml.MathMLParser;
import ru.biosoft.util.XmlStream;
import ru.biosoft.util.XmlUtil;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.util.DiagramXmlReader;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.SpecieReference;
import biouml.workbench.graph.DiagramToGraphTransformer;

import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.ApplicationUtils;

import static biouml.plugins.cellml.CellMLConstants.*;

/**
 *
 * @pending whether we should layout the diagram here
 */
public class CellMLModelReader
{
    protected boolean shouldLayout = true;
    protected int visibleVariableNumber;
    protected int maxVisibleVariableNumber = 10;

    protected HashMap<String, ComponentInfo> componentMap;
    protected HashMap<String, ConnectionInfo> connectionMap;

    protected MathMLParser mathMLParser = new MathMLParser();
    protected LinearFormatter linearFormatter = new LinearFormatter();
    protected Logger log = Logger.getLogger(CellMLModelReader.class.getName());
    protected Diagram diagram;
    protected String modelName;
    protected Document document;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors and public methods
    //

    public CellMLModelReader(File file) throws Exception
    {
        // trick to avoid DTD loading
        String str = ApplicationUtils.readAsString(file);
        int offset = str.indexOf("<!DOCTYPE");
        if( offset > 1 )
            str = str.substring(0, offset) + str.substring(str.indexOf('>', offset + 9) + 1);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.parse( new ByteArrayInputStream( str.getBytes( StandardCharsets.UTF_8 ) ) );
        this.modelName = file.getName();
    }
    
    public CellMLModelReader(String name, Document document)
    {
        this.modelName = name;
        this.document = document;
    }

    public Diagram read(DataCollection<?> origin) throws Exception
    {
        diagram = null;
        visibleVariableNumber = 0;

        long time = System.currentTimeMillis();

        // root element should be MODEL_ELEMENT
        Element root = document.getDocumentElement();
        if( !root.getTagName().equals(MODEL_ELEMENT) )
        {
            CellMLUtils.error("ERROR_ROOT_IS_NOT_MODEL", new String[] {modelName});
        }
        else
        {
            diagram = readDiagram(root, origin);

            log.info("CellML diagram '" + modelName + "' is parsed, time=" + ( System.currentTimeMillis() - time ) + " ms.");

            if( shouldLayout )
                DiagramToGraphTransformer.layout(diagram, new ForceDirectedLayouter());
        }

        return diagram;
    }

    protected Diagram readDiagram(Element element, DataCollection<?> origin) throws Exception
    {
        DiagramType diagramType = new CellMLDiagramType();

        CellMLDiagramInfo diagramInfo = new CellMLDiagramInfo(modelName);
        Element rdf = getElement(element, RDF_ELEMENT);
        if( rdf != null )
            diagramInfo.setRdf(DomElementTransformer.transform(rdf, RdfHelper.getInstance()));

        diagram = new Diagram(origin, diagramInfo, diagramType);
        diagram.setNotificationEnabled(false);

        diagram.setRole(new EModel(diagram));

        // while model name is optional, then we try to use it as diagram title
        String title = element.getAttribute(NAME_ATTR);
        if( title != null && title.length() > 0 )
            diagram.setTitle(title);

        // read compartment info as BioUML extension
        Element compartmentInfoElement = getElement(element, BIOUML_COMPARTMENT_INFO_ELEMENT);
        if( compartmentInfoElement != null )
        {
            shouldLayout = false;
            DiagramXmlReader.readCompartmentInfo(compartmentInfoElement, diagram, diagram.getName());
        }

        readUnits(element);
        initConnectionMap(element);
        readComponents(element);
        readConnections(element);

        diagram.setNotificationEnabled(true);
        return diagram;
    }

    /**
     * Read unit definitions.
     *
     * @pending todo
     */
    protected void readUnits(Element model)
    {
    }

    ///////////////////////////////////////////////////////////////////
    // Component processing issues
    //

    /**
     * Read components.
     *
     * @pending todo
     */
    protected void readComponents(Element model)
    {
        initComponentMap(model);
        readGroups(model);
        readReactions(model);

        // check whether all components were processed
        for(ComponentInfo info : componentMap.values())
        {
            if( info.node == null && !info.name.equals(ENVIRONMENT) )
                CellMLUtils.warn("WARNING_UNPROCESSED_COMPONENT", new String[] {modelName, info.name, info.type});
        }
    }

    protected void initComponentMap(Element model)
    {
        componentMap = new HashMap<>();

        NodeList componentList = model.getElementsByTagName(COMPONENT_ELEMENT);
        for(Element component : XmlUtil.elements(componentList))
        {
            String name = component.getAttribute(NAME_ATTR);
            if( name.isEmpty() )
            {
                CellMLUtils.error("ERROR_COMPONENT_NAME_ABSENTS", new String[] {modelName});
                continue;
            }

            // by default we are suggesting that component type is unknown
            ComponentInfo info = new ComponentInfo(diagram, component, name, ComponentInfo.UNKNOWN);
            componentMap.put(name, info);

            // check whether the component is a reaction
            NodeList reactionList = component.getElementsByTagName(REACTION_ELEMENT);
            if( reactionList.getLength() > 0 )
                info.type = ComponentInfo.REACTION;
        }
    }

    /**
     * Read groups. This information is used to form compartments hierarchy
     * and refine component types.
     *
     * The following rule can be used:
     * if relationship is <code>containment</code> and
     * <component_ref component="A> contains other <component_ref>s,
     * then component A is a compartment.
     *
     * @pending todo
     */
    protected void readGroups(Element model)
    {
    }

    /**
     * Creates node for species.
     */
    protected @Nonnull Node createSpecies(ComponentInfo speciesInfo) throws Exception
    {
        // create node and kernel
        Species kernel = new Species(null, speciesInfo.name);
        Node species = new Node(speciesInfo.parent, kernel);
        speciesInfo.parent.put(species);
        speciesInfo.node = species;

        Element rdf = getElement(speciesInfo.element, RDF_ELEMENT);
        if( rdf != null )
            kernel.setRdf(new Preferences(DomElementTransformer.transform(rdf, RdfHelper.getInstance())));

        // create variable
        VariableRole var = new VariableRole(species, 0.0);
        species.setRole(var);

        EModel executableModel = diagram.getRole(EModel.class);
        executableModel.put(var);

        // read variables, we will use only the variable with the same name as species
        readVariable(var, speciesInfo, speciesInfo.name, true);

        // read species info as BioUML extension
        Element nodeInfoElement = getElement(speciesInfo.element, BIOUML_NODE_INFO_ELEMENT);
        if( nodeInfoElement == null )
            visibleVariableNumber++;
        else
        {
            shouldLayout = false;
            DiagramXmlReader.readNodeInfo(nodeInfoElement, species, diagram.getName());
        }

        Element specieInfoElement = getElement(speciesInfo.element, BIOUML_SPECIES_INFO_ELEMENT);
        if( specieInfoElement != null && specieInfoElement.hasAttribute(BIOUML_SPECIES_TYPE_ATTR) )
            kernel.setType(specieInfoElement.getAttribute(BIOUML_SPECIES_TYPE_ATTR));

        return species;
    }

    ///////////////////////////////////////////////////////////////////
    // Reaction processing issues
    //

    protected void readReactions(Element model)
    {
        for(ComponentInfo info : componentMap.values())
        {
            if( info.type.equals(ComponentInfo.REACTION) )
            {
                try
                {
                    createReaction(info);
                }
                catch( Throwable t )
                {
                    CellMLUtils.error("ERROR_REACTION_PROCESSING", new String[] {modelName, info.name, t.getMessage()});
                }
            }
        }
    }

    protected void createReaction(ComponentInfo reactionInfo) throws Exception
    {
        // create reaction node and kernel
        CellMLReaction kernel = new CellMLReaction(null, reactionInfo.name);
        Node reaction = new Node(reactionInfo.parent, reactionInfo.name, kernel);
        reactionInfo.parent.put(reaction);
        reactionInfo.node = reaction;

        Element rdf = getElement(reactionInfo.element, RDF_ELEMENT);
        if( rdf != null )
            kernel.setRdf(DomElementTransformer.transform(rdf, RdfHelper.getInstance()));

        if( reactionInfo.element.hasAttribute(REACTION_REVERSIBLE_ATTR) )
        {
            String reversible = reactionInfo.element.getAttribute(REACTION_REVERSIBLE_ATTR);
            kernel.setReversible("yes".equals(reversible));
        }
        else
            kernel.setReversible(true);

        // read roles
        Element kineticLaw = null;
        String kineticLawVar = null;
        Element reactionElement = getElement(reactionInfo.element, REACTION_ELEMENT);
        NodeList variableRefs = reactionElement.getElementsByTagName(VARIABLE_REF_ELEMENT);
        for(Element variableRef : XmlUtil.elements(variableRefs))
        {
            // we are suggesting that variableRef corresponds to species name
            String variable = variableRef.getAttribute(VARIABLE_ATTR);

            NodeList roles = variableRef.getElementsByTagName(ROLE_ELEMENT);
            if( roles.getLength() == 0 )
            {
                CellMLUtils.error("ERROR_VARIABLE_REF_ROLE_UNDEFINED", new String[] {modelName, variable});
                continue;
            }

            for(Element roleElement : XmlUtil.elements(roles))
            {
                String role = roleElement.getAttribute(ROLE_ATTR);
                if( role == null || role.length() == 0 )
                {
                    CellMLUtils.error("ERROR_ROLE_ATTR_UNDEFINED", new String[] {modelName, variable});
                    continue;
                }

                if( role.equals(ROLE_RATE) )
                {
                    kineticLaw = roleElement;
                    kineticLawVar = variable;
                    continue;
                }

                readRole(reaction, roleElement, variable, role);
            }
        }
        
        String varName = "$$rate_"+reaction.getName();
        EModel emodel = diagram.getRole( EModel.class );
        emodel.put(new Variable(varName, emodel, emodel.getVariables()));
        
        // read kinetic law and related variables declaration
        if( kineticLaw == null )
            CellMLUtils.error("ERROR_RATE_MISSING", new String[] {modelName, reaction.getName()});
        else
            readKineticLaw(reactionInfo, reaction, kineticLaw, kineticLawVar);

        // read reaction info as BioUML extension
        Element nodeInfoElement = getElement(reactionInfo.element, BIOUML_NODE_INFO_ELEMENT);
        if( nodeInfoElement != null )
        {
            shouldLayout = false;
            DiagramXmlReader.readNodeInfo(nodeInfoElement, reaction, diagram.getName());
        }
    }

    /**
     *
     * @pending  to which  modifier action should correspond CellML ROLE_MODIFIER?
     * @pending  for modifier stoichiometry should be 0. Whether it is correct for CellML models?
     * @pending  direction is not supported yet.
     */
    protected void readRole(Node reactionNode, Element element, String speciesName, String role) throws Exception
    {
        if( role.equals(ROLE_REACTANT) )
        {
            role = SpecieReference.REACTANT;
        }
        else if( role.equals(ROLE_PRODUCT) )
        {
            role = SpecieReference.PRODUCT;
        }
        else if( role.equals(ROLE_ACTIVATOR) )
        {
            role = SpecieReference.MODIFIER;
        }
        else if( role.equals(ROLE_CATALYST) )
        {
            role = SpecieReference.MODIFIER;
        }
        else if( role.equals(ROLE_INHIBITOR) )
        {
            role = SpecieReference.MODIFIER;
        }
        else if( role.equals(ROLE_MODIFIER) )
        {
            role = SpecieReference.MODIFIER; /* modifierAction = ??? */
        }
        else
        {
            CellMLUtils.error("ERROR_UNKNOWN_ROLE", new String[] {modelName, reactionNode.getName(), role});
        }

        String stoichiometry = ""; // by default stoichiometry value is unknown

        if( element.hasAttribute(STOICHIOMETRY_ATTR) )
        {
            stoichiometry = element.getAttribute(STOICHIOMETRY_ATTR);
            try
            {
                Double.parseDouble(stoichiometry);
            }
            catch( Throwable t )
            {
                CellMLUtils.error("ERROR_STOICHIOMETRY", new String[] {modelName, reactionNode.getName(), speciesName, stoichiometry, t.toString()});
            }
        }

        // for modifier stoichiometry should be 0
        if( role.equals(SpecieReference.MODIFIER) )
            stoichiometry = "0";

        // generate unique name
        CellMLReaction reactionKernel = (CellMLReaction)reactionNode.getKernel();

        // find specie node and create it if it absents
        ComponentInfo speciesInfo = componentMap.get(speciesName);
        if( speciesInfo == null )
        {
            CellMLUtils.error("ERROR_SPECIE_REFERENCE_INVALID", new String[] {modelName, reactionNode.getName(), speciesName});
            return;
        }

        Node speciesNode = speciesInfo.node;
        if( speciesNode == null )
        {
            speciesInfo.type = ComponentInfo.SPECIES;
            speciesNode = createSpecies(speciesInfo);
        }

        SpecieReference ref = new SpecieReference(reactionKernel, reactionKernel.getName(), speciesName, role);
        ref.setTitle(speciesName);
        ref.setSpecie(speciesName);
        ref.setStoichiometry(stoichiometry);
        reactionKernel.put(ref);

        // here we create corresponding edge
        Edge edge = null;

        if( role.equals(SpecieReference.PRODUCT) )
            edge = new Edge(ref, reactionNode, speciesNode);
        else
            edge = new Edge(ref, speciesNode, reactionNode);

        if( ref.isReactantOrProduct() )
        {
            VariableRole var = speciesNode.getRole(VariableRole.class);
            Equation equation = new Equation(edge, Equation.TYPE_RATE, var.getName());
            edge.setRole(equation);
        }
        // try to find the connection
        ConnectionInfo edgeInfo = connectionMap.get(speciesNode.getName() + " -> " + reactionNode.getName());
        if( edgeInfo == null )
            edgeInfo = connectionMap.get(reactionNode.getName() + " -> " + speciesNode.getName());
        if( edgeInfo == null )
            CellMLUtils.error("ERROR_SPECIE_CONNECTION_MISSING", new String[] {modelName, speciesNode.getName(), reactionNode.getName()});
        else
        {
            edgeInfo.edge = edge;

            // read edge info as BioUML extension
            Element edgeInfoElement = getElement(edgeInfo.element, BIOUML_EDGE_INFO_ELEMENT);
            if( edgeInfoElement != null )
            {
                shouldLayout = false;
                DiagramXmlReader.readEdgeInfo(edgeInfoElement, edge, diagram.getName());
            }
        }

        edge.save();
    }

    /**
     * @pending - rate units.
     */
    protected void readKineticLaw(ComponentInfo reactionInfo, Node reaction, Element element, String varName)
    {
        Equation rule = new Equation(reaction, Equation.TYPE_SCALAR, "$$rate_" + reaction.getName());
        reaction.setRole(rule);

        CellMLReaction reactionKernel = (CellMLReaction)reaction.getKernel();
        KineticLaw kineticLaw = new KineticLaw(reactionKernel);
        reactionKernel.setKineticLaw(kineticLaw);

        String formula = "";
        AstStart start = readMath(reactionInfo, element, varName);

        if( start != null )
        {
            // some check and restructure the tree
            if( start.jjtGetNumChildren() != 1 || ! ( start.jjtGetChild(0) instanceof AstFunNode )
                    || ! ( ( (AstFunNode)start.jjtGetChild(0) ).getFunction().getName().equals("=") ) )
                CellMLUtils.error("ERROR_MATH_EQUATION", new String[] {modelName, reactionInfo.name, ( linearFormatter.format(start) )[1]});
            else
            {
                try
                {
                    AstVarNode v = (AstVarNode)start.jjtGetChild(0).jjtGetChild(0);
                    EModel emodel = diagram.getRole(EModel.class);
                    emodel.getVariables().remove(v.getName());
                }
                catch( Throwable t )
                {
                }

                ru.biosoft.math.model.Node node = start.jjtGetChild(0).jjtGetChild(1);
                start = new AstStart(ru.biosoft.math.parser.ParserTreeConstants.JJTSTART);
                start.jjtAddChild(node, 0);
            }

            formula = ( linearFormatter.format(start) )[1];
        }

        System.out.println("formula=" + formula);
        kineticLaw.setFormula(formula);

    }

    ///////////////////////////////////////////////////////////////////
    // Connection issues
    //

    protected void initConnectionMap(Element model)
    {
        connectionMap = new HashMap<>();

        NodeList connectionList = model.getElementsByTagName(CONNECTION_ELEMENT);
        for(Element connection : XmlUtil.elements(connectionList))
        {
            Element components = getElement(connection, MAP_COMPONENTS_ELEMENT);

            String component_1 = components.getAttribute(COMPONENT_1_ATTR);
            String component_2 = components.getAttribute(COMPONENT_2_ATTR);
            if( component_1 == null || component_1.length() == 0 )
            {
                CellMLUtils.error("ERROR_COMPONENT_1_MISSSING", new String[] {modelName, component_2});
                continue;
            }
            if( component_2 == null || component_2.length() == 0 )
            {
                CellMLUtils.error("ERROR_COMPONENT_2_MISSSING", new String[] {modelName, component_1});
                continue;
            }

            // skip environment connections
            if( ENVIRONMENT.equals(component_1) || ENVIRONMENT.equals(component_2) )
                continue;

            ConnectionInfo info = new ConnectionInfo(connection, component_1, component_2);
            connectionMap.put(component_1 + " -> " + component_2, info);
            connectionMap.put(component_2 + " -> " + component_1, info);
        }
    }

    /**
     * Read connections.
     *
     * @pending todo
     */
    protected void readConnections(Element model)
    {
        // check whether all connections were processed
        for(ConnectionInfo info : connectionMap.values())
        {
            if( info.edge == null )
                CellMLUtils.warn("WARNING_UNPROCESSED_CONNECTION", new String[] {modelName, info.component_1, info.component_2});
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Utility classes
    //

    static class ConnectionInfo
    {
        String component_1;
        String component_2;

        /** DOM element corresponding to CellML connection. */
        Element element;

        /** BioUML edge corresponding to CellML connection. */
        Edge edge;

        public ConnectionInfo(Element element, String component_1, String component_2)
        {
            this.component_1 = component_1;
            this.component_2 = component_2;
            this.element = element;
        }
    }

    /**
     * Utility class that is used to define to which BioUML type
     * CellML component corresponds.
     *
     * @pending mapping of components into Rule, Event and Function.
     */
    static class ComponentInfo
    {
        // possible component types
        public static final String COMPARTMENT = "comartment";
        public static final String SPECIES = "species";
        public static final String REACTION = "reaction";
        public static final String UNKNOWN = "unknown";

        /**
         * Compartment where component should be located.
         * This information can be deduced from analysis <group> element.
         */
        Compartment parent;

        /** Component name in CellML model. */
        String name;

        /** Component type in BioUML model. */
        String type;

        /** DOM element corresponding to CellML component. */
        Element element;

        /** BioUML node corresponding to CellML component. */
        Node node;

        public ComponentInfo(Compartment parent, Element element, String name, String type)
        {
            this.parent = parent;
            this.element = element;
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString()
        {
            return ( "Component: " + name + ", type=" + type );
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Math processing issues
    //

    /**
     * Read variables information.
     *
     * @return DOM element corresponding to variable declaration.
     */
    protected Element readVariable(Variable var, ComponentInfo componentInfo, String variableName, boolean checkInitialValue)
    {
        // will try to find corresponding component variable declaration
        NodeList variables = componentInfo.element.getElementsByTagName(VARIABLE_ELEMENT);
        Element variableElement = XmlStream.elements( variables ).findFirst( e -> variableName.equals( e.getAttribute(NAME_ATTR) ) )
                .orElse( null );

        // process variable initial value
        if( variableElement == null )
        {
            CellMLUtils.error("ERROR_UNDECLARED_VARIABLE", new String[] {modelName, componentInfo.name, variableName});
            return null;
        }

        if( !variableElement.hasAttribute(INITIAL_VALUE_ATTR) )
        {
            if( checkInitialValue )
                CellMLUtils.warn("WARNING_INITIAL_VALUE_ABSENTS", new String[] {modelName, variableName, componentInfo.name});

            var.setInitialValue(0.0);
        }
        else
        {
            String initialValue = variableElement.getAttribute(INITIAL_VALUE_ATTR);
            initialValue = initialValue.replace(',', '.');
            try
            {
                var.setInitialValue(Double.parseDouble(initialValue));
            }
            catch( Throwable t )
            {
                CellMLUtils.error("ERROR_INITIAL_VALUE", new String[] {modelName, componentInfo.name, initialValue, t.toString()});
            }
        }

        String units = variableElement.getAttribute(UNITS_ATTR);
        if( units != null && units.length() > 0 )
            var.setUnits(units);
        else
        {
            var.setUnits("substance");
            CellMLUtils.error("ERROR_VARIABLE_UNITS_ABSENTS", new String[] {modelName, componentInfo.name});
        }

        return variableElement;
    }

    protected AstStart readMath(ComponentInfo componentInfo, Element element, String equationVar)
    {
        AstStart start = null;

        // get math element
        Element math = null;
        if( element.getTagName().equals(MATH_ELEMENT) )
            math = element;
        else
        {
            math = getElement(element, MATH_ELEMENT);
            if( math == null )
            {
                CellMLUtils.error("ERROR_MATH_MISSING", new String[] {modelName, componentInfo.name});
                return start;
            }
        }

        try
        {
            EModel emodel = (EModel)diagram.getRole();

            mathMLParser.setContext(emodel);
            mathMLParser.setDeclareUndefinedVariables(false);
            mathMLParser.setVariableResolver(new ComponentVariableResolver(componentInfo, equationVar));

            int status = mathMLParser.parse(math);

            if( status > Parser.STATUS_OK )
            {
                CellMLUtils.error("ERROR_MATHML_PARSING", new String[] {modelName, componentInfo.name, Utils.formatErrors(mathMLParser)});
                System.out.println("Math=" + math);
            }

            if( status < Parser.STATUS_FATAL_ERROR )
                start = mathMLParser.getStartNode();
        }
        catch( Throwable t )
        {
            CellMLUtils.error("ERROR_MATHML_PARSING", new String[] {modelName, componentInfo.name, t.getMessage()});
        }

        mathMLParser.setVariableResolver(null);

        return start;
    }

    public Element getElement(Element element, String childName)
    {
        Element child = null;
        String elementName = element.getAttribute(NAME_ATTR);
        if( elementName.isEmpty() )
            elementName = element.getTagName();

        try
        {
            NodeList list = element.getChildNodes();
            Element result = null;
            for(Element node : XmlUtil.elements(list))
            {
                if( node.getNodeName().equals(childName) )
                {
                    if( result == null )
                        result = node;
                    else
                        CellMLUtils.warn("WARN_MULTIPLE_DECLARATION", new String[]{modelName, elementName, childName});
                }
            }

            return result;
        }
        catch(Throwable t)
        {
            CellMLUtils.error("ERROR_ELEMENT_PROCESSING", new String[]{modelName, elementName, childName, t.getMessage()});
        }
    
        return child;
    }

    /**
     * Utility class to process variables declaration during math parsing.
     */
    class ComponentVariableResolver implements VariableResolver
    {
        ComponentInfo componentInfo;
        String equationVar;

        public ComponentVariableResolver(ComponentInfo componentInfo, String equationVar)
        {
            this.componentInfo = componentInfo;
            this.equationVar = equationVar;
        }

        /**
         * @pending - compartment hierarchy issues during species name resolving
         */
        @Override
        public String getVariableName(String variableName)
        {
            EModel emodel = diagram.getRole(EModel.class);

            // try to resolve as a species name
            String name = "$" + variableName;
            if( emodel.containsVariable(name) )
                return name;

            // check, whether it is global parameter that was already declared
            if( emodel.containsVariable(variableName) )
                return variableName;

            // check whether it is local parameter
            name = componentInfo.name + "." + variableName;
            if( emodel.containsVariable(name) )
                return name;

            // create new variable (parameter) and try to initialize it
            // from component information.
            Variable var = new Variable(variableName, emodel, emodel.getVariables());
            boolean checkInitialValue = !variableName.equals(equationVar);
            Element variableElement = readVariable(var, componentInfo, variableName, checkInitialValue);

            // expand variable name if it was declared as private or it is undeclared variable
            // if variable has the same numeric suffix as a component, we suggest that it's name
            // is unique and do not expand it
            if( variableElement == null || !variableElement.hasAttribute(PUBLIC_INTERFACE_ATTR)
                    || PUBLIC_INTERFACE_NONE.equals(variableElement.getAttribute(PUBLIC_INTERFACE_ATTR)) )
            {
                boolean expand = true;

                int len_1 = variableName.length();
                if( '_' == variableName.charAt(len_1 - 1) )
                    len_1--;

                int len_2 = componentInfo.name.length();
                for( int i = 1; i < len_1 && i < len_2; i++ )
                {
                    char ch = variableName.charAt(len_1 - i);
                    if( Character.isDigit(ch) )
                        expand = false;
                    else
                        break;

                    if( ch != componentInfo.name.charAt(len_2 - i) )
                        expand = true;
                }

                if( expand )
                    var.setName(componentInfo.name + "_" + variableName);
            }

            try
            {
                emodel.getVariables().put(var);
            }
            catch( Throwable t )
            {
                throw new RuntimeException(t);
            }

            return var.getName();
        }

        @Override
        public String resolveVariable(String variableName)
        {
            return variableName;
        }
    }

}
