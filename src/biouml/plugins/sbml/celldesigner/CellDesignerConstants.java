package biouml.plugins.sbml.celldesigner;

public interface CellDesignerConstants
{
    public static final String CELLDESIGNER_BASE = "celldesigner";
    public static final String CELLDESIGNER_EXTENSION = CELLDESIGNER_BASE + ":extension";
    public static final String CELLDESIGNER_SPECIE_ALIASES_LIST = CELLDESIGNER_BASE + ":listOfSpeciesAliases";
    public static final String CELLDESIGNER_SPECIE_ALIAS = CELLDESIGNER_BASE + ":speciesAlias";
    public static final String CELLDESIGNER_SPECIE_INCLUDED_LIST = CELLDESIGNER_BASE + ":listOfIncludedSpecies";
    public static final String CELLDESIGNER_SPECIE = CELLDESIGNER_BASE + ":species";
    public static final String CELLDESIGNER_COMPARTMENT_ALIASES_LIST = CELLDESIGNER_BASE + ":listOfCompartmentAliases";
    public static final String CELLDESIGNER_COMPARTMENT_ALIAS = CELLDESIGNER_BASE + ":compartmentAlias";
    public static final String CELLDESIGNER_COMPLEX_ALIASES_LIST = CELLDESIGNER_BASE + ":listOfComplexSpeciesAliases";
    public static final String CELLDESIGNER_COMPLEX_ALIAS = CELLDESIGNER_BASE + ":complexSpeciesAlias";
    public static final String CELLDESIGNER_PROTEIN_LIST = CELLDESIGNER_BASE + ":listOfProteins";
    public static final String CELLDESIGNER_PROTEIN = CELLDESIGNER_BASE + ":protein";
    public static final String CELLDESIGNER_MODIFICATION_RESIDUES_LIST = CELLDESIGNER_BASE + ":listOfModificationResidues";
    public static final String CELLDESIGNER_MODIFICATION_RESIDUE = CELLDESIGNER_BASE + ":modificationResidue";
    public static final String CELLDESIGNER_SPECIE_IDENTITY = CELLDESIGNER_BASE + ":speciesIdentity";
    public static final String CELLDESIGNER_BOUNDS = CELLDESIGNER_BASE + ":bounds";
    public static final String CELLDESIGNER_DOUBLE_LINE = CELLDESIGNER_BASE + ":doubleLine";
    public static final String CELLDESIGNER_POINT = CELLDESIGNER_BASE + ":point";
    public static final String CELLDESIGNER_NAME_POINT = CELLDESIGNER_BASE + ":namePoint";
    public static final String CELLDESIGNER_USUAL_VIEW = CELLDESIGNER_BASE + ":usualView";
    public static final String CELLDESIGNER_PAINT = CELLDESIGNER_BASE + ":paint";
    public static final String CELLDESIGNER_CLASS = CELLDESIGNER_BASE + ":class";
    public static final String CELLDESIGNER_EDIT_POINTS = CELLDESIGNER_BASE + ":editPoints";
    public static final String CELLDESIGNER_REACTION_TYPE = CELLDESIGNER_BASE + ":reactionType";
    public static final String CELLDESIGNER_LINE = CELLDESIGNER_BASE + ":line";
    public static final String CELLDESIGNER_MODIFICATION_LIST = CELLDESIGNER_BASE + ":listOfModification";
    public static final String CELLDESIGNER_MODIFICATION = CELLDESIGNER_BASE + ":modification";
    public static final String CELLDESIGNER_STATE = CELLDESIGNER_BASE + ":state";
    public static final String CELLDESIGNER_HOMODIMER = CELLDESIGNER_BASE + ":homodimer";
    public static final String CELLDESIGNER_STRUCTUAL_STATES_LIST = CELLDESIGNER_BASE + ":listOfStructuralStates";
    public static final String CELLDESIGNER_STRUCTUAL_STATE = CELLDESIGNER_BASE + ":structuralState";
    public static final String CELLDESIGNER_MODIFICATIONS_LIST = CELLDESIGNER_BASE + ":listOfModifications";
    public static final String CELLDESIGNER_ANNOTATION = CELLDESIGNER_BASE + ":annotation";
    public static final String CELLDESIGNER_COMPLEX_SPECIES = CELLDESIGNER_BASE + ":complexSpecies";
    public static final String CELLDESIGNER_PROTEIN_REFERENCE = CELLDESIGNER_BASE + ":proteinReference";
    public static final String CELLDESIGNER_REACTANT_LIST = CELLDESIGNER_BASE + ":baseReactants";
    public static final String CELLDESIGNER_REACTANT = CELLDESIGNER_BASE + ":baseReactant";
    public static final String CELLDESIGNER_PRODUCT_LIST = CELLDESIGNER_BASE + ":baseProducts";
    public static final String CELLDESIGNER_PRODUCT = CELLDESIGNER_BASE + ":baseProduct";
    public static final String CELLDESIGNER_LINK_ANCHOR = CELLDESIGNER_BASE + ":linkAnchor";
    public static final String CELLDESIGNER_LINK_TARGET = CELLDESIGNER_BASE + ":linkTarget";
    public static final String CELLDESIGNER_REACTANT_LINKS_LIST = CELLDESIGNER_BASE + ":listOfReactantLinks";
    public static final String CELLDESIGNER_REACTANT_LINK = CELLDESIGNER_BASE + ":reactantLink";
    public static final String CELLDESIGNER_PRODUCT_LINKS_LIST = CELLDESIGNER_BASE + ":listOfProductLinks";
    public static final String CELLDESIGNER_PRODUCT_LINK = CELLDESIGNER_BASE + ":productLink";
    public static final String CELLDESIGNER_ACTIVITY = CELLDESIGNER_BASE + ":activity";
    public static final String CELLDESIGNER_ALIAS = CELLDESIGNER_BASE + ":alias";
    public static final String CELLDESIGNER_INNER_ALIAS_LIST = CELLDESIGNER_BASE + ":listOfInnerAliases";
    public static final String CELLDESIGNER_INNER_ALIAS = CELLDESIGNER_BASE + ":innerAlias";
    public static final String CELLDESIGNER_HETERODIMER = CELLDESIGNER_BASE + ":heterodimerIdentity";
    public static final String CELLDESIGNER_HETERODIMER_ENRTY_LIST = CELLDESIGNER_BASE + ":listOfHeterodimerEntries";
    public static final String CELLDESIGNER_HETERODIMER_ENRTY = CELLDESIGNER_BASE + ":heterodimerEntry";
    public static final String CELLDESIGNER_NAME = CELLDESIGNER_BASE + ":name";

