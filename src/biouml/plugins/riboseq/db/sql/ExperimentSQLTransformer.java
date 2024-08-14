package biouml.plugins.riboseq.db.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;
import biouml.plugins.riboseq.db.DatabaseCollections;
import biouml.plugins.riboseq.db.model.CellSource;
import biouml.plugins.riboseq.db.model.Condition;
import biouml.plugins.riboseq.db.model.Experiment;
import biouml.plugins.riboseq.db.model.SequenceAdapter;
import biouml.plugins.riboseq.db.model.SequenceData;
import biouml.plugins.riboseq.db.model.SequenceData.Format;
import biouml.plugins.riboseq.db.model.SequencingPlatform;
import biouml.plugins.riboseq.db.model.Species;

public class ExperimentSQLTransformer extends PersistentSQLTransformer<Experiment>
{
    private DatabaseCollections dbCollections;
    @Override
    public Class<Experiment> getTemplateClass()
    {
        return Experiment.class;
    }
    
    @Override
    public boolean init(SqlDataCollection<Experiment> owner)
    {
        boolean result =  super.init( owner );
        dbCollections = DatabaseCollections.getInstanceForExperimentCollection( owner );
        return result;
    }

    @Override
    public Experiment create(ResultSet resultSet, Connection connection) throws Exception
    {
        Experiment result = super.create( resultSet, connection );
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
        
        result.setTranslationInhibition( resultSet.getString( "translation_inhibition" ) );
        result.setMinFragmentSize( resultSet.getInt( "min_fragment_size" ) );
        result.setMaxFragmentSize( resultSet.getInt( "max_fragment_size" ) );
        result.setDigestion( resultSet.getString( "digestion" ) );
        
        String sequenceAdapterId = resultSet.getString( "sequence_adapter_id" );
        if( sequenceAdapterId != null )
        {
            DataCollection<SequenceAdapter> adapterCollection = dbCollections.getAdapterCollection();
            result.setSequenceAdapter( adapterCollection.get( sequenceAdapterId ) );
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
        List<Integer> idList = SqlUtil.queryInts( connection, "SELECT pubmed_id FROM experiment_pubmed WHERE experiment_id=" + validateValue( experimentId ) );
        return idList.toArray( new Integer[idList.size()] );
    }
    
    private Condition[] fetchConditions(Connection connection, String experimentId)
    {
        return SqlUtil
                .stringStream( connection,
                        new Query( "SELECT condition_id FROM experiment_condition WHERE experiment_id=$id$" ).str( experimentId ) )
                .map( id -> DatabaseCollections.CONDITION_COLLECTION_PATH.getChildPath( id ).getDataElement( Condition.class ) )
                .toArray( Condition[]::new );
    }
    
    private SequenceData[] fetchSequenceData(Connection connection, String experimentId)
    {
        return SqlUtil.stream( connection,
                new Query( "SELECT sequence_data_id,format,url FROM sequence_data WHERE experiment_id=$id$" ).str( experimentId ), rs -> {
                    SequenceData sequenceData = new SequenceData();
                    sequenceData.setId( rs.getInt( 1 ) );
                    Format format = SequenceData.Format.valueOf( rs.getString( 2 ) );
                    sequenceData.setFormat( format );
                    sequenceData.setUrl( rs.getString( 3 ) );
                    return sequenceData;
                } ).toArray( SequenceData[]::new );
    }
    
    @Override
    public void addInsertCommands(Statement statement, Experiment exp) throws Exception
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
            query = new Query("INSERT INTO experiment_condition (experiment_id, condition_id) VALUES($experiment_id$,$condition_id$)")
                .str( "experiment_id", exp.getName() )
                .str( "condition_id", condition.getName() );
            statement.addBatch( query.get() );
        }
        
        for(int pubmed : exp.getPubMedIds())
        {
            query = new Query("INSERT INTO pubmed (pubmed_id) VALUES($id$) ON DUPLICATE KEY UPDATE pubmed_id=pubmed_id")
                .num( pubmed );
            statement.addBatch( query.get() );
            query = new Query("INSERT INTO experiment_pubmed (experiment_id, pubmed_id) VALUES($experiment_id$,$pubmed_id$)")
                .str( "experiment_id", exp.getName() )
                .num( "pubmed_id", pubmed );
            statement.addBatch( query.get() );
        }
        
        for(SequenceData sequenceData : exp.getSequenceData())
        {
            query = new Query("INSERT INTO sequence_data(experiment_id,format,url) VALUES($experiment_id$,$format$,$url$)")
                .str( "experiment_id", exp.getName() )
                .str( "format", sequenceData.getFormat().toString() )
                .str( "url", sequenceData.getUrl() );
            statement.addBatch( query.get() );
        }
    }

    @Override
    protected Query getInsertQuery(Experiment exp)
    {
        String template = "INSERT INTO experiment ("
                + "experiment_id,title,description,species_id,cell_source_id,translation_inhibition,"
                + "min_fragment_size,max_fragment_size,digestion,sequence_adapter_id,"
                + "sequencing_platform_id,sra_project_id,sra_experiment_id,"
                + "geo_series_id,geo_sample_id) VALUES("
                + "$experiment_id$,$title$,$description$,$species_id$,$cell_source_id$,$translation_inhibition$,"
                + "$min_fragment_size$,$max_fragment_size$,$digestion$,$sequence_adapter_id$,"
                + "$sequencing_platform_id$,$sra_project_id$,$sra_experiment_id$,"
                + "$geo_series_id$,$geo_sample_id$)";
        return new Query(template)
            .str( "experiment_id", exp.getName())
            .str( "title", exp.getTitle() )
            .str( "description", exp.getDescription() )
            .str( "species_id",  exp.getSpecies() == null ? null : exp.getSpecies().getName())
            .str( "cell_source_id", exp.getCellSource() == null ? null : exp.getCellSource().getName() )
            .str( "translation_inhibition", exp.getTranslationInhibition() )
            .num( "min_fragment_size", exp.getMinFragmentSize() )
            .num( "max_fragment_size", exp.getMaxFragmentSize() )
            .str( "digestion", exp.getDigestion() )
            .str( "sequence_adapter_id", exp.getSequenceAdapter() == null ? null : exp.getSequenceAdapter().getName() )
            .str( "sequencing_platform_id", exp.getSequencingPlatform() == null ? null : exp.getSequencingPlatform().getName() )
            .str( "sra_project_id", exp.getSraProjectId() )
            .str( "sra_experiment_id", exp.getSraExperimentId() )
            .str( "geo_series_id", exp.getGeoSeriesId() )
            .str( "geo_sample_id", exp.getGeoSampleId() );
    }
}
