package biouml.plugins.gtrd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlUtil;
import biouml.standard.type.Species;

public class HistonesExperimentSQLTransformer extends SqlTransformerSupport<HistonesExperiment>
{
    private static final Logger log = Logger.getLogger(HistonesExperimentSQLTransformer.class.getName());
    // Temporary 1 !!!!!!!!!!!!!
    //public static final String DEFAULT_GTRD_ROOT = "databases/GTRD/Data";
    public static final String DEFAULT_GTRD_ROOT = "databases/GTRD_20.06/Data";
    
    public static final String DEFAULT_GTRD_READS = DEFAULT_GTRD_ROOT + "/sequences";
    public static final String DEFAULT_GTRD_ALIGNMENTS = DEFAULT_GTRD_ROOT + "/alignments";
    public static final String DEFAULT_GTRD_PEAKS = DEFAULT_GTRD_ROOT + "/peaks/Histone Modifications/macs2";

    // Temporary 2 !!!!!!!!!!!!!
    //public static final String DEFAULT_GTRD_CELLS = "databases/GTRD/Dictionaries/cells";
    public static final String DEFAULT_GTRD_CELLS = DEFAULT_GTRD_ROOT + "/Dictionaries/cells";
    
    private String gtrdReads;
    private String gtrdAlignments;
    private String gtrdPeaks;
    private String gtrdCells;
    
    private ArticleCollection articles;
    
    @Override
    public boolean init(SqlDataCollection<HistonesExperiment> owner)
    {
        this.table = "hist_experiments";
        String gtrdRoot = owner.getInfo().getProperty( "gtrd.root" );
        if( gtrdRoot == null )
            gtrdRoot = DEFAULT_GTRD_ROOT;
        gtrdReads = owner.getInfo().getProperty( "gtrd.reads" );
        if( gtrdReads == null )
            gtrdReads = DEFAULT_GTRD_READS;
        gtrdAlignments = owner.getInfo().getProperty( "gtrd.aligns" );
        if( gtrdAlignments == null )
            gtrdAlignments = gtrdRoot + "/alignments";
        gtrdPeaks = DEFAULT_GTRD_ALIGNMENTS;
        if( gtrdPeaks == null )
            gtrdPeaks = DEFAULT_GTRD_PEAKS;
        gtrdCells = owner.getInfo().getProperty( "gtrd.cells" );
        if( gtrdCells == null )
            gtrdCells = DEFAULT_GTRD_CELLS;
        
        try
        {
            articles = ArticleCollection.getInstance( owner.getConnection() );
        }
        catch( Exception e )
        {
            throw new RuntimeException(e);
        }
        return super.init(owner);
    }

    @Override
    public void addInsertCommands(Statement statement, HistonesExperiment exp) throws Exception
    {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public HistonesExperiment create(ResultSet resultSet, Connection connection) throws Exception
    {
        String id = resultSet.getString(1);
        String antibody = resultSet.getString(2);
        String target = resultSet.getString(3);
        String cellLine = resultSet.getString(4);
        String specieName = resultSet.getString(5);
        String treatment = resultSet.getString(6);
        String controlId = resultSet.getString(7);
        String expTypeStr = resultSet.getString( 8 );
        ExperimentType expType = ExperimentType.valueOf( expTypeStr.toUpperCase() );

        Species specie = Species.getSpecies(specieName);
        if( specie == null )
            throw new IllegalArgumentException("Invalid specie " + specieName + " for " + id);

        DataElementPath cellsCollectionPath = DataElementPath.create( gtrdCells );
        CellLine cell = cellsCollectionPath.exists()
                ? cellsCollectionPath.getChildPath( cellLine ).getDataElement( CellLine.class )
                : new CellLine( cellLine, cellLine, specie, null );
        
                HistonesExperiment exp = new HistonesExperiment(owner, id, antibody, target, cell, specie, treatment, controlId, expType);

        initExperimentDataElements( exp, connection );
        initProperties(exp, connection );
        initExternalRefs( exp, connection );
        initArticles(exp, connection);

        return exp;
    }
    
    private void initArticles(HistonesExperiment exp, Connection connection)
    {
        List<Article> articleList = new ArrayList<>();
        List<String> pubmedIds = exp.getExternalIDs( "PUBMED" );
        for(String pubmedId : pubmedIds)
        {
            Article article = articles.getArticleByPubmedId( pubmedId );
            if(article == null)
            {
                log.warning( "Article PMID:" + pubmedId + " not found in articles table" );
                continue;
            }
            articleList.add( article );
        }
        exp.setArticles( articleList );
    }

    private static final String COLUMNS = "id,antibody,target,cell_id,specie,treatment,control_id,experiment_type";
    @Override
    public String getSelectQuery()
    {
        return "SELECT " + COLUMNS + " FROM hist_experiments";
    }
    
    @Override
    public String getElementQuery(String name)
    {
        return "SELECT " + COLUMNS + " FROM hist_experiments WHERE id=" + validateValue(name);
    }

    private void initExperimentDataElements(HistonesExperiment exp, Connection con) throws Exception
    {
        DataElementPathSet reads = new DataElementPathSet();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT input FROM hub WHERE input_type='HistonesReads' AND output_type='HistonesExperiment' AND output=?" ))
        {
            ps.setString(1, exp.getName());
            try (ResultSet rs = ps.executeQuery())
            {
                while( rs.next() )
                {
                	exp.getReadsIds().add( rs.getString( 1 ) );
                    reads.add( DataElementPath.create( gtrdReads ).getChildPath( rs.getString( 1 ) + ".fastq.gz" ) );
                }
                if( reads.isEmpty() )
                {
                    log.warning( "No reads for " + exp.getName() );
                    //throw new Exception("No reads for " + exp.getName());
                }
            }
        }

        DataElementPath alignment = null;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT input FROM hub WHERE input_type='HistonesAlignments' AND output_type='HistonesExperiment' AND output=?" ))
        {
            ps.setString(1, exp.getName());
            try (ResultSet rs = ps.executeQuery())
            {
                if( rs.next() )
                {
                    alignment = DataElementPath.create( gtrdAlignments ).getChildPath( rs.getString( 1 ) + ".bam" );
                    exp.setAlignmentId( rs.getString( 1 ) );
                }
                else
                    throw new Exception( "No alignment for " + exp.getName() );
                if( rs.next() )
                    throw new Exception( "Multiple alignments for " + exp.getName() );
            }
        }

        DataElementPath peak = null;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT input FROM hub WHERE input_type='HistonesPeaks' AND output_type='HistonesExperiment' AND output=?" ))
        {
            ps.setString(1, exp.getName());
            try (ResultSet rs = ps.executeQuery())
            {
                if( rs.next() )
                {
                    peak = DataElementPath.create( gtrdPeaks ).getChildPath( rs.getString( 1 ) );
                    exp.setPeakId( rs.getString( 1 ) );
                }
                if( rs.next() )
                    throw new Exception( "Multiple peaks for " + exp.getName() );
            }
        }
        exp.setReads( reads );
        exp.setAlignment( alignment );
        exp.setPeak( peak );
    }

    private void initExternalRefs(HistonesExperiment exp, Connection con) throws SQLException
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

    private void initProperties(HistonesExperiment exp, Connection con) throws SQLException
    {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT input, property_name, property_value FROM hist_experiments JOIN hub use index(output_idx) on(hist_experiments.id=output)"
                        + " JOIN properties on(properties.id=input) WHERE hist_experiments.id = ?" ))
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
    public Class<HistonesExperiment> getTemplateClass()
    {
        return HistonesExperiment.class;
    }
}
