package biouml.plugins.riboseq.db.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import biouml.plugins.riboseq.db.DatabaseCollections;
import biouml.plugins.riboseq.db.model.CellSource;
import biouml.plugins.riboseq.db.model.Condition;
import biouml.plugins.riboseq.db.model.MRNAExperiment;
import biouml.plugins.riboseq.db.model.SequenceData;
import biouml.plugins.riboseq.db.model.SequenceData.Format;
import biouml.plugins.riboseq.db.model.SequencingPlatform;
import biouml.plugins.riboseq.db.model.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;

public class MRNAExperimentSQLTransformer extends PersistentSQLTransformer<MRNAExperiment>
{
    private DatabaseCollections dbCollections;
    
    public MRNAExperimentSQLTransformer()
    {
        table = "mrna_experiment";
        idField = "mrna_experiment_id";
    }
    
    @Override
    public Class<MRNAExperiment> getTemplateClass()
    {
        return MRNAExperiment.class;
    }
    
    @Override
    public boolean init(SqlDataCollection<MRNAExperiment> owner)
    {
        boolean result =  super.init( owner );
        dbCollections = DatabaseCollections.getInstanceForExperimentCollection( owner );
        return result;
    }

    @Override
    public MRNAExperiment create(ResultSet resultSet, Connection connection) throws Exception
    {
        MRNAExperiment result = super.create( resultSet, connection );
        result.setTitle( resultSet.getString( "title" ) );
        result.setDescription( resultSet.getString( "description" ) );
        
        String speciesId = resultSet.getString( "species_id" );
        if(speciesId != null)
        {
            DataCollection<Species> speciesCollection = dbCollections.getSpeciesCollection();
            result.setSpecies( speciesCollection.get( speciesId ) );
        }
        
        String cellSourceId = resultSet.getString( "cell_source_id" );
        if( cellSourceId != null )
        {
            DataCollection<CellSource> cellCollection = dbCollections.getCellCollection();
            result.setCellSource( cellCollection.get( cellSourceId ) );
        }
        
        String sequencingPlatformId = resultSet.getString( "sequencing_platform_id" );
        if( sequencingPlatformId != null )
        {
            DataCollection<SequencingPlatform> sequencingPlatformCollection = dbCollections.getSequencingPlatformCollection();
            result.setSequencingPlatform( sequencingPlatformCollection.get( sequencingPlatformId ) );
        }
        
        result.setSraProjectId( resultSet.getString( "sra_project_id" ) );
        result.setSraExperimentId( resultSet.getString( "sra_experiment_id" ) );
        result.setGeoSeriesId( resultSet.getString( "geo_series_id" ) );
        result.setGeoSampleId( resultSet.getString( "geo_sample_id" ) );
        
        result.setPubMedIds( fetchPubmedIds(connection, result.getName()) );
        result.setConditions( fetchConditions(connection, result.getName()) );
        result.setSequenceData( fetchSequenceData(connection, result.getName()) );
        
        return result;
    }
    
    private Integer[] fetchPubmedIds(Connection connection, String experimentId)
    {
        List<Integer> idList = SqlUtil.queryInts( connection, "SELECT pubmed_id FROM mrna_experiment_pubmed WHERE mrna_experiment_id=" + validateValue( experimentId ) );
        return idList.toArray( new Integer[idList.size()] );
    }
    
    private Condition[] fetchConditions(Connection connection, String experimentId)
    {
        return SqlUtil
                .stringStream( connection,
                        new Query( "SELECT condition_id FROM mrna_experiment_condition WHERE mrna_experiment_id=$id$" ).str( experimentId ) )
                .map( id -> DatabaseCollections.CONDITION_COLLECTION_PATH.getChildPath( id ).getDataElement( Condition.class ) )
                .toArray( Condition[]::new );
    }
    
    private SequenceData[] fetchSequenceData(Connection connection, String experimentId)
    {
        return SqlUtil.stream( connection,
                new Query( "SELECT mrna_sequence_data_id,format,url FROM mrna_sequence_data WHERE mrna_experiment_id=$id$" ).str( experimentId ), rs -> {
                    SequenceData sequenceData = new SequenceData();
                    sequenceData.setId( rs.getInt( 1 ) );
                    Format format = SequenceData.Format.valueOf( rs.getString( 2 ) );
                    sequenceData.setFormat( format );
                    sequenceData.setUrl( rs.getString( 3 ) );
                    return sequenceData;
                } ).toArray( SequenceData[]::new );
    }
    
