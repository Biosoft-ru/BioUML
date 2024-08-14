package biouml.standard.type;

import java.util.ListResourceBundle;

import one.util.streamex.StreamEx;

/**
 *
 * @pending description for RE
 */
public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch( Throwable th )
        {

        }
        return key;
    }

    String[] conceptTypes = {"concept", "function", "process", "state"};

    String[] rnaTypes = {"primary transcript", "precursor RNA", "mRNA", "rRNA", "tRNA", "scRNA", "snRNA", "snoRNA", "dsRNA", "other"};

    String[] proteinFunctionalStates = {"active", "inactive", "unknown"};
    String[] proteinStructures = {"monomer", "homodimer", "heterodimer", "multimer", "unknown"};
    String[] proteinModifications = {"none", "phosphorylated", "fatty_acylation", "prenylation", "cholesterolation", "ubiquitination",
            "sumolation", "glycation", "gpi_anchor", "unknown"};

    String[] specieRoles = {SpecieReference.REACTANT, SpecieReference.PRODUCT, SpecieReference.MODIFIER};
    String[] modifierActions = {SpecieReference.ACTION_CATALYST, SpecieReference.ACTION_INHIBITOR, SpecieReference.ACTION_SWITCH_ON,
            SpecieReference.ACTION_SWITCH_OFF};
    String[] participationTypes = {Relation.PARTICIPATION_DIRECT, Relation.PARTICIPATION_INDIRECT, Relation.PARTICIPATION_UNKNOWN};

    /** @pending move in some utility class */
    static String makeHtmlList(String[] values)
    {
        return StreamEx.of( values ).map( val -> "<li>" + val + "</li>" ).joining( "", "<ul>", "</ul>" );
    }


    /**
     * @pending description for functional state values.
     * @pending description for protein structure values.
     * @pending extend protein modifications and make description for them.
     */
    private final Object[][] contents = {
            //-----Unit & constant specific properties -------------------------/
            {"CN_UNIT", "Unit"},
            {"CD_UNIT", "The unit expressed in base units. Constant, paremeter or varable unit."},
            
            {"CN_BASE_UNIT", "Base unit"},
            {"CD_BASE_UNIT", "Base unit (type | multiplier | scale | exponent)."},

            {"CN_CONSTANT", "Constant"},
            {"CD_CONSTANT", "Equation or function constant."},

            {"PN_CONSTANT_VALUE", "Value"},
            {"PD_CONSTANT_VALUE", "Constant value."},

            {"CN_DATABASE_INFO", "Database info"},
            {"CD_DATABASE_INFO", "Describes basic information about the database " + "used in this database."},

            {"PN_DATABASE_INFO", "Database info"},
            {"PD_DATABASE_INFO", "Describes basic information about the database " + "used in this database."},

            {"PN_QUERY_BY_ID", "Query by ID"},
            {
                    "PD_QUERY_BY_ID",
                    "Template to generate URL to get access for corresponding " + "database record by its identifer (ID). "
                            + "Here <code>$id$</code> will be replaced " + "by actual record identifier."},

            {"PN_QUERY_BY_AC", "Query by AC"},
            {
                    "PD_QUERY_BY_AC",
                    "Template to generate URL to get access for corresponding " + "database record by its accession number (AC). "
                            + "Here <code>$id$</code> will be replaced " + "by actual record accession number."},

            //----- Diagram specific fields ------------------------------------/
            {"CN_DIAGRAM_INFO", "Diagram info"},
            {"CD_DIAGRAM_INFO", "Information about the diagram."},

            //----- Note and NoteEdges specific fields ------------------------------------/
            {"CN_NOTE", "Note"},
            {
                    "CD_NOTE",
                    "Arbitrary HTML text that will be shown<br>" + "on the diagram as text box.<br>"
                            + "Option 'textOnly' allows hide the border."},

            {"CN_NOTE_LINK", "Note link"},
            {"CD_NOTE_LINK", "An edge that shows with which diagram element note is assotiated."},
            
            {"CN_DIRECTED_LINK", "Directed link"},
            {"CD_DIRECTED_LINK", "Directed connection link."},
            
            {"CN_UNDIRECTED_LINK", "Undirected link"},
            {"CD_UNDIRECTED_LINK", "Undirected connection link."},
            
            {"CN_SUBDIAGRAM", "Submodel"},
            {"CD_SUBDIAGRAM", "Submodel element."},
            
            {"CN_DEPENDENCY", "Dependency"},
            {"CD_DEPENDENCY", "Parameter dependency."},
            
            {"CN_BLOCK", "Block"},
            {"CD_BLOCK", "A group of equations."},
            
            {"CN_TABLE_ELEMENT", "Table element"},
            {"CD_TABLE_ELEMENT", "Table element."},
            
            {"PN_TABLE_DATA_ELEMENT", "Table data collection"},
            {"PD_TABLE_DATA_ELEMENT", "Table data collection."},
            
            {"CN_CONNECTION", "Connection"},
            {"CD_CONNECTION", "Connection point."},

            //----- Literature specific fields ---------------------------------/
            {"PN_LITERATURE_REFERENCES", "Bibliography"},
            {
                    "PD_LITERATURE_REFERENCES",
                    "The field contains references to the original papers. "
                            + "The reference provides access to the paper within the database from which "
                            + "the data has been extracted. " + "<p> The format is:" + "<pre>Reference identifier</pre>" + "Example:"
                            + "<pre>Gilmour K.C. and Reich N.C., 1995</pre>"},

            {"CN_PUBLICATION", "Bibliography"},
            {"CD_PUBLICATION", "The most important literature references relevant to this data element."},

            {"PN_REFERENCE", "Reference"},
            {
                    "PD_REFERENCE",
                    "Unique human readable reference to publication. <br>" + "This field is not editable. <br>"
                            + "Reference value is generated automatically using authirs and year of publication."},

            {"PN_PUBMED_ID", "PubMed Id"},
            {"PD_PUBMED_ID", "Unique number assigned to each PubMed citation. (MEDLINE PMID field)."},

            {"PN_AUTHORS", "Authors"},
            {"PD_AUTHORS", "Authors (MEDLINE AU field)."},

            {"PN_AFFILIATION", "Affiliation"},
            {"PD_AFFILIATION", "Institutional affiliation and address of the first author, " + "and grant numbers. (MEDLINE AD field)."},

            {"PN_ARTICLE_TITLE", "Title"},
            {"PD_ARTICLE_TITLE", "The title of the article (MEDLINE TI field)."},

            {"PN_LITERATURE_SOURCE", "Source"},
            {"PD_LITERATURE_SOURCE", "Composite field containing bibliographic information. " + "(MEDLINE SO field)."},

            {"PN_JOURNAL_TITLE", "Journal"},
            {"PD_JOURNAL_TITLE", "Standard journal title abbreviation (MEDLINE TA field)."},

            {"PN_JOURNAL_VOLUME", "Volume"},
            {"PD_JOURNAL_VOLUME", "Journal volume (MEDLINE VI field)."},

            {"PN_JOURNAL_ISSUE", "Issue"},
            {
                    "PD_JOURNAL_ISSUE",
                    "The number of the issue, part, or supplement of the journal "
                            + "in which the article was published (MEDLINE IP field)."},

            {"PN_PAGE_FROM", "Page from"},
            {
                    "PD_PAGE_FROM",
                    "The full pagination of the article (first part of MEDLINE PG field)."
                            + "<br>Sometimes page number can be a string value, for example 653s."},

            {"PN_PAGE_TO", "Page to"},
            {
                    "PD_PAGE_TO",
                    "The full pagination of the article (last part of MEDLINE PG field)."
                            + "<br>Sometimes page number can be a string value, for example 653s."},

            {"PN_PUBLICATION_ABSTRACT", "Abstract"},
            {"PD_PUBLICATION_ABSTRACT", "The publication abstract. (MEDLINE AB field)."},

            {"PN_FULL_TEXT_URL", "URL"},
            {"PD_FULL_TEXT_URL", "Link to the full-text of article at provider's website."},

            {"PN_PUBLICATION_YEAR", "Publication year"},
            {"PD_PUBLICATION_YEAR", "The year the article was published (MEDLINE DP field)."},

            {"PN_PUBLICATION_MONTH", "Publication month"},
            {"PD_PUBLICATION_MONTH", "The month (or month and day) the article was published (MEDLINE DP field)."},

            {"PN_LANGUAGE", "Language"},
            {"PD_LANGUAGE", "The language in which the article was published (MEDLINE LA field)."},

            {"PN_PUBLICATION_TYPE", "Publication type"},
            {"PD_PUBLICATION_TYPE", "The type of material the article represents (MEDLINE PT field)."},

            {"PN_STATUS", "Status"},
            {"PD_STATUS", "Using this property you can assign some status to the artice,<br>" + "for example: readed, wanted."},

            {"PN_KEYWORDS", "Keywords"},
            {"PD_KEYWORDS", "A set of user defined key words assotioated with a literature reference."},

            {"PN_IMPORTANCE", "Importance"},
            {"PD_IMPORTANCE", "The reference importance. " + "Using this property user can organise references by their importance."},

            {"PN_DBNAME", "DB"},
            {"PD_DBNAME", "DB name."},
            
            {"PN_DBVERSION", "DB version"},
            {"PD_DBVERSION", "DB version."},
            
            {"PN_IDNAME", "ID"},
            {"PD_IDNAME", "Identificator."},
            
            {"PN_IDVERSION", "ID version"},
            {"PD_IDVERSION", "ID version."},
            
            {"PN_SIMPLESOURCE", "Source"},
            {"PD_SIMPLESOURCE", "Source."},

            //----- Compartment specific fields ---------------------------------/
            {"CN_COMPARTMENT", "Compartment"},
            {"CD_COMPARTMENT", "Compartment."},
            {"PN_TYPE", "Type"},
            {"PD_TYPE", "Compartment Type."},
            {"PN_CL", "Default color"},
            {"PD_CL", "Compartment default color."},
            {"PN_SPATIAL_DIMENSION", "Spatial dimension"},
            {"PD_SPATIAL_DIMENSION", "Spatial dimension of the compartment."},

            //----- Gene specific fields -------------------------------------/
            {"CN_GENE", "Gene"},
            {"CD_GENE", "Gene."},
            {"PN_CHROMOSOME", "Chromosome"},
            {"PD_CHROMOSOME", "Gene location (locus) on the chromosome."},

          //----- Specie specific fields -------------------------------------/
            {"CN_SPECIE", "Specie"},
            {"CD_SPECIE", "Specie"},
            
            //----- Substance specific fields -------------------------------------/
            {"CN_SUBSTANCE", "Substance"},
            {"CD_SUBSTANCE", "Substance"},

            {"PN_SUBSTANCE_FORMULA", "Formula"},
            {"PD_SUBSTANCE_FORMULA", "Substance chemical formula."},

            {"PN_CAS_REGISTRY_NUMBER", "CAS"},
            {
                    "PD_CAS_REGISTRY_NUMBER",
                    "CAS Registry Number. <br>" + "It provides unique substance numeric identifier and <br>"
                            + "can contain up to 9 digits, divided by hyphens into 3 parts. <br>"
                            + "For example, 58-08-2 is the CAS Registry Number for caffeine."},

            //----- Structure specific fields -------------------------------------/
            {"CN_STRUCTURE", "Structure"},
            {"CD_STRUCTURE", "2D or 3D molecule structure"},

            {"PN_STRUCTURE_FORMAT", "Format"},
            {"PD_STRUCTURE_FORMAT", "Format of molecule structure data."},

            {"PN_STRUCTURE_DATA", "Data"},
            {"PD_STRUCTURE_DATA", "Molecule structure data."},

            {"PN_STRUCTURE_MOLECULE_REFERENCES", "Molecules"},
            {"PD_STRUCTURE_MOLECULE_REFERENCES", "Molecule or molecules which structure is described."},

            //----- Cell specific fields -------------------------------------/
            {"CN_CELL", "Cell"},
            {"CD_CELL", "Cell"},

            //----- Concept specific fields -------------------------------------/
            {"CONCEPT_TYPES", conceptTypes},
            {"CN_CONCEPT", "Concept"},
            {"CD_CONCEPT", "Some concept, generally it is corresponds to some " + "biological state, function or process."},

            //----- Function specific fields -------------------------------------/
            {"CN_FUNCTION", "Function (activity)"},
            {
                    "CD_FUNCTION",
                    "Molecular function is an activity or task performed by a gene product or other "
                            + "biological entity. It often corresponds to something (such as enzymatic activity)"
                            + "that can be measured <i>in vitro</i>."},

            //----- Process specific fields -------------------------------------/
            {"CN_PROCESS", "Process"},
            {
                    "CD_PROCESS",
                    "Biological process is a biological goal that requires more than one function."
                            + "<br>Examples of broad biological proceses are: 'cell growth and maintenance', "
                            + "'signal transduction', examples of more specific processes are: "
                            + "'pirimidine metabolism' or 'cAMP biosynthesis."},

            //----- State specific fields -------------------------------------/
            {"CN_STATE", "Process"},
            {"CD_STATE", "State, stage, developmental stage or variant of biological system or its subunits."},


            //----- RNA specific fields -------------------------------------/
            {"CN_RNA", "RNA"},
            {"CD_RNA", "RNA"},

            {"RNA_TYPES", rnaTypes},

            {"PN_RNA_TYPE", "RNA type"},
            {
                    "PD_RNA_TYPE",
                    "RNA type. Possible values are: <ul>" + "<li>primary transript - primary (initial, unprocessed) transcript.</li>"
                            + "<li>precursor RNA - any RNA species that is not yet the mature RNA product.</li>"
                            + "<li>mRNA - messenger RNA; includes 5'untranslated region (5'UTR)</li>"
                            + "<li>rRNA - mature ribosomal RNA; RNA component of the ribonucleoprotein particle "
                            + "(ribosome) which assembles amino acids into proteins.</li>"
                            + "<li>tRNA - mature transfer RNA, a small RNA molecule (75-85 bases long) "
                            + "that mediates the translation of a nucleic acid " + "sequence into an amino acid sequence.</li>"
                            + "<li>scRNA - small cytoplasmic RNA; any one of several small "
                            + "cytoplasmic RNA molecules present in the cytoplasm and " + "(sometimes) nucleus of a eukaryote.</li>"
                            + "<li>snRNA -  small nuclear RNA molecules involved in pre-mRNA splicing " + "and processing.</li>"
                            + "<li>snoRNA - small nucleolar RNA molecules mostly involved in " + "rRNA modification and processing.</li>"
                            + "<li>dsRNA - double stranded RNA.</li>" + "<li>other - other RNA types.</li>" + "</ul>"},

            //----- DNA specific fields -------------------------------------/
            {"CN_DNA", "DNA"},
            {"CD_DNA", "DNA"},

            //----- Complex specific fields -------------------------------------/
            {"CN_COMPLEX", "Complex"},
            {"CD_COMPLEX", "Complex"},


            {"PN_COMPLEX_COMPONENTS", "Components"},
            {"PD_COMPLEX_COMPONENTS", "Components"},


            //----- Protein specific fields -------------------------------------/
            {"CN_PROTEIN", "Protein"},
            {"CD_PROTEIN", "The Protein."},
            {"PROTEIN_FUNCTIONAL_STATES", proteinFunctionalStates},
            {"PROTEIN_STRUCTURES", proteinStructures},
            {"PROTEIN_MODIFICATIONS", proteinModifications},

            {"PN_FUNCTIONAL_STATE", "Functional state"},
            {"PD_FUNCTIONAL_STATE", "Functional state of the protein." + "Possible values are: " + makeHtmlList(proteinFunctionalStates)},

            {"PN_PROTEIN_STRUCTURE", "Structure"},
            {
                    "PD_PROTEIN_STRUCTURE",
                    "Top level structure descripton of the protein." + "Possible values are:<ul>" + "<li>unknown</li>" + "<li>monomer</li>"
                            + "<li>homodimer</li>" + "<li>heterodimer</li>" + "<li>multimer - this value specifies proteins "
                            + "consisted of three or more components.</li>" + "</ul>"},

            {"PN_PROTEIN_MODIFICATION", "Modification"},
            {"PD_PROTEIN_MODIFICATION", "Modefication of the protein." + "Possible values are: " + makeHtmlList(proteinModifications)},

            //----- Reaction & Relation specific fields -------------------------------------/
            {"CN_REACTION", "Reaction"},
            {"CD_REACTION", "Reaction"},
            {"PN_REACTION_TYPE", "Type"},
            {"PD_REACTION_TYPE", "Type"},
            {"PN_ACTION_TYPE", "Action"},
            {"PD_ACTION_TYPE", "Action"},
            {"PN_ACTION_MECHANISM", "Action mechanism"},
            {"PD_ACTION_MECHANISM", "Action mechanism"},

            {"CN_SEMANTIC_RELATION", "Relation"},
            {"CD_SEMANTIC_RELATION", "Semantic relation between two diagram elements"},

            {"PN_RELATION_INPUT", "Input"},
            {
                    "PD_RELATION_INPUT",
                    "Name of input data element (kernel) participation in this realtion."
                            + "<br>The name can be given relative Module.DATA data collection "
                            + "<br>or it can be complete name of corresponding kernel data element."},

            {"PN_RELATION_OUTPUT", "Output"},
            {
                    "PD_RELATION_OUTPUT",
                    "Name of input data element (kernel) participation in this realtion."
                            + "<br>The name can be given relative Module.DATA data collection "
                            + "<br>or it can be complete name of corresponding kernel data element."},

            {"PN_RELATION_TYPE", "Relation type"},
            {"PD_RELATION_TYPE", "Indicates role of relation."},

            {"PARTICIPATION_TYPES", participationTypes},
            {"PN_PARTICIPATION_TYPE", "Participation"},
            {
                    "PD_PARTICIPATION_TYPE",
                    "Specifies whether the element directly involved into relation.<br>" + "Possible values are: "
                            + makeHtmlList(participationTypes)},

            {"CN_SPECIE_REFERENCE", "Specie reference"},
            {"CD_SPECIE_REFERENCE", "Reference to specie involved into the reaction, its role and stoichiometry."},

            {"PN_SPECIE_NAME", "Variable"},
            {"PD_SPECIE_NAME", "Name of specie involved into the reaction."},

            {"SPECIE_ROLES", specieRoles},
            {"PN_SPECIE_ROLE", "Role"},
            {"PD_SPECIE_ROLE", "Role of specie in the reaction.<br>" + "Possible values are: " + makeHtmlList(specieRoles)},

            {"PN_STOICHIOMETRY", "Stoichiometry"},
            {"PD_STOICHIOMETRY", "Specifies the involved molecule stoichiometry."},

            {"MODIFIER_ACTIONS", modifierActions},
            {"PN_MODIFIER_ACTION", "Modifier action"},
            {
                    "PD_MODIFIER_ACTION",
                    "Specifies the modifier action. <br>" + "This value is needed only if role is MODIFIER, othervise it should be empty."
                            + "Possible values are: " + makeHtmlList(modifierActions)},

            {"CN_KINETIC_TYPE", "Kinetic type"},
            {"CD_KINETIC_TYPE", "Reaction kinetic type"},
            {"PN_KINETIC_NAME", "Name"},
            {"PD_KINETIC_NAME", "Unique name of kinetic type in database."},
            {"PN_KINETIC_TITLE", "Title"},
            {"PD_KINETIC_TITLE", "Short kinetic type name to be used in the diagram as an diagram element title."},
            {"PN_KINETIC_DESCRIPTION", "Description"},
            {"PD_KINETIC_DESCRIPTION", "Description of kinetic type. It can be plain or HTML text."},
            {"PN_KINETIC_REVERSIBLE", "Reversible"},
            {"PD_KINETIC_REVERSIBLE", "Indicates whether this kinetic type describes a reversible reaction."},
            {"PN_KINETIC_FUNCTION", "Function"},
            {"PD_KINETIC_FUNCTION", "Kinetic function specifies reaction rate."},
            {"PN_KINETIC_ELEMENTS", "Elements"},
            {"PD_KINETIC_ELEMENTS",
                    "Elements of kinetic function. " + "You should specify role for each element. By default all elements are constants."},
            {"PN_KINETIC_ROLE", "Elements"},
            {"PD_KINETIC_ROLE", "Elements of kinetic function."},

            {"PN_REACTION_TITLE", "Name"},
            {"PD_REACTION_TITLE", "Reaction name/scheme."},

            {"PN_REVERSIBLE", "Reversible"},
            {"PD_REVERSIBLE", "Indicates whether the reaction is reversible."},

            {"PN_FAST", "Fast"},
            {"PD_FAST", "Indicates whether the reaction is fast."},

            {"PN_KINETIC_LAW", "Kinetic law"},
            {"PD_KINETIC_LAW", "Kinetic law describes the reaction kinetics."},
            {"PN_KINETIC_TYPE", "Kinetic type"},
            {"PD_KINETIC_TYPE", "Reaction kinetic type name."},
            {"PN_KINETIC_FORMULA", "Formula"},
            {"PD_KINETIC_FORMULA", "Reaction rate formula."},
            {"PN_KINETIC_TIME_UNITS", "Time units"},
            {"PD_KINETIC_TIME_UNITS", "Reaction rate time units."},
            {"PN_KINETIC_SUBSTANCE_UNITS", "Substance units"},
            {"PD_KINETIC_SUBSTANCE_UNITS", "Reaction rate substance units."},
            {"PN_KINETIC_COMMENT", "Comment"},
            {"PD_KINETIC_COMMENT", "Comment on reaction rate."},

            {"PN_SPECIE_REFERENCIES", "Species"},
            {"PD_SPECIE_REFERENCIES", "Species (reactants, products or modifiers) that are involved into the reaction."},

            //----- Common fields ---------------------------------------/

            {"PN_IDENTIFIER", "Identifier"},
            {
                    "PD_IDENTIFIER",
                    "Identifier of the object in the database." + "General format is:"
                            + "<pre>Species abbreviation:object abbreviation</pre>" + "Example:" + "<pre>Hs:IRF-1</pre>"},

            {"PN_DATE", "Date"},
            {"PD_DATE", "Date of the object creation."},
            
            {"PN_BASE_UNITS", "Base units"},
            {"PD_BASE_UNITS", "Base units."},
            
            {"PN_UNIT_TYPE", "Type"},
            {"PD_UNIT_TYPE", "Unit type."},
            
            {"PN_UNIT_MULTIPLIER", "Multiplier"},
            {"PD_UNIT_MULTIPLIER", "Unit multiplier."},
            
            {"PN_UNIT_SCALE", "Scale"},
            {"PD_UNIT_SCALE", "Unit scale."},
            
            {"PN_UNIT_EXPONENT", "Exponent"},
            {"PD_UNIT_EXPONENT", "Unit exponent."},

            {"PN_TITLE", "Title"},
            {"PD_TITLE", "The object title (generally it is object brief name)."},

            {"PN_COMPLETE_NAME", "Complete name"},
            {"PD_COMPLETE_NAME", "The object full name."},

            {"PN_DESCRIPTION", "Description"},
            {"PD_DESCRIPTION", "The object textual description (plain text or HTML)."},

            {"PN_SYNONYMS", "Synonyms"},
            {"PD_SYNONYMS", "The object name synonyms."},


            {"PN_SPECIES", "Species"},
            {
                    "PD_SPECIES",
                    "Organism Species.<br>" + "In most cases this is done by giving the Latin genus and "
                            + "species designations, followed (in parentheses) by the common " + "name in English where known."
                            + "<p>The format is:" + "<pre>Genus species (name)</pre>" + "Example:" + "<pre>Homo sapiens (human)</pre>"},

            {"PN_ORGANISM_CLASSIFICATION", "Classification"},
            {
                    "PD_ORGANISM_CLASSIFICATION",
                    "Organism Classification.<br>"
                            + "The field includes the taxonomic classification  of the source organism. The classification "
                            + "is listed top-down as nodes in a taxonomic tree in which the most general grouping "
                            + "is  given first. The classification may be distributed over several OC lines, "
                            + "but nodes are not split hyphenated between lines. The individual items are separated "
                            + "by semicolons and the list is terminated by a period. " + "<p>The format is:"
                            + "<pre>Node [; Node...]. </pre>" + "Example:"
                            + "<pre> Eukaryota; Metazoa; Chordata; Vertebrata; Tetrapoda; Mammalia;\n" + "Eutheria; Primates.</pre>"},

            {"PN_GENE_REFERENCE", "Gene ID"},
            {"PD_GENE_REFERENCE", "Identifier of  gene (in the given database) encoded this protein or RNA."},

            {"PN_REGULATION", "Regulation"},
            {"PD_REGULATION", "List of factors regulating this component."},

            {"PN_SOURCE", "Source"},
            {
                    "PD_SOURCE",
                    "Cell line (it also may be cell, tissue or organ) where an expression " + "of the gene, RNA or protein was registered."
                            + "<p>The format is:" + "<pre>cell identifier</pre>"},

            {"PN_COMMENT", "Comment"},
            {"PD_COMMENT", "Arbitrary text comments."},

            {"PN_ATTRIBUTES", "Attributes"},
            {
                    "PD_ATTRIBUTES",
                    "Dynamic set of attributes. <br>" + "This attributes can be added:<br>" + "<ul>"
                            + "<li>during mapping of information from a database into Java objects"
                            + "<li>by plug-in for some specific usage" + "<li>by customer to store some specific information formally"
                            + "<li>during import of experimental data" + "</ul>"},

            {"PN_DATABASE_REFERENCES", "Database references"},
            {
                    "PD_DATABASE_REFERENCES",
                    "Database cross-reference.<br>" + "Cross-references to other databases that contain information related to the entry. "
                            + "For instance, line pointing to the relevant SWISS-PROT entry will be in the DR " + "field for the protein. "
                            + "<p>General format is:" + "<pre>database_identifier; primary_identifier; secondary_identifier.</pre>"
                            + "Example:" + "<pre>SWISS-PROT; P03593; V90K_AMV.</pre>"},

            {"PN_STRUCTURE_REFERENCES", "Structure"},
            {
                    "PD_STRUCTURE_REFERENCES",
                    "Structure (2D or 3D) references. <br>" + "Each molecule can have several structures depending on conditions.<br>"
                            + "CDK library is used to visualise the molecule structure."},

            {"CN_IMAGE_DESCRIPTOR", "Image descriptor"}, {"CD_IMAGE_DESCRIPTOR", "Image descriptor"},

            {"PN_IMAGE_SIZE", "Size"}, {"PD_IMAGE_SIZE", "Size"},

            {"PN_ORIGINAL_IMAGE_SIZE", "Original size"}, {"PD_ORIGINAL_IMAGE_SIZE", "Original size"},

            //DatabaseReference
            {"CN_DATABASE_REFERENCE", "Database reference"},
            {"CD_DATABASE_REFERENCE", "General defintion of reference to external  database."},

            {"PN_DATABASE_REFERENCE_DATABASE_NAME", "Database name"}, {"PD_DATABASE_REFERENCE_DATABASE_NAME", "External database name."},

            {"PN_DATABASE_REFERENCE_ID", "id"}, {"PD_DATABASE_REFERENCE_ID", " Record ID (primary key) in referenced database."},

            {"PN_DATABASE_REFERENCE_AC", "ac"}, {"PD_DATABASE_REFERENCE_AC", "Record AC (secondary key) in referenced database."},

            {"PN_DATABASE_REFERENCE_COMMENT", "Comment"}, {"PD_DATABASE_REFERENCE_COMMENT", "Arbitrary comment."},

            {"PN_DATABASE_REFERENCE_DBVERSION", "DB version"}, {"PD_DATABASE_REFERENCE_DBVERSION", "Database version."},

            {"PN_DATABASE_REFERENCE_IDVERSION", "ID version"}, {"PD_DATABASE_REFERENCE_IDVERSION", "ID version."},

            {"PN_DATABASE_REFERENCE_TYPE", "Relationship type"}, {"PD_DATABASE_REFERENCE_TYPE", "Relationship type."},
            
            //Connection port element
            {"CN_OUTPUT_CONNECTION_PORT", "Output connection port"},
            {"CD_OUTPUT_CONNECTION_PORT", "Output connection port"},
            
            {"CN_INPUT_CONNECTION_PORT", "Input connection port"},
            {"CD_INPUT_CONNECTION_PORT", "Input connection port"},
            
            {"CN_CONTACT_CONNECTION_PORT", "Contact connection port"},
            {"CD_CONTACT_CONNECTION_PORT", "Contact connection port"},
                        
            {"CN_VARIABLE_ELEMENT", "Variable"},
            {"CD_VARIABLE_ELEMENT", "Model variable."},
            {"PN_VARIABLE_NAME", "Name"},
            {"PD_VARIABLE_NAME", "Variable name."},
            
            {"PN_SPECIES_LATINNAME", "Latin name"},
            {"PD_SPECIES_LATINNAME", "Latin name"},
            {"PN_SPECIES_COMMONNAME", "Common name"},
            {"PD_SPECIES_COMMONNAME", "Common name"},
            {"PN_SPECIES_ABBREVIATION", "Abbreviation"},
            {"PD_SPECIES_ABBREVIATION", "Abbreviation"},
            {"PN_SPECIES_DESCRIPTION", "Description"},
            {"PD_SPECIES_DESCRIPTION", "Description"},
            
            //Composite/Agent diagram elements
            
            //Plot diagram element
            {"CN_PLOT_ELEMENT", "Plot"},
            {"CD_PLOT_ELEMENT", "Plot diagram element."},
            
            //Bus diagram element
            {"CN_CONNECTION_BUS", "Bus"},
            {"CD_CONNECTION_BUS", "Bus diagram element."},
            
            {"CN_SWITCH_ELEMENT", "Switcher"},
            {"CD_SWITCH_ELEMENT", "Module outputting one of input signals."},
            
            {"CN_AVERAGER_ELEMENT", "Averager"},
            {"CD_AVERAGER_ELEMENT", "Module calculating moving average."},
            
            {"CN_SUBDIAGRAM_ELEMENT", "Submodel"},
            {"CD_SUBDIAGRAM_ELEMENT", "Module containing submodel."},
    };
}
