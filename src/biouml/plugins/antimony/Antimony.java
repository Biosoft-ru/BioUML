package biouml.plugins.antimony;

import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementStyle;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.EModelRoleSupport;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.SimpleTableElement.VarColumn;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.antimony.astparser_v2.AntimonyNotationParser;
import biouml.plugins.antimony.astparser_v2.AstConversionFactor;
import biouml.plugins.antimony.astparser_v2.AstStart;
import biouml.plugins.antimony.astparser_v2.AstSymbolType;
import biouml.plugins.antimony.astparser_v2.TokenMgrError;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnComplexStructureManager;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbgn.SbgnUtil;
import biouml.plugins.sbgn.title.TitleElement;
import biouml.standard.diagram.MathDiagramUtility;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.diagram.Util;
import biouml.standard.type.BaseUnit;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Type;
import biouml.standard.type.Unit;
import biouml.workbench.graph.DiagramToGraphTransformer;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.exception.Assert;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.GreedyLayouter;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.editor.SelectionManager;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.table.TableDataCollection;

public class Antimony
{
    protected static final Logger log = Logger.getLogger(Antimony.class.getName());

    protected Diagram diagram;
    protected AstStart astStart;
    protected AntimonyAstUpdater diagramToAST;
    private AntimonyNotationParser parser;
    private Handler logHandler = null;

    public Antimony(Diagram diagram)
    {
        this.diagram = diagram;
        diagramToAST = new AntimonyAstUpdater(diagram);
        parser = new AntimonyNotationParser();

        importAnnotation();
        if( diagram != null )
            AntimonyAnnotationImporter.setDiagramPath(diagram.getOrigin().getCompletePath());
    }

    public final static String PATH_ANNOTATIONS = "databases/Utils/Antimony/";

