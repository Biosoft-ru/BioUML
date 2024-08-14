package biouml.plugins.sbml.composite;

import java.awt.Point;
import java.io.File;
import java.net.URI;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import one.util.streamex.EntryStream;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.XmlUtil;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.util.logging.Logger;
import org.eclipse.core.runtime.URIUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.Connection.Port;
import biouml.model.dynamics.DirectedConnection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.dynamics.util.EModelHelper;
import biouml.model.util.DiagramXmlConstants;
import biouml.model.util.DiagramXmlReader;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnCompositeDiagramType;
import biouml.plugins.sbml.SbmlConstants;
import biouml.plugins.sbml.SbmlEModel;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.sbml.SbmlModelReader;
import biouml.plugins.sbml.SbmlModelReader_21.MetaIdInfo;
import biouml.plugins.sbml.converters.SBGNConverterNew;
import biouml.plugins.sbml.SbmlModelReader_31;
import biouml.plugins.sbml.SbmlPackageReader;
import biouml.standard.diagram.PortProperties;
import biouml.standard.diagram.Util;
import biouml.standard.state.State;
import biouml.standard.type.Base;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Stub.ContactConnectionPort;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.model.ArrayProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

/**
 * Reader for "comp" extension of sbml
 * @author Ilya
*/
public class SbmlCompositeReader extends SbmlPackageReader
{
    private Map<String, ModelDefinition> modelDefinitions = new HashMap<>();
    private Map<String, Element> modelDefElements = new HashMap<>();
    private Map<String, Diagram> externalModelDefinitions = new HashMap<>();
    private Map<SubDiagram, Map<String, Object>> deletions = new HashMap<>();
        
    public SbmlCompositeReader()
    {
        log = Logger.getLogger(SbmlCompositeReader.class.getName());
    }

    @Override
    public void preprocessDiagram(Element element, Diagram diagram) throws Exception
    {
        this.diagram = diagram;
        this.emodel = diagram.getRole(SbmlEModel.class);
        this.modelName = diagram.getName();
        boolean propagationEnabled = diagram.isPropagationEnabled();
        this.diagram.setPropagationEnabled(false);
        readSubmodelList(element);
        this.readPortIds(element, diagram);
        diagram.setPropagationEnabled(propagationEnabled);
    }

    @Override
    public void postprocessDiagram(Element element, Diagram diagram) throws Exception
    {
        portToElement = new HashMap<>();
        readPortElementList(element, diagram);
        if( !portToElement.isEmpty() )
            diagram.getAttributes().add(new DynamicProperty("portInfo", HashMap.class, portToElement));

        if( !modelDefinition )
        {
            for( Element e : EntryStream.of(modelDefElements).removeKeys(modelDefinitions::containsKey).values() )
                readModelDefinition(e, diagram);

            for( ModelDefinition md : EntryStream.of(modelDefinitions).values() )
            {
                md.setOrigin(diagram);
                diagram.put(md);
            }
        }
    }

    @Override
    public void preprocess(Document doc, DataCollection<?> origin) throws Exception
    {
        this.document = doc;
        readModelDefinitionList(doc);
        readExternalModelDefinitionList(doc, origin);
    }

    @Override
    public String getPackageName()
    {
        return "comp";
    }
    
    public static Set<String> getSubModelList(Element model)
    {
        Set<String> result = new HashSet<>();
        Element subModelList = XmlUtil.findElementByTagName( model, SbmlCompositeConstants.EXTERNAL_MODEL_DEFINITION_LIST_ELEMENT);
        if( subModelList == null )
            return result;

        NodeList list = subModelList.getElementsByTagName(SbmlCompositeConstants.EXTERNAL_MODEL_DEFINITION_ELEMENT);
        int length = list.getLength();
        for( int i = 0; i < length; i++ )
        {
            Element element = (Element)list.item( i );
            String id = element.getAttribute( SbmlCompositeConstants.SOURCE );
            result.add( id );
        }
        return result;
    }

    protected void readSubmodelList(Element model)
    {
        Element subModelList = getElement(model, SbmlCompositeConstants.SUBMODEL_LIST_ELEMENT);
        if( subModelList == null )
            return;

        NodeList list = subModelList.getElementsByTagName(SbmlCompositeConstants.SUBMODEL_ELEMENT);
        int length = list.getLength();
        for( int i = 0; i < length; i++ )
        {
            String modelDefId = "";
            try
            {
                Element element = (Element)list.item(i);
                String id = element.getAttribute(SbmlCompositeConstants.ID);//getId( element );
                Node node = readSubmodel(element, model, id);
                this.diagram.put(node);
            }
            catch( Throwable t )
            {
                error("ERROR_SUB_MODEL_READING", new String[] {modelName, modelDefId, t.getMessage()});
            }
        }
    }

