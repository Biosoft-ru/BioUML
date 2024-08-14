package biouml.plugins.sbml.celldesigner;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.w3c.dom.Document;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.graph.Path;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnSemanticController;
import biouml.plugins.sbgn.Type;
import biouml.plugins.sbml.SbmlDiagramTransformer;
import biouml.plugins.sbml.SbmlImporter;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.sbml.SbmlModelReader;
import biouml.plugins.sbml.converters.SBGNConverter;
import biouml.plugins.sbml.converters.SBGNConverterNew;
import biouml.standard.diagram.Util;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import one.util.streamex.StreamEx;

/**
 * Import SBML file with CellDesigner extensions
 * TODO: remove _degraded species and replace them with empty sets
 *       redirect rections leading to subelements of complexes (Parkison model)
 *       move all thise fixes to CellDesignerExtension\PostProcessor
 */
public class CellDesignerImporter extends SbmlImporter
{
    protected static final Logger log = Logger.getLogger( CellDesignerImporter.class.getName() );

    @Override
    protected int getAcceptPriority()
    {
        return ACCEPT_HIGH_PRIORITY;
    }

    @Override
    public int accept(File file)
    {
        try
        {
            String header = ApplicationUtils.readAsString(file, 2000);

            int iXml = header.indexOf("<?xml");
            if( iXml == -1 )
            {
                return ACCEPT_UNSUPPORTED;
            }

            if( !header.substring(iXml, iXml + 100 > header.length() ? header.length() : iXml + 100)
                    .matches("(\\s)*<\\?xml(\\s)*version(\\s)*=(.|\\s)*") )
            {
                return ACCEPT_UNSUPPORTED;
            }

            int start = header.indexOf("<sbml");
            int end = header.indexOf(">", start);

            if( start == -1 || end == -1 )
                return ACCEPT_UNSUPPORTED;

            int isCellDesigner = header.indexOf("<" + CellDesignerExtension.CELLDESIGNER_BASE + ":");
            if( isCellDesigner == -1 )
            {
                //check for CellDesigner extension
                return ACCEPT_UNSUPPORTED;
            }

            String str = header.substring(start, end);
            if( SbmlImporter.checkSBMLVersion(str, log) )
            {
                return getAcceptPriority();
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "accept error :", t);
        }

        return ACCEPT_UNSUPPORTED;
    }

    @Override
    protected DataElement doImport(DataCollection origin, File file, String diagramName) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(file);

        SbmlModelReader reader = SbmlModelFactory.getReader(document);
        CellDesignerExtension extension = new CellDesignerExtension();
        reader.addExtension(CellDesignerExtension.CELLDESIGNER_BASE, extension);
        reader.setShouldLayout(false);
        Diagram diagram = reader.read(document, diagramName, origin);

        diagram.getViewOptions().setAutoLayout(false);

        CellDesignerPostProcessor processor = new CellDesignerPostProcessor(extension);
        processor.processElements(diagram);

        correctCompartmentSize(diagram);
        processCloseup(diagram);
        processTitles(diagram);
        //Axec: many elements including rules, events, auxiliary species does not have aliases, still we need them for simulation
        //removeNotUsedElements(diagram); TODO: double check why we needed this step at all (probably for better visuals)

        SBGNConverterNew converter = new SBGNConverterNew();
        Diagram sbgnDiagram = converter.convert(diagram, null);
        sbgnDiagram.getAttributes()
                .add(new DynamicProperty(SbmlDiagramTransformer.BASE_DIAGRAM_TYPE, String.class, diagram.getType().getClass().getName()));

