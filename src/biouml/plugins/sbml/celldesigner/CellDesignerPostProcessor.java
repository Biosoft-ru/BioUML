package biouml.plugins.sbml.celldesigner;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementStyle;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.Type;
import biouml.plugins.sbml.celldesigner.CellDesignerExtension.DoublePoint;
import biouml.plugins.sbml.celldesigner.CellDesignerExtension.ElementSet;
import biouml.plugins.sbml.celldesigner.CellDesignerExtension.ReactionSpeciesCache;
import biouml.plugins.sbml.converters.SBGNConverter;
import biouml.plugins.sbml.extensions.SbmlExtensionSupport;
import biouml.standard.diagram.Util;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.Pen;
import ru.biosoft.util.TextUtil;

public class CellDesignerPostProcessor extends SbmlExtensionSupport implements CellDesignerConstants
{
    protected static final Logger log = Logger.getLogger(CellDesignerUtils.class.getName());
    
    protected Map<DiagramElement, ElementSet> unprocessedElements = new HashMap<>();
    protected ReactionSpeciesCache reactionSpecieCache = new ReactionSpeciesCache();
    protected Map<String, Node> complexes = new HashMap<>();
    protected Element proteinListElement = null;
    protected Map<String, List<Object>> proteinRefs = new HashMap<>(); //proteinReference to list of node id
    protected Map<String, String> stateValues = new HashMap<>(); //state values temporary map
    
    public CellDesignerPostProcessor(CellDesignerExtension extension)
    {
        this.unprocessedElements = extension.unprocessedElements;
        this.reactionSpecieCache = extension.reactionSpecieCache;
        this.complexes = extension.complexes;
        this.stateValues = extension.stateValues;
        this.proteinListElement = extension.proteinListElement;
        this.proteinRefs = extension.proteinRefs;
    }
    
    /**
     * Process all elements from unprocessed map
     * @param diagram
     */
    public void processElements(@Nonnull Diagram diagram)
    {
        for( Map.Entry<DiagramElement, ElementSet> entry : unprocessedElements.entrySet() )
            readSpecieAttr(entry.getValue(), entry.getKey());
        if( proteinListElement != null )
            readProteinList(proteinListElement, diagram);
    }
    
