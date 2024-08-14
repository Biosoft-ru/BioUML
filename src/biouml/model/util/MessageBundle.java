package biouml.model.util;

import java.util.logging.Level;
import java.util.ListResourceBundle;

import java.util.logging.Logger;

/**
 * Stores data for initialization of ModulePackager constant and resources.
 */
public class MessageBundle extends ListResourceBundle
{
    private final Logger log = Logger.getLogger(ModulePackager.class.getName());

    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
                {"MESSAGE_DATABASE_EXIST", "Database {0} is already exist."},
                {"DATABASE_EXPORTED_SUCCESSFULLY", "Database export was completed successfully."},
                {"DATABASE_IMPORTED_SUCCESSFULLY", "Database import was completed successfully."},
                {"EXPORT_DATABASE_MESSAGE", "Export database {0} into {1}."},
                {"DATABASE_EXPORT_ERROR", "Database export error: {0}\n database = {1}"},
                {"IMPORTING_DATABASE_FROM", "Import database from {0}."},
                {"DATABASE_IMPORT_CANCELED", "Database import is canceled."},
                {"OLD_DATABASE_VERSION_REMOVED", "Old database version was removed."},
                {"CREATING_ITEM", "Creating {0}"},
                {"EXTRACTING_ITEM", "Extracting {0}"},
                {"ADDING_FILE", "Adding {0}"},
                {"CANNOT_GET_MANIFEST", "Cannot get manifest for database {0}"},
                {"CANNOT_GET_MANIFEST_ATTRIBUTE", "Cannot get manifest attribute {0}"},
        
                //--- DiagramReader errors --------------------------------------------/
                {"ERROR_ELEMENT_PROCESSING", "Model {0}: can not read element <{2}> in <{1}>, error: {3}"},
        
                {"WARN_MULTIPLE_DECLARATION",
                        "Model {0}: multiple declaration of element {2} in {1}." + "\nOnly first will be processed, other will be ignored."},
        
                {"ERROR_DIAGRAM_ELEMENT_ABSENTS", "Model {0}: diagram element absents."},
                {"ERROR_DIAGRAM_TYPE", "Model {0}: incorrect diagram type {1}, error: {2}"},
        
                {"ERROR_REQUIRED_ATTRIBUTE_MISSING", "Model {0}: missing required attribute {1} in element {2}"},
                {"ERROR_COMPARTMENT_INFO_ABSENTS", "Model {0}: compartmentInfo absents for compartment {1}"},
                {"ERROR_NODES_ABSENT", "Model {0}: nodes absent for compartment {1}"},
                {"ERROR_EDGES_ABSENT", "Model {0}: edges absent for compartment {1}"},
                {"ERROR_READ_NODES", "Model {0}: can not read nodes for compartment {1}, error: {2}."},
                {"ERROR_READ_EDGES", "Model {0}: can not read edges for compartment {1}, error: {2}."},
                {"ERROR_KERNEL_PROCESSING", "Model {0}: can not process kernel {1}, error: {2}."},
                {"ERROR_READ_SUBDIAGRAM", "Model {0}: can not read subdiagram for compartment {1}, error: {2}."},
                {"ERROR_READ_CONNECTION", "Model {0}: can not read role for edge {1}, error: {2}."},
        
                {"WARN_LOCATION_ABSENTS", "Model {0}: location for node {1} absents, x={2}, y={3}."},
                {"ERROR_LOCATION_PARSING", "Model {0}: location for node {1} is incorrect, x={2}, y={3}, error: {4}."},
                {"WARN_SIZE_ABSENTS", "Model {0}: size for compartment {1} absents, width={2}, height={3}."},
                {"ERROR_SIZE_PARSING", "Model {0}: size for compartment {1} is incorrect, width={2}, height={3}, error: {4}."},
                {"ERROR_POINT_PARSING", "Model {0}: incorrect point format, edge={1}, point={2}, error: {3}."},
                {"ERROR_COLOR_PARSING", "Model {0}: incorrect color {2} for {1}, error: {3}."},
        
                {"WARN_IMAGE_SOURCE_ABSENTS", "Model {0}: image source for node {1} absents"},
                {"ERROR_IMAGE_PROCESSING", "Model {0}: image processing error, node={1}, image={2}, error:\n{3}."},
        
                {"ERROR_NODE_NOT_FOUND", "Model {0}: can not find node {2} for edge {1}."},
        
                {"ERROR_EXECUTABLE_MODEL", "Model {0}: can not parse executable model, error: {1}"},
        
