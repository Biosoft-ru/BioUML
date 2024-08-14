package ru.biosoft.server.servlets.ubiprot;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.server.AbstractServlet;
import ru.biosoft.util.TextUtil;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.util.ImageGenerator;
import biouml.standard.type.Protein;
import biouml.standard.type.Stub;

public class DiagramServlet extends AbstractServlet
{
    protected static final Logger log = Logger.getLogger(DiagramServlet.class.getName());

    public static final String IMG_TYPE = "png";
    public static final String MAP_TYPE = "map";

    @Override
    public String service(String localAddress, Object session, Map params, OutputStream out, Map<String, String> header)
    {
        if( localAddress.endsWith("." + IMG_TYPE) )
        {
            return getImage(localAddress, out);
        }
        else if( localAddress.endsWith("." + MAP_TYPE) )
        {
            return getMap(localAddress, out);
        }
        else if( params.containsKey("line") && params.containsKey("organism") )
        {
            return generateDiagramPage(localAddress, ( (String[])params.get("line") )[0], ( (String[])params.get("organism") )[0], out);
        }
        return generateDefaultPage(out);
    }

    protected String generateDefaultPage(OutputStream out)
    {
        try
        {
            try( PrintWriter pw = new PrintWriter( out ) )
            {
                pw.write( "<html><head>" );
                pw.write( "<title>Ubiprot diagrams</title></head>" );
                pw.write( "<body>" );

                pw.write( "</body>" );
                pw.write( "</html>" );
            }
            out.close();
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not create dafault page", t);
        }
        return "text/html";
    }

    protected String generateDiagramPage(String localAddress, String line, String organism, OutputStream out)
    {
        try
        {
            Diagram diagram = DiagramGenerator.getInstance().getDiagram(line, organism);
            String imgPath = TextUtil.split( localAddress, '/' )[1] + "/" + line + "." + organism + "." + IMG_TYPE;

            try( PrintWriter pw = new PrintWriter( out ) )
            {
                ImageGenerator.generateDiagramImage( diagram );
                String map = ImageGenerator.generateImageMap( diagram.getView(), new UbiprotReferenceGenerator() );

                pw.write( "<html><head>" );
                pw.write( "<title>Diagram</title></head>" );
                pw.write( "<body>" );

                pw.write( "<h3 align=center>" + diagram.getName() + "</h3>" );

                pw.write( "<p align=\"right\"><a href=\"http://ubiquitomix.biouml.org/legend.gif\" target=\"entityView\">legend</a></p>" );

                pw.write( "<center><img border=\"0\" " + "src=\"" + imgPath + "\" usemap=\"#diagram_map\"></center>" );
                pw.write( "<map name=\"diagram_map\">" + map + "</map>" );

                pw.write( "</body>" );
                pw.write( "</html>" );
            }
            out.close();
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not generate diagram page", t);
        }
        return "text/html";
    }

    protected String getImage(String localAddress, OutputStream out)
    {
        try
        {
            String params = localAddress.substring(localAddress.lastIndexOf("/") + 1, localAddress.lastIndexOf("."));
            int pointPos = params.indexOf('.');
            String line = params.substring(0, pointPos);
            String organism = params.substring(pointPos + 1, params.length());
            Diagram diagram = DiagramGenerator.getInstance().getDiagram(line, organism);

            BufferedImage image = ImageGenerator.generateDiagramImage(diagram);
            ImageGenerator.encodeImage(image, "PNG", out);

            out.close();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not create image", e);
        }
        return "image/png";
    }

    protected String getMap(String localAddress, OutputStream out)
    {
        try
        {
            String params = localAddress.substring(localAddress.lastIndexOf("/") + 1, localAddress.lastIndexOf("."));
            int pointPos = params.indexOf('.');
            String line = params.substring(0, pointPos);
            String organism = params.substring(pointPos + 1, params.length());
            Diagram diagram = DiagramGenerator.getInstance().getDiagram(line, organism);

            try( PrintWriter pw = new PrintWriter( out ) )
            {
                ImageGenerator.generateDiagramImage( diagram );
                String map = ImageGenerator.generateImageMap( diagram.getView(), new UbiprotReferenceGenerator() );

                pw.write( map );
            }
            out.close();
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not generate diagram page", t);
        }
        return "text/html";
    }

    protected static class UbiprotReferenceGenerator implements ImageGenerator.ReferenceGenerator
    {
        @Override
        public String getReference(Object obj)
        {
            if( obj instanceof Node )
            {
                Node node = (Node)obj;
                if( node.getKernel() != null )
                {
                    if( node.getKernel() instanceof Protein )
                    {
                        String proteinLink = ( (Protein)node.getKernel() ).getComment();
                        return "q?_t_=components&uniprot=" + proteinLink;
                    }
                    else if( ( node.getKernel() instanceof Stub ) && ( (Stub)node.getKernel() ).getType().equals("Unknown") )
                    {
                        return "#";
                    }
                }
            }
            return null;
        }

        @Override
        public String getTarget(Object obj)
        {
            if( obj instanceof Node )
            {
                Node node = (Node)obj;
                if( node.getKernel() != null && ( node.getKernel() instanceof Stub )
                        && ( (Stub)node.getKernel() ).getType().equals("Unknown") )
                {
                    return "_self";
                }
            }
            return "entityView";
        }

        @Override
        public String getTitle(Object obj)
        {
            if( obj instanceof Node )
            {
                Compartment comp = (Compartment) ( (Node)obj ).getOrigin();
                Object type = comp.getAttributes().getValue("compartmentType");
                if( type != null )
                {
                    if( type.equals("E1") )
                    {
                        return "E1";
                    }
                    else if( type.equals("E2") )
                    {
                        return "E2";
                    }
                    else if( type.equals("E3") )
                    {
                        return "E3";
                    }
                    else if( type.equals("DUB") )
                    {
                        return "DUB";
                    }
                    else if( type.equals("UBP") )
                    {
                        return "UBP";
                    }
                }
                else
                {
                    Object ubi = ( (Node)obj ).getAttributes().getValue("isUbi");
                    if( ubi != null && ubi.toString().equals("true") )
                    {
                        return "Ubiquitylated substrat";
                    }
                    else
                    {
                        return "Substrate";
                    }
                }
            }
            return null;
        }
    }
}
