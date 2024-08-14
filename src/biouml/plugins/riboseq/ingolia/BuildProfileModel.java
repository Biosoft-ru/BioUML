package biouml.plugins.riboseq.ingolia;

import java.io.File;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.FileImporter;
import ru.biosoft.access.FileImporter.FileImportProperties;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TempFiles;
import biouml.plugins.riboseq.ingolia.svmlight.LearningOptions;
import biouml.plugins.riboseq.ingolia.svmlight.SVMLightTrain;
import biouml.plugins.riboseq.ingolia.svmlight.kernel.RadialBasisFunction;
import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.riboseq.transcripts.TranscriptLoader;

public class BuildProfileModel extends AnalysisMethodSupport<BuildProfileModelParameters>
{
    public BuildProfileModel(DataCollection<?> origin, String name)
    {
        super( origin, name, new BuildProfileModelParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        jobControl.pushProgress( 0, 10 );
        TranscriptLoader loader = parameters.getTranscriptSet().createTranscriptLoader();
        List<Transcript> transcripts = loader.loadTranscripts( log );

        TranscriptSetPartition transcriptSetPartition = partitionTranscripts( transcripts );
        jobControl.popProgress();

        jobControl.pushProgress( 10, 45 );
        File modelFile = learnSVM( transcriptSetPartition.getLearningSet() );
        storeModelFile( modelFile );
        jobControl.popProgress();

        jobControl.pushProgress( 45, 100 );
        ConfusionMatrix confusionMatrix = testSVM( modelFile, transcriptSetPartition.getTestingSet() );
        storeConfusionMatrix( confusionMatrix );
        jobControl.popProgress();

        return new Object[] {parameters.getModelFile().getDataElement(), parameters.getConfusionMatrix().getDataElement()};
    }

    private TranscriptSetPartition partitionTranscripts(List<Transcript> transcripts)
    {
        log.info( "Splitting transcript set into learning and testing sets" );
        TranscriptSetPartition transcriptSetPartition = new TranscriptSetPartition( transcripts, parameters.getLearningFraction(),
                parameters.getRandomSeed() );
        log.info( "Using " + transcriptSetPartition.getLearningSet().size() + " transcripts for learning and "
                + transcriptSetPartition.getTestingSet().size() + " for testing" );
        return transcriptSetPartition;
    }


    private File learnSVM(List<Transcript> learningSet) throws Exception
    {
        ObservationListBuilder builder = parameters.createObservationListBuilder();
        log.info( "Building observations for learning set" );
        ObservationList observations = builder.buildObservationListForTraining( learningSet, parameters.getBAMTracks() );
        log.info( observations.getObservations().size() + " observations built" );

        log.info( "Training SVM" );
        SVMLightTrain svmLight = new SVMLightTrain();
        RadialBasisFunction kernel = new RadialBasisFunction();
        kernel.setGamma( 2.4 );
        svmLight.setKernelOptions( kernel );
        LearningOptions learningOptions = svmLight.getLearningOptions();
        learningOptions.setTradeOff( 2.0 );
        learningOptions.setCostFactor( 4.0 );
        learningOptions.setRemoveAndRetrain( true );
        File modelFile = TempFiles.file( ".svmlight" );
        svmLight.train( observations, modelFile, log );
        log.info( "SVM trained" );
        return modelFile;
    }

    private ConfusionMatrix testSVM(File modelFile, List<Transcript> testingSet) throws Exception
    {
        ObservationListBuilder builder = parameters.createObservationListBuilder();
        log.info( "Building observations for testing set" );
        ObservationList observations = builder.buildObservationListForTraining( testingSet, parameters.getBAMTracks() );
        log.info( observations.getObservations().size() + " observations built" );

        log.info( "Testing SVM" );
        ModelValidator validator = new ModelValidator();
        ConfusionMatrix confusionMatrix = validator.validate( modelFile, observations, log );
        log.info( "SVM tested" );
        log.info( "Sensitivity " + confusionMatrix.getSensitivity() );
        log.info( "Specificity " + confusionMatrix.getSpecificity() );
        
        return confusionMatrix;
    }

    private void storeModelFile(File modelFile) throws Exception
    {
        FileImporter importer = new FileImporter();
        
        DataCollection<DataElement> parent = parameters.getModelFile().getParentCollection();
        String name = parameters.getModelFile().getName();
        
        FileImportProperties properties = importer.getProperties( parent, modelFile, name );
        properties.setPreserveExtension( false );
        
        importer.doImport( parent, modelFile, name, null, log );
    }

    private void storeConfusionMatrix(ConfusionMatrix confusionMatrix)
    {
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getConfusionMatrix() );
        result.getColumnModel().addColumn( "Real positive", DataType.Integer );
        result.getColumnModel().addColumn( "Real negative", DataType.Integer );
        TableDataCollectionUtils.addRow( result, "Predicted positive", new Object[] {confusionMatrix.getTP(), confusionMatrix.getFP()} );
        TableDataCollectionUtils.addRow( result, "Predicted negative", new Object[] {confusionMatrix.getFN(), confusionMatrix.getTN()} );
        parameters.getConfusionMatrix().save( result );
    }

}
