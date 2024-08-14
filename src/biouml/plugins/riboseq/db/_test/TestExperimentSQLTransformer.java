package biouml.plugins.riboseq.db._test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Scanner;

import biouml.plugins.riboseq.db.DatabaseCollections;
import biouml.plugins.riboseq.db.model.CellSource;
import biouml.plugins.riboseq.db.model.Condition;
import biouml.plugins.riboseq.db.model.Experiment;
import biouml.plugins.riboseq.db.model.SequenceAdapter;
import biouml.plugins.riboseq.db.model.SequenceData;
import biouml.plugins.riboseq.db.model.SequenceData.Format;
import biouml.plugins.riboseq.db.model.SequencingPlatform;
import biouml.plugins.riboseq.db.model.Species;
import biouml.plugins.riboseq.db.sql.ExperimentSQLTransformer;
import junit.framework.TestCase;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.access.sql.SqlUtil;

public class TestExperimentSQLTransformer extends TestCase
{
    @Override
    protected void setUp() throws Exception
    {
        CollectionFactory.createRepository( "../data/test/biouml/plugins/riboseq/data" );
        
        Connection con = Connectors.getConnection( "riboseq_test" );
        
        try(InputStream is = ExperimentSQLTransformer.class.getResourceAsStream( "schema.sql" );
             Scanner scanner = new Scanner( is, StandardCharsets.UTF_8.name() ))
        {
            scanner.useDelimiter( ";" );
            while(scanner.hasNext())
            {
                String query = scanner.next();
                query = query.trim();
                if(query.isEmpty())
                    continue;
                SqlUtil.execute( con, query );
            }
        }
        
    }
    
    public void testAddExperiment() throws Exception
    {
        Experiment e = createExperiment("EXP000001");
        DataCollection<Experiment> experiments = DatabaseCollections.getDefaultInstance().getExperimentCollection();
        experiments.put( e );
        
        assertEquals( 1, experiments.getSize() );
        assertEquals( "EXP000001", e.getName());
        
        experiments.release( e.getName() );
        e = experiments.get( e.getName() );
        checkExperiment( e );
    }
    
    private Experiment createExperiment(String name) throws Exception
    {
        DatabaseCollections dbCollections = DatabaseCollections.getDefaultInstance();
        Experiment e = new Experiment(dbCollections.getExperimentCollection(), name);
        e.setTitle( "Test experiment" );
        e.setDescription( "Experiment for testing" );
        
        DataCollection<Species> speciesCollection = dbCollections.getSpeciesCollection();
        Species species = new Species(speciesCollection, "SPC0001");
        species.setLatinName( "Homo sapiens" );
        species.setCommonName( "Human" );
        speciesCollection.put( species );
        e.setSpecies( species );
        
        DataCollection<CellSource> cellCollection = dbCollections.getCellCollection();
        CellSource cellSource = new CellSource(cellCollection, "CEL0001");
        cellSource.setTitle( "Test cell" );
        cellCollection.put( cellSource );
        e.setCellSource( cellSource  );
        
        e.setTranslationInhibition( "cycloheximide 5uM for 10 minutes" );
        e.setMinFragmentSize( 25 );
        e.setMaxFragmentSize( 35 );
        
        e.setDigestion( "RNAse for 5 minutes" );
        
        
        DataCollection<SequenceAdapter> adapterCollection = dbCollections.getAdapterCollection();
        SequenceAdapter sequenceAdapter = new SequenceAdapter(adapterCollection, "SQA0001");
        sequenceAdapter.setTitle( "test adapter" );
        sequenceAdapter.setSequence( "ACGTACGT" );
        adapterCollection.put( sequenceAdapter );
        e.setSequenceAdapter( sequenceAdapter  );
        
        DataCollection<Condition> conditionCollection = dbCollections.getConditionCollection();
        Condition treatmentCondition = new Condition(conditionCollection, "COND00001");
        treatmentCondition.setDescription( "treatment with some drug" );
        conditionCollection.put( treatmentCondition );
        Condition knockDownCondition = new Condition(conditionCollection, "COND00002");
        knockDownCondition.setDescription( "TP53 gene knockout" );
        conditionCollection.put( knockDownCondition );
        Condition[] conditions = new Condition[] {treatmentCondition, knockDownCondition};
        e.setConditions( conditions  );
        
        DataCollection<SequencingPlatform> spCollection = dbCollections.getSequencingPlatformCollection();
        SequencingPlatform sequencingPlatform = new SequencingPlatform(spCollection, "SQP0001");
        sequencingPlatform.setTitle( "Illumina HiSeq 2000" );
        spCollection.put( sequencingPlatform );
        e.setSequencingPlatform( sequencingPlatform  );
        
        SequenceData sd1 = new SequenceData();
        sd1.setFormat( Format.FASTQ );
        sd1.setUrl( "ftp://example.com/example.fastq.gz" );
        SequenceData sd2 = new SequenceData();
        sd2.setFormat( Format.SRA );
        sd2.setUrl( "http://example.com/example.sra" );
        SequenceData[] sequenceData = new SequenceData[] { sd1, sd2 };
        e.setSequenceData( sequenceData  );
        
        e.setSraProjectId( "SRP028325" );
        e.setSraExperimentId( "SRX329061" );
        
        e.setGeoSeriesId( "GSE49339" );
        e.setGeoSampleId( "GSM1197614" );
        
        e.setPubMedIds( new Integer[] {23453015, 24284625} );
        return e;
    }
    
    private void checkExperiment(Experiment e) throws Exception
    {
        assertEquals( "Test experiment", e.getTitle());
        assertEquals( "Experiment for testing", e.getDescription() );
        assertEquals( "Homo sapiens", e.getSpecies().getLatinName() );
        assertEquals( "Test cell", e.getCellSource().getTitle() );
        assertEquals( "cycloheximide 5uM for 10 minutes", e.getTranslationInhibition() );
        assertEquals( 25, e.getMinFragmentSize() );
        assertEquals( 35, e.getMaxFragmentSize() );
        assertEquals( "RNAse for 5 minutes" , e.getDigestion() );
        assertEquals( "test adapter", e.getSequenceAdapter().getTitle() );
        assertEquals( "ACGTACGT", e.getSequenceAdapter().getSequence() );
        Condition[] conditions = e.getConditions();
        assertEquals( 2, conditions.length );
        assertEquals( "treatment with some drug", conditions[0].getDescription() );
        assertEquals( "TP53 gene knockout", conditions[1].getDescription() );
        assertEquals("Illumina HiSeq 2000", e.getSequencingPlatform().getTitle());
        SequenceData[] sequenceData = e.getSequenceData();
        assertEquals(2, sequenceData.length);
        assertEquals("ftp://example.com/example.fastq.gz", sequenceData[0].getUrl());
        assertEquals(Format.FASTQ, sequenceData[0].getFormat());
        assertEquals("http://example.com/example.sra", sequenceData[1].getUrl());
        assertEquals(Format.SRA, sequenceData[1].getFormat());
        assertEquals( "SRP028325", e.getSraProjectId() );
        assertEquals( "SRX329061", e.getSraExperimentId() );
        assertEquals( "GSE49339", e.getGeoSeriesId() );
        assertEquals( "GSM1197614", e.getGeoSampleId() );
        Integer[] pubMedIds = e.getPubMedIds();
        assertEquals(2, pubMedIds.length);
        assertEquals( (Integer)23453015, pubMedIds[0] );
        assertEquals( (Integer)24284625, pubMedIds[1] );
    }
   
}
