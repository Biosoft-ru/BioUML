package biouml.plugins.riboseq.db;

import biouml.plugins.riboseq.db.model.CellSource;
import biouml.plugins.riboseq.db.model.Condition;
import biouml.plugins.riboseq.db.model.Experiment;
import biouml.plugins.riboseq.db.model.MRNAExperiment;
import biouml.plugins.riboseq.db.model.SequenceAdapter;
import biouml.plugins.riboseq.db.model.SequencingPlatform;
import biouml.plugins.riboseq.db.model.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public class DatabaseCollections
{
    public static final DataElementPath DEFAULT_DB_ROOT = DataElementPath.create( "databases/RiboSeqDB" );
    
    private final DataElementPath dbRoot;
    
    public DatabaseCollections(DataElementPath dbRoot)
    {
        this.dbRoot = dbRoot;
    }
    
    private static DatabaseCollections defaultInstance = new DatabaseCollections( DEFAULT_DB_ROOT );
    public static DatabaseCollections getDefaultInstance()
    {
        return defaultInstance;
    }
    
    public static DatabaseCollections getInstanceForExperimentCollection(DataCollection<?> dc)
    {
        DataElementPath dbRoot = DataElementPath.create( dc.getOrigin().getOrigin() );
        return new DatabaseCollections( dbRoot );
    }
    
    public static final DataElementPath EXPERIMENT_COLLECTION_PATH = DEFAULT_DB_ROOT.getChildPath( "Data", "experiments" );
    public static final DataElementPath CELL_COLLECTION_PATH = DEFAULT_DB_ROOT.getChildPath( "Data", "cells" );
    public static final DataElementPath ADAPTER_COLLECTION_PATH = DEFAULT_DB_ROOT.getChildPath( "Data", "adapters" );
    public static final DataElementPath CONDITION_COLLECTION_PATH = DEFAULT_DB_ROOT.getChildPath( "Data", "conditions" );
    public static final DataElementPath SEQUENCING_PLATFORM_COLLECTION_PATH = DEFAULT_DB_ROOT.getChildPath( "Data", "sequencing platforms" );
    public static final DataElementPath SPECIES_COLLECTION_PATH = DEFAULT_DB_ROOT.getChildPath( "Data", "species" );
    public static final DataElementPath MRNA_EXPERIMENT_COLLECTION_PATH = DEFAULT_DB_ROOT.getChildPath( "Data", "mRNA experiments" );
    
    
    public DataElementPath getExperimentCollectionPath()
    {
        return dbRoot.getChildPath( "Data", "experiments" );
    }
    public DataCollection<Experiment> getExperimentCollection()
    {
        return getExperimentCollectionPath().getDataCollection( Experiment.class );
    }
    
    public DataElementPath getMRNAExperimentCollectionPath()
    {
        return dbRoot.getChildPath( "Data", "mRNA experiments" );
    }
    public DataCollection<MRNAExperiment> getMRNAExperimentCollection()
    {
        return getExperimentCollectionPath().getDataCollection( MRNAExperiment.class );
    }
    
    public DataElementPath getCellCollectionPath()
    {
        return dbRoot.getChildPath( "Data", "cells" );
    }
    public DataCollection<CellSource> getCellCollection()
    {
        return getCellCollectionPath().getDataCollection( CellSource.class );
    }
    
    public DataElementPath getAdapterCollectionPath()
    {
        return dbRoot.getChildPath( "Data", "adapters" );
    }
    public DataCollection<SequenceAdapter> getAdapterCollection()
    {
        return getAdapterCollectionPath().getDataCollection( SequenceAdapter.class );
    }
    
    public DataElementPath getConditionCollectionPath()
    {
        return dbRoot.getChildPath( "Data", "conditions" );
    }
    public DataCollection<Condition> getConditionCollection()
    {
        return getConditionCollectionPath().getDataCollection( Condition.class );
    }
    
    public DataElementPath getSequencingPlatformCollectionPath()
    {
        return dbRoot.getChildPath( "Data", "sequencing platforms" );
    }
    public DataCollection<SequencingPlatform> getSequencingPlatformCollection()
    {
        return getSequencingPlatformCollectionPath().getDataCollection( SequencingPlatform.class );
    }
    
    public DataElementPath getSpeciesCollectionPath()
    {
        return dbRoot.getChildPath( "Data", "species" );
    }
    public DataCollection<Species> getSpeciesCollection()
    {
        return getSpeciesCollectionPath().getDataCollection( Species.class );
    }
}
