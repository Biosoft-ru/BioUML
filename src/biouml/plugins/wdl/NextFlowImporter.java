package biouml.plugins.wdl;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.diagram.WDLConstants;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;
import ru.biosoft.util.TempFiles;

public class NextFlowImporter
{
    private static final Logger log = Logger.getLogger( NextFlowRunner.class.getName() );

    public static void runNextFlowDry(String nextFlowScript, Diagram diagram) throws Exception
    {
        String outputDir = TempFiles.path( "nextflow" ).getAbsolutePath();
        boolean useWsl = System.getProperty( "os.name" ).startsWith( "Windows" );
        new File( outputDir ).mkdirs();
        File f = new File( outputDir, "temp.nf" );
        f.createNewFile();
        ApplicationUtils.writeString( f, nextFlowScript );

        NextFlowRunner.generateFunctions( new File( outputDir ) );
        ProcessBuilder builder;
        if( useWsl )
        {
            String parent = new File( outputDir ).getAbsolutePath().replace( "\\", "/" );
            builder = new ProcessBuilder( "wsl", "--cd", parent, "nextflow", "run", f.getName(), "-preview", "-with-dag", "dag.dot" );
        }
        else
        {
            builder = new ProcessBuilder( "nextflow", "run", f.getName(), "-preview", "-with-dag dag.dot" );
            builder.directory( new File( outputDir ) );
        }

        System.out.println( "COMMAND: " + StreamEx.of( builder.command() ).joining( " " ) );
        Process process = builder.start();
        new Thread( new Runnable()
        {
            public void run()
            {
                BufferedReader input = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
                String line = null;

                try
                {
                    while( ( line = input.readLine() ) != null )
                        log.info( line );
                }
                catch( IOException e )
                {
                    e.printStackTrace();
                }
                //                
                //for some reason cwl-runner outputs everything into error stream
                BufferedReader err = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );
                line = null;

                try
                {
                    while( ( line = err.readLine() ) != null )
                        log.info( line );
                }
                catch( IOException e )
                {
                    e.printStackTrace();
                }
            }
        } ).start();

        process.waitFor();

        File dag = new File( outputDir, "dag.dot" );
        String content = ApplicationUtils.readAsString( dag );
        DotParser parser = new DotParser();
        parser.parse( content );
        Map<String, NodeInfo> nodes = parser.getNodes();
        List<EdgeInfo> edges = parser.getEdges();

        diagram.clear();

        for( NodeInfo nodeInfo : nodes.values() )
        {
            boolean isCall = isCall( nodeInfo );
            String name = nodeInfo.getValue( "label" );
            String id = nodeInfo.id;

            if( isCall )
            {
                name = name.substring( name.indexOf( ":" ) + 1 );
                String taskName = id + "_definition";
                Compartment task = new Compartment( diagram, taskName, new Stub( null, taskName, WDLConstants.TASK_TYPE ) );
                task.setShapeSize( new Dimension( 200, 0 ) );
                task.setTitle( name );
                diagram.put( task );

                Compartment call = new Compartment( diagram, id, new Stub( null, id, WDLConstants.CALL_TYPE ) );
                call.setShapeSize( new Dimension( 200, 0 ) );
                call.setTitle( name );
                WorkflowUtil.setTaskRef( call, taskName );
                WorkflowUtil.setCallName( call, name );
                diagram.put( call );
            }
            else
            {
                Node node = new Node( diagram, id, new Stub( null, id, WDLConstants.EXPRESSION_TYPE ) );
                WorkflowUtil.setName( node, name );
                WorkflowUtil.setType( node, "File" );
                WorkflowUtil.setExpression( node, "" );
                node.setTitle( name );
                node.setShapeSize( new Dimension( 80, 60 ) );
                diagram.put( node );
            }

        }

