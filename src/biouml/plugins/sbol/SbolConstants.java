package biouml.plugins.sbol;

public class SbolConstants
{
    public static final String HTTPS_BIOUML_ORG = "https://biouml.org";
    public static final String SBOL_DOCUMENT_PROPERTY = "sbol_document";
    public static final String NODE_IMAGE = "node-image";
    
    public static final String COMPLEX = "Complex";
    public static final String SIMPLE_CHEMICAL = "Simple chemical";
    public static final String PROTEIN = "Protein";

    //Interaction types
    public static final String INHIBITION = "Inhibition";
    public static final String STIMULATION = "Stimulation";
    public static final String BIOCHEMICAL_REACTION = "Biochemical Reaction";
    public static final String DEGRADATION = "Degradation";
    public static final String DEGRADATION_PRODUCT = "degradation product";
    public static final String GENETIC_PRODUCTION = "Genetic Production";
    public static final String CONTROL = "Control";
    public static final String PROCESS = "Process";
    public static final String ASSOCIATION = "Association";
    public static final String DISSOCIATION = "Dissociation";
    
    //Participation roles
    public static final String INHIBITOR = "Inhibitor";
    public static final String INHIBITED = "Inhibited";
    public static final String STIMULATOR = "Stimulator";
    public static final String STIMULATED = "Stimulated";
    public static final String PRODUCT = "Product";
    public static final String REACTANT = "Reactant";
    public static final String PROMOTER = "Promoter";
    public static final String MODIFIER = "Modifier";
    public static final String MODIFIED = "Modified";
    public static final String TEMPLATE = "Template";
            
    public static final String[] interactionTypes = new String[] {ASSOCIATION, BIOCHEMICAL_REACTION, DISSOCIATION, DEGRADATION, CONTROL, GENETIC_PRODUCTION, INHIBITION, PROCESS, STIMULATION};
    public static final String[] participationTypes = new String[] {INHIBITOR, INHIBITED, STIMULATOR, STIMULATED, PRODUCT, REACTANT, MODIFIER, MODIFIED, TEMPLATE};

    public static final String STRAND_DOUBLE = "Double-stranded";
    public static final String STRAND_SINGLE = "Single-stranded";
    public static final String TOPOLOGY_LINEAR = "Linear";
    public static final String TOPOLOGY_CIRCULAR = "Circular";
    public static final String TOPOLOGY_LOCUS = "Locus";
    
    public static String[] topologyTypes = new String[] {TOPOLOGY_LINEAR, TOPOLOGY_CIRCULAR, TOPOLOGY_LOCUS};
    public static String[] strandTypes = new String[] {STRAND_SINGLE, STRAND_DOUBLE};


    static final String TYPE_RNA = "RNA";
    static final String TYPE_DNA = "DNA";
    static final String ROLE_SEQUENCE_FEATURE = "Sequence feature";
    public static final String TYPE_DEGRADATION_PRODUCT = "degradation product";

}