    /**
     * Process 'celldesigner:listOfProteins'
     */
    protected void readProteinList(Element element, @Nonnull Diagram diagram)
    {
        Element listElement = getTopElement(element, CELLDESIGNER_PROTEIN_LIST);
        if( listElement == null )
            return;

        NodeList list = listElement.getChildNodes();
        for( int i = 0; i < list.getLength(); i++ )
        {
            org.w3c.dom.Node node = list.item(i);
            if( ( node instanceof Element ) && ( node.getNodeName().equals(CELLDESIGNER_PROTEIN) ) )
            {
                String id = ( (Element)node ).getAttribute(ID_ATTR);
                List<Object> baseNodeList = proteinRefs.get(id);
                if( baseNodeList == null )
                    return;

                try
                {
                    for( Object baseNodeObj : baseNodeList )
                    {
                        Node baseNode = ( baseNodeObj instanceof Node ) ? (Node)baseNodeObj
                                : CellDesignerUtils.findNode(diagram, (String)baseNodeObj, null);
                        fillProteinForNode((Element)node, baseNode);
                        Object nodeAliases = baseNode.getAttributes().getValue(SBGNConverter.ALIASES_ATTR);
                        if( nodeAliases instanceof Node[] )
                        {
                            for( Node n : (Node[])nodeAliases )
                                fillProteinForNode((Element)node, n);
                        }
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Cannot parse modifications", e);
                }

            }
        }
    }

    protected void fillProteinForNode(Element element, Node baseNode) throws Exception
    {
        String title = ( element ).getAttribute(NAME_ATTR);
        if( title != null && title.trim().length() > 0 )
            baseNode.setTitle(title);

        Element modificationList = getElement(element, CELLDESIGNER_MODIFICATION_RESIDUES_LIST);
        if( modificationList != null )
        {
            NodeList childs = modificationList.getChildNodes();
            for( int j = 0; j < childs.getLength(); j++ )
            {
                org.w3c.dom.Node mNode = childs.item(j);
                if( ( mNode instanceof Element ) && ( mNode.getNodeName().equals(CELLDESIGNER_MODIFICATION_RESIDUE) ) )
                {
                    Element modification = (Element)mNode;

                    String mID = modification.getAttribute(ID_ATTR);
                    String angle = modification.getAttribute(ANGLE_ATTR);
                    if( angle.trim().length() == 0 )
                        angle = "0";

                    Compartment parent = null;
                    if( baseNode instanceof Compartment )
                        parent = (Compartment)baseNode;

                    Node variable = new Node(parent, new Stub(null, mID, Type.TYPE_VARIABLE));
                    variable.getAttributes().add(new DynamicProperty(SBGNConverter.ANGLE_ATTR, String.class, angle));
                    String value = stateValues.get(CellDesignerUtils.getCompleteNameInDiagram(baseNode) + "/" + mID);
                    if( value != null )
                        variable.setTitle(value);
                    else 
                        variable.setTitle("");

                    if( parent != null )
                        ( (Compartment)baseNode ).put(variable);
                    else
                        CellDesignerUtils.addElementToNodeAttributes(baseNode, variable, SBGNConverter.MODIFICATION_ATTR);
                }
            }
        }
    }
    
    /**
     * Base method to process element from unprocessed element list
     */
    protected void readSpecieAttr(ElementSet elementSet, DiagramElement de)
    {
        if( de instanceof Node )
        {
            Element identityElement = elementSet.getElement(CELLDESIGNER_SPECIE_IDENTITY);
            if( identityElement != null )
            {
                CellDesignerUtils.readClass(identityElement, de);

                Element nameElement = getElement(identityElement, CELLDESIGNER_NAME);
                if( nameElement != null )
                {
                    String title = getTextContent(nameElement);
                    de.setTitle(title);
                    if( de.getAttributes().getValue(SBGNConverter.ALIASES_ATTR) instanceof Node[] )
                    {
                        for( Node n : (Node[])de.getAttributes().getValue(SBGNConverter.ALIASES_ATTR) )
                            n.setTitle(title);
                    }
                }

                Element stateElement = getElement(identityElement, CELLDESIGNER_STATE);
                if( stateElement != null )
                {
                    Element homodimer = getElement(stateElement, CELLDESIGNER_HOMODIMER);
                    if( homodimer != null )
                    {
                        try
                        {
                            int multimer = Integer.parseInt(getTextContent(homodimer));
                            if( multimer > 1 )
                                CellDesignerUtils.addProperty(de, new DynamicProperty(SBGNPropertyConstants.SBGN_MULTIMER_PD, Integer.class, multimer), true);
                        }
                        catch( Exception e )
                        {
                            log.log(Level.SEVERE, "Cannot parse " + CELLDESIGNER_HOMODIMER + " field", e);
                        }
                    }

                    Element statesList = getElement(stateElement, CELLDESIGNER_STRUCTUAL_STATES_LIST);
                    if( statesList != null )
                    {
                        StringBuffer states = null;
                        NodeList list = statesList.getChildNodes();
                        for( int i = 0; i < list.getLength(); i++ )
                        {
                            org.w3c.dom.Node node = list.item(i);
                            if( ( node instanceof Element ) && ( node.getNodeName().equals(CELLDESIGNER_STRUCTUAL_STATE) ) )
                            {
                                String state = ( (Element)node ).getAttribute(STRUCTURAL_STATE_ATTR);
                                if( states == null )
                                    states = new StringBuffer();
                                else
                                    states.append(';');
                                states.append(state);
                            }
                        }
                        if(states != null)
                            CellDesignerUtils.addProperty(de, new DynamicProperty("states", String.class, states.toString()), true);
                    }

                    Element modificationsList = getElement(stateElement, CELLDESIGNER_MODIFICATIONS_LIST);
                    if( modificationsList != null )
                    {
                        NodeList list = modificationsList.getChildNodes();
                        for( int i = 0; i < list.getLength(); i++ )
                        {
                            org.w3c.dom.Node node = list.item(i);
                            if( ( node instanceof Element ) && ( node.getNodeName().equals(CELLDESIGNER_MODIFICATION) ) )
                            {
                                String id = ( (Element)node ).getAttribute(RESIDUE_ATTR);
                                String state = ( (Element)node ).getAttribute(STATE_ATTR);
                                if( state.length() > 0 )
                                {
                                    if( state.equals("phosphorylated") )
                                        stateValues.put(CellDesignerUtils.getCompleteNameInDiagram(de) + "/" + id, "P");
                                    else if( state.equals("ubiquitinated") )
                                        stateValues.put(CellDesignerUtils.getCompleteNameInDiagram(de) + "/" + id, "Ub");
                                }
                            }
                        }
                    }
                }
                addProteinRef(identityElement, de);
            }

            Element heterodimerElement = elementSet.getElement(CELLDESIGNER_HETERODIMER);
            if( heterodimerElement != null )
            {
                //this element should be a complex
                try
                {
                    CellDesignerUtils.addProperty(de, new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE, String.class, "complex"), true);
                    CellDesignerUtils.addProperty(de, new DynamicProperty(SBGNConverter.BODY_COLOR_ATTR, Color.class, new Color(255, 255, 255, 50)), true);
                    Element heterodimerEntryList = getElement(heterodimerElement, CELLDESIGNER_HETERODIMER_ENRTY_LIST);
                    if( heterodimerEntryList != null )
                    {
                        NodeList list = heterodimerEntryList.getChildNodes();
                        for( int i = 0; i < list.getLength(); i++ )
                        {
                            org.w3c.dom.Node n = list.item(i);
                            if( ( n instanceof Element ) && ( n.getNodeName().equals(CELLDESIGNER_HETERODIMER_ENRTY) ) )
                            {
                                readInnerHeterodimer(de, (Element)n);
                                Object nodeAliases = de.getAttributes().getValue(SBGNConverter.ALIASES_ATTR);
                                if( nodeAliases instanceof Node[] )
                                {
                                    for( Node aliasNode : (Node[])nodeAliases )
                                        readInnerHeterodimer(aliasNode, (Element)n);
                                }
                            }
                        }
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Cannot parse heterodimer attr", e);
                }
            }

            if( ( (Node)de ).getKernel() instanceof Reaction )
            {
                Diagram diagram = Diagram.getDiagram(de);
                Node rNode = (Node)de;

                List<SpeciesInfo> reactants = getSpeciesList(elementSet, CELLDESIGNER_REACTANT_LIST, CELLDESIGNER_REACTANT,
                        rNode.getName(), SpecieReference.REACTANT);
                List<SpeciesInfo> products = getSpeciesList(elementSet, CELLDESIGNER_PRODUCT_LIST, CELLDESIGNER_PRODUCT, rNode.getName(),
                        SpecieReference.PRODUCT);

                Element typeElement = elementSet.getElement(CELLDESIGNER_REACTION_TYPE);
                if( typeElement != null )
                {
                    try
                    {
                        String type = getTextContent(typeElement);
                        CellDesignerUtils.addProperty(rNode, new DynamicProperty(SBGNConverter.REACTION_TYPE_ATTR, String.class, type), true);
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, "Cannot read reaction type", e);
                    }
                }

                try
                {
                    Edge[] edges = rNode.getEdges();
                    Element pointsElement = elementSet.getElement(CELLDESIGNER_EDIT_POINTS);
                    if( pointsElement != null )
                    {

                        String[] pointValues = TextUtil.split( getTextContent(pointsElement), ' ' );
                        if( reactants.size() == 1 && products.size() == 1 )
                        {
                            //calculation without central point
                            String startBaseName = reactants.get(0).getName();
                            Node start = CellDesignerUtils.findNode(diagram, startBaseName, reactants.get(0).getAlias());
                            if( start != null )
                                start = getNodeAlias(start, reactants.get(0).getAlias());

                            String endBaseName = products.get(0).getName();
                            Node end = CellDesignerUtils.findNode(diagram, endBaseName, products.get(0).getAlias());
                            if( end != null )
                                end = getNodeAlias(end, products.get(0).getAlias());

                            if( start != null && end != null )
                            {
                                SpeciesInfo si1 = reactants.get(0);
                                DoublePoint p1 = CellDesignerUtils.getNodePort(start, si1.getPortX(), si1.getPortY());
                                SpeciesInfo si2 = products.get(0);
                                DoublePoint p2 = CellDesignerUtils.getNodePort(end, si2.getPortX(), si2.getPortY());
                                DoublePoint p3 = CellDesignerUtils.getOrthogonalPoint(p1, p2);

                                DoublePoint[] points = new DoublePoint[pointValues.length];
                                for( int i = 0; i < pointValues.length; i++ )
                                {
                                    points[i] =  CellDesignerUtils.getPointByBase(pointValues[i], p1, p2, p3);
                                }

                                int center = ( points.length % 2 == 0 ) ? points.length / 2 : points.length / 2 + 1;
                                if( center == 0 )
                                {
                                    rNode.setLocation((int) ( points[0].x - REACTION_DELTA ), (int) ( points[0].y - REACTION_DELTA ));
                                }
                                else
                                {
                                    DoublePoint c1 = points[center - 1];
                                    DoublePoint c2 = center < points.length ? points[center]: p2;
                                    
                                    DoublePoint centerPoint = new DoublePoint( ( c1.x + c2.x ) / 2.0 - REACTION_DELTA,
                                            ( c1.y + c2.y ) / 2.0 - REACTION_DELTA);
                                    rNode.setLocation((int)centerPoint.x, (int)centerPoint.y);
                                    for( Edge edge : rNode.getEdges() )
                                    {
                                        boolean input = edge.getInput().getName().equals(startBaseName)
                                                || CellDesignerUtils.containsComplexElement(edge.getInput(), startBaseName);
                                        boolean ouput = edge.getOutput().getName().equals(endBaseName)
                                                || CellDesignerUtils.containsComplexElement(edge.getOutput(), endBaseName);
                                        if( input || ouput )
                                        {
                                            Point inport = getInPort(edge);
                                            Point outport = getOutPort(edge);

                                            Path path = new Path();
                                            if( input )
                                            {
                                                path.addPoint(inport.x, inport.y);
                                                for( int i = 0; i < center; i++ )
                                                    path.addPoint((int)points[i].x, (int)points[i].y);

                                                path.addPoint((int)centerPoint.x, (int)centerPoint.y);
                                                CellDesignerUtils.addProperty(edge, new DynamicProperty(SBGNConverter.SPECIE_NAME_ATTR, String.class,
                                                        CellDesignerUtils.getCompleteName(start)), false);
                                            }
                                            if( ouput )
                                            {
                                                path.addPoint((int)centerPoint.x, (int)centerPoint.y);
                                                for( int i = center; i < points.length; i++ )
                                                    path.addPoint((int)points[i].x, (int)points[i].y);

                                                path.addPoint(outport.x, outport.y);
                                                CellDesignerUtils.addProperty(edge, new DynamicProperty(SBGNConverter.SPECIE_NAME_ATTR, String.class,
                                                        CellDesignerUtils.getCompleteName(end)), false);
                                            }
                                            edge.setPath(path);
                                            diagram.getType().getSemanticController().recalculateEdgePath(edge);
                                        }
                                    }
                                }
                            }
                        }
                        else if( ( reactants.size() + products.size() ) >= 3 )
                        {
                            //calculation with central point
                            DoublePoint[] portPoints = new DoublePoint[3];
                            DoublePoint[] centerPoints = new DoublePoint[3];
                            int ind = 0;
                            for( SpeciesInfo si : reactants )
                            {
                                if( ind > 2 )
                                    break;
                                Node node = CellDesignerUtils.findNode(diagram, si.getName(), si.getAlias());
                                Edge edge = CellDesignerUtils.findEdge(rNode, node, SpecieReference.REACTANT);
                                node = getNodeAlias(node, si.getAlias());
                                portPoints[ind] = CellDesignerUtils.getNodePort(node, si.getPortX(), si.getPortY());
                                centerPoints[ind++] = CellDesignerUtils.getNodeCenter(node);
                                CellDesignerUtils.addProperty(edge, new DynamicProperty(SBGNConverter.SPECIE_NAME_ATTR, String.class, CellDesignerUtils.getCompleteName(node)),
                                        false);
                            }
                            for( SpeciesInfo si : products )
                            {
                                if( ind > 2 )
                                    break;
                                Node node = CellDesignerUtils.findNode(diagram, si.getName(), si.getAlias());
                                Edge edge = CellDesignerUtils.findEdge(rNode, node, SpecieReference.PRODUCT);
                                node = getNodeAlias(node, si.getAlias());
                                portPoints[ind] = CellDesignerUtils.getNodePort(node, si.getPortX(), si.getPortY());
                                centerPoints[ind++] = CellDesignerUtils.getNodeCenter(node);
                                CellDesignerUtils.addProperty(edge, new DynamicProperty(SBGNConverter.SPECIE_NAME_ATTR, String.class, CellDesignerUtils.getCompleteName(node)),
                                        false);
                            }

                            DoublePoint cp = CellDesignerUtils.getPointByBase(pointValues[pointValues.length - 1], centerPoints[0], centerPoints[1],
                                    centerPoints[2]);
                            rNode.setLocation((int)cp.x, (int)cp.y);

                            int currentIdx = 0;
                            String numAttr = pointsElement.getAttribute(NUM_ATTR + "0");
                            if( numAttr != null && numAttr.length() > 0 )
                            {
                                int edgeSegments1 = Integer.parseInt(numAttr);
                                if( edgeSegments1 > 0 )
                                {
                                    String[] pairs = new String[edgeSegments1];
                                    for( int i = 0; i < edgeSegments1; i++ )
                                        pairs[i] = pointValues[currentIdx++];
                                    DoublePoint pt = CellDesignerUtils.getOrthogonalPoint(cp, portPoints[0]);
                                    fillPath(diagram, rNode, edges[0], pairs, cp, portPoints[0], pt, ( edges[0].getInput() == rNode ));
                                }
                            }

                            numAttr = pointsElement.getAttribute(NUM_ATTR + "1");
                            if( numAttr != null && numAttr.length() > 0 )
                            {
                                int edgeSegments2 = Integer.parseInt(numAttr);
                                if( edgeSegments2 > 0 )
                                {
                                    String[] pairs = new String[edgeSegments2];
                                    for( int i = 0; i < edgeSegments2; i++ )
                                        pairs[i] = pointValues[currentIdx++];
                                    DoublePoint pt = CellDesignerUtils.getOrthogonalPoint(cp, portPoints[1]);
                                    fillPath(diagram, rNode, edges[1], pairs, cp, portPoints[1], pt, ( edges[1].getInput() == rNode ));
                                }
                            }

                            numAttr = pointsElement.getAttribute(NUM_ATTR + "2");
                            if( numAttr != null && numAttr.length() > 0 )
                            {
                                int edgeSegments3 = Integer.parseInt(numAttr);
                                if( edgeSegments3 > 0 )
                                {
                                    String[] pairs = new String[edgeSegments3];
                                    for( int i = 0; i < edgeSegments3; i++ )
                                        pairs[i] = pointValues[currentIdx++];
                                    DoublePoint pt = CellDesignerUtils.getOrthogonalPoint(cp, portPoints[2]);
                                    fillPath(diagram, rNode, edges[2], pairs, cp, portPoints[2], pt, ( edges[2].getInput() == rNode ));
                                }
                            }
                        }
                    }
                    else
                    {
                        //move reaction node to the center by default
                        DoublePoint average = new DoublePoint(0.0, 0.0);
                        int count = 0;
                        for( SpeciesInfo si : reactants )
                        {
                            Node node = CellDesignerUtils.findNode(diagram, si.getName(), si.getAlias());
                            String completeName = CellDesignerUtils.getCompleteName(node);
                            Edge edge = CellDesignerUtils.findEdge(rNode, node, SpecieReference.REACTANT);
                            node = getNodeAlias(node, si.getAlias());
                            if( node != null )
                            {
                                completeName = CellDesignerUtils.getCompleteName(node);
                                DoublePoint p = CellDesignerUtils.getNodePort(node, si.getPortX(), si.getPortY());
                                average.x += p.x;
                                average.y += p.y;
                                count++;
                            }
                            CellDesignerUtils.addProperty(edge, new DynamicProperty(SBGNConverter.SPECIE_NAME_ATTR, String.class, completeName), false);
                        }
                        for( SpeciesInfo si : products )
                        {
                            Node node = CellDesignerUtils.findNode(diagram, si.getName(), si.getAlias());
                            String completeName = CellDesignerUtils.getCompleteName(node);
                            Edge edge = CellDesignerUtils.findEdge(rNode, node, SpecieReference.PRODUCT);
                            node = getNodeAlias(node, si.getAlias());
                            if( node != null )
                            {
                                completeName = CellDesignerUtils.getCompleteName(node);
                                DoublePoint p = CellDesignerUtils.getNodePort(node, si.getPortX(), si.getPortY());
                                average.x += p.x;
                                average.y += p.y;
                                count++;
                            }
                            CellDesignerUtils.addProperty(edge, new DynamicProperty(SBGNConverter.SPECIE_NAME_ATTR, String.class, completeName), false);
                        }
                        if( count != 0 )
                        {
                            average.x = ( average.x / count ) - REACTION_DELTA;
                            average.y = ( average.y / count ) - REACTION_DELTA;
                            rNode.setLocation((int)average.x, (int)average.y);
                        }
                    }
                    //set edge titles
                    for( Edge edge : edges )
                    {
                        if( ( edge.getKernel() instanceof SpecieReference )
                                && ! ( ( (SpecieReference)edge.getKernel() ).getRole().equals(SpecieReference.PRODUCT) ) )
                        {
                            edge.getAttributes().add(new DynamicProperty("text", String.class, rNode.getName()));
                        }
                    }
                    //alias indicates that this element is useful
                    CellDesignerUtils.addProperty(rNode, new DynamicProperty(SBGNConverter.ALIAS_ATTR, String.class, rNode.getName()), false);
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Cannot calculate edge path", e);
                }

                //parse line pen
                Element lineElement = elementSet.getElement(CELLDESIGNER_LINE);
                if( lineElement != null )
                {
                    Pen linePen = readLinePen(lineElement);
                    if( linePen != null )
                    {
                        try
                        {
                            for( Edge edge : ( (Node)de ).getEdges() )
                            {
                                edge.setPredefinedStyle(DiagramElementStyle.STYLE_NOT_SELECTED);
                                edge.getCustomStyle().setPen(linePen.clone());
                            }
                        }
                        catch( Exception e )
                        {
                            log.log(Level.SEVERE, "Parse line brush error", e);
                        }
                    }
                }

                //parse modifications
                Element modificationsElement = elementSet.getElement(CELLDESIGNER_MODIFICATION_LIST);
                if( modificationsElement != null )
                {
                    NodeList list = modificationsElement.getChildNodes();
                    for( int i = 0; i < list.getLength(); i++ )
                    {
                        org.w3c.dom.Node node = list.item(i);
                        if( ( node instanceof Element ) && ( node.getNodeName().equals(CELLDESIGNER_MODIFICATION) ) )
                            readModification((Node)de, (Element)node, diagram);
                    }
                }

                //parse reactants and products links info
                if( reactants.size() > 0 )
                {
                    parseReactionLinks(elementSet, (Node)de, CELLDESIGNER_REACTANT_LINKS_LIST, CELLDESIGNER_REACTANT_LINK, REACTANT_ATTR,
                            diagram, reactants.get(0), SpecieReference.REACTANT);
                }
                if( products.size() > 0 )
                {
                    parseReactionLinks(elementSet, (Node)de, CELLDESIGNER_PRODUCT_LINKS_LIST, CELLDESIGNER_PRODUCT_LINK, PRODUCT_ATTR,
                            diagram, products.get(0), SpecieReference.PRODUCT);
                }
            }
        }
    }
    
