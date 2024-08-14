package biouml.plugins.sbml.composite;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.swing.undo.UndoableEdit;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ru.biosoft.access.core.undo.DataCollectionAddUndo;
import ru.biosoft.access.core.undo.DataCollectionRemoveUndo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.util.XmlUtil;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.util.DiagramXmlWriter;
import biouml.plugins.sbml.SbmlConstants;
import biouml.plugins.sbml.SbmlModelReader_21.MetaIdInfo;
import biouml.plugins.sbml.SbmlModelWriter_21;
import biouml.plugins.sbml.SbmlModelWriter_31;
import biouml.plugins.sbml.SbmlModelWriter_32;
import biouml.plugins.sbml.SbmlPackageWriter;
import biouml.plugins.sbml.SbmlUtil;
import biouml.standard.diagram.Util;
import biouml.standard.state.State;
import biouml.standard.state.StatePropertyChangeUndo;
import biouml.standard.state.StateXmlSerializer;
import biouml.standard.state.TransactionUtils;
import biouml.standard.type.Stub.ConnectionPort;

import com.developmentontheedge.beans.DynamicProperty;

/**
 * Writer for "comp" extension of sbml
 * @author Ilya
*/
public class SbmlCompositeWriter extends SbmlPackageWriter
{
    private Set<String> sbmlIds;
    private Map<Node, String> nodeToSbmlIds;
    private Element externalModelDefList = null;
    
    public SbmlCompositeWriter()
    {
        log = Logger.getLogger(SbmlCompositeWriter.class.getName());
    }

    @Override
    protected void init(Document document, Diagram diagram)
    {
        super.init(document, diagram);
        sbmlIds = new HashSet<>();
        nodeToSbmlIds = new HashMap<>();
    }

    @Override
    protected void processSBML(Element sbmlElement, Diagram diagram)
    {
        this.diagram = diagram;
        this.document = sbmlElement.getOwnerDocument();

        if( !modelDefinition )
        {
            writeModelDefinitionList(sbmlElement);
            if( externalModelDefList != null && externalModelDefList.hasChildNodes() )
                sbmlElement.appendChild(externalModelDefList);
        }
        sbmlElement.setAttribute("comp:required", "true");
    }

    @Override
    protected void processModel(Element modelElement, Diagram diagram)
    {
        this.diagram = diagram;
        this.document = modelElement.getOwnerDocument();
        writeSubmodelList(modelElement);
        writePortList(modelElement, diagram);
    }

    protected void writeSubmodelList(Element modelElement)
    {
        Element submodelList = document.createElement(SbmlCompositeConstants.SUBMODEL_LIST_ELEMENT);
        for( Node node : diagram.recursiveStream().select(SubDiagram.class) )
        {
            try
            {
                writeSubmodel((SubDiagram)node, submodelList);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Error during submode " + node.getName() + "writing", t);
            }
        }
        if( submodelList.hasChildNodes() )
            modelElement.appendChild(submodelList);
    }

