package biouml.plugins.gtrd.legacy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.Logger;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.ExternalReference;
import biouml.standard.type.Species;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;

public class ExperimentSQLTransformer extends SqlTransformerSupport<Experiment>
{
    private static final Logger log = Logger.getLogger(ExperimentSQLTransformer.class.getName());
    
 // GTRD dir temporarly changed to GTRD_20.06
    public static final String DEFAULT_GTRD_ROOT = "data/Collaboration/GTRD_20.06/Data";
    public static final String DEFAULT_GTRD_READS = DEFAULT_GTRD_ROOT + "/sequences";
    public static final String DEFAULT_GTRD_ALIGNMENTS = DEFAULT_GTRD_ROOT + "/alignments";
    public static final String DEFAULT_GTRD_PEAKS = DEFAULT_GTRD_ROOT + "/peaks";
    public static final String DEFAULT_GTRD_CELLS = "databases/GTRD_20.06/Dictionaries/cells";

    private String gtrdReads;
    private String gtrdAlignments;
    private String gtrdPeaks;
    private String gtrdCells;
    
    @Override
    public boolean init(SqlDataCollection<Experiment> owner)
    {
        this.table = "chip_experiments";
        String gtrdRoot = owner.getInfo().getProperty( "gtrd.root" );
        if( gtrdRoot == null )
            gtrdRoot = DEFAULT_GTRD_ROOT;
        gtrdReads = owner.getInfo().getProperty( "gtrd.reads" );
        if( gtrdReads == null )
            gtrdReads = gtrdRoot + "/sequences";
        gtrdAlignments = owner.getInfo().getProperty( "gtrd.aligns" );
        if( gtrdAlignments == null )
            gtrdAlignments = gtrdRoot + "/alignments";
        gtrdPeaks = owner.getInfo().getProperty( "gtrd.peaks" );
        if( gtrdPeaks == null )
            gtrdPeaks = gtrdRoot + "/peaks";
        gtrdCells = owner.getInfo().getProperty( "gtrd.cells" );
        if( gtrdCells == null )
            gtrdCells = DEFAULT_GTRD_CELLS;
        return super.init(owner);
    }

