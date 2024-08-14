package biouml.plugins.psimi;

import java.util.logging.Level;
import java.util.ListResourceBundle;

import java.util.logging.Logger;

/**
 * Stores data for initialization of ModulePackager constant and resources.
 */
public class MessageBundle extends ListResourceBundle
{
    private Logger log = Logger.getLogger(MessageBundle.class.getName());

    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents =
    {
        //--- Errors --------------------------------------------/
        {"ERROR_ELEMENT_PROCESSING",            "PSI-MI model {0}: can not read element <{2}> in <{1}>, error: {3}"},
        {"WARN_MULTIPLE_DECLARATION",           "PSI-MI model {0}: multiple declaration of element {2} in {1}." +
                                                "\nOnly first will be processed, other will be ignored."},
        {"ERROR_ENTRYSET_ABSENT",               "PSI-MI model {0}: entry set is not declared"},
        {"ERROR_SOURCE_ADD",                    "PSI-MI model {0}: can't add source"},
        {"ERROR_ATTRIBUTES_ADD",                "PSI-MI model {0}: can't add attributes"},
        {"ERROR_AVAILABILITY_ADD",              "PSI-MI model {0}: can't add availability: id = {1}"},
        {"ERROR_EXPERIMENT_ADD",                "PSI-MI model {0}: can't add experiment: id = {1}"},
        {"ERROR_INTERACTOR_ADD",                "PSI-MI model {0}: can't add interactor: id = {1}"},
        {"ERROR_INTERACTION_ADD",               "PSI-MI model {0}: can't add interaction: id = {1}"},
        {"ERROR_XREF_REQUIRED_ATTRIBUTES_ABSENT", "PSI-MI model {0}: xRef required attribute absent"},
        {"ERROR_ATTRIBUTE_NAME_ABSENT",         "PSI-MI model {0}: attribute name absent"},
        {"ERROR_ATTRIBUTE_ADD",                 "PSI-MI model {0}: can't add attribute"},
        {"ERROR_FEATURE_RANGE",                 "PSI-MI model {0}: can't read feature range: {1}"},
        {"ERROR_PARAMETER",                     "PSI-MI model {0}: can't read parameter: {1}"},
        
        
        // ----- Experiment specific fields -------------------------------------/
        {"CN_EXPERIMENT", "Experiment"},
        {"CD_EXPERIMENT", "Experiment"},
        {"PN_ID_METHOD", "interactionDetectionMethod"},
        {"PD_ID_METHOD", "interactionDetectionMethod"},
        {"PN_PI_METHOD", "interactionDetectionMethod"},
        {"PD_PI_METHOD", "interactionDetectionMethod"},
        {"PN_FD_METHOD", "featureDetectionMethod"},
        {"PD_FD_METHOD", "featureDetectionMethod"},
        {"PN_HOSTORGANISM_LIST", "hostOrganismList"},
        {"PD_HOSTORGANISM_LIST", "hostOrganismList"},
        {"PN_CONFIDENCE_LIST", "confidenceList"},
        {"PD_CONFIDENCE_LIST", "confidenceList"},
        
        {"CN_ORGANISM", "Organism"},
        {"CD_ORGANISM", "Organism"},
        {"PN_ORGANISM_CELLTYPE", "Cell Type"},
        {"PD_ORGANISM_CELLTYPE", "Cell Type"},
        {"PN_ORGANISM_COMPARTMENT", "Compartment"},
        {"PD_ORGANISM_COMPARTMENT", "Compartment"},
        {"PN_ORGANISM_TISSUE", "Tissue"},
        {"PD_ORGANISM_TISSUE", "Tissue"},
        
        {"CN_CONFIDENCE", "Confidence"},
        {"CD_CONFIDENCE", "Confidence"},
        {"PN_CONFIDENCE_UNIT", "Unit"},
        {"PD_CONFIDENCE_UNIT", "Unit"},
        {"PN_CONFIDENCE_VALUE", "Value"},
        {"PD_CONFIDENCE_VALUE", "Value"},
        {"PN_CONFIDENCE_REFLIST", "ExperimentRefList"},
        {"PD_CONFIDENCE_REFLIST", "ExperimentRefList"},
        
        {"CN_INTERACTOR", "Interactor"},
        {"CD_INTERACTOR", "Interactor"},
        {"PN_INTERACTOR_TYPE", "InteractorType"},
        {"PD_INTERACTOR_TYPE", "InteractorType"},
        {"PN_INTERACTOR_ORGANISM", "Organism"},
        {"PD_INTERACTOR_ORGANISM", "Organism"},
        {"PN_INTERACTOR_SEQUENCE", "Sequence"},
        {"PD_INTERACTOR_SEQUENCE", "Sequence"},
        
        {"CN_INTERACTION", "Interaction"},
        {"CD_INTERACTION", "Interaction"},
        {"PN_INTERACTION_AR", "AvailabilityRef"},
        {"PD_INTERACTION_AR", "AvailabilityRef"},
        {"PN_INTERACTION_EXLIST", "ExperimentList"},
        {"PD_INTERACTION_EXLIST", "ExperimentList"},
        {"PN_INTERACTION_TYPE", "InteractionType"},
        {"PD_INTERACTION_TYPE", "InteractionType"},
        {"PN_INTERACTION_MODELLED", "Modelled"},
        {"PD_INTERACTION_MODELLED", "Modelled"},
        {"PN_INTERACTION_IM", "IntraMolecular"},
        {"PD_INTERACTION_IM", "IntraMolecular"},
        {"PN_INTERACTION_NEGATIVE", "Negative"},
        {"PD_INTERACTION_NEGATIVE", "Negative"},
        {"PN_INTERACTION_CFLIST", "ConfidenceList"},
        {"PD_INTERACTION_CFLIST", "Confidencelist"},
        {"PN_INTERACTION_PARTICIPANT_LIST", "ParticipantList"},
        {"PD_INTERACTION_PARTICIPANT_LIST", "Participantlist"},
        {"PN_INTERACTION_PARAMETER_LIST", "ParameterList"},
        {"PD_INTERACTION_PARAMETER_LIST", "ParameterList"},
        {"PN_INTERACTION_II_LIST", "InferredInteractionList"},
        {"PD_INTERACTION_II_LIST", "InferredInteractionList"},
        
        {"CN_PARTICIPANT", "Participant"},
        {"CD_PARTICIPANT", "Participant"},
        {"PN_PARTICIPANT_INTERACTORREF", "InteractorRef"},
        {"PD_PARTICIPANT_INTERACTORREF", "InteractorRef"},
        {"PN_PARTICIPANT_INTERACTIONREF", "InteractionRef"},
        {"PD_PARTICIPANT_INTERACTIONREF", "InteractionRef"},
        {"PN_PARTICIPANT_BIOLOGICALROLE", "BiologicalRole"},
        {"PD_PARTICIPANT_BIOLOGICALROLE", "BiologicalRole"},
        {"PN_PARTICIPANT_EXROLE_LIST", "ExperimentalRoleList"},
        {"PD_PARTICIPANT_EXROLE_LIST", "ExperimentalRoleList"},
        {"PN_PARTICIPANT_EXPREPARATION_LIST", "ExperimentalPreparationList"},
        {"PD_PARTICIPANT_EXPREPARATION_LIST", "ExperimentalPreparationList"},
        {"PN_PARTICIPANT_FEATURE_LIST", "FeatureList"},
        {"PD_PARTICIPANT_FEATURE_LIST", "FeatureList"},
        {"PN_PARTICIPANT_HOSTORGANISM_LIST", "HostOrganismList"},
        {"PD_PARTICIPANT_HOSTORGANISM_LIST", "HostOrganismList"},
        {"PN_PARTICIPANT_CONFIDENCE_LIST", "ConfidenceList"},
        {"PD_PARTICIPANT_CONFIDENCE_LIST", "ConfidenceList"},
        {"PN_PARTICIPANT_PARAMETER_LIST", "ParameterList"},
        {"PD_PARTICIPANT_PARAMETER_LIST", "ParameterList"},
        {"PN_PARTICIPANT_PIMETHOD_LIST", "ParticipantIdentificationMethodList"},
        {"PD_PARTICIPANT_PIMETHOD_LIST", "ParticipantIdentificationMethodList"},
        {"PN_PARTICIPANT_EI_LIST", "ExperimentalInteractorList"},
        {"PD_PARTICIPANT_EI_LIST", "ExperimentalInteractorList"},
        
        {"CN_FEATURE", "Feature"},
        {"CD_FEATURE", "Feature"},
        {"PN_FEATURE_FTYPE", "Type"},
        {"PD_FEATURE_FTYPE", "Type"},
        {"PN_FEATURE_FDMETHOD", "DetectionMethod"},
        {"PD_FEATURE_FDMETHOD", "DetectionMethod"},
        {"PN_FEATURE_ERLIST", "ExperimentRefList"},
        {"PD_FEATURE_ERLIST", "ExperimentRefList"},
        {"PN_FEATURE_FRLIST", "RangeList"},
        {"PD_FEATURE_FRLIST", "RangeList"},
    };

    /**
     * Returns string from the resource bundle for the specified key.
     * If the sting is absent the key string is returned instead and
     * the message is printed in <code>java.util.logging.Logger</code> for the component.
     */
    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch (Throwable t)
        {
            log.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}
