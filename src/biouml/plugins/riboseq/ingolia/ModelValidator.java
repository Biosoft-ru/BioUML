package biouml.plugins.riboseq.ingolia;

import java.io.File;

import one.util.streamex.DoubleCollector;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import biouml.plugins.riboseq.ingolia.Observation.Type;
import biouml.plugins.riboseq.ingolia.svmlight.SVMLightPredict;

public class ModelValidator
{
    public ConfusionMatrix validate(File modelFile, ObservationList observations, Logger log) throws Exception
    {
        SVMLightPredict svmLight = new SVMLightPredict();
        double[] scores = svmLight.predict( hideResponse( observations ), modelFile, log );
        
        boolean[] predicted = DoubleStreamEx.of( scores ).collect( DoubleCollector.toBooleanArray( score -> score > 0 ) );
        boolean[] expected = StreamEx.of( observations.getObservations() ).collect(
                MoreCollectors.toBooleanArray( o -> o.getType() == Observation.Type.YES ) );
        return new ConfusionMatrix( expected, predicted );
    }
    
    private ObservationList hideResponse(ObservationList observations)
    {
        ObservationList result = new ObservationList( observations.getPredictorNames() );
        for(Observation o : observations.getObservations())
        {
            Observation noResponseObservation = new Observation( Type.UNKNOWN, o.getPredictors() );
            noResponseObservation.setDescription( o.getDescription() );
            result.addObservation( noResponseObservation );
        }
        return result;
    }
}
