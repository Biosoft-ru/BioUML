package biouml.standard.type;

/**
 * Definition of data element types (roles).
 */
public interface Type
{
    public static final String TYPE_UNKNOWN = "unknown";
    public static final String TYPE_NOTE = "note";
    public static final String TYPE_NOTE_LINK = "note-edge";
    public static final String TYPE_DIRECTED_LINK = "directed-edge";
    public static final String TYPE_UNDIRECTED_LINK = "undirected-edge";
    public static final String TYPE_SUBDIAGRAM = "subdiagram";
    public static final String TYPE_MODEL_DEFINITION = "modelDefinition";
    public static final String TYPE_BLOCK = "block";

    public static final String TYPE_TABLE = "table";
    
    public static final String TYPE_CONCEPT = "semantic-concept";
    public static final String TYPE_FUNCTION = "semantic-concept-function";
    public static final String TYPE_PROCESS = "semantic-concept-process";
    public static final String TYPE_STATE = "semantic-concept-state";

    public static final String TYPE_PHYSICAL_ENTITY = "physical-entity";

    public static final String TYPE_MOLECULE = "molecule";
    public static final String TYPE_GENE = "molecule-gene";
    public static final String TYPE_RNA = "molecule-RNA";
    public static final String TYPE_TRANSCRIPT = "molecule-transcript";
    public static final String TYPE_PROTEIN = "molecule-protein";
    public static final String TYPE_SUBSTANCE = "molecule-substance";

    public static final String TYPE_COMPARTMENT = "compartment";
    public static final String TYPE_CELL = "compartment-cell";

    public static final String TYPE_REACTION = "reaction";

    public static final String TYPE_RELATION = "relation";
    public static final String TYPE_SEMANTIC_RELATION = "relation-semantic";
    public static final String TYPE_CHEMICAL_ROLE = "relation-chemical";

    public static final String TYPE_DEPENDENCY = "dependency";
    
    public static final String TYPE_STRUCTURE = "structure";

    public static final String TYPE_DIAGRAM_REFERENCE = "diagram-reference";

    public static final String TYPE_PLOT = "plot";
    public static final String TYPE_AVERAGER = "adapter";
    public static final String TYPE_CONSTANT = "constant";
    public static final String TYPE_SWITCH = "switch";
    public static final String TYPE_MODULE = "module";
    
    public static final String TYPE_SIMULATION_RESULT = "simulation-result";
    public static final String TYPE_EXPERIMENT = "experiment";
    public static final String TYPE_INPUT_CONNECTION_PORT = "input connection port";
    public static final String TYPE_OUTPUT_CONNECTION_PORT = "output connection port";
    public static final String TYPE_CONTACT_CONNECTION_PORT = "contact connection port";
    public static final String TYPE_PORT = "port";
    public static final String TYPE_CONNECTION_BUS = "connection bus";
    public static final String TYPE_DATA_GENERATOR = "simulation-data-generator";
    
    public static final String TYPE_XML_NOTATION = "xml notation";
    
    public static final String TYPE_TABLE_ELEMENT = "table entity";

    //----- meta-data and utility types ---------------------------------------/
    public static final String DATABASE_INFO = "info-database";
    public static final String DIAGRAM_INFO = "info-diagram";

    public static final String CONSTANT = "constant";

    //----- mathematical  types -----------------------------------------------/
    public static final String MATH = "math";
    public static final String MATH_VARIABLE = "math-variable";
    public static final String MATH_EVENT = "math-event";
    public static final String MATH_FUNCTION = "math-function";
    public static final String MATH_EQUATION = "math-equation";
    public static final String MATH_STATE = "math-state";
    public static final String MATH_TRANSITION = "math-transition";
    public static final String MATH_CONSTRAINT = "math-constraint";

    //----- analysis  types ---------------------------------------------------/
    public static final String ANALYSIS_METHOD = "analysis-method";
    public static final String ANALYSIS_SCRIPT = "analysis-script";
    public static final String ANALYSIS_QUERY = "analysis-query";
    public static final String ANALYSIS_IMPORT = "analysis-import";
    public static final String ANALYSIS_TABLE = "analysis-table";
    public static final String ANALYSIS_PLOT = "analysis-plot";

    //----- workflow types  ---------------------------------------------------/
    public static final String TYPE_DATA_ELEMENT = "data-element";
    public static final String TYPE_DATA_ELEMENT_IN = "data-element-in";
    public static final String TYPE_DATA_ELEMENT_OUT = "data-element-out";
    public static final String TYPE_INPUT_PORT = "input-port";
    public static final String TYPE_OUTPUT_PORT = "output-port";
    public static final String ANALYSIS_PARAMETER = "analysis-parameter";
    public static final String ANALYSIS_EXPRESSION = "analysis-expression";
    public static final String ANALYSIS_CYCLE_VARIABLE = "cycle-variable";
    public static final String ANALYSIS_CYCLE = "cycle";
    public static final String TYPE_LISTENER_LINK = "listener-edge";
}