        Map<String, Integer> inputCount = new HashMap<>();
        Map<String, Integer> outputCount = new HashMap<>();
        for( EdgeInfo edgeInfo : edges )
        {
            Node input = diagram.findNode( edgeInfo.getInput() );
            Node output = diagram.findNode( edgeInfo.getOutput() );
            if( WorkflowUtil.isCall( input ) )
            {
                Compartment task = (Compartment)diagram.findNode( input.getName() + "_definition" );
                Integer position = inputCount.get( input.getName() );
                if( position == null )
                    position = 0;
                String name = edgeInfo.attributes.get( "label" );
                if( name == null )
                    name = DefaultSemanticController.generateUniqueName( diagram, "output" );
                WDLImporter.addPort( name, WDLConstants.OUTPUT_TYPE, position, task );
                input = WDLImporter.addPort( name, WDLConstants.OUTPUT_TYPE, position, (Compartment)input );
                inputCount.put( input.getName(), position++ );
            }
            else
            {
                String name = edgeInfo.attributes.get( "label" );
                if( name != null )
                {
                    WorkflowUtil.setName( input, name );
                    input.setTitle( name );
                }
            }
            if( WorkflowUtil.isCall( output ) )
            {
                Compartment task = (Compartment)diagram.findNode( output.getName() + "_definition" );
                Integer position = outputCount.get( output.getName() );
                if( position == null )
                    position = 0;
                String name = edgeInfo.attributes.get( "label" );
                if( name == null )
                    name = DefaultSemanticController.generateUniqueName( diagram, "input" );
                WDLImporter.addPort( name, WDLConstants.INPUT_TYPE, position, task );
                output = WDLImporter.addPort( name, WDLConstants.INPUT_TYPE, position, (Compartment)output );
                outputCount.put( output.getName(), position++ );
            }
            {
                String name = edgeInfo.attributes.get( "label" );
                if( name != null )
                {
                    WorkflowUtil.setName( output, name );
                    output.setTitle( name );
                }
            }
            String name = input.getName() + "->" + output.getName();
            Edge edge = new Edge( new Stub( null, name, WDLConstants.LINK_TYPE ), input, output );
            input.addEdge( edge );
            output.addEdge( edge );
            diagram.put( edge );
        }

        for( Compartment c : diagram.stream( Compartment.class ).filter( c -> WorkflowUtil.isCall( c ) || WorkflowUtil.isTask( c ) ) )
        {
            long inputs = c.stream( Node.class ).filter( n -> WorkflowUtil.isInput( n ) ).count();
            long outputs = c.stream( Node.class ).filter( n -> WorkflowUtil.isOutput( n ) ).count();
            long maxPorts = Math.max( inputs, outputs );
            int height = Math.max( 50, 24 * (int)maxPorts + 8 );
            c.setShapeSize( new Dimension( 200, height ) );
            c.getAttributes().add( new DynamicProperty( "innerNodesPortFinder", Boolean.class, true ) );
        }

