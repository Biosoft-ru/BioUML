package biouml.plugins.sbml;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

import ru.biosoft.access.core.DataCollection;
import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.util.DiagramXmlWriter;
import biouml.standard.type.BaseSupport;

public class SbmlModelWriter_22 extends SbmlModelWriter_21
{
    @Override
    protected void writeModelAttributes(Element model)
    {
        super.writeModelAttributes( model );
        writeSBOTerm( model, diagram.getAttributes() );
    }

    @Override
    protected void setVersion(Element sbmlElement)
    {
        sbmlElement.setAttribute( SBML_VERSION_ATTR, SBML_VERSION_VALUE_2 );
    }

    @Override
    protected void writeCompartmentTypeList(Element model)
    {
        Element cTypeListElement = document.createElement( COMPARTMENT_TYPE_LIST_ELEMENT );
        List<String> compartmentTypes = new ArrayList<>();
        for( Compartment compartment : compartmentList )
        {
            BaseSupport cType = (BaseSupport) ( compartment.getAttributes().getValue( COMPARTMENT_TYPE_ATTR ) );
            if( cType != null )
            {
                if( compartmentTypes.size() == 0 )
                {
                    model.appendChild( cTypeListElement );
                }
                if( !compartmentTypes.contains( cType.getName() ) )
                {
                    Element cTypeElement = document.createElement( COMPARTMENT_TYPE_ELEMENT );
                    setId( cTypeElement, cType.getName() );

                    String title = cType.getTitle();
                    if( title != null && title.length() > 0 )
                        cTypeElement.setAttribute( NAME_ATTR, cType.getTitle() );

                    cTypeListElement.appendChild( cTypeElement );
                    compartmentTypes.add( cType.getName() );
                }
            }
        }
    }

    @Override
    public Element writeCompartment(Element compartmentListElement, Compartment compartment)
    {
        Element compartmentElement = document.createElement( COMPARTMENT_ELEMENT );
        compartmentListElement.appendChild( compartmentElement );

        setId( compartmentElement, getSbmlId(compartment));//castStringToSId( compartment.getCompleteNameInDiagram() ) );
        setTitle( compartmentElement, compartment.getTitle() );

        BaseSupport cType = (BaseSupport) ( ( compartment ).getAttributes().getValue( COMPARTMENT_TYPE_ATTR ) );
        if( cType != null )
            compartmentElement.setAttribute( COMPARTMENT_TYPE_ATTR, cType.getName() );
        writeSBOTerm( compartmentElement, compartment.getAttributes() );
        
        writeNotes( compartmentElement, compartment.getComment() );

        if( compartment != diagram )
        {
            DataCollection<?> parent = compartment.getOrigin();
            if( parent != null && !parent.getName().equals( diagram.getName() ) )
                compartmentElement.setAttribute( COMPARTMENT_OUTSIDE_ATTR, parent.getName() );
        }

        int dimension = 3; //default value
        if (compartment.getKernel() instanceof biouml.standard.type.Compartment)
        dimension = ( (biouml.standard.type.Compartment)compartment.getKernel() ).getSpatialDimension();
        compartmentElement.setAttribute( COMPARTMENT_DIMENSION_ATTR, String.valueOf( dimension));

        if( compartment.getRole() instanceof Variable )
        {
            Variable var = compartment.getRole( VariableRole.class );
            compartmentElement.setAttribute( getCompartmentVolumeAttr(), String.valueOf(var.getInitialValue()) );
            if( var.getUnits() != null && !var.getUnits().isEmpty() )
                compartmentElement.setAttribute( UNITS_ATTR, var.getUnits() );
        }
        else
        compartmentElement.setAttribute( getCompartmentVolumeAttr(), "1"); // default

        if (!isAutoCreated( compartment ))
        {
            boolean isConstant = compartment.getRole( VariableRole.class ).isConstant();
            if (!isConstant)
                compartmentElement.setAttribute( CONSTANT_ATTR, Boolean.toString(isConstant) );
        }
        
        // write compartment info as BioUML extension
        Element annotationElement = document.createElement( ANNOTATION_ELEMENT );
        writeAnnotation( annotationElement, compartment );

        if( writeBioUMLAnnotation )
        {
            Element bioumlElement = document.createElement( BIOUML_ELEMENT );
            bioumlElement.setAttribute( BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE );
            Element compartmentInfoElement = document.createElement( BIOUML_COMPARTMENT_INFO_ELEMENT );
            DiagramXmlWriter.writeCompartmentInfo( compartmentInfoElement, compartment, document );
            compartmentInfoElement.setAttribute( BIOUML_COMPLETE_NAME_ATTR, compartment.getCompleteNameInDiagram() );
            if (isAutoCreated( compartment ))
                compartmentInfoElement.setAttribute( BIOUML_DEFAULT_COMPARTMENT_ATTR, "true" );
            else
                writeVariable( compartment.getRole( VariableRole.class ), bioumlElement );
            bioumlElement.appendChild( compartmentInfoElement );
            annotationElement.appendChild( bioumlElement );

        }
        if( annotationElement.hasChildNodes() )
            compartmentElement.appendChild( annotationElement );

        return compartmentElement;
    }

