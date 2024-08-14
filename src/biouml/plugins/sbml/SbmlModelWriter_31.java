package biouml.plugins.sbml;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.sbml.SbmlModelReader_21.MetaIdInfo;
import biouml.standard.type.BaseUnit;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Unit;

public class SbmlModelWriter_31 extends SbmlModelWriter_24
{
    List<SbmlPackageWriter> packageWriters;


    /**
     * Method used for recursive writing of hierarchical models
     * @param sbml
     * @param doc
     * @param diagram
     * @throws Exception
     */
    public void writeDiagram(Element model, Document doc, Diagram diagram) throws Exception
    {
        this.document = doc;
        this.diagram = diagram;

        initPackages( diagram, true);

        writeDiagram(model);
    }

    @Override
    protected Element createSBMLElement()
    {
        initPackages( diagram, false);

        Element sbml = super.createSBMLElement();
        sbml.setAttribute(XMLNS_ATTR, getSbmlNamespace());
        for (SbmlPackageWriter writer: packageWriters)
            sbml.setAttribute( XMLNS_ATTR + ":" + writer.getPackageName(), writer.getNameSpace() );
        setLevel(sbml);
        setVersion(sbml);

        return sbml;
    }


    @Override
    protected void writeDiagram(Element model) throws Exception
    {
        for (SbmlPackageWriter writer: packageWriters)
        {
            writer.setParent(this);
            writer.setNewPaths( newPaths );
            writer.init( document, diagram );
            writer.preprocess( diagram );
        }

        super.writeDiagram( model );

        Element sbmlElement = (Element)model.getOwnerDocument().getElementsByTagName( SBML_ELEMENT ).item( 0 );

        for (SbmlPackageWriter writer: packageWriters)
        {
            writer.processModel( model, diagram );
            writer.processSBML( sbmlElement, diagram );
        }
    }

    protected void initPackages(Diagram diagram, boolean modelDefinition)
    {
        packageWriters = new ArrayList<>();
        DynamicProperty dp = diagram.getAttributes().getProperty( "Packages" );
        if( dp == null )
            return;
        String[] packageNames = (String[])dp.getValue();
        for( String packageName : packageNames )
        {
            SbmlPackageWriter packageWriter = SbmlPackageRegistry.getWriter( packageName );
            packageWriter.setModelDefinition( modelDefinition );
            packageWriter.setWriteBioUMLAnnotation(writeBioUMLAnnotation);
            packageWriters.add( packageWriter );
        }
    }

    @Override
    protected void setLevel(Element sbmlElement)
    {
        sbmlElement.setAttribute(SBML_LEVEL_ATTR, SBML_LEVEL_VALUE_3);
    }

    @Override
    protected void setVersion(Element sbmlElement)
    {
        sbmlElement.setAttribute(SBML_VERSION_ATTR, SBML_VERSION_VALUE_1);
    }

    @Override
    protected String getSbmlNamespace()
    {
        return SBML_LEVEL3_XMLNS_VALUE + "/version1/core";
    }

    @Override
    public Element writeCompartment(Element compartmentListElement, Compartment compartment)
    {
        Element compartmentElement = super.writeCompartment(compartmentListElement, compartment);
        String isConstant = "false";
        if( !isAutoCreated( compartment ) && compartment.getRole() instanceof VariableRole )
            isConstant = Boolean.toString( compartment.getRole( VariableRole.class ).isConstant() );
        compartmentElement.setAttribute( CONSTANT_ATTR, isConstant );
        if( compartmentElement.hasAttribute(COMPARTMENT_OUTSIDE_ATTR) )
            compartmentElement.removeAttribute(COMPARTMENT_OUTSIDE_ATTR);

        for (SbmlPackageWriter writer: packageWriters)
        {
            if (!isAutoCreated(compartment))
                writer.processSpecie(compartmentElement, compartment );
        }
        return compartmentElement;
    }

    @Override
    public Element writeSpecie(Element speciesListElement, Node species)
    {
        Element speciesElement = super.writeSpecie(speciesListElement, species);

        VariableRole role = species.getRole( VariableRole.class );

        String conversionFactor = role.getConversionFactor();
        if( conversionFactor != null )
            speciesElement.setAttribute(CONVERSION_FACTOR_ATTR, conversionFactor);

        //became mandatory in l3 v1
        if( !speciesElement.hasAttribute( CONSTANT_ATTR ) )
            speciesElement.setAttribute( CONSTANT_ATTR, Boolean.toString( role.isConstant() ) );

        //became mandatory in l3 v1
        if( !speciesElement.hasAttribute( SPECIE_BOUNDARY_CONDITION_ATTR ) )
            speciesElement.setAttribute(SPECIE_BOUNDARY_CONDITION_ATTR, Boolean.toString(role.isBoundaryCondition()));
        
        for (SbmlPackageWriter writer: packageWriters)
            writer.processSpecie(speciesElement, species );
        return speciesElement;
    }

    @Override
    public Element writeParameter(Element parameterListElement, Variable parameter)
    {
        Element parameterElement = super.writeParameter( parameterListElement, parameter );
        for (SbmlPackageWriter writer: packageWriters)
            writer.processParameter(parameterElement, parameter);
        return parameterElement;
    }

