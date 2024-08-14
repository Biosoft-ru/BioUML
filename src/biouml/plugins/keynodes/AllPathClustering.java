package biouml.plugins.keynodes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.util.AddElementsUtils;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;

public class AllPathClustering extends ShortestPathClustering
{
    public AllPathClustering(DataCollection<?> origin, String name)
    {
        super( origin, name, new AllPathClusteringParameters() );
    }

    @Override
    protected Diagram fillDiagram(Diagram diagram, List<String> names, KeyNodesHub<?> bioHub, TargetOptions dbOptions, String[] relTypes,
            int maxRadius, int direction, boolean fullPath)
    {
        if( names.size() > parameters.getInputSizeLimit() )
        {
            log.warning( "Input table contains too many entries: first " + parameters.getInputSizeLimit() + " will be used" );
            names = names.subList( 0, parameters.getInputSizeLimit() );
        }
        Set<String> input = new HashSet<>( names );

        int cntNodes = 0;
        int totalNodes = names.size();
        for( String name : names )
        {
            input.remove( name );

            Element element1 = new Element( "stub/%//" + name );
            Element[] elements2 = StreamEx.of( input ).map( n -> new Element( "stub/%//" + n ) ).toArray( Element[]::new );
            try
            {
                List<Element> reactions = bioHub.getAllReactions( element1, elements2, dbOptions, relTypes, maxRadius, direction );
                AddElementsUtils.addNodesToCompartment( reactions.stream().toArray( Element[]::new ), diagram, null, null );
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "Error while creating shortest path graph for " + element1.getAccession(), e );
            }

            jobControl.setPreparedness( 3 + cntNodes * 70 / totalNodes );
            cntNodes++;
            if( direction != BioHub.DIRECTION_BOTH )
                input.add( name );
        }
        if( jobControl.isStopped() )
            return null;

        Node[] reactionNodes = diagram.getNodes();
        for( Node reactionNode : reactionNodes )
            AddReactantsAnalysis.addReactants( diagram, bioHub, parameters.getSpecies(), reactionNode, false );

        if( diagram.isEmpty() )
            return null;

        input = new HashSet<>( names );
        for( Node c : diagram.stream( Node.class ) )
        {
            if( input.contains( c.getName() ) )
            {
                c.setPredefinedStyle( KeyNodeConstants.HIT_HIGHLIGHT );
            }
        }

        diagram.setView( null );
        jobControl.setPreparedness( 80 );
        if( jobControl.isStopped() )
            return null;
        return diagram;
    }

    @SuppressWarnings ( "serial" )
    @PropertyName ( "Parameters" )
    public static class AllPathClusteringParameters extends ShortestPathClusteringParameters
    {
        public AllPathClusteringParameters()
        {
            setUseFullPath( true );
        }
    }

    public static class AllPathClusteringParametersBeanInfo extends ShortestPathClusteringParametersBeanInfo
    {
        public AllPathClusteringParametersBeanInfo()
        {
            super( AllPathClusteringParameters.class );
        }
        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            properties.removeIf( pd -> ( pd.getName().equals( "useFullPath" ) || pd.getName().equals( "outputPath" ) ) );
            property( "outputPath" ).outputElement( FolderCollection.class ).auto( "$sourcePath$ all paths $direction$ $maxRadius$" ).add();
        }
    }
}