    protected Node readSubmodel(Element submodelElement, Element rootElement, String id) throws Exception
    {
        String modelRef = submodelElement.getAttribute(SbmlCompositeConstants.MODEL_REF);

        if (newPaths != null && newPaths.containsKey( modelRef ))
            modelRef = newPaths.get(modelRef);
            
        Diagram diagram = findModelDefinition(modelRef);
        if( diagram == null )
        {
            error("ERROR_MODEL_DEFINITION_MISSING", new String[] {modelName, id, modelRef});
            return null;
        }
        subModels.put(modelRef, diagram);
        SubDiagram subDiagram = new SubDiagram(this.diagram, diagram, id);
        String timeConvFactor = submodelElement.getAttribute(SbmlCompositeConstants.TIME_CONVERSION_FACTOR);
        subDiagram.getAttributes().add(new DynamicProperty(Util.TIME_SCALE, String.class, timeConvFactor));
        String extentConvFactor = submodelElement.getAttribute(SbmlCompositeConstants.EXTENT_CONVERSION_FACTOR);
        subDiagram.getAttributes().add(new DynamicProperty(Util.EXTENT_FACTOR, String.class, extentConvFactor));

        Element annotation = getElement(submodelElement, ANNOTATION_ELEMENT);
        if( annotation != null )
        {
            Element stateElement = getElement(annotation, DiagramXmlConstants.STATE_ELEMENT);
            if( stateElement != null )
                DiagramXmlReader.readSubDiagramState(stateElement, subDiagram);

            readBioUMLAnnotation(annotation, subDiagram, BIOUML_COMPARTMENT_INFO_ELEMENT);
        }
        readDeletionElementList(submodelElement, subDiagram);
        return subDiagram;
    }


    protected Diagram findModelDefinition(String modelRef) throws Exception
    {
        if( modelDefinitions.containsKey(modelRef) )
            return modelDefinitions.get(modelRef).getDiagram();
        if( subModels.containsKey(modelRef) )// if this model is read already
            return subModels.get(modelRef);
        if( modelDefElements.containsKey(modelRef) )
            return readModelDefinition(modelDefElements.get(modelRef), diagram);
        if( externalModelDefinitions.containsKey(modelRef) )
            return externalModelDefinitions.get(modelRef);
        return null;
    }

    protected void readModelDefinitionList(Document document)
    {
        Element modelDefList = getElement(document.getDocumentElement(), SbmlCompositeConstants.MODEL_DEFINITION_LIST_ELEMENT);
        if( modelDefList == null )
            return;

        NodeList list = modelDefList.getElementsByTagName(SbmlCompositeConstants.MODEL_DEFINITION_ELEMENT);
        for( int i = 0; i < list.getLength(); i++ )
        {
            Element element = (Element)list.item(i);
            String modelDefId = element.getAttribute("id");
            modelDefElements.put(modelDefId, element);
        }
    }

    protected void readExternalModelDefinitionList(Document document, DataCollection<?> origin)
    {
        Element modelDefList = getElement(document.getDocumentElement(), SbmlCompositeConstants.EXTERNAL_MODEL_DEFINITION_LIST_ELEMENT);
        if( modelDefList == null )
            return;

        NodeList list = modelDefList.getElementsByTagName(SbmlCompositeConstants.EXTERNAL_MODEL_DEFINITION_ELEMENT);
        for( int i = 0; i < list.getLength(); i++ )
        {
            Element element = (Element)list.item(i);
            String modelDefId = element.getAttribute(SbmlCompositeConstants.ID);
            
            try
            {
                Element annotationElement = getElement( element, ANNOTATION_ELEMENT );
                if( annotationElement != null )
                {
                    Element bioumlElement = getElement( annotationElement, BIOUML_DIAGRAM_REFERENCE );
                    if( bioumlElement != null )
                    {
                        Element diagramReferenceElement = getElement( annotationElement, BIOUML_DIAGRAM_REFERENCE );
                        if( diagramReferenceElement != null )
                        {
                            String path = diagramReferenceElement.getAttribute( BIOUML_DIAGRAM_PATH );
                            if (newPaths != null && newPaths.containsKey( path ))
                                path = newPaths.get( path );
                            Diagram diagram = DataElementPath.create( path ).getDataElement( Diagram.class );
                            externalModelDefinitions.put( modelDefId, diagram );
                            continue;
                        }
                    }
                }
            }
            catch( Exception ex )
            {

            }
            String source = element.getAttribute( SbmlCompositeConstants.SOURCE );
            if( newPaths != null && newPaths.containsKey( source ) )
            {
                String path = newPaths.get( source );
                Diagram diagram = DataElementPath.create( path ).getDataElement( Diagram.class );
                externalModelDefinitions.put( modelDefId, diagram );
                continue;
            }
            String modelRef = element.getAttribute(SbmlCompositeConstants.MODEL_REF);
            try
            {
                File externalFile = getFile(source);

                if( modelRef.isEmpty() )
                {
                    //TODO: probably we should transform diagram to SBGN here?
                    Diagram externalDiagram = SbmlModelFactory.readDiagram(externalFile, origin,
                            ApplicationUtils.getFileNameWithoutExtension(externalFile.getName()));
                    externalModelDefinitions.put(modelDefId, externalDiagram);
                }
                else
                {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document externalDocument = builder.parse(externalFile);

                    SbmlCompositeReader newReader = new SbmlCompositeReader();
                    newReader.document = externalDocument;
                    newReader.readExternalModelDefinitionList(externalDocument, origin);
                    newReader.readModelDefinitionList(externalDocument);

                    if( newReader.modelDefElements.containsKey(modelRef) )
                    {
                        externalModelDefinitions.put(modelDefId,
                                newReader.readModelDefinition(newReader.modelDefElements.get(modelRef), null));
                    }
                    else if( newReader.externalModelDefinitions.containsKey(modelRef) )
                    {
                        externalModelDefinitions.put(modelDefId, newReader.externalModelDefinitions.get(modelRef));
                    }
                    else //modelRef is broken?
                    {
                        Diagram externalDiagram = SbmlModelFactory.readDiagram(externalFile, origin,
                                ApplicationUtils.getFileNameWithoutExtension(externalFile.getName()));
                        externalModelDefinitions.put(modelDefId, externalDiagram);
                    }
                }
            }
            catch( Exception ex )
            {
                error("ERROR_EXTERNAL_MODEL_DEFINITION_READING", new String[] {modelName, modelRef, ex.getMessage()});
            }
        }
    }

