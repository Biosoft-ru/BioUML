package biouml.plugins.sbml;

import org.w3c.dom.Element;

import biouml.model.Node;
import biouml.standard.type.SpecieReference;

public class SbmlModelWriter_12 extends SbmlModelWriter_L1
{
    @Override
    protected Element createSpecieElement()
    {
        return document.createElement(SPECIE_ELEMENT);
    }

    @Override
    protected void setVersion(Element sbmlElement)
    {
        sbmlElement.setAttribute(SBML_VERSION_ATTR, SBML_VERSION_VALUE_2);
    }

    @Override
    protected Element writeReactionList(Element model)
    {
        Element reactionListElement = document.createElement(REACTION_LIST_ELEMENT);
        writeReactionList(reactionListElement, diagram);
        return reactionListElement;
    }

    @Override
    protected void writeReactants(Element reactionElement, Node reaction)
    {
        Element reactantListElement = document.createElement(REACTANT_LIST_ELEMENT);
        writeSpecieReferences(reactantListElement, reaction, SpecieReference.REACTANT);

        // we write modifiers as reactants, for level2 this method is rewrited
        writeSpecieReferences(reactantListElement, reaction, SpecieReference.MODIFIER);

        if( reactantListElement.hasChildNodes() )
            reactionElement.appendChild(reactantListElement);
    }

    @Override
    protected void writeProducts(Element reactionElement, Node reaction)
    {
        Element productListElement = document.createElement(PRODUCT_LIST_ELEMENT);
        reactionElement.appendChild(productListElement);
        if( productListElement.hasChildNodes() )
            writeSpecieReferences(productListElement, reaction, SpecieReference.PRODUCT);
    }
}