    private void importAnnotation()
    {
        if( diagram != null )
        {
            try
            {

                if( diagram.getType() instanceof SbgnDiagramType )
                {
                    AntimonyAnnotationImporter.setAnnotation(PATH_ANNOTATIONS + "biouml.yaml", "biouml");
                    AntimonyAnnotationImporter.setAnnotation(PATH_ANNOTATIONS + "sbgn.yaml", "sbgn");
                }
            }
            catch( Exception e )
            {
                log.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }

    public void createAst()
    {
        if( diagram != null )
            generateAstFromDiagram(diagram);
        else
            astStart = new AstStart(AntimonyNotationParser.JJTSTART);
    }

    public Diagram generateDiagram(AstStart astStart, boolean applyLayout) throws Exception
    {
        Assert.notNull("diagram", diagram);
        AntimonyDiagramGenerator diagramGenerator = new AntimonyDiagramGenerator();
        if( logHandler != null )
            diagramGenerator.getLogger().addHandler(logHandler);

        Diagram newDiagram = diagramGenerator.generateDiagram(astStart, diagram);

        DiagramViewOptions viewOptions = diagram.getViewOptions();
        DiagramViewOptions newViewOptions = newDiagram.getViewOptions();
        if( newViewOptions.getClass().isAssignableFrom(viewOptions.getClass()) )
            newDiagram.setViewOptions(viewOptions);
        else
        {
            newViewOptions.setAutoLayout(viewOptions.isAutoLayout());
            newViewOptions.setDependencyEdges(viewOptions.isDependencyEdges());
            newViewOptions.setStyles(viewOptions.getStyles());
        }

        PathwaySimulationDiagramType.DiagramPropertyChangeListener listener = new PathwaySimulationDiagramType.DiagramPropertyChangeListener(
                newDiagram);
        newViewOptions.addPropertyChangeListener(listener);

        Layouter layouter = diagram.getPathLayouter();
        if( layouter == null )
            layouter = new GreedyLayouter();

        if( applyLayout )
        {
            if( diagram.getNodes().length == 0 )
            {
                DiagramToGraphTransformer.layout(newDiagram, layouter);
                moveGraph(newDiagram);

                //layout for ModelDefinition
                for( ModelDefinition conteiner : AntimonyUtility.getModelDefinitions(newDiagram) )
                {
                    Diagram subDiagram = conteiner.getDiagram();
                    DiagramToGraphTransformer.layout(subDiagram, layouter);
                    moveGraph(subDiagram);
                }
                //layout for SubDiagram
                for( SubDiagram conteiner : AntimonyUtility.getSubdiagrams(newDiagram) )
                {
                    Diagram subDiagram = conteiner.getDiagram();
                    DiagramToGraphTransformer.layout(subDiagram, layouter);
                    moveGraph(subDiagram);
                }
            }
            else
            {
                reApplyLayout(diagram, newDiagram, layouter);
                //reapply layout for modelDefinitions
                for( ModelDefinition newModel : AntimonyUtility.getModelDefinitions(newDiagram) )
                {
                    Node oldModel = diagram.findNode(newModel.getName());
                    if( ! ( oldModel instanceof ModelDefinition ) )
                        continue;
                    Diagram oldSubDiagram = ( (ModelDefinition)oldModel ).getDiagram();
                    Diagram newSubDiagram = newModel.getDiagram();
                    reApplyLayout(oldSubDiagram, newSubDiagram, layouter);
                }
                //reapply layout for SubDiagram
                for( SubDiagram newModel : AntimonyUtility.getSubdiagrams(newDiagram) )
                {
                    Node oldModel = diagram.findNode(newModel.getName());
                    if( ! ( oldModel instanceof SubDiagram ) )
                        continue;
                    Diagram oldSubDiagram = ( (SubDiagram)oldModel ).getDiagram();
                    Diagram newSubDiagram = newModel.getDiagram();
                    reApplyLayout(oldSubDiagram, newSubDiagram, layouter);
                }
            }

        }

        this.diagram = newDiagram;
        diagramToAST.setDiagram(newDiagram);
        return newDiagram;
    }

    private static void moveGraph(Diagram newDiagram)
    {
        // move graph to left and up
        double minX = -1, minY = -1;
        for( Node node : newDiagram.getNodes() )
        {
            if( node.getLocation().getX() < minX || minX == -1 )
                minX = node.getLocation().getX();
            if( node.getLocation().getY() < minY || minY == -1 )
                minY = node.getLocation().getY();
        }

        //move nodes
        for( Node node : newDiagram.getNodes() )
            moveNode(minX, minY, node);

        //move edges
        for( DiagramElement de : newDiagram )
        {
            if( de instanceof Edge )
            {
                Edge edge = ( (Edge)de );
                int[] x = edge.getPath().xpoints;
                int[] y = edge.getPath().ypoints;
                int n = edge.getPath().npoints;
                for( int i = 0; i < n; i++ )
                {
                    x[i] -= (int)Math.floor(minX);
                    y[i] -= (int)Math.floor(minY);
                }

                edge.getInPort().translate( -(int)Math.floor(minX), -(int)Math.floor(minY));
                edge.getOutPort().translate( -(int)Math.floor(minX), -(int)Math.floor(minY));
                edge.setPath(new Path(x, y, n));
            }
        }
    }

    private static void moveNode(double minX, double minY, Node node)
    {
        for( Node n : node.recursiveStream().select(Node.class) )
        {
            int x = (int)Math.floor(n.getLocation().getX() - minX);
            int y = (int)Math.floor(n.getLocation().getY() - minY);
            n.setLocation(x, y);
        }
    }

    public Diagram generateDiagram(Reader reader) throws Exception
    {
        return generateDiagram(reader, true);
    }

    public Diagram generateDiagram(Reader reader, boolean applyLayout) throws Exception
    {
        Diagram newDiagram = generateDiagram(generateAstFromText(reader), applyLayout);
        diagram = newDiagram;
        diagramToAST.setDiagram(newDiagram);
        return newDiagram;
    }

    public Diagram generateDiagram(String text) throws Exception
    {
        return generateDiagram(text, true);
    }

    public Diagram generateDiagram(String text, boolean applyLayout) throws Exception
    {
        return generateDiagram(new StringReader(text), applyLayout);
    }

    public String generateText()
    {
        return generateText(astStart);
    }

    public AstStart generateAstFromDiagram(Diagram diagram)
    {
        diagramToAST.setDiagram(diagram);
        astStart = diagramToAST.getAST();
        return astStart;
    }

    public AstStart generateAstFromText(Reader antimonyReader) throws Exception
    {
        try
        {
            astStart = parser.parse(antimonyReader);
        }
        catch( Exception | TokenMgrError e )
        {
            log.log(Level.SEVERE, "Antimony parsing error: " + e.getMessage());
            throw new Exception("Incorrect antimony. See log for details.");
        }

        diagramToAST.updateBlockLocations(astStart);

        return astStart;
    }

    public AstStart generateAstFromText(String antimonyText) throws Exception
    {
        return generateAstFromText(new StringReader(antimonyText));
    }

    public String generateText(AstStart astStart)
    {
        AntimonyTextGenerator textGenerator = new AntimonyTextGenerator(astStart);
        try
        {
            return textGenerator.generateText();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can't generate text", e);
        }
        return null;
    }

    /**
     * Copy layout from one diagram to another
     */
    private static void copyLayout(Compartment from, Compartment to, Set<DiagramElement> common, Set<DiagramElement> extra)
    {
        for( DiagramElement deTo : to.getNodes() )
        {
            DiagramElement deFrom = from.get(deTo.getName());

            if( deFrom == null && deTo.getRole() instanceof Equation )
            {
                Equation toEquation = (Equation)deTo.getRole();
                List<Node> fromNodes = from.stream(Node.class).filter(n -> {
                    Role r = n.getRole();
                    return ( r instanceof Equation && ( (Equation)r ).getType().equals(toEquation.getType())
                            && ( (Equation)r ).getVariable().equals(toEquation.getVariable()) );
                }).collect(Collectors.toList());

                deFrom = fromNodes.isEmpty() ? null : fromNodes.get(0);
            }

            if( deFrom != null )
            {
                deTo.setPredefinedStyle(deFrom.getPredefinedStyle());
                if( DiagramElementStyle.STYLE_NOT_SELECTED.equals(deFrom.getPredefinedStyle()) )
                    deTo.setCustomStyle(deFrom.getCustomStyle());

                if( deFrom instanceof Node )
                {
                    Node nFrom = (Node)deFrom;
                    Node nTo = (Node)deTo;

                    nTo.setShowTitle(nFrom.isShowTitle());
                    common.add(nTo);

                    if( Util.hasOrientation(nFrom) )
                        Util.setPortOrientation(nTo, Util.getPortOrientation(nFrom));
                    ( (Node)nFrom ).setVisible( ( (Node)nTo ).isVisible());

                    Diagram d = Diagram.getDiagram(deTo);

                    if( nTo.getAttributes().hasProperty("AutogeneratedView") && !isEqual((Compartment)nTo, (Compartment)nFrom) )//something changed in Antimony - view is regenerated, just move nodes
                    {
                        moveWithElements(nTo, nFrom.getLocation());
                    }
                    else if( !d.getType().needLayout(nTo) ) //this is entity, inner elements should not be addressed by their names
                    {
                        copyLayoutByTitle(nFrom, nTo);
                    }
                    else
                    {
                        nTo.setShapeSize( ( (Node)nFrom ).getShapeSize());
                        nTo.setLocation( ( (Node)nFrom ).getLocation());
                        if( nTo instanceof Compartment && nFrom instanceof Compartment )
                            copyLayout((Compartment)nFrom, (Compartment)nTo, common, extra);
                    }
                }
                else if( deFrom instanceof Edge )
                {
                    ( (Edge)deTo ).setPath( ( (Edge)deFrom ).getPath());
                }
                common.add(deTo);
            }
            else
                extra.add(deTo);
        }
    }

    private static void copyLayoutByTitle(Node from, Node to)
    {
        to.setShapeSize(from.getShapeSize());
        to.setLocation(from.getLocation());

        if( from instanceof Compartment && to instanceof Compartment )
        {
            Compartment cFrom = (Compartment)from;
            Compartment cTo = (Compartment)to;
            Map<Compartment, TitleElement> node2TitleTo = cTo.stream(Compartment.class).filter(c -> SbgnUtil.isComplex(c))
                    .toMap(n -> SbgnComplexStructureManager.getTitleByComplex(n));

            Map<String, Node> title2Node = cTo.stream(Node.class).filter(n -> !SbgnUtil.isComplex(n) && !Util.isPort(n))
                    .toMap(n -> n.getTitle(), n -> n);

            Set<Node> used = new HashSet<>();
            for( Node nFrom : cFrom.getNodes() )
            {
                if( SbgnUtil.isComplex(nFrom) )
                {
                    TitleElement tFrom = SbgnComplexStructureManager.getTitleByComplex((Compartment)nFrom);
                    for( Entry<Compartment, TitleElement> entryTo : node2TitleTo.entrySet() )
                    {
                        if( SbgnComplexStructureManager.equals(tFrom, entryTo.getValue()) )
                        {
                            copyLayoutByTitle(nFrom, entryTo.getKey());
                            used.add(entryTo.getKey());
                        }
                    }
                }
                else if( Util.isPort(nFrom) )
                {
                    Node nTo = findOldPort(cTo, nFrom);
                    if( nTo != null )
                    {
                        copyLayoutByTitle(nFrom, nTo);
                        if( Util.hasOrientation(nFrom) )
                            Util.setPortOrientation(nTo, Util.getPortOrientation(nFrom));
                    }
                }
                else
                {
                    String title = nFrom.getTitle();
                    Node nTo = title2Node.get(title);
                    if( nTo != null )
                        copyLayoutByTitle(nFrom, nTo);
                }
            }
        }
    }

    private static Node findOldPort(Compartment newDiagram, Node oldPort)
    {
        if( newDiagram instanceof SubDiagram || newDiagram instanceof Diagram )
        {
            Collection<Node> ports = AntimonyUtility.getPortNodes(newDiagram, false);
            if( Util.isPropagatedPort(oldPort) )
            {

                DynamicProperty moduleName = oldPort.getAttributes().getProperty(ConnectionPort.BASE_MODULE_NAME_ATTR);
                DynamicProperty basePortName = oldPort.getAttributes().getProperty(ConnectionPort.BASE_PORT_NAME_ATTR);

                if( moduleName == null || basePortName == null )
                    return null;

                for( Node port : ports )
                {
                    DynamicProperty dp = port.getAttributes().getProperty(ConnectionPort.BASE_MODULE_NAME_ATTR);
                    DynamicProperty dp2 = port.getAttributes().getProperty(ConnectionPort.BASE_PORT_NAME_ATTR);
                    if( dp != null && dp2 != null && moduleName.getValue().equals(dp.getValue())
                            && basePortName.getValue().equals(dp2.getValue())
                            && !Util.getAccessType(port).equals(Util.getAccessType(oldPort)) )
                        return port;
                }
                return null;
            }

            String variableName = Util.getPortVariable(oldPort);

            for( Node port : ports )
            {
                DynamicProperty dp = port.getAttributes().getProperty(ConnectionPort.VARIABLE_NAME_ATTR);
                if( dp != null && variableName.equals(dp.getValue()) && Util.getAccessType(port).equals(Util.getAccessType(oldPort)) )
                    return port;
            }
        }
        return null;
    }

    public static void moveWithElements(Node n, Point translation)
    {
        Point p = n.getLocation();
        p.translate(translation.x, translation.y);
        n.setLocation(p);
        if( n instanceof Compartment )
        {
            for( Node inner : ( (Compartment)n ).getNodes() )
            {
                moveWithElements(inner, translation);
            }
        }
    }

    /**
     * Checks if two complexes or species have equal inner structure
     */
    private static boolean isEqual(Compartment c1, Compartment c2)
    {
        Set<String> mods1 = new HashSet<>();
        Set<String> mods2 = new HashSet<>();
        Set<String> infos1 = new HashSet<>();
        Set<String> infos2 = new HashSet<>();
        Map<String, Compartment> sub1 = new HashMap<>();
        Map<String, Compartment> sub2 = new HashMap<>();

        for( Node node : c1.getNodes() )
        {
            if( node.getKernel().getType().equals(biouml.plugins.sbgn.Type.TYPE_VARIABLE) )
                mods1.add(node.getTitle());
            else if( node.getKernel().getType().equals(biouml.plugins.sbgn.Type.TYPE_UNIT_OF_INFORMATION) )
                infos1.add(node.getTitle());
            else if( node.getKernel() instanceof Specie )
                sub1.put(node.getTitle(), (Compartment)node);
        }
        for( Node node : c2.getNodes() )
        {
            if( node.getKernel().getType().equals(biouml.plugins.sbgn.Type.TYPE_VARIABLE) )
                mods2.add(node.getTitle());
            else if( node.getKernel().getType().equals(biouml.plugins.sbgn.Type.TYPE_UNIT_OF_INFORMATION) )
                infos2.add(node.getTitle());
            else if( node.getKernel() instanceof Specie )
                sub2.put(node.getTitle(), (Compartment)node);
        }
        if( !mods1.containsAll(mods2) || !mods2.containsAll(mods1) || !infos1.containsAll(infos2) || !infos2.containsAll(infos1) )
            return false;

        if( !c1.getTitle().equals(c2.getTitle()) )
            return false;
        if( !sub1.keySet().containsAll(sub2.keySet()) || !sub2.keySet().containsAll(sub1.keySet()) )
            return false;
        for( Entry<String, Compartment> e : sub1.entrySet() )
        {
            Compartment n1 = e.getValue();
            Compartment n2 = sub2.get(e.getKey());
            if( !isEqual(n1, n2) )
                return false;
        }


        return true;
    }

    public static void reApplyLayout(Diagram diagramFrom, Diagram diagramTo, Layouter layouter) throws Exception
    {
        Set<DiagramElement> commonElements = new HashSet<>();
        Set<DiagramElement> extraElements = new HashSet<>();
        copyLayout(diagramFrom, diagramTo, commonElements, extraElements);

        Graph oldGraph = DiagramToGraphTransformer.generateGraph(diagramFrom, diagramFrom.getType().getSemanticController().getFilter());
        Graph graph = DiagramToGraphTransformer.generateGraph(diagramTo, diagramTo.getType().getSemanticController().getFilter());

        //try to layout new nodes if necessary
        if( !extraElements.isEmpty() )
        {
            //TODO: select nodes in a more correct way
            Map<ru.biosoft.graph.Node, Boolean> stateOfFixed = StreamEx.of(graph.getNodes())
                    .filter(n -> oldGraph.getNode(n.getName()) != null || oldGraph.getNode(n.getName().replaceAll("_", "-")) != null)
                    .toMap(n -> n.fixed);

            stateOfFixed.keySet().forEach(n -> n.fixed = true);

            layouter.doLayout(graph, null);
            //            if( diagramTo.getSize() < 500 )
            //            {
            //                FastGridLayouter layouter = new FastGridLayouter();
            //                layouter.setGridY(40);
            //                //layouter.setStrongRepulsion( -1);
            //                //layouter.setIterations(1);
            //                //layouter.setThreadCount(1);
            //                //layouter.setCool(0.7);
            //                layouter.doLayout(graph, null);
            //            }
            //            else
            //            {
            //                PathwayLayouter layouter = new PathwayLayouter(new ForceDirectedLayouter());
            //                layouter.doLayout(graph, null);
            //            }

            stateOfFixed.forEach((n, f) -> n.fixed = f);

            DiagramToGraphTransformer.applyLayout(graph, diagramTo, false);
        }

        if( diagramTo.getViewOptions().isDependencyEdges() )
            MathDiagramUtility.generateDependencies(diagramTo);
    }

    public String updateText(DataCollectionEvent e) throws Exception
    {
        switch( e.getType() )
        {
            case DataCollectionEvent.ELEMENT_ADDED:
                DataElement dataElement = e.getDataElement();
                if( dataElement instanceof DiagramElement )
                    diagramToAST.addElement(astStart, (DiagramElement)dataElement);
                else if( dataElement instanceof Variable )
                    diagramToAST.addElement(astStart, (Variable)dataElement);
                break;

            case DataCollectionEvent.ELEMENT_REMOVED:
                DataElement oldElement = e.getOldElement();
                if( oldElement instanceof DiagramElement )
                    diagramToAST.removeElement((DiagramElement)oldElement, astStart);
                else if( oldElement instanceof Variable )
                    diagramToAST.removeElement((Variable)oldElement);
                break;
            default:
                break;
        }
        return generateText(astStart);
    }

    public String updateText(PropertyChangeEvent e) throws Exception
    {
        String propertyName = e.getPropertyName();
        if( e.getSource() instanceof DatabaseReference )
        {
            DatabaseReference dr = (DatabaseReference)e.getSource();
            if( dr.getDatabaseName() != null && !dr.getDatabaseName().isEmpty() && dr.getId() != null && !dr.getId().isEmpty()
                    && dr.getRelationshipType() != null && !dr.getRelationshipType().isEmpty() )
            {
                if( propertyName.equals("databaseName") || propertyName.equals("id") )
                    diagramToAST.changeURI(e, astStart);
                else if( propertyName.equals("relationshipType") )
                    diagramToAST.changeRelationshipType(e, astStart);
            }
        }
        else if( e.getSource() instanceof VarColumn )
        {
            if( propertyName.equals("name") || propertyName.equals("variable") )
                diagramToAST.changeTableProperty(propertyName, e, astStart.getMainModel());
        }
        else if( e.getSource() instanceof SimpleTableElement )
        {
            if( propertyName.equals("columns") )
            {
                VarColumn[] oldCols = (VarColumn[])e.getOldValue();
                VarColumn[] newCols = (VarColumn[])e.getNewValue();
                if( oldCols.length > newCols.length )
                    diagramToAST.removeColumn(e, astStart.getMainModel());
            }
            else if( propertyName.equals("table") )
            {
                TableDataCollection newTDC = (TableDataCollection)e.getNewValue();
                diagramToAST.changePath(newTDC.getCompletePath(), (Node)e.getPropagationId(), astStart.getMainModel());
            }

        }
        else if( propertyName.equals("databaseReferences") && e.getOldValue() != null )
        {
            if( ( (DatabaseReference[])e.getNewValue() ).length < ( (DatabaseReference[])e.getOldValue() ).length )
                diagramToAST.removeDatabaseReference(e, astStart);
        }
        else if( propertyName.equals("initialValue") )
        {
            Variable var = (Variable)e.getSource();
            diagramToAST.changeInitialValue(var, astStart);
        }
        else if( propertyName.equals("formula") )
        {
            if( e.getSource() instanceof KineticLaw )
            {
                String name = ( (Reaction) ( (KineticLaw)e.getSource() ).getParent() ).getName();
                Node reactionNode = diagram.findNode(name);
                if( reactionNode != null && reactionNode.getKernel() instanceof Reaction )
                    diagramToAST.changeKineticLaw(reactionNode);
            }
            else if( e.getSource() instanceof Equation || e.getSource() instanceof Function || e.getSource() instanceof Reaction
                    || e.getSource() instanceof Constraint )
            {
                DiagramElement de = ( (EModelRoleSupport)e.getSource() ).getDiagramElement();
                diagramToAST.changeFormula(de);
            }
        }
        else if( propertyName.equals("constant") )
        {
            if( e.getSource() instanceof Variable )
                diagramToAST.changeConstantProperty((Variable)e.getSource(), astStart);
        }
        else if( propertyName.equals("variable") )
        {
            if( e.getSource() instanceof Equation )
                diagramToAST.changeEquationVariable((Equation)e.getSource());
            else if( e.getSource() instanceof Assignment && ( (Assignment)e.getSource() ).getParent() instanceof Event )
                diagramToAST.changeEventAssignment((Assignment)e.getSource(), e);
        }
        else if( propertyName.equals("type") )
        {
            Node node;
            if( e.getSource() instanceof Specie && ( node = diagram.findNode( ( (Specie)e.getSource() ).getName()) ) != null )
            {
                if( ! ( diagram.getType() instanceof SbgnDiagramType ) )
                {
                    String type = e.getNewValue().toString().equals(Type.TYPE_GENE) ? AstSymbolType.GENE : AstSymbolType.SPECIES;
                    diagramToAST.changeTypeSpecie(node, type, astStart);
                }
                else
                {
                    diagramToAST.changeProperty(node, AntimonyConstants.SBGN_TYPE, e.getNewValue().toString(), astStart);
                    if( e.getNewValue().toString().equals(biouml.plugins.sbgn.Type.TYPE_COMPLEX)
                            || e.getOldValue().toString().equals(biouml.plugins.sbgn.Type.TYPE_COMPLEX) )
                        diagramToAST.changeSbgnViewTitle(node, astStart);
                }
            }
            if( e.getSource() instanceof Equation )
                diagramToAST.changeEquationType((Equation)e.getSource());
        }
        else if( propertyName.equals("math") )
        {
            if( e.getSource() instanceof Assignment && ( (Assignment)e.getSource() ).getParent() instanceof Event )
                diagramToAST.changeEventAssignment((Assignment)e.getSource(), e);
        }
        else if( propertyName.equals("eventAssignment") )
        {
            diagramToAST.updateEventAssignment(e);
        }
        else if( propertyName.equals("message") )
        {
            diagramToAST.updateConstraintMessage(e);
        }
        else if( propertyName.equals("stoichiometry") )
        {
            if( e.getSource() instanceof SpecieReference )
                diagramToAST.changeStoichiometry((SpecieReference)e.getSource());
        }
        else if( propertyName.equals("reversible") )
        {
            if( e.getSource() instanceof Equation && ( (Equation)e.getSource() ).getDiagramElement().getKernel() instanceof Reaction )
            {
                diagramToAST.changeReversibleType( ( (Equation)e.getSource() ).getDiagramElement());
            }
        }
        else if( propertyName.equals("modifierAction") )
        {
            if( e.getSource() instanceof SpecieReference )
            {
                diagramToAST.changeModifierType((SpecieReference)e.getSource(), e.getNewValue().toString(), astStart);
            }
        }
        else if( propertyName.equals("title") )
        {
            //we can't change diagram title in antimony text

            //            if( e.getSource() instanceof Diagram )
            //                astStart.getMainModel().setModelName( ( (Diagram)e.getSource() ).getTitle());
            //            else
            if( e.getSource() instanceof Node )
            {
                Node node = (Node)e.getSource();

                boolean isAuxiliary = SbgnUtil.isVariableNode(node) || SbgnUtil.isUnitOfInformation(node);
                boolean complexWithTitle = ( SbgnUtil.isComplex(node) && node.isShowTitle() );
                boolean complexSub = ( SbgnUtil.isNotComplexEntity(node)
                        && ( SbgnUtil.isComplex((Node)node.getParent()) || !AntimonyUtility.isEmptyCompartment(node) ) );
                boolean isClone = SbgnUtil.isClone(node);
                boolean isPort = Util.isPort(node);
                boolean isNote = Util.isNote(node);

                if( isPort )
                    diagramToAST.changeProperty(node, SBGNPropertyConstants.SBGN_TITLE, e.getNewValue().toString(), astStart);
                else if( isNote )
                    diagramToAST.changeNoteText(node, e.getNewValue().toString());
                else if( Util.isBus(node) )
                    diagramToAST.changeTitleInProperty(node, "bus");
                else
                {
                    if( isAuxiliary || complexWithTitle || complexSub )
                        diagramToAST.changeSbgnViewTitle(node, astStart);

                    if( !isClone && node.getKernel() instanceof Specie && ! ( node.getCompartment().getKernel() instanceof Specie ) )
                        diagramToAST.changeDisplayName(node, astStart);

                    if( isClone )
                        diagramToAST.changeTitleInProperty(node, AntimonyConstants.SBGN_CLONE);
                }
            }
        }
        else if( propertyName.equals("showTitle") )
        {
            if( e.getSource() instanceof Node && SbgnUtil.isComplex((Node)e.getSource()) )
                diagramToAST.changeSbgnViewTitle((Node)e.getSource(), astStart);

        }
        else if( propertyName.equals("mainVariable") )
        {
            if( !e.getNewValue().equals(MainVariableType.NOT_SELECTED) && e.getPropagationId() instanceof Edge )
                diagramToAST.changeMainVariable((Edge)e.getPropagationId());
        }
        else if( propertyName.startsWith("attributes/") )
        {
            String attributeName = propertyName.substring("attributes/".length());
            if( attributeName.equals("Extent factor") && e.getSource() instanceof SubDiagram )
            {
                diagramToAST.changeConversionFactor((SubDiagram)e.getSource(), Util.EXTENT_FACTOR,
                        AstConversionFactor.EXTENT_CONVERSION_FACTOR);
            }
            else if( attributeName.equals("Time scale") && e.getSource() instanceof SubDiagram )
            {
                diagramToAST.changeConversionFactor((SubDiagram)e.getSource(), Util.TIME_SCALE, AstConversionFactor.TIME_CONVERSION_FACTOR);
            }
            else if( attributeName.equals(SBGNPropertyConstants.SBGN_EDGE_TYPE) )
            {
                diagramToAST.changeProperty((Edge)e.getPropagationId(), AntimonyConstants.SBGN_EDGE_TYPE, (String)e.getNewValue(),
                        astStart);
            }
            else if( e.getSource() instanceof Node )
            {
                if( SBGNPropertyConstants.SBGN_REACTION_TYPE.equals(attributeName) )
                    diagramToAST.changeProperty((Node)e.getSource(), SBGNPropertyConstants.SBGN_REACTION_TYPE, (String)e.getNewValue(),
                            astStart);
                else if( SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR.equals(attributeName) )
                    diagramToAST.changeLogicalType((Node)e.getSource(), astStart);
                else if( SBGNPropertyConstants.SBGN_CLONE_MARKER.equals(attributeName) )
                    diagramToAST.addCloneProperty((Node)e.getSource(), astStart);
                else if( SBGNPropertyConstants.SBGN_MULTIMER.equals(attributeName) )
                    diagramToAST.changeSbgnViewTitle((Node)e.getSource(), astStart);
                else if( e.getNewValue() instanceof String )
                {
                    diagramToAST.changeProperty((Node)e.getSource(), attributeName, (String)e.getNewValue(), astStart);
                }
                else
                {
                    System.out.println("Property " + e.getNewValue());
                }
            }
        }
        else if( propertyName.equals("quantityType") )
        {
            if( e.getSource() instanceof VariableRole )
                diagramToAST.changeQuantityType((VariableRole)e.getSource(), astStart);
        }
        else if( propertyName.equals("initialQuantityType") )
        {
            if( e.getSource() instanceof VariableRole )
                diagramToAST.changeInitialQuantityType((VariableRole)e.getSource(), astStart);
        }
        else if( propertyName.equals("units") )
        {
            if( e.getSource() instanceof Variable )
                diagramToAST.setUnit((Variable)e.getSource(), astStart);
            else
                diagramToAST.changeUnits(e, astStart);
        }
        else if( propertyName.equals("base_units") )
            diagramToAST.changeUnitFormula((Unit)e.getSource());
        else if( e.getSource() instanceof BaseUnit )
            diagramToAST.changeUnitFormula((Unit) ( (BaseUnit)e.getSource() ).getParent());
        else if( e.getSource() instanceof Edge )
        {
            Edge edge = (Edge)e.getSource();
            if( e.getNewValue() instanceof Node && e.getOldValue() instanceof Node )
            {
                Node newNode = (Node)e.getNewValue();
                Node oldNode = (Node)e.getOldValue();
                Node reactionNode = null;

                if( propertyName.equals("input") )
                    reactionNode = edge.getOutput();
                else if( propertyName.equals("output") )
                    reactionNode = edge.getInput();

                if( reactionNode != null && reactionNode.getKernel() instanceof Reaction )
                    diagramToAST.updateCloneReaction(oldNode, newNode, reactionNode);
            }
        }
        if( e.getSource() instanceof Event )
        {
            if( propertyName.equals("trigger") )
                diagramToAST.changeTriggerEvent((Event)e.getSource());
            else if( propertyName.equals("delay") )
                diagramToAST.changeDelayEvent((Event)e.getSource());
            else if( propertyName.equals("priority") )
                diagramToAST.changePriorityEvent((Event)e.getSource());
            else if( propertyName.equals("useValuesFromTriggerTime") )
                diagramToAST.changeUseValuesFromTriggerTimeEvent((Event)e.getSource());
            else if( propertyName.equals("triggerPersistent") )
                diagramToAST.changeTriggerPersistentEvent((Event)e.getSource());
            else if( propertyName.equals("triggerInitialValue") )
                diagramToAST.changeTriggerInitialValueEvent((Event)e.getSource());
        }
        return generateText(astStart);
    }

    public String highlight(ViewPaneEvent e)
    {
        ViewPane vp = e.getViewPane();
        SelectionManager sm = vp.getSelectionManager();
        if( sm.getSelectedViewCount() != 1 )
            return null;
        final Object de = sm.getSelectedView(0).getModel();
        if( de instanceof DiagramElement )
        {
            diagramToAST.highlight((DiagramElement)de);
            return generateText(astStart);
        }
        return null;
    }

    public void init(Diagram diagram)
    {
        this.diagram = diagram;
        diagramToAST = new AntimonyAstUpdater(diagram);
        parser = new AntimonyNotationParser();
        importAnnotation();
    }

    public Logger getLogger()
    {
        return log;
    }

    public void setLogHandler(Handler handler)
    {
        log.addHandler(handler);
        logHandler = handler;
    }
}
