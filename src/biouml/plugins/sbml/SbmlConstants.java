package biouml.plugins.sbml;

public class SbmlConstants
{
    public static final String SBML_ELEMENT             = "sbml";
    public static final String SBML_LEVEL_ATTR             = "level";
    public static final String SBML_LEVEL_VALUE_1         = "1";
    public static final String SBML_LEVEL_VALUE_2         = "2";
    public static final String SBML_LEVEL_VALUE_3       = "3";
    public static final String SBML_VERSION_ATTR         = "version";
    public static final String SBML_VERSION_VALUE_1        = "1";
    public static final String SBML_VERSION_VALUE_2        = "2";
    public static final String SBML_VERSION_VALUE_3     = "3";
    public static final String SBML_VERSION_VALUE_4     = "4";
    public static final String SBML_VERSION_VALUE_5     = "5";
    
    public static final String[] SBML_SUPPORTED_FORMATS     = new String[]{"L1V1", "L1V2", "L2V1", "L2V2", "L2V3", "L2V4", "L2V5", "L3V1", "L3V2"};
    
    public static final String SBML_LEVEL1_XMLNS_VALUE = "http://www.sbml.org/sbml/level1";
    public static final String SBML_LEVEL2_XMLNS_VALUE = "http://www.sbml.org/sbml/level2";
    public static final String SBML_LEVEL3_XMLNS_VALUE = "http://www.sbml.org/sbml/level3";

    public static final String XMLNS_ATTR                 = "xmlns";

    public static final String ID_ATTR                 = "id";
    public static final String METAID_ATTR             = "metaid";
    public static final String NAME_ATTR               = "name";
    public static final String UNITS_ATTR              = "units";
    public static final String NOTES_ELEMENT           = "notes";
    public static final String ANNOTATION_ELEMENT      = "annotation";
    public static final String TYPE_ATTR               = "type";

    public static final String CONSTANT_ATTR           = "constant";
    public static final String SBO_TERM_ATTR           = "sboTerm";

    public static final String MODEL_ELEMENT           = "model";

    //model properties
    public static final String CONVERSION_FACTOR_ATTR         = "conversionFactor";
    public static final String VOLUME_UNITS_ATTR              = "volumeUnits";
    public static final String SUBSTANCE_UNITS_ATTR         = "substanceUnits";
    public static final String AREA_UNITS_ATTR              = "areaUnits";
    public static final String LENGTH_UNITS_ATTR            = "lengthUnits";
    public static final String EXTENT_UNITS_ATTR            = "extentUnits";
    
    public static final String XML_DIAGRAM_TYPE_ELEMENT       = "xmlDiagramType";
    
    public static final String FUNCTION_LIST_ELEMENT           = "listOfFunctionDefinitions";
    public static final String FUNCTION_DEFINITION_ELEMENT       = "functionDefinition";
    
    public static final String UNIT_DEFINITION_LIST_ELEMENT   = "listOfUnitDefinitions";
    public static final String UNIT_DEFINITION_ELEMENT        = "unitDefinition";
    public static final String UNIT_LIST_ELEMENT              = "listOfUnits";
    public static final String UNIT_ELEMENT                   = "unit";
    public static final String UNIT_COMMENT_ATTR              = "comment";
    public static final String UNIT_KIND_ATTR                 = "kind";
    public static final String UNIT_EXPONENT_ATTR             = "exponent";
    public static final String UNIT_SCALE_ATTR                = "scale";
    public static final String UNIT_MULTIPLIER_ATTR           = "multiplier";
    public static final String UNIT_SUBSTANCE                   = "substance";
    public static final String UNIT_TIME                       = "time";
    public static final String UNIT_VOLUME                       = "volume";

    public static final String COMPARTMENT_TYPE_LIST_ELEMENT  = "listOfCompartmentTypes";
    public static final String COMPARTMENT_TYPE_ELEMENT       = "compartmentType";
    public static final String COMPARTMENT_LIST_ELEMENT       = "listOfCompartments";
    public static final String COMPARTMENT_ELEMENT               = "compartment";
    public static final String COMPARTMENT_VOLUME_ATTR           = "volume";
    public static final String COMPARTMENT_OUTSIDE_ATTR       = "outside";
    public static final String COMPARTMENT_DIMENSION_ATTR       = "spatialDimensions";
    public static final String COMPARTMENT_SIZE_ATTR          = "size";
    public static final String COMPARTMENT_TYPE_ATTR          = "compartmentType";