    protected void addModelDefinition(String name, ModelDefinition modelDef)
    {
        this.modelDefinitions.put(name, modelDef);
    }

    protected Diagram readModelDefinition(Element element, Diagram origin) throws Exception
    {
        String modelId = element.getAttribute(ID_ATTR);
        try
        {
            SbmlModelReader_31 reader = new SbmlModelReader_31();
            SbmlCompositeReader compReader = null;
            reader.initPackageReaders(element.getOwnerDocument(), true);

            for( SbmlPackageReader packageReader : reader.getPackageReaders() )
            {
                if( packageReader instanceof SbmlCompositeReader )
                    compReader = ( (SbmlCompositeReader)packageReader );
            }

            //if we already read some modelDefinitions, we should reuse them while reading other model definitions
            //In the case when this modelDefiniton will be referenced in other model definition
            EntryStream.of( this.modelDefinitions ).forKeyValue( compReader::addModelDefinition );

            Diagram innerDiagram = reader.readModelDefinition( element, modelId );
          

            if( origin != null )
            {
                String fullName = reader.getCompleteId(element, BIOUML_COMPARTMENT_INFO_ELEMENT);
                String id = SbmlModelReader.getBriefName(fullName);

                Compartment parent = origin;
                String parentName = SbmlModelReader.getParentId(fullName);
                if( parentName != null )
                {
                    Node parentNode = origin.findNode(parentName);
                    if( parentNode != null && parentNode instanceof Compartment )
                        parent = origin;
                }

                if( Diagram.getDiagram(parent).getType() instanceof SbgnCompositeDiagramType )
                {
                    Object sbgnDiagram = innerDiagram.getAttributes().getValue( SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME );
                    if( ! ( sbgnDiagram instanceof Diagram ) )
                        innerDiagram = SBGNConverterNew.convert( innerDiagram ); //convert anyway
                    else
                        innerDiagram = (Diagram)sbgnDiagram;
                }
                
                ModelDefinition modelDefinition = new ModelDefinition(parent, innerDiagram, id);
                Element annotationElement = getElement(element, ANNOTATION_ELEMENT);
                if( annotationElement != null )
                {
                    Element bioumlElement = getElement(annotationElement, BIOUML_ELEMENT);
                    if( bioumlElement != null )
                    {
                        Element compartmentInfoElement = getElement(bioumlElement, BIOUML_COMPARTMENT_INFO_ELEMENT);
                        if( compartmentInfoElement != null )
                            DiagramXmlReader.readCompartmentInfo(compartmentInfoElement, modelDefinition, diagram.getName());
                    }
                }

                modelDefinitions.put(modelId, modelDefinition);

                if( !this.modelDefinition )
                    modelDefinition.save();
            }

            //If we have read model definition while reading another model definition - store it and reuse
            EntryStream.of(compReader.modelDefElements).forKeyValue(modelDefElements::putIfAbsent);
            EntryStream.of(compReader.modelDefinitions).forKeyValue(modelDefinitions::putIfAbsent);
            return innerDiagram;
        }
        catch( Throwable t )
        {
            error("ERROR_MODEL_DEFINITION_READING", new String[] {modelName, modelId, t.getMessage()});
        }
        return null;
    }

    @Override
    public void processSpecie(Element element, Node node) throws Exception
    {
        Element replacedBy = getElement(element, SbmlCompositeConstants.REPLACED_BY_ELEMENT);
        if( replacedBy != null )
            readReplacedByElement(replacedBy, node);

        readReplacedElementList(element, node);
    }

    @Override
    public void processCompartment(Element element, Compartment compartment) throws Exception
    {
        Element replacedBy = getElement(element, SbmlCompositeConstants.REPLACED_BY_ELEMENT);
        if( replacedBy != null )
            readReplacedByElement(replacedBy, compartment);

        readReplacedElementList(element, compartment);
    }

    @Override
    public void processParameter(Element element, Variable parameter) throws Exception
    {
        Element replacedBy = getElement(element, SbmlCompositeConstants.REPLACED_BY_ELEMENT);
        if( replacedBy != null )
            readReplacedByElement(replacedBy, parameter);

        readReplacedElementList(element, parameter);
    }

    @Override
    public void processRule(Element element, Node ruleNode)
    {
        readReplacedElementList(element, ruleNode);
    }

    @Override
    public void processReaction(Element element, Node node) throws Exception
    {
        readReplacedElementList(element, node);
    }

    @Override
    public void processSpecieReference(Element element, Edge edge) throws Exception
    {
        String id = element.getAttribute(ID_ATTR);
        Variable parameter = emodel.getVariable(id);
        if( parameter != null )
            readReplacedElementList(element, parameter);
    }

    protected void readDeletionElementList(Element element, SubDiagram subDiagram)
    {
        Element replacedList = getElement(element, SbmlCompositeConstants.DELETION_LIST_ELEMENT);
        if( replacedList == null )
            return;

        NodeList list = replacedList.getElementsByTagName(SbmlCompositeConstants.DELETION_ELEMENT);
        int length = list.getLength();
        for( int i = 0; i < length; i++ )
        {
            try
            {
                Element deletionElement = (Element)list.item(i);
                readDeletionElement(deletionElement, subDiagram);
            }
            catch( Throwable t )
            {
                error("ERROR_DELETION_ELEMENT_READING", new String[] {modelName, subDiagram.getName(), t.getMessage()});
            }
        }
    }

