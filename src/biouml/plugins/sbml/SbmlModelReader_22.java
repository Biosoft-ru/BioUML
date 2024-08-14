package biouml.plugins.sbml;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.util.XmlUtil;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Node;
import biouml.model.dynamics.Constraint;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

public class SbmlModelReader_22 extends SbmlModelReader_21
{
    protected Map<String, DataElement> compartmentTypes = new HashMap<>();
    protected Map<String, DataElement> specieTypes = new HashMap<>();
    
    public SbmlModelReader_22()
    {
        log = Logger.getLogger(SbmlModelReader_22.class.getName());
    }

    @Override
    protected DiagramType getDiagramType(Element modelElement)
    {
        return new SbmlDiagramType_L2v2();
    }

    @Override
    protected boolean isValid(String elementName, Object element, String name)
    {
        if( elementName.equals(COMPARTMENT_TYPE_LIST_ELEMENT) )
            return validateList(element, COMPARTMENT_TYPE_ELEMENT, name);

        if( elementName.equals(SPECIE_TYPE_LIST_ELEMENT) )
            return validateList(element, SPECIE_TYPE_ELEMENT, name);

        if( elementName.equals(INITIAL_ASSIGNMENT_LIST_ELEMENT) )
            return validateList(element, INITIAL_ASSIGNMENT_ELEMENT, name);

        if( elementName.equals(CONSTRAINT_LIST_ELEMENT) )
            return validateList(element, CONSTRAINT_ELEMENT, name);

        return super.isValid(elementName, element, name);
    }

    @Override
    public void readModelAttributes(Element element, Diagram diagram)
    {
        super.readModelAttributes( element, diagram );
        readSBOTerm(element, diagram.getAttributes());
    }

    @Override
    public Node readFunctionDefinition(Element funcDefElement, Map<String, Element> functions, Set<String> alreadyRead) throws Exception
    {
        Node fd = super.readFunctionDefinition(funcDefElement, functions, alreadyRead);
        readSBOTerm(funcDefElement, fd.getAttributes());
        return fd;
    }

    @Override
    protected void readCompartmentTypeList(Element model)
    {
        Element compartmentTypeList = getElement(model, COMPARTMENT_TYPE_LIST_ELEMENT);
        if( !isValid(COMPARTMENT_TYPE_LIST_ELEMENT, compartmentTypeList, null) )
            return;

        NodeList list = compartmentTypeList.getElementsByTagName(COMPARTMENT_TYPE_ELEMENT);
        for(Element element : XmlUtil.elements(list))
        {
            String id = getId(element);
            if( !compartmentTypes.containsKey(id) )
            {
                BaseSupport cType = new BaseSupport(null, id, COMPARTMENT_TYPE_ELEMENT);
                String name = getTitle(element);
                if( !name.isEmpty() )
                    cType.setTitle(name);
                compartmentTypes.put(id, cType);
            }
        }
    }

    @Override
    public Compartment readCompartment(Element element, String compartmentId, String parentId, String compartmentName) throws Exception
    {
        Compartment compartment = super.readCompartment( element, compartmentId, parentId, compartmentName);
        if (compartment == null)
            return null;
        if( element.hasAttribute(COMPARTMENT_TYPE_ATTR) )
        {
            String id = element.getAttribute(COMPARTMENT_TYPE_ATTR);
            if( compartmentTypes.containsKey(id) )
                compartment.getAttributes().add(new DynamicProperty(COMPARTMENT_TYPE_ATTR, BaseSupport.class, compartmentTypes.get(id)));
        }
        readSBOTerm(element, compartment.getAttributes());
        return compartment;
    }