    protected String getSbmlId(Node species)
    {
        if( nodeToSbmlIds.containsKey(species) )
            return nodeToSbmlIds.get(species);
        String name = castStringToSId(species.getName());        
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

    protected void writeSubmodel(SubDiagram subdiagram, Element submodelList)
    {
        Element submodelElement = document.createElement(SbmlCompositeConstants.SUBMODEL_ELEMENT);

        submodelElement.setAttribute(SbmlCompositeConstants.ID, getSbmlId(subdiagram));
        submodelElement.setAttribute(SbmlCompositeConstants.MODEL_REF, getSbmlId(subdiagram.getDiagram()));

        DynamicProperty dp = subdiagram.getAttributes().getProperty(Util.TIME_SCALE);
        if( dp != null && !dp.getValue().toString().isEmpty() )
            submodelElement.setAttribute(SbmlCompositeConstants.TIME_CONVERSION_FACTOR, dp.getValue().toString());

        dp = subdiagram.getAttributes().getProperty(Util.EXTENT_FACTOR);
        if( dp != null && !dp.getValue().toString().isEmpty() )
            submodelElement.setAttribute(SbmlCompositeConstants.EXTENT_CONVERSION_FACTOR, dp.getValue().toString());

        writeSBOTerm(submodelElement, subdiagram.getAttributes());
        writeDeletionsList(submodelElement, subdiagram);
        submodelList.appendChild(submodelElement);
        Diagram innerDiagram = subdiagram.getDiagram();
        if( writeBioUMLAnnotation )
        {
            Element annotationElement = document.createElement(ANNOTATION_ELEMENT);
            writeSubDiagramState(annotationElement, innerDiagram);
            Element bioumlElement = document.createElement(BIOUML_ELEMENT);
            bioumlElement.setAttribute(BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE);
            Element compartmentInfoElement = document.createElement(BIOUML_COMPARTMENT_INFO_ELEMENT);
            DiagramXmlWriter.writeCompartmentInfo(compartmentInfoElement, subdiagram, document);
            bioumlElement.appendChild(compartmentInfoElement);
            annotationElement.appendChild(bioumlElement);
            submodelElement.appendChild(annotationElement);
        }

        if( !SbmlUtil.isInternal(innerDiagram, diagram) )
            writeExternalModelDefintion(innerDiagram);
    }

    private void writeSubDiagramState(Element submodelElement, Diagram diagram)
    {
        State state = diagram.getState(SubDiagram.SUBDIAGRAM_STATE_NAME);
        if( state != null )
            submodelElement.appendChild(StateXmlSerializer.getStateXmlElement(state, document, new DiagramXmlWriter(document, diagram)));
    }
    
    protected void writeExternalModelDefintion(Diagram diagram)
    { 
        if( externalModelDefList == null )
            externalModelDefList = document.createElement(SbmlCompositeConstants.EXTERNAL_MODEL_DEFINITION_LIST_ELEMENT);
        else if( XmlUtil.getChildElement(externalModelDefList, SbmlCompositeConstants.ID, diagram.getName()) != null ) //such external model already exist
            return;
        Element externalModelDef = document.createElement(SbmlCompositeConstants.EXTERNAL_MODEL_DEFINITION_ELEMENT);
        externalModelDef.setAttribute(SbmlCompositeConstants.ID, castStringToSId(diagram.getName()));

        String path = diagram.getCompletePath().toString();
        if (newPaths != null && newPaths.containsKey( path ))
            path = newPaths.get( path );

        externalModelDef.setAttribute(SbmlCompositeConstants.SOURCE, path);
        
        if( writeBioUMLAnnotation )
        {
            Element annotationElement = document.createElement(ANNOTATION_ELEMENT);
            Element diagramElement = document.createElement(BIOUML_DIAGRAM_REFERENCE);
            diagramElement.setAttribute( BIOUML_DIAGRAM_PATH, path );
            annotationElement.appendChild( diagramElement );
            externalModelDef.appendChild( annotationElement );
        }        
        externalModelDefList.appendChild(externalModelDef);
    }

    protected void writeDeletionsList(Element subModelElement, SubDiagram subDiagram)
    {
        Element deletionList = document.createElement(SbmlCompositeConstants.DELETION_LIST_ELEMENT);

        State state = subDiagram.getState();
        if( state == null )
            return;

        for( Object elem : getRemovedElements(state) )
        {
            Element deletion = document.createElement(SbmlCompositeConstants.DELETION_ELEMENT);
            if( elem instanceof String )
                deletion.setAttribute(SbmlCompositeConstants.ID_REF, (String)elem);
            else if( elem instanceof MetaIdInfo )
                deletion.setAttribute(SbmlCompositeConstants.META_ID_REF, ( (MetaIdInfo)elem ).getId());
            deletionList.appendChild(deletion);
        }
        if( deletionList.hasChildNodes() )
            subModelElement.appendChild(deletionList);
    }

    protected void writeModelDefinitionList(Element modelElement)
    {
        NodeList nodeList = modelElement.getElementsByTagName(SbmlCompositeConstants.MODEL_DEFINITION_LIST_ELEMENT);
        boolean noModeDefListYet = nodeList.getLength() == 0;
        Element modelDefinitionList = noModeDefListYet ? document.createElement(SbmlCompositeConstants.MODEL_DEFINITION_LIST_ELEMENT)
                : (Element)nodeList.item(0);

        for( ModelDefinition modelDefinition : diagram.stream(ModelDefinition.class) )
        {
            try
            {
                Diagram innerDiagram = modelDefinition.getDiagram();
                
                SbmlModelWriter_31 writer;
                if( parentWriter instanceof SbmlModelWriter_32 )
                    writer = new SbmlModelWriter_32();
                else 
                    writer = new SbmlModelWriter_31();
                
                writer.setWriteBioUMLAnnotation(writeBioUMLAnnotation);
                Element modelDefinitionElement = document.createElement(SbmlCompositeConstants.MODEL_DEFINITION_ELEMENT);
                boolean notificationEnabled = innerDiagram.isNotificationEnabled();
                innerDiagram.setNotificationEnabled(false);
                State currentState = innerDiagram.getCurrentState();
                if( currentState != null )
                    innerDiagram.restore();

                writer.writeDiagram(modelDefinitionElement, document, innerDiagram);

                if( writeBioUMLAnnotation )
                {
                    Element annotation = getElement(modelDefinitionElement, ANNOTATION_ELEMENT);// BIOUML_ELEMENT);
                    Element biouml = getElement(annotation, BIOUML_ELEMENT);
                    Element compartmentInfoElement = document.createElement(BIOUML_COMPARTMENT_INFO_ELEMENT);
                    compartmentInfoElement.setAttribute(BIOUML_COMPLETE_NAME_ATTR, modelDefinition.getCompleteNameInDiagram());
                    DiagramXmlWriter.writeCompartmentInfo(compartmentInfoElement, modelDefinition, document);
                    biouml.appendChild(compartmentInfoElement);
                }

                if( currentState != null )
                    innerDiagram.setStateEditingMode(currentState);
                innerDiagram.setNotificationEnabled(notificationEnabled);
                modelDefinitionList.appendChild(modelDefinitionElement);
            }
            catch( Throwable t )
            {
                error("ERROR_MODEL_DEFINITION_WRITING", new String[] {diagram.getName(), modelDefinition.getName(), t.getMessage()});
            }
        }
        if( noModeDefListYet && modelDefinitionList.hasChildNodes() )
            modelElement.appendChild(modelDefinitionList);
    }


    @Override
    protected void processSpecie(Element speciesElement, Node species)
    {
        writeReplacementElements(speciesElement, species.getRole(VariableRole.class));
    }

    @Override
    protected void processReaction(Element reactionElement, Node reactionNode)
    {
        Variable variable = emodel.getVariable(reactionNode.getRole(Equation.class).getVariable());
        if( variable != null )
            writeReplacementElements(reactionElement, variable);
    }

    protected void writeReplacementElements(Element element, @Nonnull Variable variable)
    {
        Element list = document.createElement(SbmlCompositeConstants.REPLACED_LIST_ELEMENT);

        List<Node> portNodes = Util.findPrivatePorts(diagram, variable.getName());
        for (Node port: portNodes)
            port.edges().filter(e -> Util.isConnection(e)).forEach(e -> writeReplacementElement(e, port, list, element));

        if( variable instanceof VariableRole )
        {
            Node node = (Node) ( (VariableRole)variable ).getDiagramElement();
            node.edges().filter(e -> Util.isConnection(e) && e.getRole() != null)
                    .forEach(e -> writeReplacementElement(e, node, list, element));
        }

        if( list.hasChildNodes() )
            element.appendChild(list);
    }

    protected void writeReplacementElement(Edge e, Node node, Element replacedElements, Element specieElement)
    {
        try
        {
            boolean inputIsUpperNode = e.getInput().equals(node);
            Node otherNode = e.getOtherEnd(node);
            boolean directed = Util.isDirectedConnection(e);
            if( Util.isModulePort(otherNode) )
            {
                boolean forward; //forward means that upper element replaces element from submodel
                if( directed )
                {
                    forward = inputIsUpperNode;
                }
                else
                {
                    boolean unknown = MainVariableType.NOT_SELECTED.equals(e.getRole(UndirectedConnection.class).getMainVariableType());
                    boolean mainInput = MainVariableType.INPUT.equals(e.getRole(UndirectedConnection.class).getMainVariableType());
                    forward = unknown || inputIsUpperNode == mainInput;
                }

                String elementTag = forward ? SbmlCompositeConstants.REPLACED_ELEMENT : SbmlCompositeConstants.REPLACED_BY_ELEMENT;
                Element replacementElement = document.createElement(elementTag);
                replacementElement.setAttribute(SbmlConstants.METAID_ATTR, e.getName());

                SubDiagram subDiagram = (SubDiagram)otherNode.getParent();
                Diagram innerDiagram = subDiagram.getDiagram();
                boolean innerIsComposite = innerDiagram.getType() instanceof SbmlCompositeDiagramType;

                replacementElement.setAttribute(SbmlCompositeConstants.SUBMODEL_REF, getSbmlId(subDiagram));

                if( e.getRole() instanceof UndirectedConnection )
                {
                    String convFactor = e.getRole(UndirectedConnection.class).getConversionFactor();
                    if( !convFactor.isEmpty() )
                        replacementElement.setAttribute(SbmlCompositeConstants.PARAMETER_COMVERSION_FACTOR, convFactor);
                }

                if( Util.isPropagatedPort2(otherNode) ) //means that replacement goes deeper
                {
                    Node nextPort = this.getNextPort(otherNode);
                    SubDiagram nextSubDiagram = (SubDiagram)nextPort.getParent();
                    replacementElement.setAttribute(SbmlCompositeConstants.ID_REF, getSbmlId(nextSubDiagram));
                    Element sBaseRef = createSbaseRef(nextPort, subDiagram.getState());
                    replacementElement.appendChild(sBaseRef);
                }
                else
                {
                    Node innerPortNode = innerDiagram.findNode(otherNode.getName());
                    if( isStateNode(innerPortNode, subDiagram.getState()) || !innerIsComposite )
                    {
                        String varName = Util.getPortVariable(otherNode);
                        EModel emodel = ( innerDiagram.getRole(EModel.class) );
                        Variable var = emodel.getVariable(varName);
                        if( var instanceof VariableRole )
                            varName = getSbmlId((Node) ( (VariableRole)var ).getDiagramElement());
                        replacementElement.setAttribute( SbmlCompositeConstants.ID_REF, varName );
                    }
                    else
                    {
                        replacementElement.setAttribute( SbmlCompositeConstants.PORT_REF, getSbmlId( otherNode ) );
                    }
                }

                if( forward ) //reaplcementElements goes ot replacedEdlementList
                    replacedElements.appendChild( replacementElement );
                else //replacedBy goes to specie itself
                    specieElement.appendChild( replacementElement );

                if( writeBioUMLAnnotation )
                {
                    Element annotationElement = document.createElement(ANNOTATION_ELEMENT);
                    Element bioumlElement = document.createElement(BIOUML_ELEMENT);
                    bioumlElement.setAttribute(BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE);
                    Element edgeInfoElement = document.createElement(BIOUML_EDGE_INFO_ELEMENT);
                    edgeInfoElement.setAttribute("type", directed ? "directed" : "undirected");
                    bioumlElement.appendChild(edgeInfoElement);
                    annotationElement.appendChild(bioumlElement);
                    replacementElement.appendChild(annotationElement);
                }
                

            }
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error while writing of connection edge " + e.getName() + ": " + ex.getMessage(), ex);
        }
    }

    protected Element createSbaseRef(Node portNode, State state) throws Exception
    {
        Element result = document.createElement(SbmlCompositeConstants.BASE_REF_ELEMENT);
        Node nextPortNode = getNextPort(portNode);

        if( nextPortNode == null )
        {
            Diagram diagram = ( (SubDiagram)portNode.getParent() ).getDiagram();
            EModel emodel = diagram.getRole(EModel.class);
            if( isStateNode(portNode, state) || ! ( diagram.getType() instanceof SbmlCompositeDiagramType ) )
            {
                String variableRef = Util.getPortVariable(portNode);
                Variable var = emodel.getVariable(variableRef);
                if( var instanceof VariableRole )
                    variableRef = ( (VariableRole)var ).getDiagramElement().getName();
                result.setAttribute(SbmlCompositeConstants.ID_REF, variableRef);
            }
            else
            {
                result.setAttribute(SbmlCompositeConstants.PORT_REF, getSbmlId(portNode));
            }
            return result;
        }

        SubDiagram subDiagram = (SubDiagram)nextPortNode.getParent();
        result.setAttribute(SbmlCompositeConstants.ID_REF, getSbmlId(subDiagram));
        result.appendChild(createSbaseRef(nextPortNode, subDiagram.getState()));

        return result;
    }

    /**
     * Returns true if given node appears in its diagram only in current state and is not presented in diagram version without states
     * @param node
     * @return
     * @throws Exception
     */
    protected boolean isStateNode(Node node, State state) throws Exception
    {
        if( node.getParent() instanceof SubDiagram )
        {
            SubDiagram subDiagram = (SubDiagram)node.getParent();
            state = subDiagram.getState();
            node = subDiagram.getDiagram().findNode(node.getName());
        }

        return state != null && StreamEx.of(state.getStateUndoManager().getEditsFlat()).flatMap(TransactionUtils::editsFlat)
                .select(DataCollectionAddUndo.class).map(DataCollectionAddUndo::getDataElement).has(node);
    }

    protected Node getNextPort(Node node) throws Exception
    {
        if( !Util.isPropagatedPort2(node) )
            return null;

        SubDiagram subDiagram = (SubDiagram)node.getParent();
        String associatedPortName = node.getAttributes().getProperty(SubDiagram.ORIGINAL_PORT_ATTR).getValue().toString();
        Diagram associatedDiagram = subDiagram.getDiagram();
        Node associatedPort = associatedDiagram.findNode(associatedPortName);

        return associatedPort.edges().findFirst(Util::isConnection).map(edge -> edge.getOtherEnd(associatedPort)).orElse(null);
    }

    @Override
    protected void processParameter(Element parameterElement, Variable parameter)
    {
        writeReplacementElements(parameterElement, parameter);
    }

    @Override
    protected void processCompartment(Element parameterElement, Compartment compartment)
    {
        writeReplacementElements(parameterElement, compartment.getRole(VariableRole.class));
    }


    @Override
    protected void preprocess(Diagram diagram)
    {

    }

    protected void writePortList(Element modelElement, Diagram diagram)
    {
        Element portList = document.createElement(SbmlCompositeConstants.PORT_LIST_ELEMENT);

        for( Node node : diagram.stream(Node.class).filter(node -> Util.isPublicPort(node)) )
        {
            try
            {
                Element portElement = document.createElement(SbmlCompositeConstants.PORT_ELEMENT);
                portElement.setAttribute(SbmlCompositeConstants.ID, getSbmlId(node));
                String variableRef = Util.getPortVariable(node);
                Variable var = emodel.getVariable(variableRef);
                if( var instanceof VariableRole )
                    variableRef = ( (VariableRole)var ).getDiagramElement().getName();

                if( variableRef.startsWith("$$") )
                    variableRef = variableRef.substring(7); //dirty hack for the case of reaction variables
                portElement.setAttribute(SbmlCompositeConstants.ID_REF, variableRef);
                portList.appendChild(portElement);
                writeSBOTerm(portElement, node.getAttributes());

                if( writeBioUMLAnnotation )
                {
                    Element annotationElement = document.createElement(ANNOTATION_ELEMENT);
                    Element bioumlElement = document.createElement(BIOUML_ELEMENT);
                    bioumlElement.setAttribute(BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE);
                    Element nodeInfoElement = document.createElement(BIOUML_NODE_INFO_ELEMENT);
                    DiagramXmlWriter.writeNodeInfo(nodeInfoElement, node, document);
                    nodeInfoElement.setAttribute(BIOUML_COMPLETE_NAME_ATTR, node.getCompleteNameInDiagram());
                    nodeInfoElement.setAttribute(ConnectionPort.PORT_TYPE, Util.getPortType(node));
                    bioumlElement.appendChild(nodeInfoElement);
                    annotationElement.appendChild(bioumlElement);
                    portElement.appendChild(annotationElement);
                }
            }
            catch( Exception ex )
            {
                error("ERROR_PORT_ELEMENT_WRITING", new String[] {diagram.getName(), node.getName(), ex.getMessage()});
            }
        }

        if( portList.hasChildNodes() )
            modelElement.appendChild(portList);
    }

    @Override
    public String getNameSpace()
    {
        return "http://www.sbml.org/sbml/level3/version1/comp/version1";
    }

    @Override
    public String getPackageName()
    {
        return "comp";
    }

    /**
     * returns list of diagram nodes that were removed in given state
     * @param diagram
     * @param state
     * @return
     */
    public static List<Object> getRemovedElements(State state)
    {
        List<Object> result = new ArrayList<>();
        for( UndoableEdit edit : state.getStateUndoManager().getEditsFlat() )
        {
            if( edit instanceof DataCollectionRemoveUndo )
            {
                DataElement de = ( (DataCollectionRemoveUndo)edit ).getDataElement();
                if( de instanceof DiagramElement )
                    result.add(de.getName());
            }
            else if( edit instanceof StatePropertyChangeUndo )
            {
                StatePropertyChangeUndo propertyEdit = (StatePropertyChangeUndo)edit;
                String propertyName = propertyEdit.getPropertyName();
                Object source = propertyEdit.getSource();
                if( source instanceof Node )
                {
                    Node node = (Node)source;
                    if( propertyEdit.getOldValue() instanceof Object[] && propertyEdit.getNewValue() instanceof Object[] )
                    {
                        Object[] oldValue = (Object[])propertyEdit.getOldValue();
                        Object[] newValue = (Object[])propertyEdit.getNewValue();
                        if( newValue.length == oldValue.length - 1 ) //check that it is exactly deletion of one array element
                        {
                            if( newValue.length == 0 )
                            {
                                MetaIdInfo metaIdInfo = SbmlModelWriter_21.getMetaId(node, propertyName, 0);
                                if( metaIdInfo != null )
                                    result.add(metaIdInfo);
                            }
                            for( Object element : newValue )
                            {
                                int oldIndex = -1;
                                for( int j = 0; j < oldValue.length; j++ )
                                {
                                    if( oldValue[j] != element )
                                    {
                                        oldIndex = j;
                                        break;
                                    }
                                }
                                if( oldIndex != -1 )
                                {
                                    MetaIdInfo metaIdInfo = SbmlModelWriter_21.getMetaId(node, propertyName, oldIndex);
                                    if( metaIdInfo != null )
                                        result.add(metaIdInfo);
                                }
                            }
                        }
                    }
                    else
                    {
                        MetaIdInfo metaIdInfo = SbmlModelWriter_21.getMetaId(node, propertyName);
                        if( metaIdInfo != null )
                        {
                            //TODO: handle situation when removed property has no meta id!
                            result.add(metaIdInfo);
                        }
                    }
                }
            }
        }
        return result;
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
}