    protected void readDeletionElement(Element element, SubDiagram subDiagram) throws Exception
    {
        Stack<SubDiagram> nestedSubDiagrams = new Stack<>();
        nestedSubDiagrams.add(subDiagram);
        String reference = readSBaseRef(element, nestedSubDiagrams, subDiagram);
        SubDiagram lowestSubDiagram = nestedSubDiagrams.pop();

        Diagram innerDiagram = lowestSubDiagram.getDiagram();

        Object refObject = null;
        String propertyName = null;
        MetaIdInfo metaIdInfo = null;
        Class<?> objectClass;
        Map<String, MetaIdInfo> metaIdMap = (Map<String, MetaIdInfo>)innerDiagram.getAttributes().getProperty("metaIds").getValue();
        if( metaIdMap != null && metaIdMap.containsKey(reference) )
        {
            metaIdInfo = metaIdMap.get(reference);
            reference = metaIdInfo.getObjectName();
            propertyName = metaIdInfo.getProperty();
            objectClass = metaIdInfo.getOjectClass();
            refObject = ( Node.class.isAssignableFrom(objectClass) ) ? innerDiagram.findNode(reference)
                    : innerDiagram.getRole(EModel.class).getVariable(reference);
        }

        //TODO: handle deletion of reaction private paramater as a replacement if top level has parameter with the same name
        //        if (refObject instanceof Variable && ( (Variable)refObject ).getAttributes().getValueAsString("baseId") != null)
        //        {
        //            Variable upperVariable =
        //            createConnectionChain( upperVariable, reference, nestedSubDiagrams, element, forward);
        //        }

        if( refObject == null )
            refObject = innerDiagram.findNode(reference);

        if( refObject == null )
            return;

        setState(lowestSubDiagram);

        //check if this element is already deleted by state
        for( Object obj : SbmlCompositeWriter.getRemovedElements(lowestSubDiagram.getState()) )
        {
            if( obj instanceof MetaIdInfo )
            {
                MetaIdInfo existInfo = (MetaIdInfo)obj;
                if( metaIdInfo.equals(existInfo) )
                    return;
            }
        }

        if( refObject instanceof Variable )
        {
            EModel emodel = innerDiagram.getRole(EModel.class);
            Variable var = (Variable)refObject;
            if( var.getAttributes().getProperty("baseId") != null )
            {
                String baseId = var.getAttributes().getProperty("baseId").getValue().toString();
                //TODO: find better way to handle local to global variables substitution when deletion eleminates local variable...
                Variable globalVariable = emodel.getVariable(baseId);
                if( globalVariable != null )
                    new EModelHelper(emodel).renameVariable(var.getName(), globalVariable.getName(), true);
            }
        }
        else if( refObject instanceof Node )
        {
            Node node = (Node)refObject;
            if( propertyName == null )
            {
                SemanticController controller = innerDiagram.getType().getSemanticController();
                controller.remove(node);
            }
            else
            {
                Role role = node.getRole();
                ComponentModel model = ComponentFactory.getModel(role, Policy.UI, true);
                Property property = model.findProperty(propertyName);

                if( property instanceof ArrayProperty )
                    ( (ArrayProperty)property ).removeItem(metaIdInfo.getIndex());
                else
                    property.setValue(null);
            }
        }

        String deletionId = element.getAttribute(SbmlCompositeConstants.ID);

        Map<String, Object> subDiagramDeletions = deletions.get(subDiagram);
        if( subDiagramDeletions == null )
        {
            subDiagramDeletions = new HashMap<>();
            deletions.put(subDiagram, subDiagramDeletions);
        }
        subDiagramDeletions.put(deletionId, refObject);
    }


    protected void readReplacedElementList(Element element, Object de)
    {
        Element replacedList = getElement(element, SbmlCompositeConstants.REPLACED_LIST_ELEMENT);
        if( replacedList == null )
            return;

        NodeList list = replacedList.getElementsByTagName(SbmlCompositeConstants.REPLACED_ELEMENT);
        int length = list.getLength();
        for( int i = 0; i < length; i++ )
        {
            try
            {
                readReplacedElement((Element)list.item(i), de);
            }
            catch( Throwable t )
            {
                //                                                error( "ERROR_REPLACED_PROCESSING", new String[] {modelName, modelDefId, t.getMessage()} );
            }
        }
    }

    protected void readReplacedByElement(Element element, Object de)
    {
        createConnectionChain(element, de, false);
    }

    protected void readReplacedElement(Element element, Object de)
    {
        createConnectionChain(element, de, true);
    }

    private boolean readConnectionType(Element element)
    {
        boolean directed = false;
        Element annotationElement = getElement(element, ANNOTATION_ELEMENT);
        if( annotationElement != null )
        {
            Element bioumlElement = getElement(annotationElement, BIOUML_ELEMENT);
            if( bioumlElement != null )
            {
                Element edgeInfo = getElement(bioumlElement, BIOUML_EDGE_INFO_ELEMENT);
                directed = edgeInfo != null && "directed".equals(edgeInfo.getAttribute("type"));
            }
        }
        return directed;
    }

