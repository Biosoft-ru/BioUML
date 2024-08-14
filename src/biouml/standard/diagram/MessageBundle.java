package biouml.standard.diagram;

import java.util.ListResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;

/**
 *
 * @pending enhance short descriptions.
 */
public class MessageBundle extends ListResourceBundle
{
    private Logger log = Logger.getLogger(MessageBundle.class.getName());

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

    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents =
    {
        {"CN_PATHWAY_DIAGRAM",              "Pathway diagram"},
        {"CD_PATHWAY_DIAGRAM",              "Pathway diagram"},
        
        {"CN_XML_DIAGRAM",                  "XML diagram"},
        {"CD_XML_DIAGRAM",                  "XML diagram"},

        {"CN_SEMANTIC_NETWORK_DIAGRAM",     "Semantic network"},
        {"CD_SEMANTIC_NETWORK_DIAGRAM",     "Semantic network diagram type ..."},

        {"CN_METABOLIC_PATHWAY_DIAGRAM",    "Metabolic pathway"},
        {"CD_METABOLIC_PATHWAY_DIAGRAM",    "Metabolic pathway diagram type ..."},

        {"CN_GENE_NETWORK_DIAGRAM",         "Gene network"},
        {"CD_GENE_NETWORK_DIAGRAM",         "Gene network diagram type ..."},

        {"CN_PATHWAY_SIMULATION_DIAGRAM",   "Pathway simulation"},
        {"CD_PATHWAY_SIMULATION_DIAGRAM",   "Pathway simulation diagram type ..."},
        
        {"CN_COMPOSITE_DIAGRAM",           "Composite diagram"},
        {"CD_COMPOSITE_DIAGRAM",           "Composite diagram type ..."},
        
        //--- Dialog messages -----------------------------------------/

        // NewDiagramElementDialog
        {"NEW_ELEMENT_DIALOG",      "Add new diagram element: {0}"},
        {"NEW_ELEMENT_DIALOG_ERROR","Cannot find data element"},
        {"NULL_MODEL_ERROR",        "Cannot add diagram with empty model. Select another diagram"},
        {"INCORRECT_ELEMENT_ERROR", "Cannot add this element. Select another element"},
        {"DATABASE_COMBO_BOX",        "Database: "},
        {"NAME_COMBO_BOX",          "ID: "},
        {"TITLE_COMBO_BOX",         "Title: "},
        {"EXPLORER_BORDER_TITLE",   "  Details:  "},
        {"NEW_BUTTON",              "New"},

        // SemanticRelationPane
        {"RELATION_DESCRIPTION",
        "<html>Specify 'from' and 'to' nodes." +
        "<br>For this purpose click by mouse under the corresponding node on the diagram.</html>"},

        {"RELATION_IN",     "From:  "},
        {"RELATION_OUT",    "To: "},

        // ReactionPane

        {"REACTION_COMPONENT_PANEL", "Add/remove reaction component: "},
        {"REACTION_NAME", "Reaction name: "},
        {"REACTION_TITLE", "Title: "},
        {"REACTION_COMPONENT_NAME",     "Component: "},
        {"REACTION_VARIABLE_NAME",      "Variable name:"},
        {"REACTION_COMPONENT_ROLE",     "Role: "},
        {"REACTION_ADD_COMPONENT",      "Add"},
        {"REACTION_REMOVE_COMPONENT",   "Remove"},

        {"REACTION_COMPONENTS_PANEL",   "Reaction components: "},
        {"REACTION_RATE_PANEL",         "Formula: "},
        
        {"REACTION_HELP_MESSAGE",       "<html><font color='red'>Click element on the diagram to select it as reaction component</font>"},

        {"REACTION_ERROR_TITLE",        "Create reaction error"},
        {"REACTION_ERROR_MESSAGE",      "Can not create reaction. Error: "},

        {"REACTION_COMPONENT_DUPLICATED", "<html>Reaction already contains component <{0}> with role <{1}>." +
                                        "<br>You can use stoichiometric coefficient to indicate " +
                                        "<br>how much molecules of the same specie is involved in reation.</html>"},

        {"REACTION_COMPONENT_NOT_VALID", "Reaction should contain at least one reactant or product."},
        {"REACTION_IS_EMPTY", "Reaction will contain no reactants or products. Are you sure you want to create empty reaction?"},
        
        //Edge info
        {"CN_EDGE", "Edge"},
        {"CD_EDGE", "Edge"},
        {"PN_EDGE_INPUTNAME", "Input"},
        {"PD_EDGE_INPUTNAME", "Input"},
        {"PN_EDGE_OUTPUTNAME", "Output"},
        {"PD_EDGE_OUTPUTNAME", "Output"},
        {"CN_POINT", "Point"},
        {"CD_POINT", "Point"},
        {"PN_POINT_X", "X"},
        {"PD_POINT_X", "X"},
        {"PN_POINT_Y", "Y"},
        {"PD_POINT_Y", "Y"},
        {"PN_POINT_TYPE", "Type"},
        {"PD_POINT_TYPE", "Type"},

        // not used yet
        {"NEW_REACTION_DIALOG_TITLE", "New Reaction"},
        {"NEW_ELEMENT_DIALOG_TITLE", "New Element"},
                
//      Database references tab properties
        { DatabaseReferencesPane.EDIT    + Action.SMALL_ICON           , "edit.gif"},
        { DatabaseReferencesPane.EDIT    + Action.NAME                 , "Edit"},
        { DatabaseReferencesPane.EDIT    + Action.SHORT_DESCRIPTION    , "Open edit dialog."},
        { DatabaseReferencesPane.EDIT    + Action.ACTION_COMMAND_KEY   , "cmd-edit"},
        
        { DatabaseReferencesPane.REMOVE    + Action.SMALL_ICON           , "remove.gif"},
        { DatabaseReferencesPane.REMOVE    + Action.NAME                 , "Remove"},
        { DatabaseReferencesPane.REMOVE    + Action.SHORT_DESCRIPTION    , "Remove selected reference."},
        { DatabaseReferencesPane.REMOVE    + Action.ACTION_COMMAND_KEY   , "cmd-remove"},
    };
}
