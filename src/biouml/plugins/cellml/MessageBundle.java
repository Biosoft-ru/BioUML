package biouml.plugins.cellml;

import java.util.ListResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Messages for CellML models.
 */
public class MessageBundle extends ListResourceBundle
{
    private final Logger cat = Logger.getLogger(MessageBundle.class.getName());

    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            //--- CellMLReader error messages --------------------------------------/
        
            {"ERROR_ROOT_IS_NOT_MODEL",                 "Model {0}: model element should be a root of XML document."},
        
            {"ERROR_ELEMENT_PROCESSING",                "Model {0}: can not read element {2} in {1}, error: {3}"},
        
            {"WARN_MULTIPLE_DECLARATION",               "Model {0}: multiple declaration of element {2} in {1}." +
                                                        "\nOnly first will be processed, other will be ignored."},
        
            {"ERROR_COMPONENT_NAME_ABSENTS",            "Model {0}: component name absents or empty."},
            {"WARNING_UNPROCESSED_COMPONENT",           "Model {0}: unprocessed component, name={1}, type={2}."},
        
            {"ERROR_REACTION_PROCESSING",               "Model {0}: error during reaction {1} processing: {2}."},
            {"ERROR_UNKNOWN_ROLE",                      "Model {0}: unkonown role {1} in reaction<{2}." },
            {"ERROR_VARIABLE_REF_ROLE_UNDEFINED",       "Model {0}: role is undefined for species (variable_ref) {1}."},
            {"ERROR_STOICHIOMETRY",                     "Model {0}: can not parse stoichiometry, reaction={1}, specie={2}, stoichiometry={3}, error: {4}."},
            {"ERROR_SPECIE_REFERENCE_INVALID",          "Model {0}: can not find specie {2} for reaction {1}."},
            {"ERROR_RATE_MISSING",                      "Model {0}: reaction rate is missing in reaction {1}."},
        
            {"ERROR_COMPONENT_1_MISSSING",              "Model {0}: first component is missed in connection, second component={1}."},
            {"ERROR_COMPONENT_2_MISSSING",              "Model {0}: second component is missed in connection, first component={1}."},
            {"ERROR_SPECIE_CONNECTION_MISSING",         "Model {0}: can not find connection, species={1}, reaction={2}."},
            {"WARNING_UNPROCESSED_CONNECTION",          "Model {0}: unprocessed connection, component_1={1}, component_2={2}."},
        
            {"ERROR_MATH_MISSING",                      "Model {0}: math element is missing in component {1}."},
            {"ERROR_MATHML_PARSING",                    "Model {0}: there were errors or warnings during MathML parsing\n" +
                                                        "  component={1}, errors: \n{2}"},
            {"ERROR_MATH_EQUATION",                     "Model {0}: error in equation, component={1}\nequation={2}."},
            {"ERROR_UNDECLARED_VARIABLE",               "Model {0}: undeclared variable {2} in component {1}."},
            {"WARNING_INITIAL_VALUE_ABSENTS",           "Model {0}: initial value is not specified for variable {1} in component {2}."},
            {"ERROR_INITIAL_VALUE",                     "Model {0}: can not parse vasriable initial value, variable={1}, value={2}, error: {3}."},
            {"ERROR_VARIABLE_UNITS_ABSENTS",            "Model {0}: units is not specified for variale {1}."},
        
            //--- CellMLWriter error messages --------------------------------------/
        
            {"ERROR_DIAGRAM_NULL",                      "Can not write null as CellML model."},
        
        };
    }

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
            cat.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}