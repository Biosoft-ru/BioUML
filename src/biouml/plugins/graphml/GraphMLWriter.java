package biouml.plugins.graphml;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.Point;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.GraphicsUtils;
import ru.biosoft.graphics.HtmlView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import biouml.model.Compartment;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Type;

public class GraphMLWriter
{
    public void writeGraph(Diagram diagram, OutputStream os, boolean useYSchema)
    {
        XmlWriter xml = new XmlWriter(new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8)));
        if(useYSchema)
            xml.begin(Constants.GRAPHML_HEADER_Y, 2);
        else
            xml.begin(Constants.GRAPHML_HEADER, 2);

        idCount = 1;
        name2id = new HashMap<>();
        
        // print the graph schema
        printSchemaComponent(xml, Constants.DESCRIPTION, Constants.NODE, null, Constants.STRING, null);
        printSchemaComponent(xml, Constants.DESCRIPTION, Constants.EDGE, null, Constants.STRING, null);
        if(useYSchema)
        {
            printSchemaComponent(xml, Constants.NODEGRAPHICS, Constants.NODE, "yfiles.type", null, null);
            printSchemaComponent(xml, Constants.EDGEGRAPHICS, Constants.EDGE, "yfiles.type", null, null);
        }
        xml.println();

        // print graph contents
        xml.start(Constants.GRAPH, Constants.EDGEDEF, Constants.DIRECTED);
        writeCompartment(xml, diagram, diagram);
        xml.end();

        // finish writing file
        xml.finish("</" + Constants.GRAPHML + ">\n");
    }

    private void writeCompartment(XmlWriter xml, Compartment compartment, Diagram parent)
    {
        for(DiagramElement de: compartment)
        {
            if( de instanceof Node )
            {
                xml.start(Constants.NODE, Constants.ID, de.getCompleteNameInDiagram());
                xml.contentTag(Constants.DATA, Constants.KEY, getPropertyId(Constants.DESCRIPTION, Constants.NODE), de.getTitle());
                printNodeGraphics((Node)de, xml);
                xml.end();

                if( de instanceof Compartment && parent.getType().needLayout((Node)de) )
                {
                    writeCompartment(xml, (Compartment)de, parent);
                }
            }
        }
        xml.println();
        // print the edges
        String[] attr = new String[] {Constants.ID, Constants.SOURCE, Constants.TARGET};
        String[] vals = new String[3];

        for(DiagramElement de: compartment)
        {
            if( de instanceof Edge )
            {
                Edge e = (Edge)de;
                vals[0] = e.getName();
                vals[1] = e.getInput().getCompleteNameInDiagram();
                vals[2] = e.getOutput().getCompleteNameInDiagram();

                xml.start(Constants.EDGE, attr, vals, 3);
                xml.contentTag(Constants.DATA, Constants.KEY, getPropertyId(Constants.DESCRIPTION, Constants.EDGE), de.getTitle());
                printEdgeGraphics((Edge)de, xml);
                xml.end();
            }
        }
    }
    
    private String formatColor(Color c)
    {
        return String.format(Locale.ENGLISH, "#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private void printNodeGraphics(Node node, XmlWriter xml)
    {
        String id = getPropertyId(Constants.NODEGRAPHICS, Constants.NODE);
        if(id == null)
            return;
        xml.start(Constants.DATA, Constants.KEY, id);
        xml.start("y:ShapeNode", null, null);
        Dimension d = node.getShapeSize();
        if( d.getHeight() != 0.0 || d.getWidth() != 0.0 ) //TODO: read about yEd
        {
            //Rectangle bounds = node.getView().getBounds();
            Point bounds = node.getLocation();
            xml.tag("y:Geometry", new String[] {"height", "width", "x", "y"}, new String[] {String.valueOf(d.height),
                    String.valueOf(d.width), String.valueOf(bounds.x), String.valueOf(bounds.y)}, 4, true);
        }
        if( node instanceof Compartment )
        {
            Paint paint = DefaultDiagramViewBuilder.getBrush(node, new Brush((Color)null)).getPaint();
            if( paint instanceof Color )
            {
                if(( (Color)paint ).getAlpha() > 0)
                {
                    xml.tag("y:Fill", "color", formatColor((Color)paint), true);
                }
            }
            else if( paint instanceof GradientPaint )
            {
                String rgb1 = formatColor( ( (GradientPaint)paint ).getColor1());
                String rgb2 = formatColor( ( (GradientPaint)paint ).getColor2());
                xml.tag("y:Fill", new String[] {"color", "color2"}, new String[] {rgb1, rgb2}, 2, true);
            }
        }
        String shape = "roundrectangle";
        boolean titleNeeded = true;
        if( node.getKernel() != null )
        {
            String type = node.getKernel().getType();
            if( type.equals(Type.TYPE_REACTION) )
            {
                shape = "ellipse";
                titleNeeded = false;
            }
            else if( type.equals(Type.TYPE_NOTE) )
                shape = "rectangle";
        }
        if( titleNeeded )
        {
            String title = "";
            View view = node.getView();
            if( view instanceof CompositeView )
            {
                for(View textView: (CompositeView)view)
                {
                    if( textView instanceof ComplexTextView )
                    {
                        title = getTitleFromView((ComplexTextView)textView);
                        break;
                    }
                    else if(textView instanceof HtmlView)
                    {
                    	title = getTitleFromView(GraphicsUtils.getAsComplexTextView((HtmlView)textView));
                    }
                }
            }
            if( title.isEmpty() )
                title = node.getTitle();
            xml.contentTag("y:NodeLabel", title);
        }
        xml.tag("y:Shape", "type", shape, true);
        xml.end();
        xml.end();
    }

    private void printEdgeGraphics(Edge edge, XmlWriter xml)
    {
        String id = getPropertyId(Constants.EDGEGRAPHICS, Constants.EDGE);
        if(id == null)
            return;
        xml.start(Constants.DATA, Constants.KEY, id);
        xml.start("y:PolyLineEdge", null, null);
        String lineStyle = "line";
        String arrowSource = "none";
        String arrowTarget = "standard";
        if( edge.getKernel() != null )
        {
            String type = edge.getKernel().getType();
            if( type.equals(Type.TYPE_NOTE_LINK) )
            {
                lineStyle = "dashed";
                arrowTarget = "none";
            }
            else if( type.equals(Type.TYPE_UNDIRECTED_LINK) )
                arrowTarget = "none";
            if(edge.getKernel() instanceof SpecieReference)
            {
                String role = ((SpecieReference)edge.getKernel()).getRole();
                if(role.equals(SpecieReference.REACTANT))
                    arrowTarget = "none";
                else if(role.equals(SpecieReference.MODIFIER))
                    arrowTarget = "white_diamond";
            }
        }
        xml.tag("y:LineStyle", "type", lineStyle, true);
        xml.tag("y:Arrows", new String[] {"source", "target"}, new String[] {arrowSource, arrowTarget}, 2, true);
        xml.end();
        xml.end();
    }
    
    private String getTitleFromView(ComplexTextView view)
    {
        StringBuffer buf = new StringBuffer();
        for( View lineView : view )
        {
            if( lineView instanceof CompositeView )
            {
                for( View lineTextView : (CompositeView)lineView )
                {
                    if( lineTextView instanceof TextView )
                    {
                        buf.append( ( (TextView)lineTextView ).getText());
                        buf.append("\n");
                    }
                }
            }
        }
        if( buf.length() > 0 )
            buf.deleteCharAt(buf.length() - 1);
        return buf.toString();
    }


    private void printSchemaComponent(XmlWriter xml, String name, String group, String attributeName, String attributeType,
            String defaultValue)
    {
        List<String> names = new ArrayList<>();
        List<String> values = new ArrayList<>();

        //"id" and "for" attributes should always exist
        String id = generatePropertyId(name, group);
        names.add(Constants.ID);
        values.add(id);
        names.add(Constants.FOR);
        values.add(group);
        if( attributeName == null )
            attributeName = Constants.ATTRNAME;
        names.add(attributeName);
        values.add(name);
        if( attributeType != null )
        {
            names.add(Constants.ATTRTYPE);
            values.add(attributeType);
        }

        int numFields = names.size();
        if( defaultValue == null )
        {
            xml.tag(Constants.KEY, names.toArray(new String[numFields]), values.toArray(new String[numFields]), numFields);
        }
        else
        {
            xml.start(Constants.KEY, names.toArray(new String[numFields]), values.toArray(new String[numFields]), numFields);
            xml.contentTag(Constants.DEFAULT, defaultValue);
            xml.end();
        }
    }
    
    private int idCount;
    private Map<String, String> name2id;
    
    private String generatePropertyId(String propertyName, String objectType)
    {
        String id = "id" + idCount++;
        name2id.put(propertyName + "." + objectType, id);
        return id;
    }
    
    private String getPropertyId(String propertyName, String objectType)
    {
        return name2id.get(propertyName + "." + objectType);
    }
    
    
}