    @Override
    public void addInsertCommands(Statement statement, Experiment exp) throws Exception
    {
        String[] values = new String[] {
                exp.getName(),
                exp.getAntibody(),
                exp.getTfClassId(),
                exp.getCell().getName(),
                exp.getSpecie().getLatinName(),
                exp.getTreatment(),
                exp.getControlId() };

        statement.addBatch( StreamEx.of( values ).map( this::validateValue ).joining( ",", "INSERT INTO chip_experiments VALUES(", ")" ) );
        
        if(exp.getReads() != null)
            for(DataElementPath readPath : exp.getReads())
            {
                String readId = readPath.getName();
                readId = readId.substring(0, readId.indexOf('.'));

                for( Map.Entry<String, String> e : exp.getElementProperties( readId ).entrySet() )
                {
                    String propertyName = e.getKey();
                    String propertyValue = e.getValue();
                    statement.addBatch( "INSERT INTO properties VALUES(" + validateValue( readId ) + "," + validateValue( propertyName )
                            + "," + validateValue( propertyValue ) + ")" );
                }

                readId = validateValue(readId);
                //statement.addBatch("INSERT INTO data_elements VALUES(" + readId + ",'ReadsGTRDType')");
                statement.addBatch("INSERT INTO hub VALUES(" + readId + ",'ReadsGTRDType'," + values[0] + ",'ExperimentGTRDType'," + values[4] + ")");
            }
        if(exp.getAlignment() != null)
        {
            String alignmentId = validateValue(exp.getAlignment().getName());
            //statement.addBatch("INSERT INTO data_elements VALUES(" + alignmentId + ",'AlignmentsGTRDType')");
            statement.addBatch("INSERT INTO hub VALUES(" + alignmentId + ",'AlignmentsGTRDType'," + values[0] + ",'ExperimentGTRDType'," + values[4] + ")");
        }
        if(exp.getPeak() != null)
        {
            String peakId = validateValue(exp.getPeak().getName());
            //statement.addBatch("INSERT INTO data_elements VALUES(" + peakId + ",'AlignmentsGTRDType')");
            statement.addBatch("INSERT INTO hub VALUES(" + peakId + ",'PeaksGTRDType'," + values[0] + ",'ExperimentGTRDType'," + values[4] + ")");
        }
        
        for( ExternalReference ref : exp.getExternalRefs() )
            statement.addBatch( "INSERT INTO external_refs VALUES(" + values[0] + "," + validateValue( ref.getExternalDB() ) + ","
                    + validateValue( ref.getId()) + ")" );

    }
    
    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        statement.addBatch( "DELETE chip_experiments, hub, properties, external_refs"
                         + " FROM chip_experiments LEFT JOIN hub on(chip_experiments.id=hub.output) LEFT JOIN properties on(properties.id=hub.input) LEFT JOIN external_refs on(chip_experiments.id=external_refs.id)"
                         + " WHERE hub.output_type='ExperimentGTRDType' AND chip_experiments.id=" + validateValue( name ) );
    }

    @Override
    public Experiment create(ResultSet resultSet, Connection connection) throws Exception
    {
        String id = resultSet.getString(1);
        String antibody = resultSet.getString(2);
        String tfClassId = resultSet.getString(3);
        String cellLine = resultSet.getString(4);
        String specieName = resultSet.getString(5);
        String treatment = resultSet.getString(6);
        String controlId = resultSet.getString(7);

        Species specie = Species.getSpecies(specieName);
        if( specie == null )
            throw new IllegalArgumentException("Invalid specie " + specieName + " for " + id);

        DataElementPath cellsCollectionPath = DataElementPath.create( gtrdCells );
        CellLine cell = cellsCollectionPath.exists()
                ? cellsCollectionPath.getChildPath( cellLine ).getDataElement( CellLine.class )
                : new CellLine( cellLine, cellLine, specie, null );
        
        Experiment exp = new Experiment(owner, id, antibody, tfClassId, cell, specie, treatment, controlId);

        initExperimentDataElements( exp, connection );
        initProperties(exp, connection );
        initExternalRefs( exp, connection );
        initTFInfo(exp, connection);

        return exp;
    }
    
    private void initTFInfo(Experiment exp, Connection connection) throws BiosoftSQLException
    {
        if( exp.getTfClassId() != null )
        {
            String tfTitle = SqlUtil.queryString( connection, "SELECT title FROM classification WHERE name=" + SqlUtil.quoteString( exp.getTfClassId() ) );
            exp.setTfTitle( tfTitle );
        }
    }

    private static final String COLUMNS = "id,antibody,tfClassId,cell_id,specie,treatment,control_id";
    @Override
    public String getSelectQuery()
    {
        return "SELECT " + COLUMNS + " FROM chip_experiments id";
    }
    
    @Override
    public String getElementQuery(String name)
    {
        return "SELECT " + COLUMNS + " FROM chip_experiments WHERE id=" + validateValue(name);
    }

    private void initExperimentDataElements(Experiment exp, Connection con) throws Exception
    {
        DataElementPathSet reads = new DataElementPathSet();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT input FROM hub WHERE input_type='ReadsGTRDType' AND output_type='ExperimentGTRDType' AND output=?" ))
        {
            ps.setString(1, exp.getName());
            try (ResultSet rs = ps.executeQuery())
            {
                while( rs.next() )
                    reads.add( DataElementPath.create( gtrdReads ).getChildPath( rs.getString( 1 ) + ".fastq.gz" ) );
                if( reads.isEmpty() )
                {
                    log.warning( "No reads for " + exp.getName() );
                    //throw new Exception("No reads for " + exp.getName());
                }
            }
        }

        DataElementPath alignment = null;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT input FROM hub WHERE input_type='AlignmentsGTRDType' AND output_type='ExperimentGTRDType' AND output=?" ))
        {
            ps.setString(1, exp.getName());
            try (ResultSet rs = ps.executeQuery())
            {
                if( rs.next() )
                    alignment = DataElementPath.create( gtrdAlignments ).getChildPath( rs.getString( 1 ) );
                else
                    throw new Exception( "No alignment for " + exp.getName() );
                if( rs.next() )
                    throw new Exception( "Multiple alignments for " + exp.getName() );
            }
        }

        DataElementPath peak = null;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT input FROM hub WHERE input_type='PeaksGTRDType' AND output_type='ExperimentGTRDType' AND output=?" ))
        {
            ps.setString(1, exp.getName());
            try (ResultSet rs = ps.executeQuery())
            {
                if( rs.next() )
                    peak = DataElementPath.create( gtrdPeaks ).getChildPath( rs.getString( 1 ) );
                if( rs.next() )
                    throw new Exception( "Multiple peaks for " + exp.getName() );
            }
        }
        exp.setReads( reads );
        exp.setAlignment( alignment );
        exp.setPeak( peak );
    }

    private void initExternalRefs(Experiment exp, Connection con) throws SQLException
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

    private void initProperties(Experiment exp, Connection con) throws SQLException
    {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT input, property_name, property_value FROM chip_experiments JOIN hub on(chip_experiments.id=output)"
                        + " JOIN properties on(properties.id=input) WHERE chip_experiments.id = ?" ))
        {
            ps.setString( 1, exp.getName() );
            try( ResultSet rs = ps.executeQuery() )
            {
                while( rs.next() )
                    exp.setElementProperty( rs.getString( 1 ), rs.getString( 2 ), rs.getString( 3 ) );
            }
        }
    }

    @Override
    public Class<Experiment> getTemplateClass()
    {
        return Experiment.class;
    }
    
    private static volatile Map<String, AtomicInteger> nextIdByType;
    
    public static String getNextId(String elementType)
    {
        if(nextIdByType == null)
        {
            synchronized(ExperimentSQLTransformer.class)
            {
                if(nextIdByType == null)
                {
                    Map<String, AtomicInteger> localNextIdByType = new HashMap<>();
                    Connection con = SqlConnectionPool.getConnection( DataElementPath.create( DEFAULT_GTRD_ROOT ).getDataCollection() );
                    String lastIdString = SqlUtil.queryString( con, "SELECT MAX(id) FROM chip_experiments" );
                    int nextId = lastIdString == null ? 0 : Integer.parseInt( lastIdString.substring( "EXP".length() ) ) + 1;
                    localNextIdByType.put( "EXP", new AtomicInteger( nextId ) );
                    
                    lastIdString = SqlUtil.queryString( con, "SELECT MAX(input) FROM hub WHERE input_type='ReadsGTRDType'" );
                    nextId = lastIdString == null ? 0 : Integer.parseInt( lastIdString.substring( "READS".length() ) ) + 1;
                    localNextIdByType.put( "READS", new AtomicInteger( nextId ) );
                    
                    lastIdString = SqlUtil.queryString( con, "SELECT MAX(input) FROM hub WHERE input_type='AlignmentsGTRDType'" );
                    nextId = lastIdString == null ? 0 : Integer.parseInt( lastIdString.substring( "ALIGNS".length() ) ) + 1;
                    localNextIdByType.put( "ALIGNS", new AtomicInteger( nextId ) );
                    
                    lastIdString = SqlUtil.queryString( con, "SELECT MAX(input) FROM hub WHERE input_type='PeaksGTRDType'" );
                    nextId = lastIdString == null ? 0 : Integer.parseInt( lastIdString.substring( "PEAKS".length() ) ) + 1;
                    localNextIdByType.put( "PEAKS", new AtomicInteger( nextId ) );
                    nextIdByType = localNextIdByType;
                }
            }
        }
        int id = nextIdByType.get( elementType ).getAndIncrement();
        return String.format( elementType + "%06d", id );
    }
}