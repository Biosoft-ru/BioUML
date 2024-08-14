package biouml.plugins.gxl;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.logging.Logger;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;


/**
 * Extracts only GXL-representable part of the digram.
 */
public class GxlWriter extends GxlSupport
{
    static String endl = System.getProperty("line.separator");

    @Override
    protected Logger initLog()
    {
        return Logger.getLogger(GxlWriter.class.getName());
    }

    public void writeDiagram(File file, Diagram diagram)
    {
        if( diagram == null )
            return;

        try(PrintWriter out = new PrintWriter(file, "UTF-8"))
        {
            out.write("<?xml version='1.0' encoding='utf-8'?>" + endl);
            writeGXL(out, diagram);
            writeGXLExtension(out, diagram);
        }
        catch( Exception e )
        {
            error("ERROR_DIAGRAM_WRITING", new String[] {diagram.getName()});
        }
    }

    protected void writeGXL(Writer out, Diagram diagram) throws Exception
    {
        out.write("<gxl xmlns:xlink=\"http://www.w3.org/1999/xlink\">" + endl);
        // write whole diagram as one hypergraph graph
        writeGraph(out, diagram);
        out.write("</gxl>");
    }

    protected void writeGXLExtension(Writer out, Diagram diagram)
    {
        // TODO
    }

    protected void writeGraph(Writer out, Diagram diagram) throws Exception
    {
        out.write("<graph id=\"" + diagram.getName() + "\"" + " edgeids=\"true\">" + endl);
        writeElements(out, diagram);
        // add more to have support of other elements
        out.write("</graph>" + endl);
    }

    protected void writeElements(Writer out, Compartment compartment)
    {
        for(DiagramElement de : compartment)
        {
            try
            {
                if( de instanceof Node )
                {
                    if( de instanceof Compartment )
                    {
                        out.write("    <node id=\"" + de.getName() + "\">" + endl);
                        out.write("    <graph id=\"" + de.getName() + "\">" + endl);
                        writeElements(out, (Compartment)de);
                        out.write("    </graph>" + endl);
                        out.write("    </node>" + endl);
                    }
                    else
                        out.write("    <node id=\"" + de.getName() + "\"/>" + endl);
                }
                else if( de instanceof Edge )
                {
                    Edge edge = (Edge)de;
                    out.write("    <edge id=\"" + edge.getName() + "\" from=\"" + edge.getInput().getName() + "\"" + " to=\""
                            + edge.getOutput().getName() + "\"/>" + endl);
                }
            }
            catch( Throwable t )
            {
                error("ERROR_DIAGRAM_ELEMENT_WRITING", new String[] {de.getName()});
            }
        }
    }
}