    protected void createConnectionChain(String upperVariable, String reference, Stack<SubDiagram> nestedSubDiagrams, Element element,
            boolean forward) throws Exception
    {
        boolean directed = readConnectionType(element);

        SubDiagram subDiagram = nestedSubDiagrams.pop();

        Node innerPort = getInnerNodeByReference(subDiagram, reference, false);
        Node upperPort;

        if( upperVariable == null || innerPort == null )
            return;

        String convFactor = element.getAttribute(SbmlCompositeConstants.PARAMETER_COMVERSION_FACTOR);
        String replacementName = element.getAttribute(SbmlConstants.METAID_ATTR);
        if( replacementName.isEmpty() )
            replacementName = "connection";

        //We go from the bottom to the top in the hierarchy creating propagated ports
        while( true )
        {
            Diagram nextDiagram = Diagram.getDiagram(subDiagram);
            if( nextDiagram.equals(diagram) ) //meaning we reached top level
            {
                PortProperties portProperties = new PortProperties(diagram, ContactConnectionPort.class);
                portProperties.setAccessType(ConnectionPort.PRIVATE);
                portProperties.setVarName(upperVariable);
                portProperties.setName(createPortName(portProperties.getName(), diagram));
                upperPort = Util.findPort(diagram, portProperties);
                if( upperPort == null )
                {
                    DiagramElementGroup elements = portProperties.createElements( diagram, new Point( 0, 0 ), null );
                    upperPort = (Node)elements.getElement( Util::isPort );
                }
                createConnection(diagram, upperPort, subDiagram, innerPort, convFactor, forward, replacementName, directed);
                return;
            }
            else
            {
                SubDiagram parentSubDiagram = SubDiagram.getParentSubDiagram(nextDiagram);
                setState(parentSubDiagram);
                PortProperties portProperties = new PortProperties(nextDiagram, ContactConnectionPort.class);
                portProperties.setAccessType(ConnectionPort.PROPAGATED);
                portProperties.setModuleName(subDiagram.getName());
                portProperties.setBasePortName(innerPort.getName());
                upperPort = Util.findPort(nextDiagram, portProperties);
                if( upperPort == null )
                {
                    DiagramElementGroup elements = portProperties.createElements( nextDiagram, new Point( 0, 0 ), null );
                    upperPort = (Node)elements.getElement( Util::isPort );
                    parentSubDiagram.updatePorts();
                }
            }
            subDiagram = nestedSubDiagrams.pop();
            innerPort = subDiagram.findNode(upperPort.getName());
        }
    }

    protected String createPortName(String baseName, Diagram diagram)
    {
        int i = 1;
        String name = baseName;
        while( portIds.contains(name) || diagram.contains(name) )
            name = baseName + "_" + i++;
        return name;
    }

