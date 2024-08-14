package biouml.plugins.kegg.access;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.plugins.kegg.KeggPathwayDiagramType;
import biouml.standard.type.Base;
import biouml.standard.type.Protein;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;

public class DiagramReader
{
    final public static String RECT_SHAPE = "rect";
    final public static String CIRC_SHAPE = "circ";
    final public static String ENZYME_TYPE   = "enzyme";
    final public static String COMPOUND_TYPE = "compound";
    final public static String REACTION_MAIN_DIR = "reaction.main";
    final public static String REACTION_EXT      = ".rea";

    private static Logger log = Logger.getLogger(DiagramReader.class.getName());

    public DiagramReader( File file )
    {
        this.file = file;
    }

    public Diagram read( DataCollection origin, String name, Module module ) throws Exception
    {
        this.module = module;
        diagram = new Diagram(origin, new Stub(null, name), new KeggPathwayDiagramType());
        readNodes();
        readReactions();
        Diagram retDiagram = diagram;
        diagram = null;
        this.module  = null;
        return retDiagram;
    }

    private void readNodes() throws Exception
    {
        try(BufferedReader reader = ApplicationUtils.utfReader( file ))
        {
            String line = null;
            while( (line=reader.readLine())!=null )
            {
                Node node = createNode( line );
                if( node!=null )
                {
                    diagram.put( node );
                }
                else
                {
                    log.log(Level.SEVERE, "Can't create node by '"+line+"' line from "+file+".");
                }
            }
        }
    }

    private void readReactions() throws Exception
    {
        String name = file.getName();
        int pos = name.lastIndexOf( "." );
        name = name.substring(0,pos)+REACTION_EXT;
        File reactionFile = new File(file.getParent(),REACTION_MAIN_DIR+"/"+name);
        try(BufferedReader reader = ApplicationUtils.utfReader( reactionFile ))
        {
            String line = null;
            while( (line=reader.readLine())!=null )
            {
                createEdges( line );
            }
        }
    }

    private Node createNode( String urlLine )
    {
        Node node = null;
        Info info = parseInfo( urlLine );
        Base kernel = null;
        try
        {
            if( info.clazz==null || info.name==null )
            {
                log.log(Level.SEVERE,  "Can't extract class or name from '"+urlLine+"'" );
            }
            else
            {
                kernel = module.getKernel( info.clazz, info.name );
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, t.getMessage(), t);
        }
        if( kernel != null )
        {
            node = new Node(diagram,kernel);
            node.setLocation( info.rect.getLocation() );
        }
        else
        {
            log.log(Level.SEVERE,  "Can't find kernel '"+info.name+"' of class "+info.clazz );
        }
        return node;
    }

    private void createEdges( String line ) throws Exception
    {
        StringTokenizer st = new StringTokenizer(line,":");
        st.nextToken(); // reaction name: ignored
        String enzymeName   = "EC "+st.nextToken();
        String links = st.nextToken();
        String inLinks  = links.substring( 0,links.indexOf("<=>") );
        String outLinks = links.substring( links.indexOf("<=>")+3 );

        Node enzyme = (Node)diagram.get( enzymeName );
        if( enzyme==null )
        {
            Base kernel = module.getKernel( Protein.class, enzymeName );
            if( kernel==null )
            {
                log.log(Level.SEVERE, "Enzyme '"+enzymeName+"' not found in createEgdes.");
                return;
            }
            enzyme = new Node(diagram,kernel);
            diagram.put( enzyme );
            enzyme.setLocation( 0,0 );
        }
        st = new StringTokenizer( inLinks," +" );
        while( st.hasMoreTokens() )
        {
            String inName = st.nextToken().trim();
            Node inCompound = (Node)diagram.get(inName);
            if( inCompound != null )
            {
                String reactionName = inCompound.getName() + " -> " + enzyme.getName();
                Edge edge = new Edge(diagram, new Stub(null, reactionName), inCompound, enzyme );
                diagram.put( edge );
            }
        }
        st = new StringTokenizer( outLinks," +" );
        while( st.hasMoreTokens() )
        {
            String outName = st.nextToken().trim();
            Node outCompound = (Node)diagram.get(outName);
            if( outCompound != null )
            {
                String reactionName = enzyme.getName() + " -> " + outCompound.getName();
                Edge edge = new Edge(diagram, new Stub(null, reactionName), enzyme, outCompound );
                diagram.put( edge );
            }
        }
    }

    private Info parseInfo( String line )
    {
        final String coordDelimeters = " (),\t";
        Info info = new Info();

        StringTokenizer st = new StringTokenizer( line );
        String token = st.nextToken();
        if( token.equals(RECT_SHAPE) )
        {
            info.shapeType = Compartment.SHAPE_RECTANGLE;
            int left   = 2*Integer.parseInt(st.nextToken(coordDelimeters));
            int top    = 2*Integer.parseInt(st.nextToken(coordDelimeters));
            int right  = 2*Integer.parseInt(st.nextToken(coordDelimeters));
            int bottom = 2*Integer.parseInt(st.nextToken(coordDelimeters));
            info.rect = new Rectangle(left,top,right-left,bottom-top);
        }
        else if( token.equals(CIRC_SHAPE) )
        {
            info.shapeType = Compartment.SHAPE_ELLIPSE;
            int left   = 2*Integer.parseInt(st.nextToken(coordDelimeters));
            int top    = 2*Integer.parseInt(st.nextToken(coordDelimeters));
            int radius = 2*Integer.parseInt(st.nextToken(coordDelimeters));
//            info.rect = new Rectangle(left-radius,top-radius,2*radius,2*radius);
            info.rect = new Rectangle(left-4*radius,top-4*radius,2*radius,2*radius);
        }

        int pos = line.indexOf('/');
        if( pos>0 )
        {
            String url = line.substring( pos );
            pos = url.indexOf('?');
            url = url.substring( pos+1 );
            st = new StringTokenizer(url,"+");
            String type = st.nextToken();
            if( type.equals(ENZYME_TYPE) )
            {
                info.clazz = Protein.class;
                info.name = "EC "+st.nextToken();
            }
            else if( type.equals(COMPOUND_TYPE) )
            {
                info.clazz = Substance.class;
                info.name = st.nextToken();
            }
            else
            {
                log.log(Level.FINE, "VLADZ!!! parseInfo found unknown type : "+type);
            }
        }
        return info;
    }

    private static class Info
    {
        Rectangle rect = new Rectangle(0,0,10,10);
        int shapeType;
        Class<? extends Base> clazz;
        String name;
    }

    private final File file;
    private Module module = null;
    private Diagram diagram = null;
}