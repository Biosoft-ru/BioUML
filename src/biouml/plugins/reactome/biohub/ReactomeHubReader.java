package biouml.plugins.reactome.biohub;

import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;

import ru.biosoft.exception.InternalException;
import ru.biosoft.access.sql.SqlUtil;
import biouml.plugins.keynodes.graph.HubGraph;
import biouml.plugins.keynodes.graph.MemoryHubGraph;
import biouml.plugins.keynodes.graph.MemoryHubGraph.HubRelation;

public class ReactomeHubReader
{
    private static final String SQL_STOP_LIST = "SELECT input, _displayName, count(re2i.DB_ID) AS cnt FROM ReactionlikeEvent_2_input re2i "
            + "JOIN SimpleEntity se on se.DB_ID=re2i.input INNER JOIN DatabaseObject do ON do.DB_ID=se.DB_ID GROUP BY input "
            + "UNION SELECT output, _displayName, count(re2o.DB_ID) AS cnt FROM ReactionlikeEvent_2_output re2o "
            + "JOIN SimpleEntity se on se.DB_ID=re2o.output INNER JOIN DatabaseObject do ON do.DB_ID=se.DB_ID GROUP BY output";


    public HubGraph<ReactomeElement> readHub(Connection conn)
    {
        Set<Integer> stopList = createStopList( conn );

        AccessionIdMap accId = new AccessionIdMap(conn);

        Map<Integer, List<Rel>> reactions = SqlUtil.stream(
                conn,
                "SELECT db_id,input,1 FROM ReactionlikeEvent_2_input "
                        + "UNION SELECT db_id,output,2 FROM ReactionlikeEvent_2_output "
                        + "UNION SELECT rc.db_id,physicalEntity,3 FROM ReactionlikeEvent_2_catalystActivity rc "
                        + "JOIN CatalystActivity ca on (rc.catalystActivity=ca.db_id)",
                resultSet -> new Rel( resultSet.getInt( 1 ), resultSet.getInt( 2 ), resultSet.getInt( 3 ) ) ).groupingBy(
                rel -> rel.react );
        return StreamEx.ofValues( reactions ).remove(
                rels -> rels.stream().anyMatch( rel -> rel.type != Rel.MODIFIER && stopList.contains( rel.target ) ) )
                .flatMap(rels -> convertRelations(rels, accId))
                .collect(MemoryHubGraph.toMemoryHub());
    }

    private Set<Integer> createStopList(Connection conn)
    {
        Map<String, Set<Integer>> prefix2id = new HashMap<>();
        Map<String, Integer> prefix2cnt = new HashMap<>();
        Pattern p = Pattern.compile("(\\[[^\\]]+\\])$");
        SqlUtil.iterate( conn, SQL_STOP_LIST, rs ->
        {
            int id = rs.getInt( 1 );
            String name = rs.getString( 2 );
            int cnt = rs.getInt( 3 );
            String prefix = p.matcher(name).replaceAll("");
            prefix2cnt.compute( prefix, (k, v) -> ( v == null ? cnt : cnt + v ) );
            prefix2id.computeIfAbsent( prefix, k -> new HashSet<>() ).add( id );
        });
        Set<Integer> stopList = StreamEx.ofKeys( prefix2cnt, cnt -> cnt > 1000 ).flatCollection( prefix2id::get ).toSet();
        return stopList;
    }

    private Stream<HubRelation<ReactomeElement>> convertRelations(List<Rel> rels, AccessionIdMap accId)
    {
        String reactAcc = accId.getMoleculeAcc( rels.stream().findAny().get().react );
        if(reactAcc == null)
            return null;
        List<ReactomeElement> outputs = StreamEx.of( rels ).filter( rel -> rel.type == Rel.PRODUCT )
                .map( rel -> rel.createId( accId ) ).nonNull().toList();
        return rels.stream().filter( rel -> rel.type != Rel.PRODUCT )
            .flatMap( rel -> {
                ReactomeElement input = rel.createId( accId );
                if(input == null)
                    return null;
                return outputs.stream().map(output -> new HubRelation<>(input, output, new ReactomeRelation(reactAcc, rel.getType()), 1.0f));
            });
    }

    private static class Rel {
        static final int REACTANT = 1;
        static final int PRODUCT = 2;
        static final int MODIFIER = 3;
        int react;
        int target;
        int type;

        public Rel(int react, int target, int type)
        {
            this.react = react;
            this.target = target;
            this.type = type;
        }

        public String getType()
        {
            switch( type )
            {
                case REACTANT:
                    return "reactant";
                case PRODUCT:
                    return "product";
                case MODIFIER:
                    return "modifier";
                default:
                    throw new InternalException( "Unexpected type: " + type );
            }
        }

        public ReactomeElement createId(AccessionIdMap accId)
        {
            String acc = accId.getMoleculeAcc( target );
            return acc == null ? null : new ReactomeElement(acc);
        }
    }
}