    public static final String SPECIE_TYPE_LIST_ELEMENT       = "listOfSpeciesTypes";
    public static final String SPECIE_TYPE_ELEMENT            = "speciesType";
    public static final String SPECIE_LIST_ELEMENT               = "listOfSpecies";
    public static final String SPECIE_ELEMENT_11                 = "specie";
    public static final String SPECIE_ELEMENT                   = "species";
    public static final String COMPARTMENT_ATTR               = "compartment";
    public static final String SPECIE_INITIAL_AMOUNT_ATTR       = "initialAmount";
    public static final String SPECIE_SUBSTANCE_UNITS_ATTR       = "substanceUnits";
    public static final String SPECIE_CHARGE_ATTR               = "charge";
    public static final String SPECIE_TYPE_ATTR               = "speciesType";
    
    public static final String INITIAL_ASSIGNMENT_LIST_ELEMENT     = "listOfInitialAssignments";
    public static final String INITIAL_ASSIGNMENT_ELEMENT          = "initialAssignment";
    public static final String CONSTRAINT_LIST_ELEMENT      = "listOfConstraints";
    public static final String CONSTRAINT_ELEMENT           = "constraint";

    public static final String SPECIE_DEFAULT_SUBSTANCE_UNITS     = "dimensionless";
    
    public static final String SPECIE_EMPTY_SET             = "EmptySet";

    public static final String DEFAULT_SPATIAL_SIZE_UNITS(int dimention)
    {
        switch (dimention)
        {
            case 1:
                return "meter";
            case 2:
                return "square meter";
            case 3:
                return "cubical meter";
            default:
                return null;
        }
    }
    
//    private static final double SBML2LEVEL_DEFAULT_COMPARTMENT_SIZE = 0.9999999999;
//    public static final double DEFAULT_COMPARTMENT_SIZE(String sbmlLevel)
//    {
//        if (sbmlLevel.equals("1")) return 1.0;
//        else if (sbmlLevel.equals("2")) return SBML2LEVEL_DEFAULT_COMPARTMENT_SIZE;
//        else return 1.0;
//    }
    

    public static final String SPECIE_BOUNDARY_CONDITION_ATTR         = "boundaryCondition";
    public static final String SPECIE_INITIAL_CONCENTRATION_ATTR     = "initialConcentration";
    public static final String SPECIE_HAS_ONLY_SUBSTANCE_UNITS_ATTR = "hasOnlySubstanceUnits";

    public static final String PARAMETER_LIST_ELEMENT         = "listOfParameters";
    public static final String PARAMETER_ELEMENT             = "parameter";
    public static final String PARAMETER_VALUE_ATTR         = "value";
    
    public static final String POSITIVE_INFINITY      = "INF";
    public static final String NAN                    = "NaN";
    public static final String NEGATIVE_INFINITY      = "-INF";

    public static final String RULE_LIST_ELEMENT             = "listOfRules";
    public static final String RULE_RATE_ELEMENT             = "rateRule";
    public static final String RULE_ALGEBRAIC_ELEMENT         = "algebraicRule";
    public static final String RULE_ASSIGNEMENT_ELEMENT     = "assignmentRule";

    public static final String RULE_VARIABLE_ATTR             = "variable";
    public static final String RULE_FORMULA_ATTR             = "formula";
    public static final String RULE_COMMENT_ATTR             = "comment";
    public static final String RULE_PARAMETER_ELEMENT         = "parameterRule";

    public static final String RULE_SPECIE_CONCENTRATION_ELEMENT    = "specieConcentrationRule";
    public static final String RULE_SPECIE_CONCENTRATION_ELEMENT_L1V2    = "speciesConcentrationRule";
    public static final String RULE_COMPARTMENT_VOLUME_ELEMENT         = "compartmentVolumeRule";
    
    public static final String INITIAL_ASSIGNMENT_VARIABLE_ATTR         = "symbol";

    public static final String CONSTRAINT_MESSAGE_ELEMENT = "message";
    public static final String CONSTRAINT_P_ELEMENT = "p";
    
    public static final String REACTION_LIST_ELEMENT         = "listOfReactions";
    public static final String REACTION_ELEMENT             = "reaction";
    public static final String REACTION_REVERSIBLE_ATTR     = "reversible";
    public static final String REACTION_FAST_ATTR             = "fast";
    public static final String REACTANT_LIST_ELEMENT         = "listOfReactants";
    public static final String PRODUCT_LIST_ELEMENT         = "listOfProducts";
    public static final String LOCAL_PARAMETER_LIST_ELEMENT_L2         = "listOfParameters";
    public static final String LOCAL_PARAMETER_LIST_ELEMENT_L3         = "listOfLocalParameters";
    public static final String LOCAL_PARAMETER_ELEMENT_L2         = "parameter";
    public static final String LOCAL_PARAMETER_ELEMENT_L3         = "localParameter";
    public static final String MODIFIER_LIST_ELEMENT         = "listOfModifiers";
    public static final String REACTANT_STUB                 = "_in_empty_set";
    public static final String PRODUCT_STUB                 = "_out_empty_set";

