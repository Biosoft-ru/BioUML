package biouml.plugins.sbml;

import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.DefaultParserContext;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.parser.Parser;
import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Node;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Unit;


/**
 * @pending generation of name for parameters defined in reaction.
 * Currently name is generated as <code>reactioName_parameterName</code>.
 * @pending name validations that they are valid SNames
 */
public class SbmlModelReader_11 extends SbmlModelReader
{
    ////////////////////////////////////////////////////////////////////////////
    // Constructors and public methods
    //
    public SbmlModelReader_11()
    {
        log = Logger.getLogger(SbmlModelReader_11.class.getName());
    }

    @Override
    protected DiagramType getDiagramType(Element modelElement)
    {
        return new SbmlDiagramType_L1();
    }

    @Override
    public String getId(Element element)
    {
        return element.getAttribute(NAME_ATTR);
    }

    @Override
    public String getTitle(Element element)
    {
        return element.getAttribute(NAME_ATTR);
    }

    @Override
    public NodeList getSpecieElement(Element specieList)
    {
        return specieList.getElementsByTagName(SPECIE_ELEMENT_11);
    }

    @Override
    protected NodeList getSpecieReference(Element list)
    {
        return list.getElementsByTagName(SPECIE_REFERENCE_ELEMENT_11);
    }

    @Override
    protected String getSpecieAttribute(Element specieRef)
    {
        return specieRef.getAttribute(SPECIE_ATTR_11);
    }

    ///////////////////////////////////////////////////////////////////
    // Validation issues
    //

    @Override
    protected boolean isValid(String elementName, Object element, String name)
    {
        if( elementName.equals( UNIT_DEFINITION_LIST_ELEMENT ) )
            return validateList( element, UNIT_DEFINITION_ELEMENT, name );
        
        if( elementName.equals(COMPARTMENT_LIST_ELEMENT) )
            return validateList(element, COMPARTMENT_ELEMENT, "ERROR_COMPARTMENT_LIST_MISSING", "ERROR_COMPARTMENT_LIST_EMPTY", name);

        if( elementName.equals(SPECIE_LIST_ELEMENT) )
            return validateList(element, SPECIE_ELEMENT_11, "ERROR_SPECIE_LIST_MISSING", "ERROR_SPECIE_LIST_EMPTY", name);

        if( elementName.equals(REACTION_LIST_ELEMENT) )
            return validateList(element, REACTION_ELEMENT, "ERROR_REACTION_LIST_MISSING", "ERROR_REACTION_LIST_EMPTY", name);

        if( elementName.equals(RULE_LIST_ELEMENT) )
            return true;

        if( elementName.equals(PARAMETER_LIST_ELEMENT) )
        {
            if( element instanceof Element )
            {
                NodeList list = ( (Element)element ).getElementsByTagName(PARAMETER_ELEMENT);
                if( list != null && list.getLength() > 0 )
                    return true;
            }

            return false;
        }

        if( elementName.equals(REACTANT_LIST_ELEMENT) )
            return validateList(element, SPECIE_REFERENCE_ELEMENT_11, "ERROR_REACTANT_LIST_MISSING", "ERROR_REACTANT_LIST_EMPTY", name);

        if( elementName.equals(PRODUCT_LIST_ELEMENT) )
            return validateList(element, SPECIE_REFERENCE_ELEMENT_11, "ERROR_PRODUCT_LIST_MISSING", "ERROR_PRODUCT_LIST_EMPTY", name);

        
        //--- check attributes ----

        if( elementName.equals(SPECIE_INITIAL_AMOUNT_ATTR) )
        {
            if( element instanceof Element && ( (Element)element ).hasAttribute(SPECIE_INITIAL_AMOUNT_ATTR) )
                return true;

            error("ERROR_SPECIE_AMOUNT_NOT_SPECIFIED", new String[] {modelName, name});
            return false;
        }

        if( elementName.equals(PARAMETER_VALUE_ATTR) )
        {
            if( element instanceof Element && ( (Element)element ).hasAttribute(PARAMETER_VALUE_ATTR) )
                return true;

            error("ERROR_PARAMETER_VALUE_NOT_SPECIFIED", new String[] {modelName, name});
            return false;
        }

        
        return true;
    }