    /**
     * Process 'celldesigner:modifiers'
     */
    protected void readModification(Node reaction, Element element, @Nonnull Diagram diagram)
    {
        SpeciesInfo si = null;
        Element specieInfoElement = getElement(element, CELLDESIGNER_LINK_TARGET);
        if( specieInfoElement != null )
        {
            si = getSpeciesInfo(specieInfoElement);
        }
        else
        {
            String id = element.getAttribute(MODIFIERS_ATTR);
            String alias = element.getAttribute(ALIASES_ATTR);
            if( id.length() > 0 && alias.length() > 0 )
                si = new SpeciesInfo(id, alias);
        }
        if( si != null )
        {
            try
            {
                String type = element.getAttribute(TYPE_ATTR);
                Node node = CellDesignerUtils.findNode(diagram, si.getName(), si.getAlias());
                if( node == null )
                    throw new Exception("Can not find node '" + si.getName() + "' in diagram " + diagram.getName());
                Edge edge = CellDesignerUtils.findEdge(reaction, node, SpecieReference.MODIFIER);

                node = getNodeAlias(node, si.getAlias());
                if( ( node != null ) && ( edge != null ) )
                {
                    if( type.equals("INHIBITION") )
                        CellDesignerUtils.addProperty(edge, new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, "inhibition"), false);
                    String editPoints = element.getAttribute(EDIT_POINTS_ATTR);
                    if( editPoints != null && editPoints.length() > 0 )
                    {
                        String[] pointValues = TextUtil.split( editPoints, ' ' );
                        DoublePoint p1 = CellDesignerUtils.getNodePort(node, si.getPortX(), si.getPortY());
                        DoublePoint cp = CellDesignerUtils.getNodeCenter(reaction);
                        DoublePoint p3 = CellDesignerUtils.getOrthogonalPoint(p1, cp);
                        fillPath(diagram, node, edge, pointValues, p1, cp, p3, true);
                    }
                    CellDesignerUtils.addProperty(edge, new DynamicProperty(SBGNConverter.SPECIE_NAME_ATTR, String.class, CellDesignerUtils.getCompleteName(node)), false);
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Cannot parse modification info", e);
            }
        }
    }
    