    public static final String SPECIE_REFERENCE_ELEMENT_11     = "specieReference";
    public static final String SPECIE_REFERENCE_ELEMENT     = "speciesReference";
    public static final String MODIFIER_SPECIE_REFERENCE_ELEMENT     ="modifierSpeciesReference";
    public static final String SPECIE_ATTR_11                 = "specie";
    public static final String SPECIE_ATTR                     = "species";
    public static final String STOICHIOMETRY_ATTR             = "stoichiometry";
    public static final String DENOMINATOR_ATTR             = "denominator";

    public static final String KINETIC_LAW_ELEMENT             = "kineticLaw";
    public static final String STOICHIOMETRY_MATH_ELEMENT     = "stoichiometryMath";
    public static final String MATH_ELEMENT                 = "math";
    public static final String FORMULA_ATTR                 = "formula";
    public static final String TIME_UNITS_ATTR                 = "timeUnits";

    public static final String EVENT_LIST_ELEMENT             = "listOfEvents";
    public static final String EVENT_ELEMENT                 = "event";
    public static final String EVENT_USE_VALUES_FROM_TRIGGER_TIME_ATTR = "useValuesFromTriggerTime";
    public static final String PRIORITY_ELEMENT             = "priority";
    public static final String TRIGGER_ELEMENT                 = "trigger";
    public static final String TRIGGER_PERSISTENT_ATTR      = "persistent";
    public static final String TRIGGER_INITIAL_VALUE_ATTR   = "initialValue";
    public static final String DELAY_ELEMENT                 = "delay";
    public static final String DELAY_TIME_UNITS_ATTR        = "units";
    public static final String ASSIGNEMENT_LIST_ELEMENT     = "listOfEventAssignments";
    public static final String ASSIGNEMENT_ELEMENT             = "eventAssignment";
    public static final String ASSIGNEMENT_VARIABLE_ATTR     = "variable";
    

    // BioUML extension
    public static final String BIOUML_XMLNS_ATTR                = "xmlns:biouml";
    public static final String BIOUML_XMLNS_VALUE               = "http://www.biouml.org/ns";
    public static final String BIOUML_ELEMENT                   = "biouml:BIOUML";
    public static final String BIOUML_COMPARTMENT_INFO_ELEMENT  = "biouml:compartmentInfo";
    public static final String BIOUML_NODE_INFO_ELEMENT         = "biouml:nodeInfo";
    public static final String BIOUML_EDGE_INFO_ELEMENT         = "biouml:edgeInfo";
    public static final String BIOUML_DIAGRAM_INFO_ELEMENT      = "biouml:diagramInfo";
    public static final String BIOUML_SIMULATION_INFO_ELEMENT   = "biouml:simulationInfo";
    public static final String BIOUML_DB_INFO_ELEMENT           = "biouml:dbInfo";
    public static final String BIOUML_VIEW_OPTIONS_ELEMENT      = "biouml:viewOptions";
    public static final String BIOUML_SPECIE_INFO_ELEMENT       = "biouml:specieInfo";
    public static final String BIOUML_REACTION_INFO_ELEMENT     = "biouml:reactionInfo";
    public static final String BIOUML_VARIABLE_INFO_ELEMENT     = "biouml:variableInfo";
    public static final String BIOUML_PLOT_INFO_ELEMENT         = "biouml:plotInfo";
    
    protected static final String BIOUML_DIAGRAM_REFERENCE = "biouml:diagramReference";
    protected static final String BIOUML_DIAGRAM_PATH = "path";
    
    public static final String BIOUML_VARIABLE_SHOW_IN_PLOT     = "showInPlot";
    public static final String BIOUML_VARIABLE_PLOT_SPEC        = "plotSpec";
    public static final String BIOUML_VARIABLE_COMMENT          = "comment";
    
    public static final String BIOUML_AUTOLAYOUT_ATTR           = "autoLayout";
    public static final String BIOUML_DEPENDENCY_EDGES_ATTR     = "dependencyEdges";
    public static final String BIOUML_CONVERTER_ATTR            = "converter";
    public static final String BIOUML_REFERENCE_TYPE_ATTR       = "referenceType";
    public static final String BIOUML_BIOHUB_ATTR               = "bioHub";
    public static final String BIOUML_COMPLETE_NAME_ATTR        = "completeName";
    public static final String BIOUML_DEFAULT_COMPARTMENT_ATTR  = "isDefault";
    public static final String BIOUML_SPECIE_TYPE_ATTR          = "type";
    public static final String BIOUML_INITIAL_TIME_ATTR         = "initialTime";
    public static final String BIOUML_COMPLETION_TIME_ATTR      = "completionTime";
    public static final String BIOUML_TIME_INCREMENT_ATTR       = "timeIncrement";
    public static final String BIOUML_SPECIES_ATTR              = "species";
    
    public static final String COMP_PACKAGE = "comp";
    public static final String PACKAGES_ATTR = "Packages";
}