    @Override
    public Element writeReaction(Element reactionListElement, Node reaction)
    {
        super.writeReaction(reactionListElement, reaction);
        NodeList list = reactionListElement.getChildNodes();
        Element reactionElement = (Element)list.item(list.getLength() - 1);
        for (SbmlPackageWriter writer: packageWriters)
            writer.processReaction(reactionElement, reaction);
        return reactionElement;
    }
    
    @Override
    protected void writeReactionAttributes(Reaction reaction, Element element)
    {
        //both attributes became mandatory
        element.setAttribute( REACTION_REVERSIBLE_ATTR, Boolean.toString(reaction.isReversible()) );
        element.setAttribute( REACTION_FAST_ATTR, Boolean.toString(reaction.isFast()) );
    }

    @Override
    protected void writeSpeciesReferenceAttributes(Element specieReferenceElement, SpecieReference species, Node reaction)
    {
        super.writeSpeciesReferenceAttributes(specieReferenceElement, species, reaction);
        if( specieReferenceElement.getNodeName().equals(SPECIE_REFERENCE_ELEMENT) )
        {
            VariableRole var = (VariableRole)emodel.getVariable(species.getSpecie());
            if( var != null )
                specieReferenceElement.setAttribute(CONSTANT_ATTR, Boolean.toString(var.isConstant()));
        }
    }

    @Override
    public Element writeEvent(Event event, Element eventListElement)
    {
        Element element = super.writeEvent(event, eventListElement);

        if( !event.getPriority().isEmpty() )
        {
            Element priorityElement = document.createElement(PRIORITY_ELEMENT);
            appendMathChild(priorityElement, event.getPriority(), event);
            element.appendChild(priorityElement);

            MetaIdInfo info = getMetaId( (Node)event.getDiagramElement(), "priority" );
            if (info !=null)
                priorityElement.setAttribute( METAID_ATTR, info.id );
        }
        NodeList childNodes = element.getChildNodes();
        for( int i = 0; i < childNodes.getLength(); i++ )
        {
            if( childNodes.item(i).getNodeName().equals(TRIGGER_ELEMENT) )
            {
                Element triggerElement = (Element)childNodes.item(i);
                triggerElement.setAttribute(TRIGGER_PERSISTENT_ATTR, String.valueOf(event.isTriggerPersistent()));
                triggerElement.setAttribute(TRIGGER_INITIAL_VALUE_ATTR, String.valueOf(event.isTriggerInitialValue()));
            }
            //TODO: somehow set time units of delay to cn element of MathML
        }

        return element;
    }

    @Override
    protected void writeModelAttributes(Element model)
    {
        super.writeModelAttributes(model);
        if( !SbmlEModel.CONVERSION_FACTOR_UNDEFINED.equals(emodel.getConversionFactor()) )
            model.setAttribute(CONVERSION_FACTOR_ATTR, emodel.getConversionFactor());
        if( !Unit.UNDEFINED.equals(emodel.getLengthUnits()) )
            model.setAttribute(LENGTH_UNITS_ATTR, emodel.getLengthUnits());
        if( !Unit.UNDEFINED.equals(emodel.getAreaUnits()) )
            model.setAttribute(AREA_UNITS_ATTR, emodel.getAreaUnits());
        if( !Unit.UNDEFINED.equals(emodel.getVolumeUnits()) )
            model.setAttribute(VOLUME_UNITS_ATTR, emodel.getVolumeUnits());
        if( !Unit.UNDEFINED.equals(emodel.getExtentUnits()) )
            model.setAttribute(EXTENT_UNITS_ATTR, emodel.getExtentUnits());
        if( !Unit.UNDEFINED.equals(emodel.getTimeUnits()) )
            model.setAttribute(TIME_UNITS_ATTR, emodel.getTimeUnits());
        else if (!Unit.UNDEFINED.equals(emodel.getVariable( "time" ).getUnits()))
                model.setAttribute(TIME_UNITS_ATTR, emodel.getVariable( "time" ).getUnits());
        if( !Unit.UNDEFINED.equals(emodel.getSubstanceUnits()) )
            model.setAttribute(SUBSTANCE_UNITS_ATTR, emodel.getSubstanceUnits());
    }

    @Override
    public Element writeBaseUnit(BaseUnit baseUnit, Element baseUnitsListElement)
    {
        Element baseUnitElement = document.createElement(UNIT_ELEMENT);
        baseUnitsListElement.appendChild(baseUnitElement);
        baseUnitElement.setAttribute(UNIT_KIND_ATTR, baseUnit.getType());
        baseUnitElement.setAttribute(UNIT_EXPONENT_ATTR, Integer.toString(baseUnit.getExponent()));
        baseUnitElement.setAttribute(UNIT_SCALE_ATTR, Integer.toString(baseUnit.getScale()));
        baseUnitElement.setAttribute(UNIT_MULTIPLIER_ATTR, Double.toString(baseUnit.getMultiplier()));
        return baseUnitElement;
    }
}
