package biouml.plugins.sbml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.math.model.AstFunctionDeclaration;
import ru.biosoft.math.model.Parser;
import ru.biosoft.math.model.ParserContext;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.model.UndeclaredFunction;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.xml.MathMLParser;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.XmlUtil;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Node;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import biouml.standard.type.Unit;
import one.util.streamex.StreamEx;

/**
 * @pending generation of name for parameters defined in reaction.
 * Currently name is generated as <code>reactioName_parameterName</code>.
 * @pending name validations that they are valid SNames
 */
public class SbmlModelReader_21 extends SbmlModelReader
{

    protected MathMLParser mathMLParser = new MathMLParser();
    protected Map<String, MetaIdInfo> metaIds;

    public SbmlModelReader_21()
    {
        log = Logger.getLogger(SbmlModelReader_21.class.getName());
        metaIds = new HashMap<>();
    }

    @Override
    protected Diagram readDiagram(Element model, DataCollection origin) throws Exception
    {
        replacements = createReplacements(model);
        this.mathMLParser.setReplacements( replacements );
        Diagram result = super.readDiagram(model, origin);
        result.getAttributes().add(DPSUtils.createTransient("metaIds", Map.class, metaIds));
        return result;
    }

    @Override
    protected DiagramType getDiagramType(Element modelElement)
    {
        return new SbmlDiagramType_L2();
    }

    @Override
    public String getId(Element element)
    {
        String wrongLetters = ": .,;|()[]{}+-*/%^!~&|$'\"<>";
        char[] data = element.getAttribute(ID_ATTR).toCharArray();
        for( int i = 0; i < data.length; i++ )
        {
            if( wrongLetters.indexOf(data[i]) >= 0 )
                data[i] = '_';
        }
        return new String(data);
    }

    @Override
    public String getTitle(Element element)
    {
        return element.getAttribute(NAME_ATTR);
    }
    
    @Override
    protected boolean isValid(String elementName, Object element, String name)
    {
        if( elementName.equals(COMPARTMENT_LIST_ELEMENT) )
            return validateList(element, COMPARTMENT_ELEMENT, name);

        if( elementName.equals(SPECIE_LIST_ELEMENT) )
            return validateList(element, SPECIE_ELEMENT, name);

        if( elementName.equals(REACTION_LIST_ELEMENT) )
            return validateList(element, REACTION_ELEMENT, name);

        if( elementName.equals(PARAMETER_LIST_ELEMENT) )
            return validateList(element, PARAMETER_ELEMENT, name);

        if( elementName.equals(REACTANT_LIST_ELEMENT) )
            return validateList(element, SPECIE_REFERENCE_ELEMENT, name);

        if( elementName.equals(PRODUCT_LIST_ELEMENT) )
            return validateList(element, SPECIE_REFERENCE_ELEMENT, name);

        if( elementName.equals(MODIFIER_LIST_ELEMENT) )
            return validateList(element, MODIFIER_SPECIE_REFERENCE_ELEMENT, name);

        if( elementName.equals(UNIT_DEFINITION_LIST_ELEMENT) )
            return validateList(element, UNIT_DEFINITION_ELEMENT, name);

        if( elementName.equals(RULE_LIST_ELEMENT) )
            return ( element instanceof Element );

        // --- validate attributes ----
        if( elementName.equals(SPECIE_INITIAL_AMOUNT_ATTR) )
            return element instanceof Element && ( (Element)element ).hasAttribute(SPECIE_INITIAL_AMOUNT_ATTR);

        if( elementName.equals(PARAMETER_VALUE_ATTR) )
            return element instanceof Element && ( (Element)element ).hasAttribute(PARAMETER_VALUE_ATTR);

        return true;
    }

    @Override
    public void readModelAttributes(Element element, Diagram diagram)
    {
        try
        {
            if( element.hasAttribute(ID_ATTR) )
                diagram.getAttributes().add(DPSUtils.createTransient(ID_ATTR, String.class, element.getAttribute(ID_ATTR)));
            if( element.hasAttribute(METAID_ATTR) )
                diagram.getAttributes().add(DPSUtils.createTransient(METAID_ATTR, String.class, element.getAttribute(METAID_ATTR)));
        }
        catch( Exception ex )
        {

        }
    }

