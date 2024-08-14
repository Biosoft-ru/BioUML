package biouml.plugins.ensembl.access;

import java.beans.PropertyDescriptor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import biouml.plugins.ensembl.type.EnsemblMapAsVector;
import biouml.plugins.ensembl.type.EnsemblSequence;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.graph.GraphAlgorithms;
import ru.biosoft.util.LazyValue;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.StaticDescriptor;

/**
 * Sequence SQL transformer for ensembl.
 */
public class EnsemblSequenceTransformer extends SqlTransformerSupport<AnnotatedSequence>
{
    protected static final Logger log = Logger.getLogger( EnsemblSequenceTransformer.class.getName() );

    public static final String COORD_SYSTEM_KEY = "coordSystemId";
    public static final String CHROMOSOMES_KEY = "chromosomes";
    public static final String CHROMOSOME_NUMBER = "chromosome";
    public static final String CHROMOSOME_ID = "chromosomeId";

    private static final PropertyDescriptor CHROMOSOME_NUMBER_DESCRIPTOR = StaticDescriptor.create(CHROMOSOME_NUMBER);
    private static final PropertyDescriptor CHROMOSOME_ID_DESCRIPTOR = StaticDescriptor.create(CHROMOSOME_ID);

    protected String constraints;

    @Override
    public boolean init(SqlDataCollection<AnnotatedSequence> owner)
    {
        super.init(owner);
        table = "seq_region";
        //idField = "seq_region_id";
        idField = "name";
        constraints = getConstraints(owner);
        return true;
    }
    @Override
    public void addInsertCommands(Statement statement, AnnotatedSequence de) throws Exception
    {
        throw new UnsupportedOperationException("Method 'addInsertCommand' is unavailable for EnsemblSequenceTransformer");
    }

    @Override
    public AnnotatedSequence create(ResultSet resultSet, Connection connection) throws Exception
    {
        int id = resultSet.getInt(1);
        List<Integer> coordSystemPath = pathFromChrToSeqLevelCoordSystem.get();
        EnsemblSequence sequence = new EnsemblSequence(id, resultSet.getInt(4), owner, coordSystemPath);
        AnnotatedSequence map = new EnsemblMapAsVector(resultSet.getString(2), owner, sequence, null);

        map.getProperties().add(new DynamicProperty(CHROMOSOME_NUMBER_DESCRIPTOR, String.class, resultSet.getString(2)));
        map.getProperties().add(new DynamicProperty(CHROMOSOME_ID_DESCRIPTOR, String.class, resultSet.getString(1)));
        fillSequenceProperties(id, map.getProperties(), connection);

        return map;
    }

    @Override
    public String getCountQuery()
    {
        return "SELECT COUNT(DISTINCT t." + idField + ") FROM " + table + " t " + constraints;
    }
    @Override
    public String getNameListQuery()
    {
        return "SELECT DISTINCT t." + idField + " FROM " + table + " t " + constraints
                + " ORDER BY " + idField;
    }

    @Override
    public String getElementExistsQuery(String name)
    {
        return getElementQuery(name);
    }

    public static String getConstraints(DataCollection<?> owner)
    {
        String coordSystemConstraints = getCoordSystemConstraints(owner);
        String chromosomeConstraints = getChromosomesConstraints(owner);
        if( coordSystemConstraints.isEmpty() && chromosomeConstraints.isEmpty() )
            return "";
        StringBuilder buffer = new StringBuilder();
        buffer.append(" WHERE ");
        if( coordSystemConstraints.isEmpty() )
            buffer.append(chromosomeConstraints);
        else if( chromosomeConstraints.isEmpty() )
            buffer.append(coordSystemConstraints);
        else
        {
            buffer.append("(");
            buffer.append(coordSystemConstraints);
            buffer.append(") AND (");
            buffer.append(chromosomeConstraints);
            buffer.append(")");
        }
        return buffer.toString();

    }

