package biouml.plugins.glycan;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.ShapeView;
import ru.biosoft.graphics.font.ColorFont;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.DiagramViewOptions;
import biouml.model.Node;
import biouml.model.NodeViewBuilder;
import biouml.model.xml.XmlDiagramSemanticController;
import biouml.model.xml.XmlDiagramType;
import biouml.plugins.glycan.parser.GlycanMolecule;
import biouml.plugins.glycan.parser.GlycanParser;
import biouml.plugins.glycan.parser.GlycanTree;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbml.SbmlDiagramType;
import biouml.plugins.sbml.SbmlSemanticController;
import biouml.standard.diagram.PathwayDiagramType;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

public class GlycanViewBuilder extends NodeViewBuilder
{
    public static final String GLYCAN_STRUCTURE = "glycanStructure";
    public static final int SIZE = 10;
    public static final int STEP_STANDART = 30;
    private static final String ATTR_FULL_GLYCAN_VIEW = "fullGlycanView";
    private static final String ATTR_SHOW_NAME = "showGlycanNames";

    private enum GlycanMoleculeName
    {
        A, AN, F, G, GN, M, NN
    }

    @Override
    public @Nonnull CompositeView createNodeView(Node node, DiagramViewOptions options, Graphics g)
    {
        DynamicPropertySet diagramAttrs = Diagram.getDiagram(node).getAttributes();
        if( diagramAttrs.getProperty(ATTR_SHOW_NAME) == null )
            diagramAttrs.add(new DynamicProperty(ATTR_SHOW_NAME, Boolean.class, true));

        DynamicPropertySet attributes = node.getAttributes();
        if( attributes.getProperty(ATTR_FULL_GLYCAN_VIEW) == null )
            attributes.add(new DynamicProperty(ATTR_FULL_GLYCAN_VIEW, Boolean.class, false));

        GlycanParser parser = new GlycanParser();
        GlycanTree tree = parser.parse(new StringReader(attributes.getValueAsString(GLYCAN_STRUCTURE)));

        CompositeView view = drawTree(tree, options, g, isFullGlycanView(node));

        ComplexTextView title = new ComplexTextView(node.getTitle(), new Point(view.getBounds().width / 2, view.getBounds().height),
                ComplexTextView.TEXT_ALIGN_CENTER, options.getNodeTitleFont(), options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_CENTER, (int)view.getBounds().getWidth(), g);

        title.setActive(false);
        title.setModel(view);
        title.setVisible( !node.getTitle().isEmpty() && showGlycanNames(diagramAttrs));
        view.add(title);

        view.setModel(node);
        view.setActive(true);

        view.setLocation(node.getLocation());
        node.setView(view);
        return view;
    }

    private boolean isFullGlycanView(Node node)
    {
        return Boolean.parseBoolean(node.getAttributes().getValueAsString(ATTR_FULL_GLYCAN_VIEW));
    }
    private boolean showGlycanNames(DynamicPropertySet dps)
    {
        return Boolean.parseBoolean(dps.getValueAsString(ATTR_SHOW_NAME));
    }

    @Override
    public boolean isApplicable(Node node)
    {
        String formula = node.getAttributes().getValueAsString(GLYCAN_STRUCTURE);
        if( formula != null )
        {
            GlycanParser parser = new GlycanParser();
            parser.parse(new StringReader(formula));
            if( parser.getStatus() == GlycanParser.STATUS_OK )
                return true;
        }
        return false;
    }

    @Override
    public boolean isApplicable(Diagram diagram)
    {
        DiagramType type = diagram.getType();
        if( type == null )
            return false;
        if( type instanceof SbmlDiagramType || type instanceof PathwayDiagramType || type instanceof SbgnDiagramType )
            return true;
        if( type instanceof XmlDiagramType && type.getSemanticController() instanceof XmlDiagramSemanticController )
        {
            if( ( (XmlDiagramSemanticController)type.getSemanticController() ).getPrototype() instanceof SbmlSemanticController )
                return true;
        }
        return false;
    }

    private CompositeView drawTree(GlycanTree tree, DiagramViewOptions options, Graphics g, boolean isFullGlycanView)
    {
        List<TreeElement> elements = new ArrayList<>();
        calculateElementsPoints(elements, getLeaves((GlycanMolecule)tree.jjtGetChild(0), new ArrayList<TreeElement>(), isFullGlycanView),
                getStep(isFullGlycanView), isFullGlycanView);

        return draw(elements, options, g, isFullGlycanView);
    }

    private CompositeView draw(List<TreeElement> elements, DiagramViewOptions options, Graphics g, boolean isFullGlycanView)
    {
        CompositeView container = new CompositeView();
        for( TreeElement element : elements )
        {
            int x1 = element.getX();
            int y1 = element.getY();

            for( TreeElement child : element.getChildren() )
            {
                int x2 = child.getX();
                int y2 = child.getY();
                LineView line = new LineView(new Pen(1, Color.black), x2 + SIZE / 2, y2 + SIZE / 2, x1 + SIZE / 2, y1 + SIZE / 2);
                if( isFullGlycanView )
                {
                    ComplexTextView title = new ComplexTextView(child.getBind(), new Point( ( x2 + x1 ) / 2 + SIZE / 2, ( y2 + y1 ) / 2
                            + SIZE / 2), ComplexTextView.TEXT_ALIGN_CENTER, new ColorFont("Arial", Font.PLAIN, 11, Color.black),
                            options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, 3, g);

                    title.setActive(true);
                    title.setModel(line);
                    container.add(title);
                }
                container.insert(line, 0);
            }
            ShapeView shape = getShape(element);
            container.add(shape);
        }
        container.add(new BoxView(null, null, new Rectangle2D.Double(container.getLocation().getX(), container.getLocation().getY(),
                container.getBounds().getWidth(), container.getBounds().getHeight())));
        return container;
    }