    @Override
    public Compartment readCompartment(Element element, String compartmentId, String parentId, String compartmentName) throws Exception
    {
        Compartment compartment = super.readCompartment(element, compartmentId, parentId, compartmentName);
        if( compartment == null )
            return null;
        int dimension = this.readInt( element, COMPARTMENT_DIMENSION_ATTR, 3 , "ERROR_INVALID_SPATIAL_DIMENSION", compartment.getName());
        ( (biouml.standard.type.Compartment)compartment.getKernel() ).setSpatialDimension(dimension);
        compartment.getRole(VariableRole.class).setConstant( !"false".equals(element.getAttribute(CONSTANT_ATTR) ));
        return compartment;
    }

    @Override
    public Node readSpecie(Element element, String speciesId, String speciesName) throws Exception
    {
        Node species = super.readSpecie(element, speciesId, speciesName);
        if( species == null )
            return null;

        VariableRole var = species.getRole(VariableRole.class);

        //process hasOnlySubstanceUntis
        if( element.hasAttribute(SPECIE_HAS_ONLY_SUBSTANCE_UNITS_ATTR) )
        {
            boolean hasOnlySubstanceUnits = Boolean.parseBoolean(element.getAttribute(SPECIE_HAS_ONLY_SUBSTANCE_UNITS_ATTR));
            var.setQuantityType(hasOnlySubstanceUnits ? VariableRole.AMOUNT_TYPE : VariableRole.CONCENTRATION_TYPE);
        }

        double initialValue = 0;
        int initialType = var.getQuantityType();

        // check conflicts
        if( element.hasAttribute(SPECIE_INITIAL_AMOUNT_ATTR) && element.hasAttribute(SPECIE_INITIAL_CONCENTRATION_ATTR) )
        {
            error("ERROR_AMOUNT_AND_CONCENTRATION", new String[] {modelName, speciesId});
        }
        else if( element.hasAttribute(SPECIE_INITIAL_AMOUNT_ATTR) )
        {
            initialValue = readDouble(element, SPECIE_INITIAL_AMOUNT_ATTR, 0.0, "ERROR_SPECIE_AMOUNT", speciesId);
            initialType = VariableRole.AMOUNT_TYPE;                 
        }
        else if( element.hasAttribute(SPECIE_INITIAL_CONCENTRATION_ATTR) )
        {
            initialValue = readDouble(element, SPECIE_INITIAL_CONCENTRATION_ATTR, 0.0, "ERROR_SPECIE_CONCENTRATION", speciesId);
            initialType = VariableRole.CONCENTRATION_TYPE;
        }
        var.setInitialValue(initialValue);
        var.setInitialQuantityType(initialType);
        var.setOutputQuantityType(initialType);
        var.setConstant("true".equals(element.getAttribute(CONSTANT_ATTR))); //default is false
        String units = element.getAttribute(SPECIE_SUBSTANCE_UNITS_ATTR);;
        //process substanceUnits
        if( !units.isEmpty() )
        {
            if (!emodel.getUnits().containsKey(units))
                emodel.addUnit(new Unit(null, units));
            var.setUnits(units);
        }
        return species;
    }

    @Override
    public Variable readParameter(Element element, String parameterId, Node reaction) throws Exception
    {
        Variable parameter = super.readParameter(element, parameterId, reaction);
        MetaIdInfo metaidInfo = readMetaId(element, parameter.getName(), Variable.class, null);
        parameter.getAttributes().add(new DynamicProperty(METAID_ATTR, MetaIdInfo.class, metaidInfo));
        parameter.setConstant(!"false".equals(element.getAttribute(CONSTANT_ATTR))); //default is true
        return parameter;
    }

    @Override
    protected void validateReaction(Node reaction)
    {
        // check if there is at least one product or reactant
        Reaction r = (Reaction)reaction.getKernel();
        if( !StreamEx.of(r.getSpecieReferences()).anyMatch(sr -> sr.isReactantOrProduct()) )
            error("ERROR_INVALID_REACTION", new String[] {modelName, r.getName()});
    }

