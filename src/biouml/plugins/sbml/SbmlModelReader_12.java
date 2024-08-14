package biouml.plugins.sbml;

import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ru.biosoft.math.model.Utils;
import biouml.model.Compartment;
import biouml.model.DiagramType;
import biouml.model.Node;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Stub;


/**
 * 1.2 version differs only by validation
 */
public class SbmlModelReader_12 extends SbmlModelReader_11
{

    ////////////////////////////////////////////////////////////////////////////
    // Constructors and public methods
    //
    public SbmlModelReader_12()
    {
        log = Logger.getLogger(SbmlModelReader_12.class.getName());
    }

    protected DiagramType getDiagramType()
    {
        return new SbmlDiagramType_L1();
    }

    @Override
    protected boolean isValid(String elementName, Object element, String name)
    {
        if( elementName.equals( UNIT_DEFINITION_LIST_ELEMENT ) )
            return validateList( element, UNIT_DEFINITION_ELEMENT, name );
        
        if( elementName.equals(COMPARTMENT_LIST_ELEMENT) )
            return validateList(element, COMPARTMENT_ELEMENT, name);

        if( elementName.equals(SPECIE_LIST_ELEMENT) )
            return validateList(element, SPECIE_ELEMENT, name);

        if( elementName.equals(REACTION_LIST_ELEMENT) )
            return validateList(element, REACTION_ELEMENT, name);

        if( elementName.equals(RULE_LIST_ELEMENT) )
            if( element instanceof Element )
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
            return validateList(element, SPECIE_REFERENCE_ELEMENT, "ERROR_REACTANT_LIST_MISSING", "ERROR_REACTANT_LIST_EMPTY", name);

        if( elementName.equals(PRODUCT_LIST_ELEMENT) )
            return validateList(element, SPECIE_REFERENCE_ELEMENT, "ERROR_PRODUCT_LIST_MISSING", "ERROR_PRODUCT_LIST_EMPTY", name);

        // --- check attributes ----
        if( elementName.equals(SPECIE_INITIAL_AMOUNT_ATTR) )
        {
            if( element instanceof Element && ( (Element)element ).hasAttribute(SPECIE_INITIAL_AMOUNT_ATTR) )
                return true;

            error("ERROR_SPECIE_AMOUNT_NOT_SPECIFIED", new String[] {modelName, name});
            return false;
        }

        return true;
    }

    @Override
    public NodeList getSpecieElement(Element specieList)
    {
        return specieList.getElementsByTagName(SPECIE_ELEMENT);
    }

    @Override
    protected NodeList getSpecieReference(Element list)
    {
        return list.getElementsByTagName(SPECIE_REFERENCE_ELEMENT);
    }

    @Override
    protected String getSpecieAttribute(Element specieRef)
    {
        return specieRef.getAttribute(SPECIE_ATTR);
    }

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
            {
                ruleElement.setAttribute(TYPE_ATTR, Equation.TYPE_SCALAR);
                error("ERROR_RULE_TYPE_ABSENT", new String[] {modelName});
            }
            else
            {
                ruleElement.setAttribute(TYPE_ATTR, ruleElement.getAttribute(TYPE_ATTR));
            }


            String variableName = "";

            if( ruleElement.getNodeName().equals(RULE_SPECIE_CONCENTRATION_ELEMENT_L1V2) )
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
                {
                    error("ERROR_PARAMETER_VARIABLE_UNKNOWN", new String[] {modelName});
                }
                else
                    emodel.getVariable(variableName).setConstant(false);
            }

            // create stub, kernel, etc.
            Stub stub = new Stub(null, "rule_" + i);
            Node node = new Node(diagram, stub);
            String expression = readMath(formula, node);

            checkForCompartments(null, variableName, formula, Equation.TYPE_ALGEBRAIC.equals( ruleElement.getAttribute( TYPE_ATTR ) ) );

            variableResolver.diagramElement = diagram;
            if( !variableName.equals("") )
                variableName = variableResolver.getVariableName(variableName);

            Equation equation = new Equation(node, ruleElement.getAttribute(TYPE_ATTR), variableName, expression);

            node.setRole(equation);
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

    /**
     * In l1v2, variables do not have attribute "constant", but we should consider compartment constant if it does not participate in equations directly.<br>
     * It may participate indirectly though e.g. if S1 participate in equation, it is concentration of species "S1", so we will replace it (during preprocessing) by "S1/C", <br>
     * 
     * e.g.:<ul>
     * <li>Model 1:<br>
     * S1 - 2 = 0<br>
     * algebraic equation will be processed into:<br>
     * S1/C - 2 = 0, <br>
     * here we have one variables and one algebraic equations, C is constant<br>
     * 
     * <li>Model 2:<br>
     * S1 - 2 = 0<br>
     * C = 5<br>
     * will be translated into<br>
     * S1/C - 2 = 0<br>
     * C = 5<br>
     */
    private void checkForCompartments(Node de, String variableName, String formula, boolean algebraic)
    {
        if( algebraic )
        {
            Utils.variables( getAst( formula, de ) ).map( emodel::getVariable ).select( VariableRole.class )
                .map( role -> compartmentMap.get( role.getDiagramElement().getCompleteNameInDiagram() ) ).nonNull()
                .map( Compartment::getRole ).select( VariableRole.class )
                .forEach( role -> role.setConstant( false ) ); //if compartment participates in algebraic then we should consider it as not constant
        }
        else if( compartmentMap.containsKey( variableName )  && compartmentMap.get(  variableName  ).getRole() instanceof VariableRole)
            compartmentMap.get( variableName ).getRole( VariableRole.class ).setConstant( false ); //if compartment participates in rules then we should consider it as not constant
    }
}