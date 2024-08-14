package biouml.plugins.psimi.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

@SuppressWarnings ( "serial" )
public class Interaction extends Concept
{
    public Interaction(DataCollection<?> origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }
    
    protected Integer[] experimentList;
    protected Concept[] interactionType;
    private Boolean modelled;
    private Boolean intraMolecular;
    private Boolean negative;
    protected Confidence[] confidenceList;
    protected Participant[] participantList;
    protected Concept[] parameterList;
    protected Integer availabilityRef;
    protected Concept[] inferredInteractionList;

    public Integer getAvailabilityRef()
    {
        return availabilityRef;
    }

    public void setAvailabilityRef(Integer availabilityRef)
    {
        this.availabilityRef = availabilityRef;
    }

    public Concept[] getParameterList()
    {
        return parameterList;
    }

    public void setParameterList(Concept[] parameterList)
    {
        this.parameterList = parameterList;
    }

    public Integer[] getExperimentList()
    {
        return experimentList;
    }

    public void setExperimentList(Integer[] experimentList)
    {
        this.experimentList = experimentList;
    }

    public Confidence[] getConfidenceList()
    {
        return confidenceList;
    }

    public void setConfidenceList(Confidence[] confidenceList)
    {
        this.confidenceList = confidenceList;
    }

    public Concept[] getInteractionType()
    {
        return interactionType;
    }

    public void setInteractionType(Concept[] interactionType)
    {
        this.interactionType = interactionType;
    }

    public Boolean getIntraMolecular()
    {
        return intraMolecular;
    }

    public void setIntraMolecular(Boolean intraMolecular)
    {
        this.intraMolecular = intraMolecular;
    }

    public Boolean getModelled()
    {
        return modelled;
    }

    public void setModelled(Boolean modelled)
    {
        this.modelled = modelled;
    }

    public Boolean getNegative()
    {
        return negative;
    }

    public void setNegative(Boolean negative)
    {
        this.negative = negative;
    }

    public Participant[] getParticipantList()
    {
        return participantList;
    }

    public void setParticipantList(Participant[] participantList)
    {
        this.participantList = participantList;
    }

    public Concept[] getInferredInteractionList()
    {
        return inferredInteractionList;
    }

    public void setInferredInteractionList(Concept[] inferredInteractionList)
    {
        this.inferredInteractionList = inferredInteractionList;
    }
}