        fixNodes(sbgnDiagram);
        fixEdges(sbgnDiagram);
        fixSpecieReference(sbgnDiagram);
        fixClones(sbgnDiagram);
        origin.put(sbgnDiagram);
        return sbgnDiagram;
    }

    @Override
    public SbmlImportProperties getProperties(DataCollection parent, File file, String elementName)
    {
        return null;
    }

    protected void fixSpecieReference(Diagram diagram)
    {
        for( Edge e : diagram.recursiveStream().select(Edge.class).filter(e -> Util.isSpecieReference(e)) )
        {
            Node specieNode = Util.isReaction(e.getInput()) ? e.getOutput() : e.getInput();
            String specieName = specieNode.getCompleteNameInDiagram();
            ( (SpecieReference)e.getKernel() ).setSpecie(specieName);
        }
    }

    //TODO: move this to extension and sbml converter
    protected void fixClones(Diagram diagram)
    {
        Map<String, Set<Node>> clones = new HashMap<>();

        for( Node n : diagram.recursiveStream().select(Node.class)
                .filter(n -> n.getRole() instanceof VariableRole && n.getKernel() instanceof Specie) )
            clones.computeIfAbsent( ( (VariableRole)n.getRole() ).getName(), k -> new HashSet()).add(n);

        clones = StreamEx.of(clones.entrySet()).filter(e -> e.getValue().size() > 1).toMap(e -> e.getKey(), e -> e.getValue());

        //here we need to distinct between nodes with and without variables in them (i.e. "active")
        //it should be done because original SBML files from pantherDB seems to be incorrect and does not distinct them
        for( Entry<String, Set<Node>> entry : StreamEx.of(clones.entrySet()) )
        {
            Set<Node> activeNodes = new HashSet<>();
            for( Node node : entry.getValue() )
            {
                if( isActive(node) )
                    activeNodes.add(node);
            }
            if( !activeNodes.isEmpty() && activeNodes.size() != entry.getValue().size() )
                entry.getValue().removeAll(activeNodes);
        }


        EModel emodel = diagram.getRole(EModel.class);
        for( Entry<String, Set<Node>> entry : StreamEx.of(clones.entrySet()) )
        {
            Node mainNode = entry.getValue().iterator().next();
            VariableRole role = mainNode.getRole(VariableRole.class);
            emodel.getVariableRoles().put(role);
            for( Node n : entry.getValue() )
            {
                n.setRole( mainNode.getRole() );
                SbgnSemanticController.setCloneMarker( n, mainNode.getTitle());
            }
        }
    }

    public static boolean isActive(Node node)
    {
        if( ! ( node instanceof Compartment ) )
            return false;

        for( Node innerNode : ( (Compartment)node ).recursiveStream().select(Node.class) )
        {
            if( Type.TYPE_VARIABLE.equals(innerNode.getKernel().getType()) )
                return true;
        }
        return false;
    }

    /**After diagram is converted to SBGN, sizes of nodes are changed. We need to shift them so centers of node will match centers of nodes on CellDesigner picture*/
    protected void fixNodes(Diagram diagram)
    {
        DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();
        for( Node n : diagram.recursiveStream().select(Node.class) )
        {
            try
            {
                if( !n.getAttributes().hasProperty(CellDesignerExtension.X_ATTR) )
                    continue;
                double x = Double.parseDouble(n.getAttributes().getValueAsString(CellDesignerExtension.X_ATTR));
                double y = Double.parseDouble(n.getAttributes().getValueAsString(CellDesignerExtension.Y_ATTR));
                double h = Double.parseDouble(n.getAttributes().getValueAsString(CellDesignerExtension.H_ATTR));
                double w = Double.parseDouble(n.getAttributes().getValueAsString(CellDesignerExtension.W_ATTR));

                double centerX = x + w / 2;
                double centerY = y + h / 2;

                Rectangle rec = viewBuilder.getNodeBounds(n);
                ;

                double actualH = rec.getHeight();
                double actualW = rec.getWidth();

                double newLocationX = centerX - actualW / 2;
                double newLocationY = centerY - actualH / 2;
                n.setLocation((int)newLocationX, (int)newLocationY);
            }
            catch( Exception ex )
            {

            }
        }
    }

    /**After diagram is converted to SBGN, node sizes are changed. We need to redirect edges*/
    protected void fixEdges(Diagram diagram)
    {
        DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();
        SemanticController semanticController = diagram.getType().getSemanticController();
        for( Edge e : diagram.recursiveStream().select(Edge.class) )
        {
            if( e.getPath() == null )
            {
                semanticController.recalculateEdgePath(e);
                continue;
            }

            Point in = new Point();
            Point out = new Point();

            viewBuilder.calculateInOut(e, in, out);
            Path path = e.getPath();


            path.xpoints[0] = in.x;
            path.ypoints[0] = in.y;

            path.xpoints[path.npoints - 1] = out.x;
            path.ypoints[path.npoints - 1] = out.y;
            e.setPath(path);
        }
    }

    protected void correctCompartmentSize(Compartment compartment) throws Exception
    {
        Point oldLocation = compartment.getLocation();
        Dimension size = compartment.getShapeSize();
        boolean changeSize = false;
        for( DiagramElement de : compartment )
        {
            if( de instanceof Compartment )
            {
                correctCompartmentSize((Compartment)de);
            }
            Object fixed = compartment.getAttributes().getValue(CellDesignerExtension.FIXED_SIZE_ATTR);
            if( fixed == null || ( ( fixed instanceof Boolean ) && ( ( (Boolean)fixed ).booleanValue() == false ) ) )
            {
                if( de instanceof Node )
                {
                    Point location = ( (Node)de ).getLocation();
                    Dimension dimension = ( (Node)de ).getShapeSize();
                    if( location != null && dimension != null )
                    {
                        if( location.x + dimension.width > oldLocation.x + size.width )
                        {
                            size.width = location.x + dimension.width - oldLocation.x;
                            changeSize = true;
                        }
                        if( location.y + dimension.height > oldLocation.y + size.height )
                        {
                            size.height = location.y + dimension.height - oldLocation.y;
                            changeSize = true;
                        }
                    }
                }
            }
        }
        if( changeSize )
        {
            compartment.setShapeSize(new Dimension(size.width + 20, size.height + 20));
        }
    }

    protected void processCloseup(Compartment comp) throws Exception
    {
        Object type = comp.getAttributes().getValue(SBGNPropertyConstants.CLOSEUP_ATTR);
        if( type instanceof String )
        {
            DataCollection<?> parent = comp.getOrigin();
            if( ! ( parent instanceof Compartment ) )
                return;
            comp.setShapeSize(new Dimension(0, 0));
            Point parentLocation = ( (Compartment)parent ).getLocation();
            Dimension parentSize = ( (Compartment)parent ).getShapeSize();
            if( type.equals("NORTH") )
            {
                resizeToBorder(comp, parentLocation, parentSize, 1);
                resizeToBorder(comp, parentLocation, parentSize, 2);
                resizeToBorder(comp, parentLocation, parentSize, 3);
            }
            else if( type.equals("EAST") )
            {
                resizeToBorder(comp, parentLocation, parentSize, 0);
                resizeToBorder(comp, parentLocation, parentSize, 2);
                resizeToBorder(comp, parentLocation, parentSize, 3);
            }
            else if( type.equals("WEST") )
            {
                resizeToBorder(comp, parentLocation, parentSize, 0);
                resizeToBorder(comp, parentLocation, parentSize, 1);
                resizeToBorder(comp, parentLocation, parentSize, 2);
            }
            else if( type.equals("SOUTH") )
            {
                resizeToBorder(comp, parentLocation, parentSize, 0);
                resizeToBorder(comp, parentLocation, parentSize, 1);
                resizeToBorder(comp, parentLocation, parentSize, 3);
            }
            else if( type.equals("NORTHWEST") )
            {
                resizeToBorder(comp, parentLocation, parentSize, 1);
                resizeToBorder(comp, parentLocation, parentSize, 2);
            }
            else if( type.equals("NORTHEAST") )
            {
                resizeToBorder(comp, parentLocation, parentSize, 2);
                resizeToBorder(comp, parentLocation, parentSize, 3);
            }
            else if( type.equals("SOUTHEAST") )
            {
                resizeToBorder(comp, parentLocation, parentSize, 3);
                resizeToBorder(comp, parentLocation, parentSize, 0);
            }
            else if( type.equals("SOUTHWEST") )
            {
                resizeToBorder(comp, parentLocation, parentSize, 0);
                resizeToBorder(comp, parentLocation, parentSize, 1);
            }
        }
        for( DiagramElement de : comp )
        {
            if( de instanceof Compartment )
            {
                processCloseup((Compartment)de);
            }
        }
    }
    /**
     * @param type 0 - NORTH, 1 - EAST, 2 - SOUTH, 3 -WEST
     */
    protected void resizeToBorder(Compartment comp, Point parentLocation, Dimension parentSize, int type)
    {
        Point oldLocation = comp.getLocation();
        Dimension d = comp.getShapeSize();
        if( type == 0 )
        {
            comp.setLocation(oldLocation.x, parentLocation.y);
            comp.setShapeSize(new Dimension(d.width, d.height + oldLocation.y - parentLocation.y));
        }
        else if( type == 1 )
        {
            comp.setShapeSize(new Dimension(parentLocation.x + parentSize.width - oldLocation.x, d.height));
        }
        else if( type == 2 )
        {
            comp.setShapeSize(new Dimension(d.width, parentLocation.y + parentSize.height - oldLocation.y));
        }
        else if( type == 3 )
        {
            comp.setLocation(parentLocation.x, oldLocation.y);
            comp.setShapeSize(new Dimension(d.width + oldLocation.x - parentLocation.x, d.height));
        }
    }

    /**
     *  Process special phrases in titles
     */
    protected void processTitles(Compartment compartment) throws Exception
    {
        for( DiagramElement de : compartment )
        {
            fixNodeTitle(de);

            Object aliases = de.getAttributes().getValue(SBGNConverter.ALIASES_ATTR);
            if( aliases instanceof Node[] )
            {
                for( Node node : (Node[])aliases )
                {
                    fixNodeTitle(node);
                }
            }

            if( de instanceof Compartment )
            {
                processTitles((Compartment)de);
            }
        }
    }

    protected void fixNodeTitle(DiagramElement de)
    {
        fixTitle(de);
        Object complexElements = de.getAttributes().getValue(SBGNConverter.COMPLEX_ATTR);
        if( complexElements instanceof Node[] )
        {
            for( Node n : (Node[])complexElements )
            {
                fixTitle(n);
            }
        }
    }

    protected void fixTitle(DiagramElement de)
    {
        String title = de.getTitle();
        title = title.replaceAll("_super_", "<sup>");
        title = title.replaceAll("_endsuper_", "</sup>");
        title = title.replaceAll("_endsuper", "</sup>");
        title = title.replaceAll("_sub_", "<sub>");
        title = title.replaceAll("_endsub_", "</sub>");
        title = title.replaceAll("_endsub", "</sub>");
        title = title.replaceAll("_plus_", "+");
        title = title.replaceAll("_minus_", "-");
        title = title.replaceAll("_br_", "<br>");
        title = title.replaceAll("_slash_", "/");
        title = title.replaceAll("_alpha_", "&alpha;");
        title = title.replaceAll("_beta_", "&beta;");
        title = title.replaceAll("_gamma_", "&gamma;");
        title = title.replaceAll("_delta_", "&delta;");

        de.setTitle(title);
    }

    /**
     *  Some elements can be used not in all aliases.
     *  This method removes all nodes without alias value
     */
    protected void removeNotUsedElements(Compartment compartment) throws Exception
    {
        List<String> names = compartment.names().collect( Collectors.toList() );
        for( String cName : names )
        {
            DiagramElement de = compartment.get(cName);
            if( de instanceof Compartment )
            {
                removeNotUsedElements((Compartment)de);
            }
            Object aliases = ( de ).getAttributes().getValue(SBGNConverter.ALIASES_ATTR);
            if( aliases instanceof Node[] )
            {
                for( Node node : (Node[])aliases )
                {
                    if( node instanceof Compartment )
                    {
                        removeNotUsedElements((Compartment)node);
                    }
                }
            }

            if( cName.equals("default") )
                continue;
            if( de instanceof Node )
            {
                Object nodeAlias = ( (Node)de ).getAttributes().getValue(SBGNConverter.ALIAS_ATTR);
                if( nodeAlias == null )
                {
                    compartment.remove(cName);
                }
            }
        }
    }
}