        for( Node node : diagram.stream( Node.class ).filter( n -> WorkflowUtil.isExpression( n ) ) )
        {
            boolean noInputs = node.edges().anyMatch( e -> e.getInput().equals( nodes ) );
            boolean noOutputs = node.edges().anyMatch( e -> e.getInput().equals( nodes ) );
            if( noInputs )
                changeType( node, WDLConstants.OUTPUT_TYPE );
            else if( noOutputs )
                changeType( node, WDLConstants.INPUT_TYPE );
        }
    }

    private static void changeType(Node node, String type) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( node );
        String name = node.getName();
        Node newNode = new Node( diagram, name, new Stub( null, name, type ) );
        WorkflowUtil.setName( newNode, name );
        WorkflowUtil.setType( newNode, "File" );
        WorkflowUtil.setExpression( newNode, "" );
        node.setTitle( name );
        node.setShapeSize( new Dimension( 80, 60 ) );
        for( Edge edge : node.edges() )
        {
            if( edge.getInput().equals( node ) )
                edge.setInput( newNode );
            else
                edge.setOutput( newNode );
            node.removeEdge( edge );
            diagram.remove( edge.getName() );
            newNode.addEdge( edge );
            diagram.put( edge );
        }
    }

    public static class DotParser
    {
        private Map<String, NodeInfo> nodes = new HashMap<>();
        private List<EdgeInfo> edges = new ArrayList<>();

        public void parse(String content)
        {
            content = content.replaceAll( "/\\*.*?\\*/", "" );
            List<String> lines = Arrays.stream( content.split( ";" ) ).map( String::strip ).filter( s -> !s.isEmpty() )
                    .collect( Collectors.toList() );

            for( String line : lines )
            {
                line = line.replaceAll( "[{}\"\\s\\t\\n]", "" ).strip();
                if( line.contains( "->" ) )
                {
                    parseEdge( line );
                }
                else if( line.contains( "[" ) )
                {
                    parseNode( line );
                }
            }
        }

        private void parseNode(String line)
        {
            Pattern nodePattern = Pattern.compile( "([a-zA-Z0-9_]+)\\s*\\[(.*?)\\]" );
            Matcher matcher = nodePattern.matcher( line );
            if( matcher.find() )
            {
                String nodeId = matcher.group( 1 );
                String attrsStr = matcher.group( 2 );

                // Parse all attributes: shape=point,label="",fixedsize=true,width=0.1
                Map<String, String> nodeData = new HashMap<>();
                Pattern attrPattern = Pattern.compile( "([a-zA-Z0-9_]+)=([^,\\]]+)" );
                Matcher attrMatcher = attrPattern.matcher( attrsStr );

                while( attrMatcher.find() )
                {
                    String key = attrMatcher.group( 1 );
                    String value = attrMatcher.group( 2 ).trim().replaceAll( "^[\"']|[\"']$", "" );
                    nodeData.put( key, value );
                }

                nodes.put( nodeId, new NodeInfo( nodeId, nodeData ) );
            }
        }

        private void parseEdge(String line)
        {
            // Match full edge: v0 -> v1 [label="name"]
            Pattern edgePattern = Pattern.compile( "([a-zA-Z0-9_]+)\\s*->\\s*([a-zA-Z0-9_]+)\\s*(\\[.*?\\])?" );
            Matcher matcher = edgePattern.matcher( line );
            if( matcher.find() )
            {
                String fromNode = matcher.group( 1 );
                String toNode = matcher.group( 2 );
                String attrs = matcher.group( 3 );

                Map<String, String> attributes = new HashMap<>();
                //                edge.put("from", fromNode);
                //                edge.put("to", toNode);

                if( attrs != null )
                {
                    // Parse all edge attributes
                    Pattern attrPattern = Pattern.compile( "([a-zA-Z0-9_]+)=([^,\\]]+)" );
                    Matcher attrMatcher = attrPattern.matcher( attrs );
                    while( attrMatcher.find() )
                    {
                        String key = attrMatcher.group( 1 );
                        String value = attrMatcher.group( 2 ).trim().replaceAll( "^[\"']|[\"']$", "" );
                        attributes.put( key, value );
                    }
                }
                edges.add( new EdgeInfo( fromNode, toNode, attributes ) );
            }
        }

        public Map<String, NodeInfo> getNodes()
        {
            return nodes;
        }
        public List<EdgeInfo> getEdges()
        {
            return edges;
        }
    }

    private static class EdgeInfo
    {
        String input;
        String output;
        Map<String, String> attributes;

        public EdgeInfo(String input, String output, Map<String, String> attributes)
        {
            this.input = input;
            this.output = output;
            this.attributes = attributes;
        }

        public String getInput()
        {
            return input;
        }

        public String getOutput()
        {
            return output;
        }

    }

    private static class NodeInfo
    {
        Map<String, String> attributes = new HashMap<>();
        String id;

        public NodeInfo(String id, Map<String, String> attributes)
        {
            this.id = id;
            this.attributes = attributes;
        }

        public Map<String, String> getAttributes()
        {
            return attributes;
        }

        public String getValue(String name)
        {
            return attributes.get( name );
        }
    }

    public static boolean isCall(NodeInfo nodeInfo)
    {
        if( nodeInfo.getAttributes().containsKey( "shape" ) )
            return false;
        return true;
    }
    //    dag" {
    //    rankdir=TB;
    //    v0 [shape=point,label="",fixedsize=true,width=0.1];
    //    v1 [label="two_steps:say_hello"];
    //    v0 -> v1 [label="name"];
    //
    //    v1 [label="two_steps:say_hello"];
    //    v3 [label="two_steps:ask_how_are_you"];
    //    v1 -> v3;
    //
    //    v2 [shape=point,label="",fixedsize=true,width=0.1];
    //    v3 [label="two_steps:ask_how_are_you"];
    //    v2 -> v3 [label="question"];
    //
    //    v3 [label="two_steps:ask_how_are_you"];
    //    v4 [shape=point];
    //    v3 -> v4 [label="final_message"];
    //
    //    }
}