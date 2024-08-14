package biouml.plugins.gtrd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.SqlTransformerSupport;

public abstract class DNaseLikeExperimentSQLTransformer<E extends Experiment> extends SqlTransformerSupport<E>
{
    private static final Logger log = Logger.getLogger(DNaseLikeExperimentSQLTransformer.class.getName());
    
    protected String hubPrefix;
    
    public DNaseLikeExperimentSQLTransformer(String table, String hubPrefix)
    {
        this.table = table;
        this.hubPrefix = hubPrefix;
    }
    
    public abstract Class<E> getTemplateClass();
    protected abstract E createExperiment(String id);
    
    
    @Override
    public void addInsertCommands(Statement statement, E exp) throws Exception
    {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        throw new UnsupportedOperationException();
    }
    
    
    @Override
    public E create(ResultSet resultSet, Connection connection) throws Exception
    {
        String id = resultSet.getString(1);
        String specieName = resultSet.getString(2);
        String treatment = resultSet.getString(3);
        String cellLine = resultSet.getString(4);     

        Species specie = Species.getSpecies(specieName);
        if( specie == null )
            throw new IllegalArgumentException("Invalid specie " + specieName + " for " + id);

        DataElementPath cellsCollectionPath = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_CELLS );
        CellLine cell = cellsCollectionPath.getChildPath( cellLine ).optDataElement( CellLine.class );
        
        if(cell == null)
        {
            log.log( Level.SEVERE, "Cell line(cell_id="+cellLine+") for " + id + " not found, experiment will be skipped" );
            return null;
        }
        
        E exp = createExperiment( id );
        exp.setSpecie( specie );
        exp.setCell( cell );
        exp.setTreatment( treatment );

        initProperties(exp, connection );
        initExperimentDataElements( exp, connection );
        initExternalRefs( exp, connection );

        return exp;
    }

    private static final String COLUMNS = "id,organism,treatment,cell_id";
    @Override
    public String getSelectQuery()
    {
        return "SELECT " + COLUMNS + " FROM " + table;
    }
    
    @Override
    public String getElementQuery(String name)
    {
        return "SELECT " + COLUMNS + " FROM "+table+" WHERE id=" + validateValue(name);
    }

    private void initExperimentDataElements(E exp, Connection con) throws Exception
    {
        DataElementPathSet reads = new DataElementPathSet();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT input FROM hub WHERE input_type='"+hubPrefix+"Reads' AND output_type='"+hubPrefix+"Experiment' AND output=?" ))
        {
            ps.setString(1, exp.getName());
            try (ResultSet rs = ps.executeQuery())
            {
                while( rs.next() )
                {
                    String readId = rs.getString( 1 );
                    exp.getReadsIds().add( readId );
                    reads.add( DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_READS ).getChildPath( readId + ".fastq.gz" ) );
                }
                if( reads.isEmpty() )
                {
                    log.warning( "No reads for " + exp.getName() );
                    //throw new Exception("No reads for " + exp.getName());
                }
            }
        }
        exp.setReads( reads );

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT input FROM hub WHERE input_type='"+hubPrefix+"Alignments' AND output_type='"+hubPrefix+"Experiment' AND output=?" ))
        {
            ps.setString(1, exp.getName());
            try (ResultSet rs = ps.executeQuery())
            {
                if( rs.next() )
                    exp.setAlignmentId( rs.getString( 1 ) );
                else
                    throw new Exception( "No alignment for " + exp.getName() );
                if( rs.next() )
                    throw new Exception( "Multiple alignments for " + exp.getName() );
            }
        }

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT input FROM hub WHERE input_type='"+hubPrefix+"Peaks' AND output_type='"+hubPrefix+"Experiment' AND output=?" ))
        {
            ps.setString(1, exp.getName());
            try (ResultSet rs = ps.executeQuery())
            {
                if( rs.next() )
                {
                	exp.setPeakId( rs.getString( 1 ) );
                }
                if( rs.next() )
                    throw new Exception( "Multiple peaks for " + exp.getName() );
            }
        }
    }

    private void initExternalRefs(E exp, Connection con) throws SQLException
    {
        try(PreparedStatement ps =con.prepareStatement( "SELECT external_db, external_db_id FROM external_refs WHERE id=?" ))
        {
            ps.setString( 1, exp.getName() );
            try( ResultSet rs = ps.executeQuery() )
            {
                while( rs.next() )
                    exp.getExternalRefs().add( new ExternalReference( rs.getString( 1 ), rs.getString( 2 ) ) );
            }
        }
    }

    private void initProperties(E exp, Connection con) throws SQLException
    {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT input, property_name, property_value FROM "+table+" JOIN hub on("+table+".id=output)"
                        + " JOIN properties on(properties.id=input) WHERE "+table+".id = ?" ))
        {
            ps.setString( 1, exp.getName() );
            try( ResultSet rs = ps.executeQuery() )
            {
                while( rs.next() )
                    exp.setElementProperty( rs.getString( 1 ), rs.getString( 2 ), rs.getString( 3 ) );
            }
        }
    }
}