    protected void createConnectionChain(Element element, Object de, boolean forward)
    {
        try
        {
            String submodelRef = element.getAttribute(SbmlCompositeConstants.SUBMODEL_REF);
            SubDiagram subDiagram = (SubDiagram)diagram.findNode(submodelRef);
            Stack<SubDiagram> nestedSubDiagrams = new Stack<>();
            nestedSubDiagrams.add(subDiagram);
            String reference = readSBaseRef(element, nestedSubDiagrams, subDiagram);
            if( reference == null )
                return;

            String upperVariable = null;
            if( de instanceof Node )
            {
                Node node = (Node)de;
                if( Util.isVariable(node) )
                    upperVariable = node.getRole(VariableRole.class).getName();
                else if( Util.isReaction(node) )
                    upperVariable = node.getRole(Equation.class).getVariable();
            }
            else if( de instanceof Variable )
            {
                upperVariable = ( (Variable)de ).getName();
            }
            else if (de instanceof Edge)
            {
                upperVariable = ((Edge)de).getName();
            }

            createConnectionChain(upperVariable, reference, nestedSubDiagrams, element, forward);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Creates if necessary and returns port in the subDiagram that corresponds to entity (variable, node, etc) described in reference
     */
    protected Node getInnerNodeByReference(SubDiagram subDiagram, String reference, boolean isTransient) throws Exception
    {
        Diagram innerDiagram = subDiagram.getDiagram();
        EModel emodel = innerDiagram.getRole(EModel.class);

        String lowerVariable;
        Class<?> clazz;
        DiagramElement refNode = null;

        //first try to resolve reference as metaId        
        if( !innerDiagram.getAttributes().hasProperty( "metaIds" ) )
        {
            refNode = innerDiagram.findDiagramElement( reference );
        }
        else
        {
            Map<String, MetaIdInfo> metaIdMap = (Map<String, MetaIdInfo>)innerDiagram.getAttributes().getProperty( "metaIds" ).getValue();
            if( metaIdMap != null && metaIdMap.containsKey( reference ) )
            {
                MetaIdInfo metaIdInfo = metaIdMap.get( reference );
                reference = metaIdInfo.getObjectName();
                clazz = metaIdInfo.getOjectClass();

                if( Variable.class.isAssignableFrom( clazz ) )
                {
                    lowerVariable = emodel.getVariable( reference ).getName();
                    return createPortNode( subDiagram, lowerVariable, ConnectionPort.PUBLIC );
                }
                else if( Node.class.isAssignableFrom( clazz ) )
                {
                    // TODO: check this branch
                    //refNode = innerDiagram.findNode( innerNode.getName() );
                }
            }
            else
            {
                refNode = innerDiagram.findDiagramElement( reference );
            }
        }

        if( refNode == null )
        {
            if( !emodel.containsVariable(reference) )
                return null;
            lowerVariable = emodel.getVariable(reference).getName();
            return createPortNode(subDiagram, lowerVariable, ConnectionPort.PUBLIC);
        }
        else if( Util.isPort(refNode) )
        {
            return subDiagram.findNode(refNode.getName());
        }
        else if( Util.isVariable(refNode) )
        {
            lowerVariable = refNode.getRole(VariableRole.class).getName();
            return createPortNode(subDiagram, lowerVariable, ConnectionPort.PUBLIC);
        }
        else if( Util.isReaction(refNode) )
        {
            lowerVariable = refNode.getRole(Equation.class).getVariable();
            return createPortNode(subDiagram, lowerVariable, ConnectionPort.PUBLIC);
        }
        else if (Util.isSpecieReference(refNode))
        {
            lowerVariable = refNode.getName();
            return createPortNode(subDiagram, lowerVariable, ConnectionPort.PUBLIC);
        }
        else
        //referenced node has no associated variable and is not a port for variable node. In that case replacement means simple deletion of this node
        {

            setState(subDiagram);
            SemanticController controller = Diagram.getDiagram(refNode).getType().getSemanticController();
            controller.remove(refNode);
            return null;
        }
    }

    protected void createConnection(Diagram diagram, Node diagramNode, SubDiagram subDiagram, Node innerNode, String convFactor,
            boolean forward, String name, boolean directed)
    {
        try
        {
            subDiagram.updatePorts();

            for( Edge e : diagramNode.getEdges() )
            {
                if( e.getOtherEnd(diagramNode).equals(innerNode) && Util.isUndirectedConnection(e) )
                    return;
            }

            String replacementVarName = Util.isPort(diagramNode) ? Util.getPortVariable(diagramNode)
                    : diagramNode.getRole(VariableRole.class).getName();
            String edgeName = DefaultSemanticController.generateUniqueNodeName(diagram, name);
            String replacedVarName = Util.getPortVariable(innerNode);

            Base kernel = directed ? new Stub.DirectedConnection(null, edgeName) : new Stub.UndirectedConnection(null, edgeName);
            Edge edge = new Edge(diagram, kernel, diagramNode, innerNode);
            diagramNode.addEdge(edge);
            innerNode.addEdge(edge);
            Connection connection;
            if( directed )
            {
                connection = new DirectedConnection(edge);
            }
            else
            {
                connection = new UndirectedConnection(edge);
                ( (UndirectedConnection)connection ).setMainVariableType(forward ? MainVariableType.INPUT : MainVariableType.OUTPUT);
                ( (UndirectedConnection)connection ).setConversionFactor(convFactor);
            }
            edge.setRole(connection);
            connection.setOutputPort(new Port(replacedVarName));
            connection.setInputPort(new Port(replacementVarName));
            edge.save();
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    protected Node createPortNode(Diagram diagram, String variableName, String accessType) throws Exception
    {
        Node portNode = Util.findPort(diagram, variableName);
        if( portNode != null )
        {
            String existingAccessType = portNode.getAttributes().getValueAsString(ConnectionPort.ACCESS_TYPE);
            if( accessType.equals(existingAccessType) )
                return portNode;
        }

        String portName = variableName.contains(".") ? variableName.substring(variableName.lastIndexOf(".") + 1) : variableName;
        portName += "_port_" + accessType;
        if( portName.startsWith("$$") )
            portName = portName.substring(2);
        else if( portName.startsWith("$") )
            portName = portName.substring(1);

        portName = DefaultSemanticController.generateUniqueNodeName(diagram, portName);
        portNode = new Node(diagram, portName, new Stub.ConnectionPort(portName, diagram, Type.TYPE_CONTACT_CONNECTION_PORT));

        addProperty(portNode, new DynamicProperty(ConnectionPort.VARIABLE_NAME_ATTR, String.class, variableName), false, true);
        addProperty(portNode, new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class, PortOrientation.LEFT), false,
                false);
        addProperty(portNode, new DynamicProperty(ConnectionPort.ACCESS_TYPE, String.class, accessType), false, true);
        portNode.save();
        return portNode;
    }

    protected Node createPortNode(SubDiagram subDiagram, String variableName, String accessType) throws Exception
    {
        Diagram innerDiagram = subDiagram.getDiagram();
        Node diagramNode = createPortNode(innerDiagram, variableName, accessType);
        subDiagram.updatePorts();
        DynamicProperty dp = innerDiagram.getAttributes().getProperty(ModelDefinition.REF_MODEL_DEFINITION);
        if( dp != null )
        {
            Diagram originalDiagram = ( (ModelDefinition)dp.getValue() ).getDiagram();
            originalDiagram.put(diagramNode.clone(originalDiagram, diagramNode.getName()));
        }
        return subDiagram.findNode(diagramNode.getName());
    }

    protected void setState(SubDiagram subDiagram)
    {
        State state = subDiagram.getState();

        if( state == null )
        {
            Diagram innerDiagram = subDiagram.getDiagram();
            String stateName = SubDiagram.SUBDIAGRAM_STATE_NAME;

            state = innerDiagram.getState(stateName);
            if( state == null )
            {
                state = new State(innerDiagram, stateName);
                innerDiagram.addState(state);
            }
            subDiagram.setState(state);
        }
    }

    protected String generateStateName(SubDiagram subDiagram)
    {
        StringBuffer stateName = new StringBuffer();

        try
        {
            Stack<SubDiagram> parentSubDiagrams = new Stack<>();
            Diagram highestDiagarm = fillParentSubDiagrams(subDiagram, parentSubDiagrams);
            stateName.append(highestDiagarm.getName());
            stateName.append("_");

            while( !parentSubDiagrams.isEmpty() )
            {
                stateName.append(parentSubDiagrams.pop().getName());
                stateName.append("_");
            }
        }
        catch( Exception ex )
        {

        }
        stateName.append(subDiagram.getName());
        stateName.append("_state");
        return stateName.toString();
    }

    /**
     * returns highest diagram
     * @param subDiagram
     * @param parentSubDiagrams
     * @return
     * @throws Exception
     */
    protected static Diagram fillParentSubDiagrams(SubDiagram subDiagram, Stack<SubDiagram> parentSubDiagrams) throws Exception
    {
        Diagram upperDiagram = Diagram.getDiagram(subDiagram);
        DynamicProperty dp = upperDiagram.getAttributes().getProperty("subDiagram");
        if( dp != null && upperDiagram.getOrigin() instanceof Diagram )
        {
            //TODO: is origin of a subdiagram inner diagram always - upper diagram?
            Diagram nextDiagram = (Diagram)upperDiagram.getOrigin();
            SubDiagram nextSubDiagram = (SubDiagram)nextDiagram.findNode(dp.getValue().toString());
            parentSubDiagrams.add(nextSubDiagram);
            upperDiagram = fillParentSubDiagrams(nextSubDiagram, parentSubDiagrams);
        }
        return upperDiagram;
    }

    protected String readSBaseRef(Element element, Stack<SubDiagram> nestedSubDiagrams, SubDiagram subDiagram) throws Exception
    {
        Diagram upperDiagram = subDiagram.getDiagram();

        String submodelElementRef = element.getAttribute(SbmlCompositeConstants.ID_REF);

        if( submodelElementRef.isEmpty() )
        {
            submodelElementRef = element.getAttribute(SbmlCompositeConstants.META_ID_REF);
            if( !submodelElementRef.isEmpty() )
                return submodelElementRef;
        }

        if( submodelElementRef.isEmpty() )
        {
            submodelElementRef = element.getAttribute(SbmlCompositeConstants.PORT_REF);
            DynamicProperty dp = upperDiagram.getAttributes().getProperty("portInfo");
            if( dp != null )
            {
                HashMap<String, String> ports = (HashMap<String, String>)dp.getValue();
                if( ports.containsKey(submodelElementRef) )
                    return ports.get(submodelElementRef);
            }
        }

        if( submodelElementRef.isEmpty() )
        {
            submodelElementRef = element.getAttribute(SbmlCompositeConstants.DELETION_ELEMENT);
            Map<String, Object> subDiagramDeletions = deletions.get(subDiagram);
            if( subDiagramDeletions != null && subDiagramDeletions.containsKey(submodelElementRef) )
            {
                Object obj = subDiagramDeletions.get(submodelElementRef);
                if( obj instanceof Node )
                    return ( (Node)obj ).getName();
            }
        }

        DiagramElement subModelElement = upperDiagram.findDiagramElement(submodelElementRef);

//        Node subModelElement = upperDiagram.findNode(submodelElementRef);

        if( subModelElement == null )
        {
            Variable var = upperDiagram.getRole(EModel.class).getVariable(submodelElementRef);
            return var != null ? var.getName() : null;
        }
        else if( Util.isVariable(subModelElement) )
        {
            return subModelElement.getRole(VariableRole.class).getName();
        }
        else if (subModelElement instanceof Edge) //spcie reference
        {
            Base kernel = ((Edge)subModelElement).getKernel();
            if (kernel instanceof SpecieReference)
                return kernel.getName();

        }
        else if( ! ( subModelElement instanceof SubDiagram ) )
        {
            return submodelElementRef;
        }

        SubDiagram nextSubDiagram = (SubDiagram)subModelElement;
        nestedSubDiagrams.add(nextSubDiagram);
        Diagram nextDiagram = nextSubDiagram.getDiagram();
        NodeList list = element.getElementsByTagName(SbmlCompositeConstants.BASE_REF_ELEMENT);

        int length = list.getLength();
        for( int i = 0; i < length; i++ )
        {
            Element onlyChild = (Element)list.item(i);

            String childRef = onlyChild.getAttribute(SbmlCompositeConstants.ID_REF);

            if( childRef.isEmpty() )
                childRef = onlyChild.getAttribute(SbmlCompositeConstants.META_ID_REF);

            if( childRef.isEmpty() )
                childRef = onlyChild.getAttribute(SbmlCompositeConstants.PORT_REF);

            if( childRef.isEmpty() )
                return null;

            subModelElement = nextDiagram.findNode(childRef);

            if( subModelElement == null )
            {
                EModel emodel = nextDiagram.getRole(EModel.class);
                Variable var = emodel.getVariable(childRef);
                if( var != null )
                    return var.getName();
            }
            else if( Util.isVariable(subModelElement) )
            {
                return subModelElement.getRole(VariableRole.class).getName();
            }
            else if( subModelElement instanceof SubDiagram )
            {
                nextSubDiagram = (SubDiagram)subModelElement;
                nestedSubDiagrams.add(nextSubDiagram);
                //                this.setState( nextSubDiagram );
                nextDiagram = nextSubDiagram.getDiagram();
                //                list = onlyChild.getElementsByTagName( SbmlCompositeConstants.BASE_REF_ELEMENT );
            }
            else if( subModelElement instanceof Compartment )
            {
                return subModelElement.getRole(VariableRole.class).getName();
            }
        }
        return subModelElement.getName();
    }

    @Override
    public DiagramType getDiagramType()
    {
        return new SbmlCompositeDiagramType();
    }

    Set<String> portIds;
    /**
     * To avoid id collisions with autogenerated ports
     */
    protected void readPortIds(Element element, Diagram diagram)
    {
        portIds = new HashSet<>();
        Element portList = getElement(element, SbmlCompositeConstants.PORT_LIST_ELEMENT);
        if( portList == null )
            return;

        NodeList list = portList.getElementsByTagName(SbmlCompositeConstants.PORT_ELEMENT);
        int length = list.getLength();
        for( int i = 0; i < length; i++ )
        {
            try
            {
                Element portElement = (Element)list.item(i);
                String id = portElement.getAttribute(SbmlCompositeConstants.ID);
                portIds.add(id);
            }
            catch( Throwable t )
            {
                //                error( "ERROR_MODEL_DECLARATION_PROCESSING", new String[] {modelName, modelDefId, t.getMessage()} );
            }
        }
    }

    protected void readPortElementList(Element element, Diagram diagram)
    {
        Element portList = getElement(element, SbmlCompositeConstants.PORT_LIST_ELEMENT);
        if( portList == null )
            return;

        NodeList list = portList.getElementsByTagName(SbmlCompositeConstants.PORT_ELEMENT);
        int length = list.getLength();
        for( int i = 0; i < length; i++ )
        {
            try
            {
                Element portElement = (Element)list.item(i);
                readPortElement(portElement, diagram);
            }
            catch( Throwable t )
            {
                //                error( "ERROR_MODEL_DECLARATION_PROCESSING", new String[] {modelName, modelDefId, t.getMessage()} );
            }
        }
    }


    HashMap<String, String> portToElement;

    protected void readPortElement(Element element, Diagram diagram) throws Exception
    {
        String idRef = element.getAttribute(SbmlCompositeConstants.ID_REF);
        if( idRef.isEmpty() )
            idRef = element.getAttribute(SbmlCompositeConstants.META_ID_REF);

        String id = element.getAttribute(SbmlCompositeConstants.ID);

        EModel model = diagram.getRole(EModel.class);
        Variable var = model.getVariable(idRef);

        if( var == null )
        {
            String fullvarName = model.getQualifiedName("$" + idRef, null);
            if( fullvarName != null )
                var = model.getVariable(fullvarName);
        }

        if( var == null )
        {
            String fullvarName = model.getQualifiedName("$$rate_" + idRef, null);
            if( fullvarName != null )
                var = model.getVariable(fullvarName);
        }

        if( var == null )
        {
            portToElement.put(id, idRef);
            return;
        }

        Node existingNode = Util.findPort(diagram, var.getName());
        if( existingNode != null && Util.isPublicPort(existingNode) )
            return;

        //TODO: create with controller
        SemanticController controller = diagram.getType().getSemanticController();
        Node portNode = new Node(diagram, id, new Stub.ContactConnectionPort(diagram, id));

        readSBOTerm(element, portNode.getAttributes());
        addProperty(portNode, new DynamicProperty(ConnectionPort.VARIABLE_NAME_ATTR, String.class, var.getName()), false, true);
        addProperty(portNode, new DynamicProperty(ConnectionPort.PORT_ORIENTATION, PortOrientation.class, PortOrientation.LEFT), false,
                false);
        addProperty(portNode, new DynamicProperty(ConnectionPort.ACCESS_TYPE, String.class, ConnectionPort.PUBLIC), false, true);

        Element annotationElement = getElement(element, ANNOTATION_ELEMENT);
        if( annotationElement != null )
        {
            Element bioumlElement = getElement(annotationElement, BIOUML_ELEMENT);
            if( bioumlElement != null )
            {
                Element nodeInfoElement = getElement(bioumlElement, BIOUML_NODE_INFO_ELEMENT);
                if( nodeInfoElement != null )
                {
                    DiagramXmlReader.readNodeInfo(nodeInfoElement, portNode, diagram.getName());
                }
            }
        }

        portNode.save();
    }

    private void addProperty(DiagramElement de, DynamicProperty dp, boolean hidden, boolean readOnly)
    {
        dp.setHidden(hidden);
        dp.setReadOnly(readOnly);
        de.getAttributes().add(dp);
    }

    @Override
    protected void warn(String key, String[] params)
    {
        MessageBundle.warn(log, key, params);
    }

    @Override
    protected void error(String key, String[] params)
    {
        MessageBundle.error(log, key, params);
    }


    Map<String, Diagram> subModels = new HashMap<>();

    private File getFile(String source) throws Exception
    {
        URI sourceURI = URI.create(source);

        if( URIUtil.isFileURI(sourceURI) )
        {
            return URIUtil.toFile(sourceURI);
        }
        else
        {
            URI currentURI = URI.create(document.getBaseURI());
            File currentDir = URIUtil.toFile(currentURI).getParentFile();
            File targetFile = new File(currentDir, sourceURI.getPath());
            //TODO: remove dirty hack (Sometimes file are stored with extensions, sometimes not)
            if( !targetFile.exists() )
            {
                String sourceName = sourceURI.getPath();
                int index = sourceName.lastIndexOf(".");
                if( index != -1 )
                    targetFile = new File(currentDir, sourceName.substring(0, index));
            }

            if( targetFile.exists() )
                return targetFile;
            else
                throw new Exception("Can not find model by URI " + source);
        }
    }
}
