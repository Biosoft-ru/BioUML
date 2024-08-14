package biouml.model.util;

public class DiagramXmlConstants
{
    public static final String DML_ELEMENT                 = "dml";
    public static final String VERSION_ATTR                = "version";
    public static final String APPVERSION_ATTR             = "appVersion";

    public static final String UNKNOWN_VALUE               = "unknown222";
    public static final String STUB                        = "stub";

    public static final String DIAGRAM_INFO_ELEMENT        = "diagramInfo";

    /** @deprecated replaced by PCDATA section in v. 0.7.3 */
    @Deprecated
    public static final String DIAGRAM_INFO_ATTR           = "value";

    ////////////////////////////////////////////////////////////////////
    // Model structure constants
    //

    public static final String DIAGRAM_ELEMENT             = "diagram";
    public static final String DIAGRAM_TYPE_ATTR           = "diagramType";
    public static final String DIAGRAM_XML_NOTATION        = "notation";

    public static final String LAYOUTER_INFO_ELEMENT        = "layouterInfo";
    public static final String LABEL_LAYOUTER_INFO_ELEMENT  = "labelLayouterInfo";

    // compartments
    public static final String COMPARTMENT_ELEMENT         = "compartment";
    public static final String SUBDIAGRAM_ELEMENT          = "subdiagram";
    public static final String MODEL_DEFINITION_ELEMENT    = "modelDefinition";
    public static final String EQUIVALENTNODEGROUP_ELEMENT = "equivalentNodeGroup";
    public static final String REPRESENTATIVE_ATTR         = "representative";

    // compartmentInfo
    public static final String COMPARTMENT_INFO_ELEMENT    = "compartmentInfo";
    public static final String ID_ATTR                     = "id";
    public static final String KERNEL_REF_ATTR             = "kernel";
    public static final String KERNEL_TYPE_ATTR            = "kernel_type";
    public static final String X_ATTR                      = "x";
    public static final String Y_ATTR                      = "y";
    public static final String WIDTH_ATTR                  = "width";
    public static final String HEIGHT_ATTR                 = "height";
    public static final String TITLE_ATTR                  = "title";
    public static final String COMMENT_ATTR                = "comment";
    public static final String SHAPE_ATTR                  = "shape";
    public static final String COLOR_ATTR                  = "color";
    public static final String DIAGRAM_REF_ATTR            = "diagram";
    public static final String MODEL_DEFINITION_REF_ATTR   = "modelDefinition";
    public static final String DIAGRAM_STATE_ATTR          = "diagramState";

    public static final String STYLE_ATTR                  = "style";
    public static final String PEN_ATTR                    = "pen";
    public static final String FONT_ATTR                   = "font";
    
    // nodes
    public static final String NODES_ELEMENT               = "nodes";
    public static final String NODE_ELEMENT                = "node";

    // notes
    public static final String BACKGROUND_VISIBLE_ATTR     = "background_visible";
    public static final String BACKGROUND_COLOR_ATTR       = "background_color";

    // image
    public static final String IMAGE_ELEMENT               = "image";
    public static final String PATH_ATTR                    = "path";
    public static final String SRC_ATTR                    = "src";
    public static final String HIDE_TITLE_ATTTR            = "hideTitle";

    // edges
    public static final String EDGES_ELEMENT               = "edges";
    public static final String EDGE_ELEMENT                = "edge";
    public static final String IN_REF_ATTR                 = "in";
    public static final String OUT_REF_ATTR                = "out";
    public static final String INPORT_ATTR                 = "inPort";
    public static final String OUTPORT_ATTR                = "outPort";
    public static final String CONNECTION_ROLE_ELEMENT     = "connection";
    public static final String CONNECTION_INPUT_ELEMENT    = "connectionInput";
    public static final String CONNECTION_OUTPUT_ELEMENT   = "connectionOutput";
    public static final String CONNECTION_LIST_ELEMENT     = "listOfConnections";
    public static final String EDGE_ID_ATTR                = "edgeID";
    public static final String FIXED_IN_OUT_ATTR           = "fixedInOut";