    public static final String ID_ATTR = "id";
    public static final String NAME_ATTR = "name";
    public static final String SPECIES_ATTR = "species";
    public static final String ALIAS_ATTR = "alias";
    public static final String COMPARTMENT_ATTR = "compartment";
    public static final String X_ATTR = "x";
    public static final String Y_ATTR = "y";
    public static final String W_ATTR = "w";
    public static final String H_ATTR = "h";
    public static final String NUM_ATTR = "num";
    public static final String COLOR_ATTR = "color";
    public static final String WIDTH_ATTR = "width";
    public static final String SCHEME_ATTR = "scheme";
    public static final String STRUCTURAL_STATE_ATTR = "structuralState";
    public static final String COMPLEX_ALIAS_ATTR = "complexSpeciesAlias";
    public static final String COMPARTMENT_ALIAS_ATTR = "compartmentAlias";
    public static final String EDIT_POINTS_ATTR = "editPoints";
    public static final String ANGLE_ATTR = "angle";
    public static final String THICKNESS_ATTR = "thickness";
    public static final String OUTER_WIDTH_ATTR = "outerWidth";
    public static final String INNER_WIDTH_ATTR = "innerWidth";
    public static final String POSITION_ATTR = "position";
    public static final String PRODUCT_ATTR = "product";
    public static final String REACTANT_ATTR = "reactant";
    public static final String TYPE_ATTR = "type";
    public static final String RESIDUE_ATTR = "residue";
    public static final String STATE_ATTR = "state";
    public static final String HETERODIMER_ATTR = "heterodimerEntry";
    public static final String INNER_ID_ATTR = "innerId";

    public static final String MODIFIERS_ATTR = "modifiers";
    public static final String ALIASES_ATTR = "aliases";

    public static final String PARENT_NODE_ATTR = "parent"; //attribute name for complex subelement parent
    public static final String FIXED_SIZE_ATTR = "fixedSize"; //attribute indicates of fixed compartment size
    
    public static final double REACTION_DELTA = 7;
}