    @Override
    public Node readSpecie(Element element, String specieId, String specieName) throws Exception
    {
        Node species = super.readSpecie(element, specieId, specieName);
        if( species == null )
            return species;

        VariableRole var = species.getRole( VariableRole.class );

        if( isValid(SPECIE_INITIAL_AMOUNT_ATTR, element, specieId) )
        {
            String initialAmount = element.getAttribute(SPECIE_INITIAL_AMOUNT_ATTR);
            initialAmount = initialAmount.replace(',', '.');
            try
            {
                var.setInitialValue(Double.parseDouble(initialAmount));
            }
            catch( Throwable t )
            {
                error("ERROR_SPECIE_AMOUNT", new String[] {modelName, specieId, initialAmount, t.toString()});
            }
        }

        var.setInitialQuantityType(VariableRole.AMOUNT_TYPE);
        var.setOutputQuantityType(VariableRole.AMOUNT_TYPE);
        var.setQuantityType( VariableRole.CONCENTRATION_TYPE );

        String units = element.getAttribute(UNITS_ATTR);
        if( !units.isEmpty() )
        {
            if( !emodel.getUnits().containsKey(units) )
                emodel.addUnit(new Unit(null, units));
            var.setUnits(units);
        }
        else
            var.setUnits(UNIT_SUBSTANCE);

        return species;
    }

    @Override
    public Compartment readCompartment(Element element, String compartmentId, String parentId, String compartmentName) throws Exception
    {
        Compartment compartment = super.readCompartment( element, compartmentId, parentId, compartmentName );
        if( compartment != null )
        {
            VariableRole var = compartment.getRole( VariableRole.class );
            if( element.hasAttribute( CONSTANT_ATTR ) )
                var.setConstant( "true".equals( element.getAttribute( CONSTANT_ATTR ) ) );
            else
                var.setConstant( true );
        }
        return compartment;
    }

    protected boolean validateList(Object element, String tag, String errorMissing, String errorEmpty, String name)
    {
        boolean status = true;
        String error = errorMissing;

        if( element instanceof Element )
            return validateList( ( (Element)element ).getElementsByTagName(tag), null, errorEmpty, errorEmpty, name);

        if( element instanceof NodeList )
        {
            if( ( (NodeList)element ).getLength() < 1 )
            {
                status = false;
                error = errorEmpty;
            }
        }
        else
        {
            status = false;
        }

        if( !status )
        {
            if( name == null )
                error(error, new String[] {diagram.getName()});
            else
                error(error, new String[] {diagram.getName(), name});
        }

        return status;
    }

    ///////////////////////////////////////////////////////////////////
    // Initial assignment issues
    //
    @Override
    public Node readInitialAssignment(Element initiaAssignment, int i)
    {
        // do nothing.
        // no initial assignments in this version
        return null;
    }


    ///////////////////////////////////////////////////////////////////
    // Reaction issues
    //
    @Override
    protected void validateReaction(Node reaction)
    {
        // do nothing.
        // Everything is validated by readReactant and readProduct
    }

    @Override
    protected void readKineticLawFormula(Element kineticLawElement, Node reaction, KineticLaw law)
    {
        if( !kineticLawElement.hasAttribute(FORMULA_ATTR) )
            warn("WARNING_FORMULA_ABSENTS", new String[] {modelName, reaction.getName()});
        else
        {
            String formula = null;
            try
            {
                formula = kineticLawElement.getAttribute(FORMULA_ATTR);
                law.setFormula(readMath(formula, reaction));
            }
            catch( Throwable t )
            {
                error("ERROR_FORMULA_PARSING", new String[] {modelName, reaction.getName(), t.getMessage()});
            }
        }
    }

    @Override
    protected void readStoichiometry(Element element, SpecieReference reference, Node reaction)
    {
        int stoichiometry = 1;
        if( element.hasAttribute(STOICHIOMETRY_ATTR) )
        {
            String s = element.getAttribute(STOICHIOMETRY_ATTR);
            try
            {
                stoichiometry = Integer.parseInt(s);
            }
            catch( Throwable t )
            {
                error("ERROR_STOICHIOMETRY",
                        new String[] {modelName, reference.getOrigin().getName(), reference.getName(), s, t.toString()});
            }
        }


        // BioUML has additional category MODIFIER and we try to respect it
        if( stoichiometry == 0 )
        {
            reference.setRole(SpecieReference.MODIFIER);
            reference.setStoichiometry("0");
            return;
        }

        int denominator = 1;
        if( element.hasAttribute(DENOMINATOR_ATTR) )
        {
            String s = element.getAttribute(DENOMINATOR_ATTR);
            try
            {
                denominator = Integer.parseInt(s);
            }
            catch( Throwable t )
            {
                error("ERROR_DENOMINATOR", new String[] {modelName, reference.getOrigin().getName(), reference.getName(), s, t.toString()});
            }
        }

        if( denominator == 1 )
            reference.setStoichiometry("" + stoichiometry);
        else
            reference.setStoichiometry("" + stoichiometry + "/" + denominator);
    }

    ///////////////////////////////////////////////////////////////////
    // Rule issues
    //