    @Override
    protected void readSpecieTypeList(Element model)
    {
        Element specieTypeList = getElement(model, SPECIE_TYPE_LIST_ELEMENT);
        if( !isValid(SPECIE_TYPE_LIST_ELEMENT, specieTypeList, null) )
            return;

        NodeList list = specieTypeList.getElementsByTagName(SPECIE_TYPE_ELEMENT);
        int length = list.getLength();
        for( int i = 0; i < length; i++ )
        {
            Element element = (Element)list.item(i);
            String id = getId(element);
            if( !specieTypes.containsKey(id) )
            {
                BaseSupport cType = new BaseSupport(null, id, SPECIE_TYPE_ELEMENT);
                String name = getTitle(element);
                if( !name.isEmpty() )
                    cType.setTitle(name);
                specieTypes.put(id, cType);
            }
        }
    }

    @Override
    public Node readSpecie(Element element, String specieId, String specieName) throws Exception
    {
        Node node = super.readSpecie(element, specieId, specieName);
        if( element.hasAttribute(SPECIE_TYPE_ATTR) )
        {
            String id = element.getAttribute(SPECIE_TYPE_ATTR);
            if( specieTypes.containsKey(id) )
                node.getAttributes().add(new DynamicProperty(SPECIE_TYPE_ATTR, BaseSupport.class, specieTypes.get(id)));
        }
        return node;
    }

    @Override
    public Node readRule(Element ruleElement, int i)
    {
        Node rule = super.readRule(ruleElement, i);
        readSBOTerm(ruleElement, rule.getAttributes());
        return rule;
    }

    @Override
    public Node readInitialAssignment(Element initialAssignmentElement, int i)
    {
        Node initialAssignment = super.readInitialAssignment(initialAssignmentElement, i);
        readSBOTerm(initialAssignmentElement, initialAssignment.getAttributes());
        return initialAssignment;
    }

    @Override
    public Node readEvent(Element eventElement, int i)
    {
        Node event = super.readEvent(eventElement, i);
        readSBOTerm(eventElement, event.getAttributes());
        return event;
    }

    @Override
    public Node readReaction(Element element, String reactionId, String reactionName) throws Exception
    {
        Node reaction = super.readReaction(element, reactionId, reactionName);
        readSBOTerm(element, reaction.getAttributes());
        return reaction;
    }

    @Override
    protected void readConstraints(Element model)
    {
        Element constraintList = getElement(model, CONSTRAINT_LIST_ELEMENT);
        if( !isValid(CONSTRAINT_LIST_ELEMENT, constraintList, null) )
            return;

        NodeList list = constraintList.getElementsByTagName(CONSTRAINT_ELEMENT);
        for (int i=0; i<list.getLength(); i++)
            readConstraint((Element)list.item(i), i);
    }
    
    protected Node readConstraint(Element constraintElement, int i)
    {
        String id = getBriefId(constraintElement, BIOUML_NODE_INFO_ELEMENT);

        if( id.isEmpty() )
            id = constraintElement.getAttribute(METAID_ATTR);

        if( id.isEmpty() )
            id = "constraint_" + i;

        Node node = new Node(diagram, new Stub(diagram, id, Type.MATH_CONSTRAINT));

        Element annotationElement = getElement(constraintElement, ANNOTATION_ELEMENT);
        if( annotationElement != null )
            readBioUMLAnnotation(annotationElement, node, BIOUML_NODE_INFO_ELEMENT);

        Constraint constraint = new Constraint(node);
        node.setRole(constraint);
        String formula = readMath(constraintElement, node);
        if( formula != null )
            constraint.setFormula(formula);

        Element message = getElement(constraintElement, CONSTRAINT_MESSAGE_ELEMENT);
        if( message != null )
        {
            Element p = getElement(message, CONSTRAINT_P_ELEMENT);
            if( p != null )
                constraint.setMessage(p.getFirstChild().getNodeValue());
        }

        try
        {
            node.save();
            return node;
        }
        catch( Throwable t )
        {
            error("ERROR_CONSTRAINT_PROCESSING", new String[] {modelName, t.getMessage()});
        }
        return null;
    }
}