    /**
     * Parse reaction not base components (:listOfReactantLinks, :listOfProductLinks)
     */
    protected void parseReactionLinks(ElementSet elementSet, Node de, String listTag, String elementTag, String elementIdAttr,
            @Nonnull Diagram diagram, SpeciesInfo baseSpecie, String role)
    {
        Element elements = elementSet.getElement(listTag);
        if( elements != null )
        {
            NodeList list = elements.getChildNodes();
            for( int i = 0; i < list.getLength(); i++ )
            {
                org.w3c.dom.Node node = list.item(i);
                if( ( node instanceof Element ) && ( node.getNodeName().equals(elementTag) ) )
                {
                    String id = ( (Element)node ).getAttribute(elementIdAttr);
                    String alias = ( (Element)node ).getAttribute(ALIAS_ATTR);

                    for( Edge edge : de.getEdges() )
                    {
                        if( ( edge.getKernel() instanceof SpecieReference )
                                && ( ( (SpecieReference)edge.getKernel() ).getRole().equals(role) )
                                && ( edge.nodes().map( Node::getName ).has( id ) ) )
                        {
                            try
                            {
                                Node specieNode = CellDesignerUtils.findNode(diagram, id, alias);
                                specieNode = getNodeAlias(specieNode, alias);

                                CellDesignerUtils.addProperty(edge, new DynamicProperty(SBGNConverter.SPECIE_NAME_ATTR, String.class,
                                        CellDesignerUtils.getCompleteName(specieNode)), false);
                                if( Util.isReaction(edge.getOutput()) )
                                    edge.setInput(specieNode);
                                else
                                    edge.setOutput(specieNode);
                                Element lineElement = getElement((Element)node, CELLDESIGNER_LINE);
                                if( lineElement != null )
                                {
                                    CellDesignerUtils.curveLine(diagram, edge, baseSpecie, specieNode, de);
                                    Pen linePen = readLinePen(lineElement);
                                    edge.getCustomStyle().setPen(linePen.clone());
                                    CellDesignerUtils.addProperty(edge, new DynamicProperty(SBGNPropertyConstants.LINE_PEN_ATTR, Pen.class, linePen), false);
                                    if( lineElement.getAttribute(TYPE_ATTR).equals("Curve") )
                                        CellDesignerUtils.curveLine(diagram, edge, baseSpecie, specieNode, de);
                                }
                                else
                                {
                                    CellDesignerUtils.curveLine(diagram, edge, baseSpecie, specieNode, de); //for old version of celldesigner
                                }
                            }
                            catch( Exception e )
                            {
                                log.log(Level.SEVERE, "Can not load edge line properties", e);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Process edge path
     */
    protected void fillPath(Diagram diagram, Node center, Edge edge, String[] pairs, DoublePoint p1, DoublePoint p2, DoublePoint p3,
            boolean direct)
    {
        Path path = new Path();
        Point inport = getInPort(edge);
        Rectangle inputBounds = CellDesignerUtils.getBounds(edge.getInput());
        Point outport = getOutPort(edge);
        Rectangle outputBounds = CellDesignerUtils.getBounds(edge.getOutput());
        boolean addAll = false;
        if( direct )
        {
            path.addPoint(inport.x, inport.y);
            for( String pair : pairs )
            {
                DoublePoint p = CellDesignerUtils.getPointByBase(pair, p1, p2, p3);
                if( outputBounds.contains((int)p.x, (int)p.y) )
                    break;

                if( addAll || !inputBounds.contains((int)p.x, (int)p.y) )
                {
                    path.addPoint((int)p.x, (int)p.y);
                    addAll = true;
                }
            }
            path.addPoint(outport.x, outport.y);
        }
        else
        {
            path.addPoint(outport.x, outport.y);
            for( int i = pairs.length - 1; i >= 0; i-- )
            {
                DoublePoint p =  CellDesignerUtils.getPointByBase(pairs[i], p1, p2, p3);
                if( inputBounds.contains((int)p.x, (int)p.y) )
                    break;

                if( addAll || !outputBounds.contains((int)p.x, (int)p.y) )
                {
                    path.addPoint((int)p.x, (int)p.y);
                    addAll = true;
                }
            }
            path.addPoint(inport.x, inport.y);
        }
        edge.setPath(path);
        diagram.getType().getSemanticController().recalculateEdgePath(edge);
    }
    
    /**
     * Get species as {@link List} by list and element tag names
     */
    protected List<SpeciesInfo> getSpeciesList(ElementSet elementSet, String listTag, String elementTag, String reactionName, String role)
    {
        List<SpeciesInfo> result = new ArrayList<>();
        Element elements = elementSet.getElement(listTag);
        if( elements != null )
        {
            String textContent = getTextContent(elements);
            if( textContent != null && textContent.trim().length() > 0 )
            {
                //old version: names separated by ','
                String[] names = TextUtil.split( textContent, ',' );
                for( String name : names )
                {
                    String specieName = name.trim();
                    String alias = null;
                    Edge edge = reactionSpecieCache.getEdge(reactionName, specieName, role);
                    if( edge != null )
                    {
                        ElementSet specieElement = unprocessedElements.get(edge);
                        if( specieElement != null )
                        {
                            Element aliasElement = specieElement.getElement(CELLDESIGNER_ALIAS);
                            if( aliasElement != null )
                                alias = getTextContent(aliasElement).trim();
                        }
                    }
                    SpeciesInfo si = new SpeciesInfo(specieName, alias);
                    result.add(si);
                }
            }
            else
            {
                //new version: special inner tags
                NodeList list = elements.getChildNodes();
                for( int i = 0; i < list.getLength(); i++ )
                {
                    org.w3c.dom.Node node = list.item(i);
                    if( ( node instanceof Element ) && ( node.getNodeName().equals(elementTag) ) )
                        result.add(getSpeciesInfo((Element)node));
                }
            }
        }
        return result;
    }
    
    /**
     * Get {@link SpeciesInfo} object by XML element
     */
    protected SpeciesInfo getSpeciesInfo(Element element)
    {
        SpeciesInfo si = new SpeciesInfo(element.getAttribute(SPECIES_ATTR), element.getAttribute(ALIAS_ATTR));
        Element anchor = getElement(element, CELLDESIGNER_LINK_ANCHOR);
        if( anchor != null )
            si.setPort( anchor.getAttribute(POSITION_ATTR));
        return si;
    }
    
    /**
     * Get specific alias for node
     */
    protected Node getNodeAlias(Node node, String alias)
    {
        //unknown alias
        if( alias == null )
            return node;

        //check current node
        Object nodeAlias = node.getAttributes().getValue(SBGNConverter.ALIAS_ATTR);
        if( nodeAlias != null )
        {
            if( nodeAlias.toString().equals(alias) )
                return node;
        }
        //check in node aliases
        Object nodeAliases = node.getAttributes().getValue(SBGNConverter.ALIASES_ATTR);
        if( nodeAliases instanceof Node[] )
        {
            for( Node n : (Node[])nodeAliases )
            {
                nodeAlias = n.getAttributes().getValue(SBGNConverter.ALIAS_ATTR);
                if( ( nodeAlias != null ) && nodeAlias.toString().equals(alias) )
                    return n;
            }
        }
        //check in other aliases compartment
        Compartment parent = (Compartment)node.getOrigin();
        Object compartmentAliases = parent.getAttributes().getValue(SBGNConverter.ALIASES_ATTR);
        if( compartmentAliases instanceof Node[] )
        {
            for( Node n : (Node[])compartmentAliases )
            {
                try
                {
                    Node n2 = (Node) ( (Compartment)n ).get(node.getName());
                    nodeAlias = n2.getAttributes().getValue(SBGNConverter.ALIAS_ATTR);
                    if( ( nodeAlias != null ) && nodeAlias.toString().equals(alias) )
                        return n2;

                    Object node2Aliases = n2.getAttributes().getValue(SBGNConverter.ALIASES_ATTR);
                    if( node2Aliases instanceof Node[] )
                    {
                        for( Node n3 : (Node[])node2Aliases )
                        {
                            nodeAlias = n3.getAttributes().getValue(SBGNConverter.ALIAS_ATTR);
                            if( ( nodeAlias != null ) && nodeAlias.toString().equals(alias) )
                                return n3;
                        }
                    }
                }
                catch( Exception e )
                {
                }
            }
        }
        return null;
    }
    
    protected Point getInPort(Edge edge)
    {
        return edge.getInPort() != null? edge.getInPort(): new Point();
    }

    protected Point getOutPort(Edge edge)
    {
        return edge.getOutPort() != null? edge.getOutPort(): new Point();
    }
    
    /**
     * Parse line pen info
     */
    protected Pen readLinePen(Element element)
    {
        Pen linePen = null;
        float width = (float)Double.parseDouble(element.getAttribute(WIDTH_ATTR));
        String colorStr = element.getAttribute(COLOR_ATTR);
        if( colorStr.length() >= 6 )
        {
            int l = colorStr.length();
            colorStr = colorStr.substring(l - 6, l);
            Color color = new Color(Integer.parseInt(colorStr, 16));
            linePen = new Pen(width, color);
        }
        return linePen;
    }

    /**
     * Read inner heterodimer element
     */
    protected void readInnerHeterodimer(DiagramElement de, Element inner)
    {
        String innerName = inner.getAttribute(INNER_ID_ATTR);
        Node childNode = CellDesignerUtils.findInComplex((Node)de, innerName, null);
        Element nameElement = getElement(inner, CELLDESIGNER_NAME);
        if( nameElement != null )
            childNode.setTitle(getTextContent(nameElement));

        CellDesignerUtils.readClass(inner, childNode);
        addProteinRef(inner, childNode);
    }
    
    
    /**
     * Read protein reference: 'celldesigner:proteinReference'
     */
    protected void addProteinRef(Element speciesIdentity, Object node)
    {
        //read protein reference
        Element prElement = getElement(speciesIdentity, CELLDESIGNER_PROTEIN_REFERENCE);
        if( prElement != null )
        {
            String pr = getTextContent(prElement);
            List<Object> list = proteinRefs.get(pr);
            if( list == null )
            {
                list = new ArrayList<>();
                proteinRefs.put(pr, list);
            }
            list.add(node);
        }
    }
    
    /**
     * CellDesigner extensions may contains celldesigner:extension element or not.
     * This method allows to combine this two formats.
     */
    public Element getTopElement(Element element, String name)
    {
        if( element.getNodeName().equals(name) )
            return element;
        return getElement(element, name);
    }

    @Override
    public void readElement(Element element, DiagramElement specie, Diagram diagram)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public Element[] writeElement(DiagramElement specie, Document document)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