    @Override
    public Node readRule(Element ruleElement, int i)
    {
        try
        {
            if( !ruleElement.hasAttribute(RULE_FORMULA_ATTR) )
            {
                error("ERROR_RULE_FORMULA_ABSENT", new String[] {modelName});
                return null;
            }

            String comment = ruleElement.getAttribute(RULE_COMMENT_ATTR);
            String formula = ruleElement.getAttribute(RULE_FORMULA_ATTR);

            if( ruleElement.getNodeName().equals(RULE_ALGEBRAIC_ELEMENT) )
                ruleElement.setAttribute(TYPE_ATTR, Equation.TYPE_ALGEBRAIC);

            if( !ruleElement.hasAttribute(TYPE_ATTR) )
                error("ERROR_RULE_TYPE_ABSENT", new String[] {modelName});

            String variableName = "";

            if( ruleElement.getNodeName().equals(RULE_SPECIE_CONCENTRATION_ELEMENT) )
            {
                variableName = getSpecieAttribute(ruleElement);
                if( !specieMap.containsKey(variableName) )
                    error("ERROR_SPECIE_VARIABLE_UNKNOWN", new String[] {modelName, variableName});
            }
            else if( ruleElement.getNodeName().equals(RULE_COMPARTMENT_VOLUME_ELEMENT) )
            {
                variableName = ruleElement.getAttribute(COMPARTMENT_ATTR);
                if( !compartmentMap.containsKey(variableName) )
                    error("ERROR_COMPARTMENT_VARIABLE_UNKNOWN", new String[] {modelName});
            }
            else if( ruleElement.getNodeName().equals(RULE_PARAMETER_ELEMENT) )
            {
                variableName = ruleElement.getAttribute(NAME_ATTR);
                if( !emodel.containsVariable(variableName) )
                    error("ERROR_PARAMETER_VARIABLE_UNKNOWN", new String[] {modelName});
            }

            variableResolver.diagramElement = diagram;
            if( !variableName.equals("") )
                variableName = variableResolver.getVariableName(variableName);

            // create stub, kernel, etc.
            Stub stub = new Stub( null, "rule_" + i );
            Node node = new Node( diagram, stub );
            String expression = readMath( formula, node );

            Equation equation = new Equation( node, ruleElement.getAttribute( TYPE_ATTR ), variableName, expression );

            node.setRole( equation );
            node.setComment(comment);
            diagram.put(node);
            return node;
        }
        catch( Throwable t )
        {
            error("ERROR_RULE_PROCESSING", new String[] {modelName, t.getMessage()});
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////
    // Read math (formula) issues
    //

    protected Parser parser = new Parser();
    protected DefaultParserContext parserContext = new DefaultParserContext();

    protected void initParser()
    {
    	parser.setContext(parserContext);
        parser.setDeclareUndefinedVariables( false );
        parser.setVariableResolver(variableResolver);

        // init substitutions
        parserContext.removeConstant("avogadro");
        parserContext.declareFunctionNameSubstitution("acos", "arccos");
        parserContext.declareFunctionNameSubstitution("asin", "arcsin");
        parserContext.declareFunctionNameSubstitution("atan", "arctan");
        parserContext.declareFunctionNameSubstitution("log10", "log");
        parserContext.declareFunctionNameSubstitution("log", "ln");
        parserContext.declareFunctionNameSubstitution("pow", DefaultParserContext.POWER);
        parserContext.declareFunctionNameSubstitution("ceil", DefaultParserContext.CEIL);
        parserContext.declareFunctionNameSubstitution("factorial", DefaultParserContext.FACTORIAL);
    }

    protected AstStart getAst(String formula, DiagramElement de)
    {
        String name = de == null ? "-" : de.getName();

        if( formula == null )
        {
            error("ERROR_FORMULA_MISSING", new String[] {modelName, name});
            return null;
        }

        try
        {
            if( parser.getContext() != parserContext )
                initParser();

            parserContext.setParentContext(emodel);
            variableResolver.diagramElement = de;

            int status = parser.parse(formula);
            if( status > Parser.STATUS_OK )
            {
                error("ERROR_FORMULA_PARSING", new String[] {modelName, name, Utils.formatErrors(parser)});
            }
            if( status < Parser.STATUS_FATAL_ERROR )
                return parser.getStartNode();
        }
        catch( Throwable t )
        {
            error("ERROR_MATH_PARSING", new String[] {modelName, name, t.getMessage()});
        }
        return null;
    }

    protected String readMath(String formula, DiagramElement de)
    {
        String name = de == null ? "-" : de.getName();
        AstStart start = getAst( formula, de );
        if( start == null )
            return null;
        try
        {
            return ( linearFormatter.format( parser.getStartNode() ) )[1];
        }
        catch( Throwable t )
        {
            error( "ERROR_MATH_PARSING", new String[] {modelName, name, t.getMessage()} );
        }

        return null;
    }
}
