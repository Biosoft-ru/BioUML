package biouml.plugins.riboseq.ingolia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ObservationList
{
    private String[] predictorNames;
    private List<Observation> observations = new ArrayList<>();
    
    public ObservationList(String[] predictorNames)
    {
        this.predictorNames = predictorNames;
    }

    public String[] getPredictorNames()
    {
        return predictorNames;
    }
    
    public void addObservation(Observation observation)
    {
        if(observation.getPredictors().length != predictorNames.length)
            throw new IllegalArgumentException();
        observations.add( observation );
    }
    
    public List<Observation> getObservations()
    {
        return Collections.unmodifiableList( observations );
    }
}