    @Override
    protected void writeSpecieTypeList(Element model)
    {
        List<Node> specieList = fillSpecieList( diagram );
        
        Element specieTypeListElement = null;
        List<String> specieTypes = new ArrayList<>();
        for( Object specie : specieList )
        {
            BaseSupport sType = (BaseSupport) ( ( (Node)specie ).getAttributes().getValue( SPECIE_TYPE_ATTR ) );
            if( sType != null )
            {
                if( specieTypes.size() == 0 )
                {
                    specieTypeListElement = document.createElement( SPECIE_TYPE_LIST_ELEMENT );
                    model.appendChild( specieTypeListElement );
                }
                if( !specieTypes.contains( sType.getName() ) )
                {
                    Element sTypeElement = document.createElement( SPECIE_TYPE_ELEMENT );
                    setId( sTypeElement, sType.getName() );

                    String title = sType.getTitle();
                    if( title != null && title.length() > 0 )
                        sTypeElement.setAttribute( NAME_ATTR, sType.getTitle() );

                    specieTypeListElement.appendChild( sTypeElement );
                    specieTypes.add( sType.getName() );
                }
            }
        }
    }

    @Override
    public Element writeFunction(Function function, Element functionListElement)
    {
        Element element = super.writeFunction( function, functionListElement );
        DiagramElement de = function.getDiagramElement();
        if( de != null )
            writeSBOTerm( element, de.getAttributes() );
        return element;
    }

    @Override
    public Element writeRule(Equation equation, Element ruleListElement)
    {
        Element element = super.writeRule( equation, ruleListElement );
        if( element != null )
        {
            DiagramElement de = equation.getDiagramElement();
            if( de != null )
                writeSBOTerm( element, de.getAttributes() );
        }
        return element;
    }

    @Override
    public Element writeInitialAssignment(Equation assignment, Element initialAssignmentsListElement)
    {
        Element element = super.writeInitialAssignment( assignment, initialAssignmentsListElement );
        if( element != null )
        {
            DiagramElement de = assignment.getDiagramElement();
            if( de != null )
                writeSBOTerm( element, de.getAttributes() );
        }
        return element;
    }

    @Override
    public Element writeEvent(Event event, Element eventListElement)
    {
        Element element = super.writeEvent( event, eventListElement );
        DiagramElement de = event.getDiagramElement();
        if( de != null )
            writeSBOTerm( element, de.getAttributes() );
        return element;
    }
    
    @Override
    public Element writeReaction(Element reactionListElement, Node reaction)
    {
        Element element = super.writeReaction(reactionListElement , reaction);
        if( element != null )
            writeSBOTerm( element, reaction.getAttributes() );
        return element;
    }
    
    
    @Override
    protected void writeConstraintList(Element modelElement)
    {
        Element constraintListElement = document.createElement(CONSTRAINT_LIST_ELEMENT);
        
        for( Constraint constraint: emodel.getConstraints() )
        {
            try
            {
                writeConstraint(constraint, constraintListElement);
            }
            catch( Throwable t )
            {
                error("ERROR_PROCESSING_CONSTRAINT", new String[] {diagram.getName(),
                        constraint.getDiagramElement() != null ? constraint.getDiagramElement().getName() : null, t.getMessage()});
            }
        }

        if( constraintListElement.hasChildNodes() )
            modelElement.appendChild(constraintListElement);
    }
    
    @Override
    public Element writeConstraint(Constraint constraint, Element constraintListElement)
    {
        Element constraintElement = document.createElement(CONSTRAINT_ELEMENT);
        Node node = (Node)constraint.getDiagramElement();
        constraintElement.setAttribute(ID_ATTR, node.getName());

        Element annotationElement = document.createElement(ANNOTATION_ELEMENT);
        writeAnnotation(annotationElement, node);
        if( writeBioUMLAnnotation )
        {
            Element bioumlElement = document.createElement( BIOUML_ELEMENT );
            bioumlElement.setAttribute( BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE );
            Element nodeInfoElement = document.createElement( BIOUML_NODE_INFO_ELEMENT );
            DiagramXmlWriter.writeNodeInfo( nodeInfoElement, node, document );
            nodeInfoElement.setAttribute(BIOUML_COMPLETE_NAME_ATTR, node.getCompleteNameInDiagram());
            bioumlElement.appendChild( nodeInfoElement );
            annotationElement.appendChild( bioumlElement );
        }

        if( annotationElement.hasChildNodes() )
            constraintElement.appendChild(annotationElement);

        appendMathChild(constraintElement, constraint.getFormula(), constraint);
        
        String message = constraint.getMessage();
        if( !message.isEmpty() )
        {
            Element messageElement = document.createElement(CONSTRAINT_MESSAGE_ELEMENT);
            Element pElement = document.createElement(CONSTRAINT_P_ELEMENT);
            Text text = document.createTextNode(message);
            pElement.appendChild(text);
            pElement.setAttribute("xmlns", "http://www.w3.org/1999/xhtml");
            messageElement.appendChild(pElement);
            constraintElement.appendChild(messageElement);
        }
        constraintListElement.appendChild(constraintElement);
        return constraintElement;
    }
}