
package biouml.plugins.cellml;


public interface CellMLConstants
{
    public static final String ENVIRONMENT        = "environment";

    public static final String MODEL_ELEMENT      = "model";
    public static final String NAME_ATTR          = "name";
    public static final String MATH_ELEMENT       = "math";

    public static final String COMPONENT_ELEMENT        = "component";
    public static final String VARIABLE_ELEMENT         = "variable";
    public static final String INITIAL_VALUE_ATTR       = "initial_value";
    public static final String UNITS_ATTR               = "units";
    public static final String PUBLIC_INTERFACE_ATTR    = "public_interface";
    public static final String PUBLIC_INTERFACE_NONE    = "none";

    public static final String REACTION_ELEMENT         = "reaction";
    public static final String REACTION_REVERSIBLE_ATTR = "reversible";
    public static final String VARIABLE_REF_ELEMENT     = "variable_ref";
    public static final String VARIABLE_ATTR            = "variable";

    public static final String ROLE_ELEMENT             = "role";
    public static final String ROLE_ATTR                = "role";
    public static final String ROLE_REACTANT            = "reactant";
    public static final String ROLE_PRODUCT             = "product";
    public static final String ROLE_ACTIVATOR           = "activator";
    public static final String ROLE_CATALYST            = "catalyst";
    public static final String ROLE_INHIBITOR           = "inhibitor";
    public static final String ROLE_MODIFIER            = "modifier";
    public static final String ROLE_RATE                = "rate";

    public static final String DIRECTION_ATTR           = "direction";
    public static final String STOICHIOMETRY_ATTR       = "stoichiometry";

    public static final String CONNECTION_ELEMENT       = "connection";
    public static final String MAP_COMPONENTS_ELEMENT   = "map_components";
    public static final String COMPONENT_1_ATTR         = "component_1";
    public static final String COMPONENT_2_ATTR         = "component_2";

    // RDF issues
    public static final String RDF_ELEMENT              = "rdf:RDF";

    // BioUML extension
    public static final String BIOUML_XMLNS_ATTR                = "xmlns:biouml";
    public static final String BIOUML_XMLNS_VALUE               = "http://www.biouml.org/ns";
    public static final String BIOUML_COMPARTMENT_INFO_ELEMENT  = "biouml:compartmentInfo";
    public static final String BIOUML_NODE_INFO_ELEMENT         = "biouml:nodeInfo";
    public static final String BIOUML_SPECIES_INFO_ELEMENT      = "biouml:speciesInfo";
    public static final String BIOUML_SPECIES_TYPE_ATTR         = "type";
    public static final String BIOUML_EDGE_INFO_ELEMENT         = "biouml:edgeInfo";
}

