package biouml.plugins.sbml;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.util.DiagramXmlWriter;
import biouml.plugins.sbml.extensions.SbmlAnnotationRegistry;
import biouml.plugins.sbml.extensions.SbmlAnnotationRegistry.SbmlAnnotationInfo;
import biouml.plugins.sbml.extensions.SbmlExtension;
import biouml.standard.diagram.Util;
import biouml.standard.type.BaseUnit;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Unit;
import ru.biosoft.access.core.DataCollection;

/**
 * Writes diagram in SBML format.
 *
 * SbmlModelWriter can correctly process following types of diagram:
 * <ul>
 *   <li>SbmlDiagramType</li>
 *   <li>PathwaySimulationDiagramType. This standard BioUML diagram type is most closely
 *       corresponds to SBML diagram type</li>
 *   <li>PathwayDiagramType</li>
 * </ul>
 *
 * Reactant and product are obligatory elements of SBML reaction,
 * but it BioUML allows user to create reaction without reactants or products.
 * So to respect SBML requirements REACTANT_STUB or PRODUCT_STUB can be added into
 * reaction and model.
 *
 * <p>SbmlModelReader can recognize such stubs and remove them from model automatically.
 * Thus "_in_empty_set" (REACTANT_STUB) and "_out_empty_set" are reserved key words that should not be used
 * as species names.
 *
 * @pending SName validation
 *
 * @pending warn if species list or reaction list is empty.
 */
abstract public class SbmlModelWriter extends SbmlSupport
{
    protected Document document;
    protected List<Compartment> compartmentList;
    protected String defaultCompartmentName;
    protected Element speciesListElement;
    protected boolean doReactantStub = false;
    protected boolean doProductStub = false;
    protected boolean writeBioUMLAnnotation = true;

    public void setWriteBioUMLAnnotation(boolean writeBioUMLAnnotation)
    {
        this.writeBioUMLAnnotation = writeBioUMLAnnotation;
    }

    public boolean isWriteBioUMLAnnotation()
    {
        return writeBioUMLAnnotation;
    }
    ////////////////////////////////////////////////////////////////////////////
    //  Abstract methods
    //

    abstract void writeSpeciesReferenceAttributes(Element speciesReference, SpecieReference ref, Node reaction);

    ////////////////////////////////////////////////////////////////////////////
    // Constructor and public methods
    //