    public static String getCoordSystemConstraints(DataCollection<?> owner)
    {
        String coordSystems = owner.getInfo().getProperties().getProperty(COORD_SYSTEM_KEY);
        if( coordSystems == null || coordSystems.isEmpty() ) return "";
        return "coord_system_id IN ("+coordSystems+")";
    }

    protected static String getChromosomesConstraints(DataCollection<?> owner)
    {
        String chromosomesString = owner.getInfo().getProperties().getProperty(CHROMOSOMES_KEY);
        if( chromosomesString == null || chromosomesString.isEmpty() ) return "";
        String[] chromosomes = TextUtil.split( chromosomesString, ',' );
        return "name IN ('"+String.join("','", chromosomes)+"')";
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT t.seq_region_id, name, coord_system_id, length " + "FROM " + table + " t " + constraints;
    }

    @Override
    public Class<AnnotatedSequence> getTemplateClass()
    {
        return AnnotatedSequence.class;
    }

    protected void fillSequenceProperties(int sequenceId, DynamicPropertySet properties, Connection connection)
    {
        String query = "SELECT t.name,t.description,sr.value FROM seq_region_attrib sr,attrib_type t WHERE sr.seq_region_id=" + sequenceId
                + " AND sr.attrib_type_id=t.attrib_type_id";
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery( query ))
        {
            while( rs.next() )
            {
                DynamicProperty property = new DynamicProperty(rs.getString(1), rs.getString(2), String.class, rs.getString(3));
                properties.add(property);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not read properties for sequence " + sequenceId, e);
        }
    }

    private LazyValue<List<Integer>> pathFromChrToSeqLevelCoordSystem = new LazyValue<List<Integer>>() {
        @Override
        protected List<Integer> doGet() throws Exception {
            return findPathToSequenceLevelCoordSystem();
        }
    };

    private int getSequenceLevelCoordSystem() throws SQLException
    {
        String query = "SELECT coord_system_id FROM coord_system WHERE FIND_IN_SET('sequence_level',attrib)";
        Connection con = getConnection();
        try(Statement st = con.createStatement();
                ResultSet rs = st.executeQuery( query );)
        {
            if(rs.next())
                return rs.getInt( 1 );
        }
        throw new RuntimeException("Sequence level coord_system not found in ensembl mysql database");
    }

    private Map<Integer, List<Integer>> loadCoordSystemMapping() throws SQLException
    {
        String query = "SELECT sa.coord_system_id, sc.coord_system_id FROM assembly"
                + " JOIN seq_region sa ON (asm_seq_region_id=sa.seq_region_id)"
                + " JOIN seq_region sc on (cmp_seq_region_id=sc.seq_region_id)"
                + " GROUP BY sa.coord_system_id, sc.coord_system_id";
        Connection con = getConnection();
        try(Statement st = con.createStatement();
                ResultSet rs = st.executeQuery( query ))
        {
            Map<Integer, List<Integer>> result = new HashMap<>();
            while(rs.next())
            {
                int from = rs.getInt( 1 );
                int to = rs.getInt( 2 );
                result.computeIfAbsent( from, k->new ArrayList<>() ).add( to );
            }
            return result;
        }
    }

    private List<Integer> findPathToSequenceLevelCoordSystem() throws SQLException
    {
        int chrCoordSystem = getChromosomeCoordSystem();
        int seqLevelCoordSystem = getSequenceLevelCoordSystem();
        Map<Integer, List<Integer>> graph = loadCoordSystemMapping();
        List<Integer> path = GraphAlgorithms.findShortestPath( chrCoordSystem, seqLevelCoordSystem, graph );
        if(path.isEmpty())
            throw new RuntimeException("Can not map chromosome coord_system to sequence level coord_system");
        return path;
    }

    private int getChromosomeCoordSystem() throws SQLException
    {
        return Integer.parseInt( owner.getInfo().getProperty( EnsemblSequenceTransformer.COORD_SYSTEM_KEY ) );
    }

}
