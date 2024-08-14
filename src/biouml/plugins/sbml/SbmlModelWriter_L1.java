package biouml.plugins.sbml;

import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;

import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Specie;

public class SbmlModelWriter_L1 extends SbmlModelWriter
{
    @Override
    protected void initContext()
    {
    }

    @Override
    protected Element createSpecieElement()
    {
        return document.createElement(SPECIE_ELEMENT_11);
    }

    @Override
    protected void setLevel(Element sbmlElement)
    {
        sbmlElement.setAttribute(SBML_LEVEL_ATTR, SBML_LEVEL_VALUE_1);
    }

    @Override
    protected void setVersion(Element sbmlElement)
    {
        sbmlElement.setAttribute(SBML_VERSION_ATTR, SBML_VERSION_VALUE_1);
    }

    @Override
    protected void setId(Element element, String name)
    {
        element.setAttribute(NAME_ATTR, name);
    }

    @Override
    protected void setTitle(Element element, String name)
    {
    }

    @Override
    protected boolean validCompartmentList(List compartmentList)
    {
        return compartmentList.size() > 0;
    }

    @Override
    protected boolean validReaction(Node reaction)
    {
        int counterReactant = 0, counterProduct = 0;

        for( SpecieReference species: ( (Reaction)reaction.getKernel() ) )
        {
            if( species.getRole().equals(SpecieReference.REACTANT) )
                counterReactant++;
            if( species.getRole().equals(SpecieReference.PRODUCT) )
                counterProduct++;
        }
        if( counterReactant > 0 && counterProduct > 0 )
            return true;

        error("ERROR_REACTION_INVALID", new String[] {diagram.getName(), reaction.getName()});
        return false;
    }

    @Override
    protected void writeFunctionDefinitionList(Element model)
    {
        error("ERROR_FUNCTION_DEFINITIONS_NOT_SUPPORTED", new String[] {diagram.getName()});
    }


    @Override
    protected void writeRuleList(Element model)
    {
    }

    @Override
    protected void writeInitialAssignmentList(Element model)
    {
    }

    @Override
    protected void writeEventList(Element model)
    {
        error("ERROR_EVENTS_NOT_SUPPORTED", new String[] {diagram.getName()});
    }
    
    @Override
    protected void writeConstraintList(Element model)
    {
        error("ERROR_CONSTRAINTS_NOT_SUPPORTED", new String[] {diagram.getName()});
    }

    @Override
    protected void writeFormula(String formula, Node reaction, Element kineticLawElement)
    {
        if( formula != null && formula.length() > 0 )
        {
            kineticLawElement.setAttribute(FORMULA_ATTR, formula);
        }
        else
        {
            kineticLawElement.setAttribute(FORMULA_ATTR, "0");
            warn("WARN_FORMULA_EMPTY", new String[] {diagram.getName(), reaction.getName()});
        }
    }

    @Override
    protected Element writeCompartmentList(Element model)
    {
        Element compartmentListElement = super.writeCompartmentList(model);
        model.appendChild(compartmentListElement);
        return compartmentListElement;
    }

    @Override
    protected Element writeSpecieList(Element model)
    {
        Element speciesListElement = super.writeSpecieList(model);
        model.appendChild(speciesListElement);
        return speciesListElement;
    }

    @Override
    protected Element writeReactionList(Element model)
    {
        Element reactionListElement = super.writeReactionList(model);
        model.appendChild(reactionListElement);
        return reactionListElement;
    }

    @Override
    protected void writeSpeciesReferenceAttributes(Element specieReferenceElement, SpecieReference species, Node reaction)
    {
        String stoichiometry = species.getStoichiometry();
        if( stoichiometry == null )
            return;
        String s = stoichiometry.trim();
        if( s.isEmpty() )
            return;

        AstStart start = emodel.readMath(s, reaction.getRole());

        if( start.jjtGetNumChildren() == 1 && start.jjtGetChild(0) instanceof AstConstant )
        {
            Object value = ( (AstConstant)start.jjtGetChild(0) ).getValue();

            if( value instanceof Integer )
                specieReferenceElement.setAttribute(STOICHIOMETRY_ATTR, "" + s);

            // approximate through stoichiometry/denominator
            else if( value instanceof Double )
            {
                double v = ( (Double)value ).doubleValue();
                specieReferenceElement.setAttribute(STOICHIOMETRY_ATTR, "" + Math.round(v * 1000));
                specieReferenceElement.setAttribute(DENOMINATOR_ATTR, "1000");

                warn("WARNING_WRITE_STOICHIOMETRY_11", new String[] {modelName, species.getName(), s, "" + Math.round(v * 1000) + "/1000"});
            }

            return;
        }

        // other variant start = a / b
        if( start.jjtGetNumChildren() == 2

        && start.jjtGetChild(0) instanceof AstFunNode && ( (AstFunNode)start.jjtGetChild(0) ).getFunction().getName().equals("/")

        && start.jjtGetChild(1) instanceof AstConstant && ( (AstConstant)start.jjtGetChild(1) ).getValue() instanceof Integer

        && start.jjtGetChild(2) instanceof AstConstant && ( (AstConstant)start.jjtGetChild(2) ).getValue() instanceof Integer )
        {
            specieReferenceElement.setAttribute(STOICHIOMETRY_ATTR, "" + ( (AstConstant)start.jjtGetChild(1) ).getValue());
            specieReferenceElement.setAttribute(DENOMINATOR_ATTR, "" + ( (AstConstant)start.jjtGetChild(1) ).getValue());
            return;
        }

        error("ERROR_WRITE_STOICHIOMETRY_11", new String[] {modelName, species.getName(), s});
    }

    /**
     * Checks if the model represented by diagram is representable
     * in SBML level 1 version 1 model.
     */
    public static boolean isValidModel(Diagram diagram)
    {
        EModel emodel = (EModel)diagram.getRole();

        if( emodel == null )
            return false;

        int modelType = emodel.getModelType();

        if( EModel.isOfType(modelType, EModel.STATE_TRANSITION_TYPE) || EModel.isOfType(modelType, EModel.EVENT_TYPE)
                || EModel.isOfType(modelType, EModel.ALGEBRAIC_TYPE) )
            return false;

        if( !hasElement(diagram, Reaction.class) || !hasElement(diagram, Specie.class) || !hasElement(diagram, Compartment.class) )
            return false;

        return true;
    }

    private static boolean hasElement(Compartment compartment, Class element)
    {
        Iterator<DiagramElement> iter = compartment.iterator();
        while( iter.hasNext() )
        {
            Object obj = iter.next();

            if( element.equals(Compartment.class) )
            {
                if( obj instanceof Compartment )
                    return true;
            }
            else
            {
                if( obj instanceof Node )
                {
                    Node node = (Node)obj;
                    if( element.isInstance(node.getKernel()) )
                        return true;
                }
                else if( obj instanceof Compartment )
                {
                    if( hasElement((Compartment)obj, element) )
                        return true;
                }
            }
        }
        return false;
    }

    @Override
    protected String getSbmlNamespace()
    {
        return SBML_LEVEL1_XMLNS_VALUE;
    }

    @Override
    protected String getUnitsAttr()
    {
        return UNITS_ATTR;
    }

    @Override
    protected String getCompartmentVolumeAttr()
    {
        return COMPARTMENT_VOLUME_ATTR;
    }
}