    private void calculateElementsPoints(List<TreeElement> allElements, List<TreeElement> leaves, int xPoint, boolean isFullGlycanView)
    {
        int size = leaves.size();
        if( size == 1 && leaves.get(0).getParent() instanceof GlycanTree )
        {
            allElements.addAll(leaves);
            return;
        }
        List<TreeElement> newLeaves = new ArrayList<>();

        GlycanMolecule parent = null;
        int start = 0;
        int shiftStep = getStep(isFullGlycanView);
        List<TreeElement> currentGroup = new ArrayList<>();

        for( int i = 0; i < size; i++ )
        {
            TreeElement currentElement = leaves.get(i);
            currentGroup.add(currentElement);
            parent = (GlycanMolecule)currentElement.getParent();

            if( i + 1 == size || leaves.get(i + 1).getParent() != parent )
            {
                if( parent.jjtGetNumChildren() == currentGroup.size() )
                {
                    TreeElement treeParent = new TreeElement(parent, xPoint, ( leaves.get(i).getY() + leaves.get(start).getY() ) / 2);
                    treeParent.setChildren(currentGroup);
                    allElements.addAll(currentGroup);
                    newLeaves.add(treeParent);
                }
                else
                {
                    for( TreeElement element : currentGroup )
                    {
                        element.shift(shiftStep);
                        newLeaves.add(element);
                    }
                }
                currentGroup = new ArrayList<>();
                if( i + 1 < size )
                    start = i + 1;
            }
        }

        calculateElementsPoints(allElements, newLeaves, xPoint + shiftStep, isFullGlycanView);
    }

    private int getStep(boolean isFullGlycanView)
    {
        if( isFullGlycanView )
            return STEP_STANDART;
        return STEP_STANDART / 2;
    }

    @Nonnull
    private ShapeView getShape(TreeElement element)
    {
        GlycanMoleculeName name;
        int xPoint = element.getX();
        int yPoint = element.getY();
        try
        {
            name = GlycanMoleculeName.valueOf(element.getName());
        }
        catch( IllegalArgumentException e )
        {
            return new EllipseView(new Pen(1, Color.red), new Brush(Color.pink), xPoint, yPoint, SIZE, SIZE);
        }
        switch( name )
        {
            case A:
                return new EllipseView(new Pen(1, Color.black), new Brush(Color.yellow), xPoint, yPoint, SIZE, SIZE);

            case AN:
                return new BoxView(new Pen(1, Color.black), new Brush(Color.yellow), xPoint, yPoint, SIZE, SIZE);

            case F:
                return new PolygonView(new Pen(1, Color.black), new Brush(Color.red), new int[] {xPoint + SIZE / 2, xPoint, xPoint + SIZE},
                        new int[] {yPoint, yPoint + SIZE, yPoint + SIZE});

            case G:
                return new EllipseView(new Pen(1, Color.black), new Brush(Color.blue), xPoint, yPoint, SIZE, SIZE);

            case GN:
                return new BoxView(new Pen(1, Color.black), new Brush(new Color(90, 0, 157)), xPoint, yPoint, SIZE, SIZE);

            case M:
                return new EllipseView(new Pen(1, Color.black), new Brush(Color.green), xPoint, yPoint, SIZE, SIZE);

            case NN:
                return new PolygonView(new Pen(1, Color.black), new Brush(new Color(255, 0, 127)), new int[] {xPoint + SIZE / 2, xPoint,
                        xPoint + SIZE / 2, xPoint + SIZE}, new int[] {yPoint, yPoint + SIZE / 2, yPoint + SIZE, yPoint + SIZE / 2});
            default:
                return new EllipseView( new Pen( 1, Color.red ), new Brush( Color.pink ), xPoint, yPoint, SIZE, SIZE );
        }
    }

    private List<TreeElement> getLeaves(GlycanMolecule parent, List<TreeElement> leaves, boolean isFullGlycanView)
    {
        if( parent.jjtGetNumChildren() == 0 )
        {
            leaves.add(new TreeElement(parent, 0, leaves.size() * getStep(isFullGlycanView)));
        }
        else
            for( int i = 0; i < parent.jjtGetNumChildren(); i++ )
                getLeaves((GlycanMolecule)parent.jjtGetChild(i), leaves, isFullGlycanView);
        return leaves;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( evt.getPropertyName().equals("attributes/" + GLYCAN_STRUCTURE) && evt.getSource() instanceof Node )
        {
            Node node = (Node)evt.getSource();
            if( !isApplicable(node) )
            {
                DynamicProperty dp = node.getAttributes().getProperty(GLYCAN_STRUCTURE);
                if( dp != null )
                    dp.setValue(evt.getOldValue());
            }
        }
    }
}