    @Override
    public void addInsertCommands(Statement statement, MRNAExperiment exp) throws Exception
    {
        Query query = new Query("INSERT INTO sra_project (sra_project_id) VALUES($id$) ON DUPLICATE KEY UPDATE sra_project_id=sra_project_id")
            .str( exp.getSraProjectId() );
        statement.addBatch( query.get() );
        
        query = new Query("INSERT INTO sra_experiment (sra_experiment_id) VALUES($id$) ON DUPLICATE KEY UPDATE sra_experiment_id=sra_experiment_id")
            .str( exp.getSraExperimentId() );
        statement.addBatch( query.get() );
        
        query = new Query("INSERT INTO geo_series (geo_series_id) VALUES($id$) ON DUPLICATE KEY UPDATE geo_series_id=geo_series_id")
            .str( exp.getGeoSeriesId() );
        statement.addBatch( query.get() );
        
        query = new Query("INSERT INTO geo_sample (geo_sample_id) VALUES($id$) ON DUPLICATE KEY UPDATE geo_sample_id=geo_sample_id")
            .str( exp.getGeoSampleId() );
        statement.addBatch( query.get() );
        
        super.addInsertCommands( statement, exp );
        
        for(Condition condition : exp.getConditions())
        {
            query = new Query("INSERT INTO mrna_experiment_condition (mrna_experiment_id, condition_id) VALUES($experiment_id$,$condition_id$)")
                .str( "experiment_id", exp.getName() )
                .str( "condition_id", condition.getName() );
            statement.addBatch( query.get() );
        }
        
        for(int pubmed : exp.getPubMedIds())
        {
            query = new Query("INSERT INTO pubmed (pubmed_id) VALUES($id$) ON DUPLICATE KEY UPDATE pubmed_id=pubmed_id")
                .num( pubmed );
            statement.addBatch( query.get() );
            query = new Query("INSERT INTO mrna_experiment_pubmed (mrna_experiment_id, pubmed_id) VALUES($experiment_id$,$pubmed_id$)")
                .str( "experiment_id", exp.getName() )
                .num( "pubmed_id", pubmed );
            statement.addBatch( query.get() );
        }
        
        for(SequenceData sequenceData : exp.getSequenceData())
        {
            query = new Query("INSERT INTO mrna_sequence_data(mrna_experiment_id,format,url) VALUES($experiment_id$,$format$,$url$)")
                .str( "experiment_id", exp.getName() )
                .str( "format", sequenceData.getFormat().toString() )
                .str( "url", sequenceData.getUrl() );
            statement.addBatch( query.get() );
        }
    }

    @Override
    protected Query getInsertQuery(MRNAExperiment exp)
    {
        String template = "INSERT INTO mrna_experiment ("
                + "mrna_experiment_id,title,description,species_id,cell_source_id,"
                + "sequencing_platform_id,sra_project_id,sra_experiment_id,"
                + "geo_series_id,geo_sample_id) VALUES("
                + "$experiment_id$,$title$,$description$,$species_id$,$cell_source_id$,"
                + "$sequencing_platform_id$,$sra_project_id$,$sra_experiment_id$,"
                + "$geo_series_id$,$geo_sample_id$)";
        return new Query(template)
            .str( "experiment_id", exp.getName())
            .str( "title", exp.getTitle() )
            .str( "description", exp.getDescription() )
            .str( "species_id",  exp.getSpecies() == null ? null : exp.getSpecies().getName())
            .str( "cell_source_id", exp.getCellSource() == null ? null : exp.getCellSource().getName() )
            .str( "sequencing_platform_id", exp.getSequencingPlatform() == null ? null : exp.getSequencingPlatform().getName() )
            .str( "sra_project_id", exp.getSraProjectId() )
            .str( "sra_experiment_id", exp.getSraExperimentId() )
            .str( "geo_series_id", exp.getGeoSeriesId() )
            .str( "geo_sample_id", exp.getGeoSampleId() );
    }
}
