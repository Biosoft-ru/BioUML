package biouml.plugins.psimi.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

@SuppressWarnings ( "serial" )
public class Participant extends Concept
{
    public Participant(DataCollection<?> origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }
    
    protected Integer interactorRef;
    protected Integer interactionRef;
    protected Concept biologicalRole;
    protected Concept[] experimentalRoleList;
    protected Concept[] experimentalPreparationList;
    protected Feature[] featureList;
    protected Organism[] hostOrganismList;
    protected Confidence[] confidenceList;
    protected Concept[] parameterList;
    protected Concept[] participantIdentificationMethodList;
    protected Concept[] experimentalInteractorList;
    
    public Concept[] getExperimentalInteractorList()
    {
        return experimentalInteractorList;
    }

    public void setExperimentalInteractorList(Concept[] experimentalInteractorList)
    {
        this.experimentalInteractorList = experimentalInteractorList;
    }

    public Concept getBiologicalRole()
    {
        return biologicalRole;
    }

    public void setBiologicalRole(Concept biologicalRole)
    {
        this.biologicalRole = biologicalRole;
    }

    public Confidence[] getConfidenceList()
    {
        return confidenceList;
    }

    public void setConfidenceList(Confidence[] confidenceList)
    {
        this.confidenceList = confidenceList;
    }

    public Organism[] getHostOrganismList()
    {
        return hostOrganismList;
    }

    public void setHostOrganismList(Organism[] hostOrganismList)
    {
        this.hostOrganismList = hostOrganismList;
    }

    public Integer getInteractionRef()
    {
        return interactionRef;
    }

    public void setInteractionRef(Integer interactionRef)
    {
        this.interactionRef = interactionRef;
    }

    public Integer getInteractorRef()
    {
        return interactorRef;
    }

    public void setInteractorRef(Integer interactorRef)
    {
        this.interactorRef = interactorRef;
    }

    public Concept[] getExperimentalRoleList()
    {
        return experimentalRoleList;
    }

    public void setExperimentalRoleList(Concept[] experimentalRoleList)
    {
        this.experimentalRoleList = experimentalRoleList;
    }

    public Concept[] getExperimentalPreparationList()
    {
        return experimentalPreparationList;
    }

    public void setExperimentalPreparationList(Concept[] experimentalPreparationList)
    {
        this.experimentalPreparationList = experimentalPreparationList;
    }

    public Feature[] getFeatureList()
    {
        return featureList;
    }

    public void setFeatureList(Feature[] featureList)
    {
        this.featureList = featureList;
    }

    public Concept[] getParameterList()
    {
        return parameterList;
    }

    public void setParameterList(Concept[] parameterList)
    {
        this.parameterList = parameterList;
    }

    public Concept[] getParticipantIdentificationMethodList()
    {
        return participantIdentificationMethodList;
    }

    public void setParticipantIdentificationMethodList(Concept[] participantIdentificationMethodList)
    {
        this.participantIdentificationMethodList = participantIdentificationMethodList;
    }
}
