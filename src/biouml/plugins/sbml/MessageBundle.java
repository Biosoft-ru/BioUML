package biouml.plugins.sbml;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import java.util.logging.Logger;
import biouml.standard.type.Type;

/**
 * Messages for SBML models.
 */
public class MessageBundle extends ListResourceBundle
{
    private final Logger log = Logger.getLogger(MessageBundle.class.getName());

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

    protected static final MessageBundle resources = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());
    protected static void warn(Logger log, String messageBundleKey, Object[] params)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, params);
        log.warning(message);
    }
    protected static void error(Logger log, String messageBundleKey, Object[] params)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, params);
        log.log(Level.SEVERE, message);
    }

    ///////////////////////////////////////////////////////////////////
    // Contents
    //

    String[] specieTypes = {Type.TYPE_SUBSTANCE, Type.TYPE_GENE, Type.TYPE_RNA, Type.TYPE_PROTEIN};

    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private final Object[][] contents =
    {
        {"SPECIE_TYPES",        specieTypes},

        //--- BeanInfo constants -----------------------------------------------/

        {"CN_SBML_DIAGRAM",     "SBML diagram"},
        {"CD_SBML_DIAGRAM",     "Special diagram type to visualise SBML models."},

        {"CN_SPECIE",           "Species"},
        {"CD_SPECIE",           "The terms <i>species</i> refers to entities that take part in reactions."},

        {"PN_SPECIE_TYPE",      "Type"},
        {"PD_SPECIE_TYPE",      "Type of species (chemical substance, enzyme, gene, etc.)"},

        {"PN_SPECIE_CHARGE",    "Charge"},
        {"PD_SPECIE_CHARGE",    "Indicates the charge of the species in terms of electrons."},

        //--- SbmlReader error messages ----------------------------------------/

        {"ERROR_SBML_PROCESSING",                   "Model {0}: can not read the diagram, error during SMBL file parsing."},

        {"ERROR_COMPARTMENT_LIST_MISSING",          "Model {0}: compartment list is missing."},
        {"ERROR_COMPARTMENT_LIST_EMPTY",            "Model {0}: compartment list is empty."},
        {"ERROR_COMPARTMENT_REORDERING",            "Model {0}: cannot reorder compartment list. Probably some compartment parent chains form a loop."},
        {"ERROR_COMPARTMENT_PROCESSING",            "Model {0}: error during compartment <{1}> processing: {2}."},
        {"ERROR_COMPARTMENT_OUTSIDE",               "Model {0}: can not locate enclosing compartment <{2}> for compartment <{1}>."},
        {"ERROR_COMPARTMENT_VOLUME",                "Model {0}: can not parse compartment volume, compartment={1}, volume={2}, error: {3}."},

        {"ERROR_INVALID_SPATIAL_DIMENSION",         "Model {0}: can not parse compartment spatial dimension, compartment={1}."},
        {"ERROR_SPECIE_LIST_MISSING",               "Model {0}: species list is missing."},
        {"ERROR_SPECIE_LIST_EMPTY",                 "Model {0}: species list is empty."},
        {"ERROR_SPECIE_PROCESSING",                 "Model {0}: error during species <{1}> processing: {2}."},
        {"ERROR_SPECIE_COMPARTMENT_NOT_SPECIFIED",  "Model {0}: compartment not specified for species <{1}>, " +
                                                    "default compartment <{2}> will be used."},
        {"ERROR_SPECIE_COMPARTMENT_NOT_FOUND",      "Model {0}: compartment <{2}> not found for species <{1}>, " +
                                                    "default compartment <{3}> will be used."},
        {"ERROR_SPECIE_AMOUNT_NOT_SPECIFIED",       "Model {0}: initial amount for species <{1}> not specified, " +
                                                    "value 0.0  will be used."},
        {"ERROR_SPECIE_AMOUNT",                     "Model {0}: can not parse initial species amount, species={1}, amount={2}, error: {3}."},
        {"ERROR_SPECIE_AMOUNT",                     "Model {0}: can not parse initial species concentration, species={1}, concentration={2}, error: {3}."},
        {"ERROR_SPECIE_CHARGE",                     "Model {0}: can not parse species charge, species={1}, charge={2}, error: {3}."},
        {"ERROR_AMOUNT_AND_CONCENTRATION",            "Model {0}: species {1} has \"intitalConcentration\" and \"initialAmount\" attributes at the same time."},
        {"ERROR_INITIAL_AMOUNT_PRESENTS",             "Model {0}: species {1} must not hava \"initialAmount\" attribute set."}, //level 1 issue
        {"WARNING_NO_INITIAL_AMOUNT_OR_CONCENTRATION", "Model {0}: species {1} has no \"initialConcentration\" or \"initialAmount\" attributes set. It will be defined by initial assignments."},
        {"UNKNOWN_CONVERSION_FACTOR",      "Model {0}: error during conversion factor processsing, species: {1}, variable not found in model"},

        {"ERROR_PARAMETER_LIST_DUPLICATED",         "Model {0}: list of parameters is duplicated, parent element <{1}>."},
        {"ERROR_GLOBAL_PARAMETER_PROCESSING",       "Model {0}: error during parameter <{1}> processing: {2}."},
        {"ERROR_REACTION_PARAMETER_PROCESSING",     "Model {0}: error during reaction parameter processing: parameter={1}, reaction={2}, error: {3}."},

        {"ERROR_UNIT_DEFINITION_PROCESSING",        "Model {0}: error during unit definition <{1}> processing: {2}."},
        {"ERROR_UNIT_PROCESSING",                   "Model {0}: error during unit <{1}> processing: {2}."},

        {"ERROR_INVALID_REACTION",                     "Model {0}: reaction {1} must comprise at least one reactant or product."},

        {"ERROR_PARAMETER_VALUE_NOT_SPECIFIED",     "Model {0}: value not specified for parameter <{1}>, " +
                                                    "value 0.0  will be used."},
        {"ERROR_PARAMETER_VALUE",                   "Model {0}: can not parse parameter value, parameter={1}, value={2}, error: {3}."},

        {"ERROR_REACTION_LIST_MISSING",             "Model {0}: reaction list is missing."},
        {"ERROR_REACTION_LIST_EMPTY",               "Model {0}: reaction list is empty."},
        {"ERROR_REACTION_INVALID",                  "Model {0}: invalid reaction: {1}"},
        {"ERROR_REACTION_PROCESSING",               "Model {0}: error during reaction <{1}> processing: {2}."},
        {"ERROR_REACTANT_LIST_MISSING",             "Model {0}: reactant list is missing for reaction <{1}>."},
        {"ERROR_REACTANT_LIST_EMPTY",               "Model {0}: reactant list is empty for reaction <{1}>."},
        {"ERROR_REACTANT_PROCESSING",               "Model {0}: error during reactant processing: reaction={1}, reactant={2}, error: {3}."},
        {"WARN_FORMULA_EMPTY",                      "Model {0}: formula for reaction <{1}> is empty."},

        {"ERROR_PRODUCT_LIST_MISSING",              "Model {0}: product list is missing for reaction <{1}>."},
        {"ERROR_PRODUCT_LIST_EMPTY",                "Model {0}: product list is empty for reaction <{1}>."},
        {"ERROR_PRODUCT_PROCESSING",                "Model {0}: error during reaction product processing: reaction={1}, reactant={2}, error: {3}."},
        {"ERROR_MODIFIER_PROCESSING",               "Model {0}: error during reaction modifier processing: reaction={1}, reactant={2}, error: {3}."},

        {"ERROR_PROCESS_ALGEBRAIC_RULES",             "Model {0}: algebraic rules are not supported at present." },
        {"ERROR_VARIABLE_ATTRIBUTE_ABSENT",         "Model {0}: rule must have attribute \"variable\" set."},
        {"ERROR_VARIABLE_UNKNOWN",                    "Model {0}: variable {1} is not declared."},
        {"ERROR_RULE_PROCESSING",                     "Model {0}: error occured while processing rule. error: {1}"},

        {"ERROR_INITIAL_ASSIGNMENT_PROCESSING",     "Model {0}: error occured while processing initial assignment. error: {1}"},

        {"ERROR_SPECIE_VARIABLE_UNKNOWN",             "Model {0}: unknown species {1}."},
        {"ERROR_COMPARTMENT_VARIABLE_UNKNOWN",         "Model {0}: unknown compartment {1}."},
        {"ERROR_PARAMETER_VARIABLE_UNKNOWN",         "Model {0}: unknown parameter {1}."},

        {"ERROR_STOICHIOMETRY",                     "Model {0}: can not parse stoichiometry, reaction={1}, species={2}, stoichiometry={3}, error: {4}."},
        {"ERROR_DENOMINATOR",                       "Model {0}: can not parse stoichiometry, reaction={1}, species={2}, stoichiometry={3}, error: {4}."},
        {"ERROR_SPECIE_REFERENCE_INVALID",          "Model {0}: can not find species {2} for reaction {1}"},

        {"ERROR_FORMULA_NISSINF",                   "Model {0}: formula is not specified for {1}."},
        {"ERROR_FORMULA_PARSING",                   "Model {0}: error during formula parsing, element={1}, error: {2}."},
        {"ERROR_MATH_MISSING",                      "Model {0}: \"math\" element is missing for {1}"},
        {"ERROR_MATHML_PARSING",                    "Model {0}: error during mathML parsing, element={1}, error: {2}."},
        {"ERROR_ELEMENT_PROCESSING",                "Model {0}: can not read element <{2}> in <{1}>, error: {3}"},


        {"ERROR_EVENT_PROCESSING",                     "Model {0}: occured while processing event. error: {1}"},
        {"ERROR_USE_VALUES_FROM_TRIGGER_TIME_PROCESSING",        "Model {0}: can not read useValuesfromTrigger attribute for event {1}, error: {2}."},
        {"ERROR_USE_VALUES_FROM_TRIGGER_TIME_ABSENT",   "Model {0}: missing useValuesfromTrigger attribute for event {1}, default value \"true\" will be used"},
        {"ERROR_TRIGGER_ELEMENT_ABSENT",             "Model {0}: missing \"trigger\" subelement of event."},
        {"ERROR_EVENT_ASSIGNMENT_PROCESSING",         "Model {0}: error occured while processing assignment of event."},
        {"WARN_MULTIPLE_DECLARATION",               "Model {0}: multiple declaration of element {2} in {1}." +
                                                    "\nOnly first will be processed, other will be ignored."},
        {"ERROR_PRIORITY_PROCESSING",               "Model {0}: occured while processing event priority. error: {1}"},


        {"ERROR_HTML_PROCESSING",                   "Model {0}: can not process html, error: {1}, \nelement ={2}"},

         {"ERROR_FUNCTION_DECLARATION_PROCESSING",  "Model {0}: error occured while processing function {1}, error: {2}" },

        //--- SbmlWriter error messages ----------------------------------------/
        {"ERROR_DIAGRAM_NULL",                      "Can not write null as SBML model."},

        {"ERROR_COMPARTMENT_WRITING",               "Model {0}: error during compartment <{1}> writing: {2}."},

        {"ERROR_SPECIE_LIST_WRITING",       "Model {0}: error during writing species list for compartment <{1}>: {2}."},
        {"WARNING_SPECIE_AMOUNT_NOT_SPECIFIED",     "Model {0}: initial amount for species <{1}> not specified, " +
                                                    "value 0.0  will be used."},
        {"ERROR_SPECIE_WRITING",                    "Model {0}: error during species <{1}> writing: {2}."},

        {"ERROR_PARAMETER_WRITING",                 "Model {0}: error during parameter <{1}> writing: {2}."},

        {"ERROR_REACTION_WRITING",                  "Model {0}: error during reaction <{1}> writing: {2}."},
        {"ERROR_REACTION_LIST_WRITING",     "Model {0}: error during writing reaction list: {1}."},
        {"ERROR_SPECIE_REFERENCE_WRITING",          "Model {0}: error during species reference writing: species={2}, reaction={1}, error: {3}."},
        {"WARN_SPECIE_REFERENCE_EDGE_ABSENTS",      "Model {0}: can not find edge for species reference {2} in reaction {1}."},

        {"ERROR_XTML_WRITING",                      "Model {0}: can not write notes, error: {2}, notes:\n{3}."},

        {"REACTION_STUB_NOTE",                      "We allow user create reactions without reactants or products." +
                                                    "<br/>However reactant and product are obligatory elements of SBML reaction." +
                                                    "<br/>To respect this requrement stubs for reactant or product are generated."},


        {"WARNING_WRITE_STOICHIOMETRY_11",          "Model {0}: stoichiometry value for {1} is double - {2}, " +
                                                    "for SBML level 1 it is approximated as {3}."},

        {"ERROR_WRITE_STOICHIOMETRY_11",            "Model {0}: for SBML level stoichiometry value should be integer: " +
                                                    "species reference={1}, stoichiometry={2}." },

        {"ERROR_CREATING_MATH_ELEMENT",             "Model {0}: could not create mathML element from the formula {1}, error: {2}"},

        {"ERROR_READ_UNKNOWN_LEVEL_VERSION",        "Model {0}: could not determine SBML level and version."},




        //--- Dialog messages -----------------------------------------/
        {"DIAGRAM_ELEMENT_TITLE",                   "New diagram element."},

        {"DIAGRAM_ELEMENT_COMPARTMENT_NAME",        "Comaprtment name"},
        {"DIAGRAM_ELEMENT_SPECIE_NAME",             "Species name"},
        {"DIAGRAM_ELEMENT_SPECIE_TYPE",             "Species type"},

        {"DIAGRAM_ELEMENT_ERROR_TITLE",             "Error"},
        {"DIAGRAM_ELEMENT_RESERVED",                 "{0} is reserved SBML key word and can not be used as a diagram element name."},
        {"DIAGRAM_ELEMENT_DUPLICATED",              "Diagram already contains {1} with name {0}."},
        {"DIAGRAM_ELEMENT_ERROR",                   "Can not create new diagram element {0},<br>error: {1}"},
    };
}