                {"WARN_PARAMETER_VALUE_ABSENTS", "Model {0}: value for constant {1} absents."},
                {"ERROR_PARAMETER_VALUE", "Model {0}: can not parse parameter value, parameter={1}, value={2}, error:\n{3}."},
                {"ERROR_PARAMETER_PROCESSING", "Model {0}: can not process parameter {1}, error:\n{2}."},
        
                {"ERROR_DIAGRAM_ELEMENT_ABSENTS", "Model {0}: can not find diagram element {1}, error:\n{2}."},
                {"WARN_VARIABLE_VALUE_ABSENTS", "Model {0}: initial value for variable {1} absents."},
                {"ERROR_VARIABLE_VALUE", "Model {0}: can not parse variable value, variable={1}, value={2}, error:\n{3}."},
                {"ERROR_VARIABLE_PROCESSING", "Model {0}: can not process variable {1}, error:\n{2}."},
        
                {"ERROR_TABLE_PROCESSING", "Model {0}: can not process table element {1} for node {2}, error:\n{3}."},
                {"ERROR_TABLE_VARIABLE_ABSENTS", "Model {0}: variable for node {1} absents."},
                {"ERROR_TABLE_DATA_ABSENTS", "Model {0}:table data for node {1} absents."},
                
                {"ERROR_EQUATION_PROCESSING", "Model {0}: can not process equation {1} for edge {2}, error:\n{3}."},
                {"ERROR_CONSTRAINT_PROCESSING", "Model {0}: can not process constraint {1} for edge {2}, error:\n{3}."},
                {"ERROR_BUS_PROCESSING", "Model {0}: can not process bus, error:\n{3}."},
                {"WARN_EQUATION_FORMULA_ABSENTS", "Model {0}: eqution formula absents, diagram element={1}"},
                {"WARN_CONSTRAINT_FORMULA_ABSENTS", "Model {0}: constraint formula absents, diagram element={1}"},
                {"WARN_EQUATION_VARIABLE_UNRESOLVED", "Model {0}: can not resolve equation variable, diagram element={1}, variable={2}."},
                {"WARN_MISSED_VARIABLE_DECLARATION",
                        "Model {0}: missed variable declaration was restored for diagram element={1}, variable={2}."},
        
                {"ERROR_DIAGRAM_ELEMENT_NOT_NODE", "Model {0}: diagram element {1} is not a node."},
                {"ERROR_EVENT_TRIGGER_ABSENTS", "Model {0}: event element {1} has no trigger element."},
                {"ERROR_ASSIGNMENT_PROCESSING", "Model {0}: error occured while processing assignments for event {1}. Message {2}"},
                {"ERROR_STATE_PROCESSING", "Model {0}: error occured while processing state {1}. Message: {2}"},
                {"ERROR_STATE_ENTRY_PROCESSING", "Model {0}: error occured while processing state {1} entry assignment. Message: {2}"},
                {"ERROR_STATE_EXIT_PROCESSING", "Model {0}: error occured while processing state {1} exit assignment. Message: {2}"},
                {"ERROR_STATE_ON_EVENT_PROCESSING", "Model {0}: error occured while processing state {1} \"on_event\" assignment. Message: {2}"},
                {"ERROR_TRANSITION_PROCESSING", "Model {0}: error occured while processing transition {1}. Message: {2}"},
                {"ERROR_HAS_WHEN_HAS_AFTER", "Model {0}: transition {1} has \"after\" and \"when\" elements simultaneously."},
        
        
                {"ERROR_ARRAY_NO_ELEMENT_TYPE", "Element named \"{0}\" with type {1} has no 'elementType' attribute"},
                {"ERROR_COULD_NOT_RESOLVE_TYPE", "Could not resolve type \"{0}\""},
                {"ERROR_PARSING_PROPERTY", "Error occured while parsing property \"{0}\""},
                {"UNDEFINED_PROPERTY_REF", "Undefined reference to property \"{0}\""},
                
                {"ERROR_SUBDIAGRAM_STATES_WRITING", "Model {0}: error occured while writing state for subdiagram {1}. Message: {3}."},
                {"ERROR_MODEL_DEFINITION_WRITING", "Model {0}: error occured while writing diagram for model definition {1}. Message: {3}."},
        
                //Convert diagram action
                {"CN_CLASS", "Parameters"},
                {"CD_CLASS", "Parameters"},
                {"PN_CONVERTED_DIAGRAM_NAME", "New diagram"},
                {"PD_CONVERTED_DIAGRAM_NAME", "Complete path to converted diagram"},
                {"PN_CONVERTED_DIAGRAM_TYPE", "Convert to type"},
                {"PD_CONVERTED_DIAGRAM_TYPE", "Type to convert diagram to"},
        
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
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}