    // DPS attributes
    public static final String PROPERTY_ELEMENT            = "property";
    public static final String PROPERTY_REF_ELEMENT        = "propertyRef";
    public static final String ITEM_ELEMENT                = "item";
    public static final String TYPE_ATTR                   = "type";
    public static final String ARRAY_ELEM_TYPE_ATTR        = "elementType";
    public static final String SHORT_DESCR_ATTR            = "short-description";

    public static final String IS_HIDDEN_ATTR              = "isHidden";
    public static final String IS_TITLE_HIDDEN_ATTR        = "isTitleHidden";
    public static final String IS_READONLY_ATTR            = "isReadOnly";

    public static final String TAGS_ELEMENT                = "tags";
    public static final String TAG_ELEMENT                 = "tag";

    // filters
    public static final String FILTERS_ELEMENT             = "filters";
    public static final String SELECTED_FILTER_ATTR        = "active";

    //states
    public static final String EXPERIMENTS_ELEMENT         = "experiments"; //old name for states
    public static final String STATES_ELEMENT              = "states";

    //view options
    public static final String VIEW_OPTIONS_ELEMENT        = "viewOptions";
    public static final String SHOW_GRID_ATTR              = "showGrid";
    public static final String GRID_STYLE_ATTR             = "gridStyle";
    public static final String GRID_CELL_SIZE_ATTR         = "gridCellSize";
    public static final String GRID_STEP_SIZE_ATTR         = "gridStepSize";

    public static final String SIMULATION_OPTIONS          = "simulationOptions";
    
    public static final String PLOTS_ATTR                  = "Plots";
    
    public static final String PLOTS_ELEMENT               = "plots";
    
    //table element
    public static final String SIMPLE_TABLE_ELEMENT                       = "table";
    public static final String ARGCOLUMN_ELEMENT           = "argColumn";
    public static final String VARCOLUMN_ELEMENT           = "varColumn";
    public static final String TABLE_COLUMN_ATTR           = "column";
    public static final String TABLE_VARIABLE_ATTR         = "variable";
    public static final String TABLE_PATH_ATTR             = "tablePath";
    
    ////////////////////////////////////////////////////////////////////
    // Executable model constants
    //

    public static final String EXECUTABLE_MODEL_ELEMENT    = "executableModel";
    public static final String MODEL_CLASS_ATTR            = "class";

    public static final String PARAMETER_ELEMENT           = "parameter";
    public static final String NAME_ATTR                   = "name";
    public static final String VALUE_ATTR                  = "value";

    public static final String CONST_ATTR                  = "constant";

    public static final String VARIABLE_ELEMENT            = "variable";
    public static final String VARIABLE_ATTR               = "variable";
    public static final String DIAGRAM_ELEMENT_ATTR        = "diagramElement";
    public static final String INITIAL_VALUE_ATTR          = "initialValue";
    public static final String UNITS_ATTR                  = "units";
    public static final String BOUNDARY_CONDITION_ATTR     = "boundaryCondition";
    public static final String SHOW_IN_PLOT_ATTR           = "showInPlot";
    public static final String PLOT_LINE_SPEC_ATTR         = "plotLineSpec";

    
    public static final String TIME_VARIABLE = "time";
    public static final String TIME_UNITS_ATTR = "timeUnits";
    
    //units
    public static final String UNIT_DEFINITION_ELEMENT = "unitDefinition";
    public static final String BASE_UNIT_ELEMENT = "baseUnit";
    public static final String BASE_UNIT_TYPE_ATTR = "type";
    public static final String MULTIPLIER_ATTR = "multiplier";
    public static final String SCALE_ATTR = "scale";
    public static final String EXPONENT_ATTR = "exponent";
    
