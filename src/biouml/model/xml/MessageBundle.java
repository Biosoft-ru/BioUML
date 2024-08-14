package biouml.model.xml;

import java.util.logging.Level;
import java.util.ListResourceBundle;

import java.util.logging.Logger;

/**
 * Stores data for initialization of ModulePackager constant and resources.
 */
public class MessageBundle extends ListResourceBundle
{
    private final Logger log = Logger.getLogger(MessageBundle.class.getName());

    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            //--- conctants --------------------------------------------/
                { "CN_XML_DIAGRAM_TYPE",          "XmlDiagramType"},
                { "CD_XML_DIAGRAM_TYPE",          "XmlDiagramType"},
                { "PN_XML_DIAGRAM_TYPE_NAME",     "Name"},
                { "PD_XML_DIAGRAM_TYPE_NAME",     "The name of diagram"},
                
            //--- mReader errors --------------------------------------------/
            {"ERROR_ELEMENT_PROCESSING",            "DiagramType {0}: can not read element <{2}> in <{1}>, error: {3}"},
        
            {"WARN_MULTIPLE_DECLARATION",           "DiagramType {0}: multiple declaration of element {2} in {1}." +
                                                    "\nOnly first will be processed, other will be ignored."},
        
            {"ERROR_UNEXPECTED_ELEMENT",            "DiagramType {0}: unexpected element {1}, only [{3}] elements are expected inside {2} element"},
        
            {"ERROR_PROPERTIES_ABSENT",             "DiagramType {0}: graphic notation properties are not declared"},
            {"ERROR_READ_PROPERTIES",               "DiagramType {0}: can not read properties declaration, error: {1}."},
            {"ERROR_PROPERTY_NAME_NOT_SPECIFIED",   "DiagramType {0}: property name is not specified, element={1}"},
            {"ERROR_PROPERTY_TYPE_NOT_SPECIFIED",   "DiagramType {0}: property type is not specified, property={1}"},
        
            {"ERROR_NODE_TYPES_ABSENT",             "DiagramType {0}: node types declaration is missing."},
            {"ERROR_READ_NODE_TYPES",               "DiagramType {0}: can not read node types declaration, error: {1}."},
            {"ERROR_NODE_TYPE_NOT_SPECIFIED",       "DiagramType {0}: node type is not specified, node={1}"},
        
            {"ERROR_EDGE_TYPES_ABSENT",             "DiagramType {0}: edge types declaration is missing."},
            {"ERROR_READ_EDGE_TYPES",               "DiagramType {0}: can not read edge types declaration, error: {1}."},
            
            {"ERROR_REACTION_TYPES_ABSENT",         "DiagramType {0}: reaction types declaration is missing."},
            {"ERROR_READ_REACTION_TYPES",           "DiagramType {0}: can not read reaction types declaration, error: {1}."},
            {"ERROR_REACTION_TYPE_NOT_SPECIFIED",   "DiagramType {0}: reaction type is not specified, node={1}"},
        
            {"ERROR_VIEW_BUILDER_ABSENT",           "DiagramType {0}: view builder is not defined"},
            {"ERROR_VIEW_BUILDER_PROTOTYPE",        "DiagramType {0}: can not create view builder prototype, error: {1}."},
            {"ERROR_VIEW_BUILDER_FUNCTION",        "DiagramType {0}: can not read view builder function, error: {1}."},
            {"ERROR_VIEW_TYPE_NOT_SPECIFIED",       "DiagramType {0}: view type is not specified, element={1}"},
            {"ERROR_VIEW_SCRIPT_NOT_SPECIFIED",     "DiagramType {0}: JavaScript is missing for view type {1}"},
            
            {"ERROR_PATH_LAYOUTER",                 "DiagramType {0}: path layouter class not found: {1}"},
            {"ERROR_SEMANTIC_CONTROLLER_ABSENT",    "DiagramType {0}: semantic controller is not defined"},
            {"ERROR_SEMANTIC_CONTROLLER_PROTOTYPE", "DiagramType {0}: can not create semantic controller prototype, error: {1}."},
            {"ERROR_SEMANTIC_CONTROLLER_FUNCTIONS", "DiagramType {0}: can not read semantic controller function, error: {1}."},
            {"ERROR_CONTROLLER_TYPE_NOT_SPECIFIED",       "DiagramType {0}: controller type is not specified, element={1}"},
            {"ERROR_CONTROLLER_SCRIPT_NOT_SPECIFIED",     "DiagramType {0}: JavaScript is missing for controller type {1}"},
            
            {"ERROR_EXAMPLES_ABSENT",               "DiagramType {0}: examples declaration is missing."},
            {"ERROR_READ_EXAMPLE",                  "DiagramType {0}: can not read example declaration, error: {1}."},
            {"ERROR_NO_EXAMPLE_NAME",               "DiagramType {0}: can not find name of example."}
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
            log.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}