    public SbmlModelWriter()
    {
        log = Logger.getLogger(SbmlModelWriter.class.getName());
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Abstract stuff
    //

    protected abstract void setLevel(Element sbmlElement);

    protected abstract void setVersion(Element sbmlElement);

    protected abstract String getSbmlNamespace();

    protected abstract String getUnitsAttr();

    protected abstract String getCompartmentVolumeAttr();

    /**
     * Sets 'id' or 'name' attribute (according to level and version)
     * equal to given string.
     */
    protected abstract void setId(Element element, String id);

    /**
     * Sets nothing or 'name' attribute (according to level and version)
     * equal to given string.
     */
    protected abstract void setTitle(Element element, String title);

    /**
     * Initializes context of the writer.
     */
    protected abstract void initContext();

    /**
     * Returns true if compartment list is valid according
     * to given version of SBML specification.
     */
    protected abstract boolean validCompartmentList(List compartmentList);

    /**
     * Returns true if reaction is valid according to given
     * version of SBML specification.
     */
    protected abstract boolean validReaction(Node reaction);

    protected Element createSBMLElement()
    {
        Element sbml = document.createElement(SBML_ELEMENT);
        sbml.setAttribute(XMLNS_ATTR, getSbmlNamespace());
        setLevel(sbml);
        setVersion(sbml);
        return sbml;
    }

    public Document createDOM(Diagram sourceDiagram) throws Exception
    {
        if( sourceDiagram == null )
        {
            error("ERROR_DIAGRAM_NULL", new String[] {});
            throw new NullPointerException(MessageBundle.resources.getResourceString("ERROR_DIAGRAM_NULL"));
        }

        diagram = sourceDiagram;
        modelName = diagram.getName();

        initContext();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.newDocument();

        Element element =  createSBMLElement();
        document.appendChild( element );

        Element model = document.createElement(MODEL_ELEMENT);
        element.appendChild(model);

        writeDiagram(model);

        return document;
    }

    public void writeNotes(Element element, String notes)
    {
        if( notes == null || notes.isEmpty() )
            return;
        Element notesElement = document.createElement(NOTES_ELEMENT);
        element.appendChild(notesElement);
        writeXhtml(document, notesElement, notes);
    }

    ////////////////////////////////////////////////////////////////////////////

    protected void writeDiagram(Element model) throws Exception
    {
        this.sbmlIds = new HashSet<>();
        this.nodeToSbmlIds = new HashMap<>();
        parameterToSbmlIds = new HashMap<>();
        compartmentList = fillCompartmentList(diagram);
        if( diagram.getKernel() instanceof DiagramInfo )
        {
            DiagramInfo info = (DiagramInfo)diagram.getKernel();
            writeNotes(model, info.getDescription());
        }

        emodel = (SbmlEModel)diagram.getRole();

        writeModelAttributes(model);

        Element annotationElement = document.createElement(ANNOTATION_ELEMENT);
        writeAnnotation(annotationElement, diagram);
        
        if( writeBioUMLAnnotation )
            writeBioUMLAnnotation(annotationElement);

        if( annotationElement.hasChildNodes() )
            model.appendChild(annotationElement);

        writeFunctionDefinitionList(model);
        writeUnitList(model);
        writeCompartmentTypeList(model);
        writeCompartmentList(model);
        writeSpecieTypeList(model);
        writeSpecieList(model);
        writeParameterList(model);
        writeInitialAssignmentList(model);
        writeRuleList(model);
        writeReactionList(model);
        writeEventList(model);
        writeConstraintList(model);
    }

    private void writeBioUMLAnnotation(Element annotationElement)
    {
        Element bioumlElement = document.createElement( BIOUML_ELEMENT );
        bioumlElement.setAttribute( BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE );
       
        Element diagrammInfoElement = document.createElement(BIOUML_DIAGRAM_INFO_ELEMENT);
        bioumlElement.appendChild(diagrammInfoElement);

        Element element = document.createElement(BIOUML_VIEW_OPTIONS_ELEMENT);
        element.setAttribute(BIOUML_DEPENDENCY_EDGES_ATTR, String.valueOf(diagram.getViewOptions().isDependencyEdges()));
        element.setAttribute(BIOUML_AUTOLAYOUT_ATTR, String.valueOf(diagram.getViewOptions().isAutoLayout()));

        Element simulationInfoElement = DiagramXmlWriter.writeSimulationOptions(document, BIOUML_SIMULATION_INFO_ELEMENT, diagram);
        if( simulationInfoElement != null )
            bioumlElement.appendChild(simulationInfoElement);

        Element plots = new DiagramXmlWriter().writePlotsInfo(document, BIOUML_PLOT_INFO_ELEMENT, diagram, newPaths);
        if( plots != null )
            bioumlElement.appendChild(plots);
 
        String bioHub = diagram.getAttributes().getValueAsString( BIOUML_BIOHUB_ATTR );
        String refType = diagram.getAttributes().getValueAsString( BIOUML_REFERENCE_TYPE_ATTR );
        String converter = diagram.getAttributes().getValueAsString( BIOUML_CONVERTER_ATTR );
        String species = diagram.getAttributes().getValueAsString( BIOUML_SPECIES_ATTR );
        Element dbInfo = document.createElement( BIOUML_DB_INFO_ELEMENT );
        if( bioHub != null )
            dbInfo.setAttribute( BIOUML_BIOHUB_ATTR, bioHub );
        if( refType != null )
            dbInfo.setAttribute( BIOUML_REFERENCE_TYPE_ATTR, refType );
        if( converter != null )
            dbInfo.setAttribute( BIOUML_CONVERTER_ATTR, converter );
        if( species != null )
            dbInfo.setAttribute( BIOUML_SPECIES_ATTR, species );

        if( dbInfo.hasAttributes() )
            bioumlElement.appendChild( dbInfo );
            
        bioumlElement.appendChild( element );
        annotationElement.appendChild( bioumlElement );
    }
    /**
     * Function definitions are only supported since level 2
     */
    protected abstract void writeFunctionDefinitionList(Element model);
    public Element writeFunction(Function function, Element functionListElement)
    {
        return null;
    }

    /**
     * Writes list of rules, that are not rules derived from reactions
     */
    protected abstract void writeRuleList(Element model);
    public Element writeRule(Equation equation, Element ruleListElement)
    {
        return null;
    }

    protected abstract void writeInitialAssignmentList(Element model);
    public Element writeInitialAssignment(Equation initialAssignments, Element initialAssignmentsListElement)
    {
        return null;
    }

    protected abstract void writeEventList(Element model);
    public Element writeEvent(Event event, Element eventListElement)
    {
        return null;
    }
    
    protected abstract void writeConstraintList(Element model);
    public Element writeConstraint(Constraint constraint, Element constraintListElement)
    {
        return null;
    }
    
    protected void writeModelAttributes(Element model)
    {
        model.setAttribute(NAME_ATTR, diagram.getTitle());
        setId(model, getSbmlId(diagram));
    }

    protected void writeCompartmentTypeList(Element model)
    {
    }
    protected void writeSpecieTypeList(Element model)
    {

    }

    ////////////////////////////////////////////////////////////////////////////
    // Write compartment issues
    //
    protected Element writeCompartmentList(Element model)
    {
        // decide whether we should diagram as compartment to the list:
        // it is needed if:
        // 1) diagram contains species without compartments

        boolean includeDiagram = diagram.stream( Node.class ).anyMatch(
                node -> node.getKernel() instanceof Specie && node.getParent() == diagram );

        if (includeDiagram)
        {
            defaultCompartmentName = DefaultSemanticController.generateUniqueNodeName( diagram, "default", true, "" );
            Compartment defaultCompartment = new Compartment(diagram, defaultCompartmentName, new biouml.standard.type.Compartment(null, diagram.getName()));
            compartmentList.add(0, defaultCompartment);
            setAutoCreated( defaultCompartment );
            //set correct diagram size
            Rectangle dRect = diagram.stream( Node.class ).append( diagram ).mapToEntry( Node::getLocation, Node::getShapeSize )
                    .mapKeyValue( (loc, size) -> new Rectangle( loc, size == null ? new Dimension( 0, 0 ) : size ) )
                    .reduce( Rectangle::union ).get();
            defaultCompartment.setShapeSize( dRect.getSize() );
        }

        Element compartmentListElement = null;
        if( validCompartmentList(compartmentList) )
        {
            compartmentListElement = document.createElement(COMPARTMENT_LIST_ELEMENT);

            for( Compartment compartment : compartmentList )
            {
                try
                {
                    writeCompartment(compartmentListElement, compartment);
                }
                catch( Throwable t )
                {
                    error("ERROR_COMPARTMENT_WRITING", new String[] {diagram.getName(), compartment != null? compartment.getName(): "", t.getMessage()});
                }
            }
        }

        return compartmentListElement;
    }

    public Element writeCompartment(Element compartmentListElement, Compartment compartment)
    {
        Element compartmentElement = document.createElement(COMPARTMENT_ELEMENT);
        compartmentListElement.appendChild(compartmentElement);

        setId(compartmentElement, getSbmlId(compartment));
        setTitle(compartmentElement, compartment.getTitle());

        if( !isAutoCreated(compartment) )
        {
            DataCollection parent = compartment.getOrigin();
            if( parent != null && !parent.getName().equals(diagram.getName()) )
                compartmentElement.setAttribute(COMPARTMENT_OUTSIDE_ATTR, parent.getName());

            if( compartment.getRole() != null && compartment.getRole() instanceof VariableRole )
            {
                Variable var = compartment.getRole( VariableRole.class );
                compartmentElement.setAttribute(getCompartmentVolumeAttr(), "" + var.getInitialValue());
                if( var.getUnits() != null && !var.getUnits().equals("") )
                    compartmentElement.setAttribute(UNITS_ATTR, var.getUnits());
                writeNotes(compartmentElement, var.getComment());
            }
        }
        
        // write compartment info as BioUML extension
        Element annotationElement = document.createElement(ANNOTATION_ELEMENT);
        writeAnnotation(annotationElement, compartment);

        if( writeBioUMLAnnotation )
        {
            Element bioumlElement = document.createElement( BIOUML_ELEMENT );
            bioumlElement.setAttribute( BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE );

            Element compartmentInfoElement = document.createElement( BIOUML_COMPARTMENT_INFO_ELEMENT );
            DiagramXmlWriter.writeCompartmentInfo( compartmentInfoElement, compartment, document );
            compartmentInfoElement.setAttribute( BIOUML_COMPLETE_NAME_ATTR, compartment.getCompleteNameInDiagram() );

            if( isAutoCreated(compartment) )
                compartmentInfoElement.setAttribute(BIOUML_DEFAULT_COMPARTMENT_ATTR, "true");
            else
                writeVariable(compartment.getRole( VariableRole.class ), bioumlElement );
            bioumlElement.appendChild( compartmentInfoElement );
            annotationElement.appendChild( bioumlElement );
        }
        if( annotationElement.hasChildNodes() )
            compartmentElement.appendChild(annotationElement);

        return compartmentElement;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Write species issues
    //

    protected Element writeSpecieList(Element model)
    {
        speciesListElement = document.createElement(SPECIE_LIST_ELEMENT);
        
        for (Node node: fillSpecieList( diagram ))
        {
            try
            {
                writeSpecie(speciesListElement, node);
            }
            catch( Throwable t )
            {
                error("ERROR_SPECIE_LIST_WRITING", new String[] {diagram.getName(), diagram.getName(), t.getMessage()});
            }
        }
        return speciesListElement;
    }

    protected abstract Element createSpecieElement();

    public Element writeSpecie(Element speciesListElement, Node species)
    {
        try
        {
            Element speciesElement = createSpecieElement();
            speciesListElement.appendChild(speciesElement);
            setId(speciesElement, getSbmlId(species));
            if( species.getTitle() != null )
                setTitle(speciesElement, species.getTitle());
            Compartment parent = (Compartment)species.getOrigin();
            String compartment = ( parent instanceof Diagram ) ? defaultCompartmentName : getSbmlId(parent);
            speciesElement.setAttribute(COMPARTMENT_ATTR, compartment);

            if( species.getRole() instanceof Variable )
            {
                VariableRole var = species.getRole( VariableRole.class );
                speciesElement.setAttribute(SPECIE_INITIAL_AMOUNT_ATTR, "" + var.getInitialValue());
                if( var.getUnits() != null && !var.getUnits().equals("") )
                    speciesElement.setAttribute(getUnitsAttr(), var.getUnits());
                writeNotes(speciesElement, var.getComment());

                if( var.isBoundaryCondition() )
                    speciesElement.setAttribute(SPECIE_BOUNDARY_CONDITION_ATTR, "true");
            }
            else
            {
                // initial amount is obligatory attribute for SBML, so we respect it
                speciesElement.setAttribute(SPECIE_INITIAL_AMOUNT_ATTR, "" + 0.0);
                warn("WARNING_SPECIE_AMOUNT_NOT_SPECIFIED", new String[] {diagram.getName(), species.getName()});
            }

            Specie speciesKernel = (Specie)species.getKernel();
            if( speciesKernel != null && speciesKernel.getCharge() != 0 )
                speciesElement.setAttribute(SPECIE_CHARGE_ATTR, "" + speciesKernel.getCharge());

            // write node info as BioUML extension
            Element annotationElement = document.createElement(ANNOTATION_ELEMENT);

            writeAnnotation(annotationElement, species);

            if( writeBioUMLAnnotation )
            {
                Element bioumlElement = document.createElement( BIOUML_ELEMENT );
                bioumlElement.setAttribute( BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE );

                Element nodeInfoElement = document.createElement( BIOUML_NODE_INFO_ELEMENT );
                DiagramXmlWriter.writeNodeInfo( nodeInfoElement, species, document );
                nodeInfoElement.setAttribute( BIOUML_COMPLETE_NAME_ATTR , species.getCompleteNameInDiagram() );
                bioumlElement.appendChild( nodeInfoElement );
                writeVariable( species.getRole( VariableRole.class ), bioumlElement );
                if( speciesKernel != null && !speciesKernel.getType().equals( Specie.TYPE_MOLECULE ) )
                {
                    Element speciesInfoElement = document.createElement( BIOUML_SPECIE_INFO_ELEMENT );
                    bioumlElement.appendChild( speciesInfoElement );
                    speciesInfoElement.setAttribute( BIOUML_SPECIE_TYPE_ATTR, speciesKernel.getType() );
                    DiagramXmlWriter.serializeDPS( document, speciesInfoElement, speciesKernel.getAttributes(), null );
                    
                    bioumlElement.appendChild( speciesInfoElement );
                }

                annotationElement.appendChild( bioumlElement );
            }
            if( annotationElement.hasChildNodes() )
                speciesElement.appendChild(annotationElement);

            return speciesElement;
        }
        catch( Throwable t )
        {
            error("ERROR_SPECIE_WRITING", new String[] {diagram.getName(), species.getName(), t.getMessage()});
        }
        return null;
    }

    
    public void writeVariable(Variable variable, Element element)
    {
        Element varInfoElement = document.createElement( BIOUML_VARIABLE_INFO_ELEMENT );

        if( variable.getComment() != null )
            varInfoElement.setAttribute(BIOUML_VARIABLE_COMMENT, variable.getComment());
        
        if (varInfoElement.hasAttributes())
            element.appendChild(varInfoElement);
    }
    ////////////////////////////////////////////////////////////////////////////
    // Write parameters issues
    //

    protected void writeParameterList(Element element)
    {
        Element parameterListElement = document.createElement(PARAMETER_LIST_ELEMENT);

        for( Variable var : emodel.getParameters() )
        {
            try
            {
                if( !var.getName().equals("time") )
                    writeParameter(parameterListElement, var);
            }
            catch( Throwable t )
            {
                error("ERROR_PARAMETER_WRITING", new String[] {diagram.getName(), var.getName(), t.toString()});
            }
        }
        if( parameterListElement.hasChildNodes() )
            element.appendChild(parameterListElement);
    }

    public Element writeParameter(Element parameterListElement, Variable parameter)
    {
        Element parameterElement = document.createElement(PARAMETER_ELEMENT);
        parameterListElement.appendChild(parameterElement);

        writeNotes(parameterElement, parameter.getComment());
        setId(parameterElement, getSbmlId(parameter));
        parameterElement.setAttribute(PARAMETER_VALUE_ATTR, "" + parameter.getInitialValue());
        if( parameter.getComment() != null && !parameter.getComment().isEmpty() )
            parameterElement.setAttribute(NAME_ATTR, parameter.getComment());

        String sbmlName = parameter.getTitle();
        if( sbmlName != null && !sbmlName.isEmpty() )
            parameterElement.setAttribute(NAME_ATTR, sbmlName);
        String units = parameter.getUnits();
        if( units != null && units.length() > 0 )
            parameterElement.setAttribute(UNITS_ATTR, units);

        parameterElement.setAttribute(CONSTANT_ATTR, parameter.isConstant() ? "true" : "false");
        
        Element annotationElement = document.createElement(ANNOTATION_ELEMENT);
        
       if( writeBioUMLAnnotation )
       {
           Element bioumlElement = document.createElement( BIOUML_ELEMENT );
           bioumlElement.setAttribute( BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE );
           writeVariable(parameter, bioumlElement);
           if (bioumlElement.hasChildNodes())
               annotationElement.appendChild(bioumlElement);
       }
       
       if (annotationElement.hasChildNodes())
           parameterElement.appendChild(annotationElement);
       
        return parameterElement;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Write reaction issues
    //

    protected Element writeReactionList(Element model)
    {
        Element reactionListElement = document.createElement(REACTION_LIST_ELEMENT);

        doReactantStub = false;
        doProductStub = false;

        writeReactionList(reactionListElement, diagram);

        if( doReactantStub )
            writeSpecieStub(speciesListElement, REACTANT_STUB, "REACTION_STUB_NOTE");

        if( doProductStub )
            writeSpecieStub(speciesListElement, PRODUCT_STUB, "REACTION_STUB_NOTE");

        return reactionListElement;
    }

    protected void writeReactionList(Element reactionListElement, Compartment compartment)
    {
        try
        {
            for( Node n : compartment.recursiveStream().select(Node.class).filter(n -> n.getKernel() instanceof Reaction) )
            {
                writeReaction(reactionListElement, n);
            }
        }
        catch( Throwable t )
        {
            error("ERROR_REACTION_LIST_WRITING", new String[] {diagram.getName(), t.getMessage()});
        }
    }

    public Element writeReaction(Element reactionListElement, Node reaction)
    {
        try
        {
            if( validReaction(reaction) )
            {
                Element reactionElement = document.createElement(REACTION_ELEMENT);
                reactionListElement.appendChild(reactionElement);

                writeNotes(reactionElement, reaction.getComment());
                setId(reactionElement, getSbmlId(reaction));//castStringToSId(reaction.getName()));

                // write reversible and fast attributes if they differ from default
                Reaction reactionKernel = (Reaction)reaction.getKernel();
                if( reaction.getTitle() != null )
                    setTitle( reactionElement, reactionKernel.getTitle() );

               writeReactionAttributes( reactionKernel, reactionElement );
               
                Element annotationElement = document.createElement(ANNOTATION_ELEMENT);
                writeAnnotation(annotationElement, reaction);

                if( writeBioUMLAnnotation )
                {
                    Element bioumlElement = document.createElement( BIOUML_ELEMENT );
                    bioumlElement.setAttribute( BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE );

                    Element nodeInfoElement = document.createElement( BIOUML_NODE_INFO_ELEMENT );
                    DiagramXmlWriter.writeNodeInfo( nodeInfoElement, reaction, document );
                    nodeInfoElement.setAttribute( BIOUML_COMPLETE_NAME_ATTR, reaction.getCompleteNameInDiagram() );
                    
                    Element reactionInfoElement = document.createElement( BIOUML_REACTION_INFO_ELEMENT );
                    DiagramXmlWriter.serializeDPS( document, reactionInfoElement, reactionKernel.getAttributes(), null );
                    if (reactionKernel.isFast())
                        reactionInfoElement.setAttribute( REACTION_FAST_ATTR, "true" );
                    
                    bioumlElement.appendChild( reactionInfoElement );
                    bioumlElement.appendChild( nodeInfoElement );
                    annotationElement.appendChild( bioumlElement );
                }

                if( annotationElement.hasChildNodes() )
                    reactionElement.appendChild(annotationElement);

                writeReactants(reactionElement, reaction);
                writeProducts(reactionElement, reaction);
                writeModifiers(reactionElement, reaction);
                writeKineticLaw(reactionElement, reaction);
                
                return reactionElement;
            }
        }
        catch( Throwable t )
        {
            error("ERROR_REACTION_WRITING", new String[] {diagram.getName(), reaction.getName(), t.getMessage()});
        }
        return null;
    }
    
    protected void writeReactionAttributes(Reaction reaction, Element element)
    {
        if( !reaction.isReversible() )
            element.setAttribute(REACTION_REVERSIBLE_ATTR, "false");

        if( reaction.isFast() )
            element.setAttribute(REACTION_FAST_ATTR, "true");
    }

    protected void writeModifiers(Element reactionElement, Node reaction)
    {

    }

    protected void writeReactants(Element reactionElement, Node reaction)
    {
        Element reactantListElement = document.createElement(REACTANT_LIST_ELEMENT);
        reactionElement.appendChild(reactantListElement);

        if( !writeSpecieReferences(reactantListElement, reaction, SpecieReference.REACTANT) )
        {
            // respect SBML requirements
            doReactantStub = true;
            writeStubReference(reactantListElement, reaction, REACTANT_STUB);
        }

        // we write modifiers as reactants, for level2 this method is rewritten
        writeSpecieReferences(reactantListElement, reaction, SpecieReference.MODIFIER);
    }

    protected void writeProducts(Element reactionElement, Node reaction)
    {
        Element productListElement = document.createElement(PRODUCT_LIST_ELEMENT);
        reactionElement.appendChild(productListElement);

        if( !writeSpecieReferences(productListElement, reaction, SpecieReference.PRODUCT) )
        {
            // respect SBML requirements
            doProductStub = true;
            writeStubReference(productListElement, reaction, PRODUCT_STUB);
        }
    }

    protected Element createSpeciesReferenceElement()
    {
        return document.createElement(SPECIE_REFERENCE_ELEMENT);
    }

    protected void setSpeciesAttribute(Element speciesReferenceElement, String speciesName)
    {
        speciesReferenceElement.setAttribute(SPECIE_ATTR, speciesName);
    }

    protected boolean writeSpecieReferences(Element listElement, Node reaction, String role)
    {
        return writeSpecieReferences(listElement, reaction, role, null);
    }

    protected boolean writeSpecieReferences(Element listElement, Node reaction, String role, String elementType)
    {
        int n = 0;

        for(Edge edge: reaction.getEdges() )
        {
            if( edge.getKernel() instanceof SpecieReference )
            {
                SpecieReference species = (SpecieReference)edge.getKernel();
                Node speciesNode  = Util.isReaction(edge.getInput())? edge.getOutput(): edge.getInput();
                String specie = this.getSbmlId(speciesNode);
                if( role.equals(species.getRole()) )
                {
                    try
                    {
                        Element speciesReferenceElement = ( elementType == null ) ? createSpeciesReferenceElement() : document
                                .createElement(elementType);

                        listElement.appendChild(speciesReferenceElement);

                        // usually we associate notes with diagram element, but this is an exception
                        writeNotes(speciesReferenceElement, species.getComment());

                        String speciesName = specie;//castStringToSId(species.getSpecie());
                        setSpeciesAttribute(speciesReferenceElement, speciesName);

                        if( elementType == null || !elementType.equals(MODIFIER_SPECIE_REFERENCE_ELEMENT) )
                            writeSpeciesReferenceAttributes(speciesReferenceElement, species, reaction);

                        n++;

                        Element annotationElement = document.createElement( ANNOTATION_ELEMENT );
                        writeAnnotation( annotationElement, reaction );

                        if( writeBioUMLAnnotation )
                        {
                            Element bioumlElement = document.createElement( BIOUML_ELEMENT );
                            bioumlElement.setAttribute( BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE );

                            Element edgeInfoElement = document.createElement( BIOUML_EDGE_INFO_ELEMENT );
                            DiagramXmlWriter.writeEdgeInfo( edgeInfoElement, edge, document );
                            bioumlElement.appendChild( edgeInfoElement );
                            annotationElement.appendChild( bioumlElement );
                        }
                        if( annotationElement.hasChildNodes() )
                            speciesReferenceElement.appendChild( annotationElement );
                    }
                    catch( Throwable t )
                    {
                        error("ERROR_SPECIE_REFERENCE_WRITING", new String[] {diagram.getName(), reaction.getName(), species.getName(),
                                t.getMessage()});
                    }
                }
            }
        }

        return n > 0;
    }

    protected void writeStubReference(Element listElement, Node reaction, String stubName)
    {
        Element speciesReferenceElement = createSpeciesReferenceElement();
        listElement.appendChild(speciesReferenceElement);
        setSpeciesAttribute(speciesReferenceElement, stubName);
    }

    protected void writeSpecieStub(Element speciesListElement, String stubName, String messageKey)
    {
        Element speciesElement = document.createElement(SPECIE_ELEMENT);
        speciesListElement.appendChild(speciesElement);
        setId(speciesElement, stubName);
        speciesElement.setAttribute(SPECIE_BOUNDARY_CONDITION_ATTR, "true");
        Compartment compartment = diagram;//compartmentList.get(0);
        speciesElement.setAttribute(COMPARTMENT_ATTR, compartment.getName());
        speciesElement.setAttribute(SPECIE_INITIAL_AMOUNT_ATTR, "0.0");
        writeNotes(speciesElement, MessageBundle.resources.getResourceString(messageKey));
    }

    protected void writeKineticLaw(Element reactionElement, Node reaction)
    {
        KineticLaw kineticLaw = ((Reaction)reaction.getKernel()).getKineticLaw();
        Element kineticLawElement = document.createElement(KINETIC_LAW_ELEMENT);
        writeNotes(kineticLawElement, reaction.getComment());
        writeFormula(parseFormula(kineticLaw.getFormula(), reaction), reaction, kineticLawElement);
        if( kineticLaw.getTimeUnits() != null && kineticLaw.getTimeUnits().length() > 0 )
            kineticLawElement.setAttribute(TIME_UNITS_ATTR, kineticLaw.getTimeUnits());

        if( kineticLaw.getSubstanceUnits() != null && kineticLaw.getSubstanceUnits().length() > 0 )
            kineticLawElement.setAttribute(SUBSTANCE_UNITS_ATTR, kineticLaw.getSubstanceUnits());

        if( kineticLawElement.getFirstChild() != null )
            reactionElement.appendChild(kineticLawElement);
    }

    protected Map<String, SbmlExtension> annotationsExtensions = null;
    public void validateAnnotationsExtensions(Set<String> namespaces)
    {
        annotationsExtensions = new LinkedHashMap<>();
        for( SbmlAnnotationInfo extension : SbmlAnnotationRegistry.getAnnotations() )
        {
            try
            {
                SbmlExtension se = extension.create();
                se.setSbmlModelWriter(this);
                if( namespaces.contains(extension.getNamespace()) )
                    annotationsExtensions.put(extension.getNamespace(), se);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not validate annotation extensions.", t);
            }
        }
    }

    protected void writeAnnotation(Element annotation, DiagramElement diagramElement)
    {
        if( annotationsExtensions == null )
            validateAnnotationsExtensions(SbmlAnnotationRegistry.getNamespaces());

        for( SbmlExtension annotationsExtension : annotationsExtensions.values() )
        {
            annotationsExtension.setSbmlModelWriter( this );
            Element[] newElements = annotationsExtension.writeElement(diagramElement, document);
            if( newElements != null )
            {
                for( Element newElement : newElements )
                    if(newElement != null)
                        annotation.appendChild(newElement);
            }
        }
    }

    protected abstract void writeFormula(String formula, Node reaction, Element kineticLawElement);

    @Override
    protected boolean parseAsSpecie(String token, StringBuffer result)
    {

        if( token.charAt(0) == '$' )
        {
            //special reaction variables treat
            if (token.startsWith( "$$rate_" ))
            {
                token = token.substring( 7 );
            }
            else
            {
                // trim $
                int i = 1;
                while( token.charAt( i ) == '$' )
                {
                    i++;
                }
                token = token.substring( i );
            }

            // cut ""
            if( token.length() > 2 && token.charAt(0) == '"' && token.charAt(token.length() - 1) == '"' )
            {
                token = token.substring(1, token.length() - 1);
            }
            // cut compartment name prefix
            /*i = token.lastIndexOf('.');
            if( i >= 0 )
            {
                token = token.substring(i + 1);
            }*/
            result.append(token);
            return true;
        }

        return false;
    }


    protected boolean isAutoCreated(Compartment compartment)
    {
        return compartment.getAttributes().getProperty( "autoCreated" ) != null;
    }

    protected void setAutoCreated(Compartment compartment)
    {
        try
        {
            compartment.getAttributes().add(new DynamicProperty("autoCreated", Boolean.class, true));
        }
        catch( Exception ex )
        {

        }
    }

    protected Set<String> sbmlIds;
    protected Map<Node, String> nodeToSbmlIds;
    protected Map<Variable, String> parameterToSbmlIds;
    public String getSbmlId(Node species)
    {
        if( nodeToSbmlIds.containsKey(species) )
            return nodeToSbmlIds.get(species);
        String name = castStringToSId(species.getName()); //try simple way (preferable)
        if( sbmlIds.contains(name) ) //try old style but generate unique name
            name = generateUniqueName(castStringToSId(species.getCompleteNameInDiagram()));

        nodeToSbmlIds.put(species, name);
        sbmlIds.add(name);
        return name;
    }

    public String generateUniqueName(String baseName)
    {
        int i = 0;
        String name = baseName;
        while( sbmlIds.contains(name) )
            name = baseName + "_" + i++;
        return name;
    }

    protected String getSbmlId(Variable var)
    {
        if( var instanceof VariableRole && ( (VariableRole)var ).getDiagramElement() instanceof Node )
        {
            return getSbmlId((Node) ( (VariableRole)var ).getDiagramElement());
        }
        else
        {
            if (parameterToSbmlIds.containsKey(var))
                return parameterToSbmlIds.get(var);
                        
            String name = generateUniqueName(var.getName());
            sbmlIds.add(name);
            parameterToSbmlIds.put(var, name);
            return name;
        }
    }
    
    protected void writeUnitList(Element modelElement)
    {
        Element unitListElement = document.createElement(UNIT_DEFINITION_LIST_ELEMENT);
        for( Unit unit : emodel.getUnits().values() )
        {
            try
            {
                writeUnit(unit, unitListElement);
            }
            catch( Throwable t )
            {
                error("ERROR_UNIT_DEFINITION_PROCESSING", new String[] {diagram.getName(), unit.getName(), t.getMessage()});
            }
        }
        if( unitListElement.hasChildNodes() )
            modelElement.appendChild(unitListElement);
    }

    public Element writeUnit(Unit unit, Element unitListElement)
    {
        Element unitElement = document.createElement(UNIT_DEFINITION_ELEMENT);
        unitListElement.appendChild(unitElement);

        setId(unitElement, unit.getName());
        setTitle(unitElement, unit.getTitle());

        Element baseUnitsListElement = document.createElement(UNIT_LIST_ELEMENT);
        BaseUnit[] baseUnits = unit.getBaseUnits();
        if( baseUnits != null )
        {
            for( BaseUnit baseUnit : baseUnits )
            {
                try
                {
                    writeBaseUnit(baseUnit, baseUnitsListElement);
                }
                catch( Throwable t )
                {
                    error("ERROR_UNIT_PROCESSING", new String[] {diagram.getName(), unit.getName(), t.getMessage()});
                }
            }
            if( baseUnitsListElement.hasChildNodes() )
                unitElement.appendChild(baseUnitsListElement);
        }
        return unitElement;
    }

    public Element writeBaseUnit(BaseUnit baseUnit, Element baseUnitsListElement)
    {
        Element baseUnitElement = document.createElement(UNIT_ELEMENT);
        baseUnitsListElement.appendChild(baseUnitElement);

        baseUnitElement.setAttribute(UNIT_KIND_ATTR, baseUnit.getType());
        if( baseUnit.getExponent() != 1 )
            baseUnitElement.setAttribute(UNIT_EXPONENT_ATTR, "" + baseUnit.getExponent());
        if( baseUnit.getScale() != 0 )
            baseUnitElement.setAttribute(UNIT_SCALE_ATTR, "" + baseUnit.getScale());
        if( baseUnit.getMultiplier() != 1 )
            baseUnitElement.setAttribute(UNIT_MULTIPLIER_ATTR, "" + baseUnit.getMultiplier());
        return baseUnitElement;
    }
}