    public static final String TABLE_ELEMENT               = "tableElement";
    public static final String TABLE_ATTR                  = "tableDataCollection";
    public static final String CYCLED_ATTR                 = "cycled";
    public static final String SPLINE_TYPE_ATTR            = "splineType";

    public static final String CONSTRAINT_ELEMENT          = "constraint";
    public static final String EQUATION_ELEMENT            = "equation";
    public static final String FUNCTION_ELEMENT            = "function";
    public static final String FORMULA_ATTR                = "formula";
    public static final String EQUATION_TYPE_ATTR          = "type";

    public static final String EVENT_ELEMENT               = "event";
    public static final String TRIGGER_ATTR                   = "trigger";
    public static final String DELAY_ATTR                          = "delay";
    public static final String PRIORITY_ATTR               = "priority";
    public static final String TRIGGER_PERSISTENT_ATTR     = "persistent";
    public static final String TRIGGER_INITIAL_VALUE_ATTR  = "initialValue";
    public static final String USE_VALUES_FROM_TRIGGER_TIME_ATTR             = "useValeusFromTriggerTime";
    public static final String DELAY_UNIT_ATTR                   = "delayUnits";
    public static final String ASSIGNMENT_ELEMENT          = "assignment";

    public static final String STATE_START_ATTR            = "start";
    public static final String STATE_ELEMENT                 = "state";
    public static final String STATE_ENTRY_ELEMENT         = "entry";
    public static final String STATE_EXIT_ELEMENT          = "exit";
    public static final String STATE_ON_EVENT_ELEMENT      = "on_event";

    public static final String TRANSITION_ELEMENT           = "transition";
    public static final String WHEN_ELEMENT                   = "when";
    public static final String AFTER_ELEMENT               = "after";
    public static final String ON_EVENT_ELEMENT               = "on_event";

    public static final String AFTER_ATTR                   = "after";

    public static final String FIXED_ATTR                   = "fixed";
    
    public static final String LINE_MOVETO                 = "moveTo";
    public static final String LINE_LINETO                 = "lineTo";
    public static final String LINE_QUADRIC                = "quadric";
    public static final String LINE_CUBIC                  = "cubic";

    public static final String PORT_ELEMENT                = "port";
    public static final String NODE_NAME_ATTR              = "node";
    public static final String ORIENTATION_ATTR            = "orientation";

    public static final String MAIN_VARIABLE_ATTR          = "mainVariable";

    public static final String BUS_ELEMENT                 = "bus";
    public static final String BUS_NAME_ATTR               = "busName";
    ////////////////////////////////////////////////////////////////////
    // Additional kernel elements
    //

    public static final String REACTION_ELEMENT            = "reaction";
    public static final String SPECIE_REFERENCE_ELEMENT    = "specieReference";

    public static final String REACTION_FORMULA_ATTR       = "formula";

    public static final String ROLE_ATTR                   = "role";
    public static final String MODIFIER_ACTION_ATTR        = "modifierAction";
    public static final String SPECIE_ATTR                 = "specie";
    public static final String STOICHIOMETRY_ATTR          = "stoichiometry";
    public static final String PARTICIPATION_ATTR          = "participation";

    public static final String ATTRIBUTES_ELEMENT          = "attributes";
    public static final String DATABASE_REFERENCE_ELEMENT = "databaseReference";
    public static final String LITERATURE_REFERENCE_ELEMENT="literatureReference";
    
    //History and authors
    public static final String AUTHORS_ELEMENT = "authors";
    public static final String AUTHOR_ELEMENT = "author";
    public static final String HISTORY_ELEMENT = "history";
    public static final String CREATED_ELEMENT = "created";
    public static final String MODIFIED_ELEMENT = "modified";
    
    public static final String DATE_ATTR = "date";
    public static final String ORGANISATION_ATTR = "organisation";
    public static final String EMAIL_ATTR = "email";
    public static final String GIVEN_NAME_ATTR = "givenName";
    public static final String FAMILY_NAME_ATTR = "familyName";
}
