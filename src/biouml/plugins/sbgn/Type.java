package biouml.plugins.sbgn;

/**
 * @author Ilya
 * kernel types for elements of SBGN-SBML diagram type
 */
public class Type
{
    public static final String TYPE_TABLE = "table";
    
    public static final String TYPE_ENTITY = "entity";
    public static final String TYPE_COMPARTMENT = "compartment";
    public static final String TYPE_PHENOTYPE = "phenotype";
    public static final String TYPE_SOURCE_SINK = "source-sink";
    public static final String TYPE_VARIABLE = "variable";
    public static final String TYPE_UNIT_OF_INFORMATION = "unit of information";
    
    public static final String TYPE_EQUATION = "math-equation";
    public static final String TYPE_FUNCTION = "math-function";
    public static final String TYPE_EVENT = "math-event";
    public static final String TYPE_CONSTRAINT = "math-constraint";
    public static final String TYPE_NOTELINK = "notelink";
    public static final String TYPE_NOTE = "note";
    public static final String TYPE_PORT = "port";
    public static final String TYPE_CONSUMPTION = "consumption";
    public static final String TYPE_PRODUCTION = "production";
    public static final String TYPE_PORTLINK = "portlink";
    public static final String TYPE_REGULATION = "regulation";
    
    //composite issues
    public static final String TYPE_SUBDIAGRAM = "subdiagram";
    public static final String TYPE_MODEL_DEFINITION = "modelDefinition";

    //entity pool node types
    public static final String TYPE_MACROMOLECULE = "macromolecule";
    public static final String TYPE_SIMPLE_CHEMICAL = "simple chemical";
    public static final String TYPE_UNSPECIFIED = "unspecified";
    public static final String TYPE_NUCLEIC_ACID_FEATURE = "nucleic acid feature";
    public static final String TYPE_PERTURBING_AGENT = "perturbing agent";
    public static final String TYPE_COMPLEX = "complex";

    //reaction types
    public static final String TYPE_PROCESS = "process";
   
    public static final String TYPE_OMITTED_PROCESS = "omitted process";
    public static final String TYPE_ASSOCIATION = "association";
    public static final String TYPE_UNCERTAIN_PROCESS = "uncertain process";
    public static final String TYPE_DISSOCIATION = "dissociation";
    
    //modifier types
    public static final String TYPE_CATALYSIS = "catalysis";
    public static final String TYPE_INHIBITION = "inhibition";
    public static final String TYPE_MODULATION = "modulation";
    public static final String TYPE_STIMULATION = "stimulation";
    public static final String TYPE_NECCESSARY_STIMULATION = "necessary stimulation";
    
    //other types
    public static final String TYPE_LOGICAL = "logical operator";
    public static final String TYPE_EQUIVALENCE = "equivalence operator";
    public static final String TYPE_EQUIVALENCE_ARC = "equivalence arc";
    public static final String TYPE_LOGIC_ARC = "logic arc";
}
    