    @Override
    protected void readStoichiometry(Element element, SpecieReference reference, Node reaction)
    {
        String stoichiometry = "1";

        if( element.hasAttribute(STOICHIOMETRY_ATTR) )
        {
            stoichiometry = element.getAttribute(STOICHIOMETRY_ATTR);
        }
        else
        {
            Element stoichiometryElement = getElement(element, STOICHIOMETRY_MATH_ELEMENT);
            if( stoichiometryElement != null )
            {
                String s = readMath(stoichiometryElement, reaction);
                if( s != null )
                    stoichiometry = s;
            }
        }
        reference.setStoichiometry(stoichiometry);
    }

    @Override
    protected void readKineticLawFormula(Element element, Node reaction, KineticLaw kineticLaw)
    {
        Element math = getElement(element, MATH_ELEMENT);
        if( math != null )
            kineticLaw.setFormula(readMath(math, reaction));
    }

    @Override
    protected void readModifiers(Element element, String reactionName, List<SpecieRefenceInfo> specieReferences)
    {
        Element modifierList = getElement(element, MODIFIER_LIST_ELEMENT);
        if( !isValid(MODIFIER_LIST_ELEMENT, modifierList, reactionName) )
            return;

        NodeList list = modifierList.getElementsByTagName(MODIFIER_SPECIE_REFERENCE_ELEMENT);
        int length = list.getLength();
        for( int i = 0; i < length; i++ )
        {
            String specieId = "";
            try
            {
                Element specieRef = (Element)list.item(i);
                specieId = sbmlNameToBioUML.get(getSpecieAttribute(specieRef));
                specieReferences.add(new SpecieRefenceInfo(specieId, SpecieReference.MODIFIER, specieRef));
            }
            catch( Throwable t )
            {
                error("ERROR_MODIFIER_PROCESSING", new String[] {modelName, reactionName, specieId, t.getMessage()});
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Rule issues
    //
    @Override
    public Node readRule(Element ruleElement, int i)
    {
        String id = getBriefId(ruleElement, BIOUML_NODE_INFO_ELEMENT);

        if( id.isEmpty() )
            id = ruleElement.getAttribute(METAID_ATTR);

        if( id.isEmpty() )
            id = "rule_" + i; //todo: generate unique name

        Stub stub = new Stub(diagram, id, Type.MATH_EQUATION);
        Node node = new Node(diagram, stub);

        Element annotationElement = getElement(ruleElement, ANNOTATION_ELEMENT);
        if( annotationElement != null )
            readBioUMLAnnotation(annotationElement, node, BIOUML_NODE_INFO_ELEMENT);

        try
        {
            String expression = readMath(ruleElement, node);
            Equation equation = null;

            if( RULE_ALGEBRAIC_ELEMENT.equals(ruleElement.getNodeName()) )
                equation = new Equation(node, Equation.TYPE_ALGEBRAIC, "unknown", expression);
            else
            {
                if( !ruleElement.hasAttribute(RULE_VARIABLE_ATTR) )
                    error("ERROR_VARIABLE_ATTRIBUTE_ABSENT", new String[] {modelName});

                String var = ruleElement.getAttribute(RULE_VARIABLE_ATTR);
                if( !var.isEmpty() )
                {
                    var = variableResolver.getVariableName(var);
                    var = normalize(var);

                    if( !emodel.containsVariable(var) )
                        error("ERROR_VARIABLE_UNKNOWN", new String[] {modelName, var});

                    String type = RULE_ASSIGNEMENT_ELEMENT.equals(ruleElement.getNodeName()) ? Equation.TYPE_SCALAR : Equation.TYPE_RATE;
                    equation = new Equation(node, type, var, expression);
                }
            }

            node.setRole(equation);
            diagram.put(node);
            return node;
        }
        catch( Throwable t )
        {
            error("ERROR_RULE_PROCESSING", new String[] {modelName, t.getMessage()});
        }
        return null;
    }
   
    
    @Override
    protected void readFunctionDefinitionList(Element model)
    {
        Element funcDefList = getElement(model, FUNCTION_LIST_ELEMENT);
        if( funcDefList == null )
            return;

        Map<String, Element> functions = new HashMap<>();
        Set<String> alreadyRead = new HashSet<>();
        
        for( Element funcDefElement : XmlUtil.elements(funcDefList, FUNCTION_DEFINITION_ELEMENT) )
        {  
            String functionName = normalize(getId(funcDefElement));
            functions.put( functionName, funcDefElement );
        }
        
        for( Entry<String, Element> e: functions.entrySet() )
        {    
            Element funcDefElement = e.getValue();                      

            String funcName = e.getKey();
            if( !alreadyRead.contains( funcName ) )
            {
                try
                {
                    readFunctionDefinition( funcDefElement, functions, alreadyRead );
                }
                catch( Throwable t )
                {
                    error( "ERROR_FUNCTION_DECLARATION_PROCESSING", new String[] {modelName, getId( funcDefElement ), t.getMessage()} );
                }
            }
        }
    }

    @Override
    public Node readFunctionDefinition(Element funcDefElement, Map<String, Element> functions, Set<String> alreadyRead) throws Exception
    {
        String nodeName = normalize(getCompleteId(funcDefElement, BIOUML_NODE_INFO_ELEMENT));   
        String functionName = normalize(getId(funcDefElement));
               
        if( functionName.isEmpty() )
        {
            error("ERROR_UNDEFINED_FUNCTION_PROCESSING", new String[] {modelName});
            return null;
        }
        if (nodeName.isEmpty())
            nodeName = functionName;
        
        //first ready any function that is called by current function
        Set<String> innerFunctions = mathMLParser.getFunctions( funcDefElement );
        for( String innerFuncName : innerFunctions )
        {
            if( emodel.getFunction( innerFuncName ) == null )
            {
                try
                {
                    Element innerFuncDefElement = functions.get( innerFuncName );
                    readFunctionDefinition( innerFuncDefElement, functions, alreadyRead );
                }
                catch( Throwable t )
                {
                    error( "ERROR_FUNCTION_DECLARATION_PROCESSING",
                            new String[] {modelName, innerFuncName, t.getMessage()} );
                }
            }
        }
        
        Stub stub = new Stub(diagram, nodeName, Type.MATH_FUNCTION);
        Node node = new Node(diagram, stub);
        
        Element annotationElement = getElement(funcDefElement, ANNOTATION_ELEMENT);
        if( annotationElement != null )
            readBioUMLAnnotation(annotationElement, node, BIOUML_NODE_INFO_ELEMENT);

        Function function = new Function(node);
        function.setName( functionName, false );
        mathMLParser.setLambdaFunctionName(functionName);
        alreadyRead.add( functionName );
        function.setFormula(readMath(funcDefElement, node));
        
        
        //declare function in context
        ParserContext context = mathMLParser.getContext();
        if( context != emodel )
        {
            setEModelAsParserContext( false );
            context = mathMLParser.getContext();
        }
        if( context.getFunction(functionName) == null )
          context.declareFunction(new UndeclaredFunction(functionName, ru.biosoft.math.model.Function.FUNCTION_PRIORITY));
            
        node.setRole(function);
        diagram.getRole(EModel.class).declareFunction((AstFunctionDeclaration)mathMLParser.getStartNode().jjtGetChild(0));
        diagram.put(node);
        return node;
    }

    @Override
    protected void readEventList(Element model)
    {
        Element eventList = getElement(model, EVENT_LIST_ELEMENT);
        if( eventList == null )
            return;

        NodeList list = eventList.getElementsByTagName(EVENT_ELEMENT);
        for( int i = 0; i < list.getLength(); i++ )
            readEvent((Element)list.item(i), i);
    }

    @Override
    public Node readEvent(Element eventElement, int i)
    {
        try
        {
            String id = getBriefId(eventElement, BIOUML_NODE_INFO_ELEMENT);
            if( id.isEmpty() )            
                id = eventElement.getAttribute(METAID_ATTR);
            if (id.isEmpty())
                id = "event_" + ( i + 1 );

            Node node = new Node(diagram, new Stub(diagram, id, Type.MATH_EVENT));
            Event event = new Event(node);
            node.setRole(event);

            //read meta id
            List<MetaIdInfo> metaIdInfos = new ArrayList<>();
            MetaIdInfo metaIdInfo = readMetaId(eventElement, node.getName(), Node.class, null);
            if( metaIdInfo != null )
                metaIdInfos.add(metaIdInfo);

            Element annotationElement = getElement(eventElement, ANNOTATION_ELEMENT);
            if( annotationElement != null )
                readBioUMLAnnotation(annotationElement, node, BIOUML_NODE_INFO_ELEMENT);

            String delay = null;
            Element delayElement = getElement(eventElement, DELAY_ELEMENT);
            if( delayElement != null )
                delay = readMath(delayElement, node);

            if( delay != null )
            {
                //meta id for delay
                metaIdInfo = readMetaId(delayElement, node.getName(), Node.class, "delay");
                if( metaIdInfo != null )
                    metaIdInfos.add(metaIdInfo);
                event.setDelay(delay);
            }

            String trigger = null;
            Element triggerElement = getElement(eventElement, TRIGGER_ELEMENT);
            if( triggerElement == null )
                error("ERROR_TRIGGER_ELEMENT_ABSENT", new String[] {modelName});
            else
                trigger = readMath(triggerElement, node);
            if( trigger != null )
            {
                //meta id for trigger
                metaIdInfo = readMetaId(triggerElement, node.getName(), Node.class, "trigger");
                if( metaIdInfo != null )
                    metaIdInfos.add(metaIdInfo);
                event.setTrigger(trigger);
            }

            event.setTimeUnits(eventElement.getAttribute(TIME_UNITS_ATTR));
            readAssignmentList(eventElement, event, metaIdInfos);

            int metaIdsCount = metaIdInfos.size();
            if( metaIdsCount != 0 )
                node.getAttributes().add(DPSUtils.createTransient( METAID_ATTR, List.class, metaIdInfos ));

            diagram.put(node);
            return node;
        }
        catch( Throwable t )
        {
            error("ERROR_EVENT_PROCESSING", new String[] {modelName, t.getMessage()});
        }
        return null;
    }

    @Override
    public Node readInitialAssignment(Element initialAssignmentsElement, int i)
    {
        String id = getBriefId(initialAssignmentsElement, BIOUML_NODE_INFO_ELEMENT);

        if( id.isEmpty() )
            id = initialAssignmentsElement.getAttribute(METAID_ATTR);

        if( id.isEmpty() )
            id = "initialAssignment_" + i;

        Node node = new Node(diagram, new Stub(diagram, id, Type.MATH_EQUATION));

        Element annotationElement = getElement(initialAssignmentsElement, ANNOTATION_ELEMENT);
        if( annotationElement != null )
            readBioUMLAnnotation(annotationElement, node, BIOUML_NODE_INFO_ELEMENT);

        try
        {
            String expression = readMath(initialAssignmentsElement, node);

            if( !initialAssignmentsElement.hasAttribute(INITIAL_ASSIGNMENT_VARIABLE_ATTR) )
                error("ERROR_VARIABLE_ATTRIBUTE_ABSENT", new String[] {modelName});

            String var = initialAssignmentsElement.getAttribute(INITIAL_ASSIGNMENT_VARIABLE_ATTR);
            if( !var.isEmpty() )
            {
                var = variableResolver.getVariableName(var);

                if( !emodel.containsVariable(var) && !specieMap.containsKey(var) && !compartmentMap.containsKey(var) )
                    error("ERROR_VARIABLE_UNKNOWN", new String[] {modelName, var});

                node.setRole(new Equation(node, Equation.TYPE_INITIAL_ASSIGNMENT, var, expression));
                diagram.put(node);
            }
            return node;

        }
        catch( Throwable t )
        {
            error("ERROR_INITIAL_ASSIGNMENT_PROCESSING", new String[] {modelName, t.getMessage()});
        }
        return null;
    }

    protected void readAssignmentList(Element eventElement, Event event, List<MetaIdInfo> metaIdInfos)
    {
        event.clearAssignments(false);
        Element assignmentList = getElement(eventElement, ASSIGNEMENT_LIST_ELEMENT);
        if( assignmentList == null )
            return;

        NodeList list = assignmentList.getElementsByTagName(ASSIGNEMENT_ELEMENT);
        int length = list.getLength();
        for( int i = 0; i < length; i++ )
        {
            try
            {
                Element assignElement = (Element)list.item(i);

                Stub stub = new Stub(null, "assignment_" + ( i + 1 ));
                Node node = new Node(diagram, stub);

                if( !assignElement.hasAttribute(ASSIGNEMENT_VARIABLE_ATTR) )
                    error("ERROR_EVENT_ASSIGNMENT_PROCESSING", new String[] {modelName});
                else
                {
                    String var = assignElement.getAttribute(ASSIGNEMENT_VARIABLE_ATTR);
                    if( !var.isEmpty() )
                    {
                        var = variableResolver.getVariableName(var);
                        String math = readMath(assignElement, node);
                        event.addEventAssignment(new Assignment(var, math), false);
                    }
                }
                MetaIdInfo metaId = readMetaId(assignElement, event.getDiagramElement().getName(), Node.class, "eventAssignment", i);
                if( metaId != null )
                    metaIdInfos.add(metaId);
            }
            catch( Throwable t )
            {
                error("ERROR_EVENT_PROCESSING", new String[] {modelName, t.getMessage()});
            }
        }
    }

    protected String readMath(Element element, DiagramElement de)
    {
        String name = de == null ? "-" : de.getName();

        Element math = null;
        if( element.getTagName().equals(MATH_ELEMENT) )
        {
            math = element;
        }
        else
        {
            math = getElement(element, MATH_ELEMENT);
            if( math == null )
            {
                error("ERROR_MATH_MISSING", new String[] {modelName, name});
                return "";
            }
        }

        try
        {
            if( mathMLParser.getContext() != emodel )
                setEModelAsParserContext( true );

            variableResolver.diagramElement = de;

            int status = mathMLParser.parse(math);
            if( status > Parser.STATUS_OK )
                error("ERROR_MATHML_PARSING", new String[] {modelName, name, Utils.formatErrors(mathMLParser)});

            if( status < Parser.STATUS_FATAL_ERROR )
                return ( linearFormatter.format(mathMLParser.getStartNode()) )[1];
        }
        catch( Throwable t )
        {
            error("ERROR_MATHML_PARSING", new String[] {modelName, name, t.getMessage()});
        }

        return "";
    }

    private void setEModelAsParserContext(boolean declareVars)
    {
        mathMLParser.setContext( emodel );
        mathMLParser.declareCSymbol( "http://www.sbml.org/sbml/symbols/delay",
                new PredefinedFunction( "delay", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 2 ) );
        mathMLParser.declareCSymbol( "http://www.sbml.org/sbml/symbols/rateOf",
                new PredefinedFunction( "rateOf", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 1 ) );
        mathMLParser.declareCSymbol( new PredefinedFunction( "delay", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 2 ) );
        mathMLParser.declareCSymbol( "time" ); //???
        mathMLParser.declareCSymbol( "http://www.sbml.org/sbml/symbols/time", "time" );
        mathMLParser.declareCSymbol( "http://www.sbml.org/sbml/symbols/avogadro", "avogadro", 6.02214179E23 );
        mathMLParser.setDeclareUndefinedVariables( declareVars );
        mathMLParser.setVariableResolver( variableResolver );
    }

    public MetaIdInfo readMetaId(Element element, String name, Class<?> type, String property)
    {
        return readMetaId(element, name, type, property, -1);
    }

    public MetaIdInfo readMetaId(Element element, String nodeName, Class<?> type, String property, int i)
    {
        String metaId = element.getAttribute(METAID_ATTR);

        if( metaId == null || metaId.isEmpty() )
            return null;
        MetaIdInfo result = new MetaIdInfo(metaId, nodeName, property, i, type);
        metaIds.put(metaId, result);
        return result;
    }

    public static class MetaIdInfo
    {
        String id;
        String nodeName;
        int index;
        String property;
        Class<?> clazz;

        public MetaIdInfo(String id, String nodeName, String property, Class<?> type)
        {
            this.id = id;
            this.nodeName = nodeName;
            this.property = property;
            this.clazz = type;
        }

        public MetaIdInfo(String id, String nodeName, String property, int index, Class<?> type)
        {
            this.id = id;
            this.nodeName = nodeName;
            this.property = property;
            this.index = index;
            this.clazz = type;
        }

        public String getObjectName()
        {
            return nodeName;
        }

        public String getProperty()
        {
            return property;
        }

        public int getIndex()
        {
            return index;
        }

        public Class<?> getOjectClass()
        {
            return clazz;
        }

        public String getId()
        {
            return id;
        }
    }
}