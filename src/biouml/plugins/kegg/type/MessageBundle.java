package biouml.plugins.kegg.type;

import java.util.ListResourceBundle;

/**
 *
 */
public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            //KEGG Compound fields
            {"CN_COMPOUND", "Compound"},
            {"CD_COMPOUND", "Compound is a collection of metabolic and other compounds including substrates, products, inhibitors of metabolic pathways as well as drugs and xenobiotic chemicals."},
        
            {"PN_COMPOUND_FORMULA", "Formula"},
            {"PD_COMPOUND_FORMULA", "Chemical formula of the compound."},
        
            {"PN_COMPOUND_PATHWAY", "Pathway"},
            {"PD_COMPOUND_PATHWAY", "Link information to the KEGG (Kyoto Encyclopedia of Genes and Genomes) pathway database: the pathway map accession number followed by the description."},
        
            {"PN_COMPOUND_ENZYME", "Enzyme"},
            {"PD_COMPOUND_ENZYME", "Link information to the ENZYME section. The enzyme reactions, in which the corresponding compound is used, are listed by their EC numbers."},
        
            //KEGG Glycan fields
            {"CN_GLYCAN", "Glycan"},
            {"CD_GLYCAN", "Glycan is a carbohydrate structure."},
        
            {"PN_GLYCAN_BINDING", "Binding"},
            {"PD_GLYCAN_BINDING", "Contains the information on the related proteins including enzymes which are involved in transferase activities using or producing the carbohydrate, proteins which recognize the carbohydate as a ligand, and proteins to which the carbohydrate is attached."},
        
            {"PN_GLYCAN_COMPOSITION", "Composition"},
            {"PD_GLYCAN_COMPOSITION", "The COMPOSITION data item contains the compositions of mono sugars, such as, Glc for D-glucose, GlcNAc for N-acetyl-D-glucose, and Man for D-mannose."},
        
            {"PN_GLYCAN_COMPOUND", "Compound"},
            {"PD_GLYCAN_COMPOUND", "Contains the corresponding COMPOUND ID to the carbohydrate."},
        
            {"PN_GLYCAN_ENZYME", "Enzyme"},
            {"PD_GLYCAN_ENZYME", "Contains EC numbers of enzyme reactions, in which the corresponding carbohydate is used."},
        
            {"PN_GLYCAN_GLYCAN_CLASS", "Class"},
            {"PD_GLYCAN_GLYCAN_CLASS", "Contains the selected class for the carbohydrate. If there is a subclass, it follows the main class with semicolon (;)."},
        
            {"PN_GLYCAN_MASS", "Mass"},
            {"PD_GLYCAN_MASS", "Contains mass of the carbohydrate that is calculated by summing up the masses of mono sugars, or unit, if available, and minus the number of bonds times mass of water."},
        
            {"PN_GLYCAN_ORTHOLOG", "Ortholog"},
            {"PD_GLYCAN_ORTHOLOG", "Contains the link information to the KEGG/KO database: the KO identifier followed by the description."},
        
            {"PN_GLYCAN_PATHWAY", "Pathway"},
            {"PD_GLYCAN_PATHWAY", "Contains the link information to the KEGG pathway database: the pathway map accession number followed by the description."},
        
            {"PN_GLYCAN_REACTION", "Reaction"},
            {"PD_GLYCAN_REACTION", "Contains numbers of reactions, in which the corresponding carbohydrate is used."},
        
            //KEGG Enzyme section.
            {"CN_ENZYME", "Enzyme"},
            {"CD_ENZYME", "Enzymatic reaction classified according to the nomenclature of the International Union of Biochemistry and Molecular Biology (IUBMB)."},
        
            {"PN_ENZYME_ENZYME_CLASS", "Class"},
            {"PD_ENZYME_ENZYME_CLASS", "Class is the meaning of the EC number. Each line corresponds to the class, subclass, and sub-subclass of the enzyme."},
        
            {"PN_ENZYME_SUBSTRATE", "Substrate"},
            {"PD_ENZYME_SUBSTRATE", "The substrate field contains chemical compounds that appear on the left side of the reaction equation."},
        
            {"PN_ENZYME_PRODUCT", "Product"},
            {"PD_ENZYME_PRODUCT", "The product field contains compounds that appear on the right side of the reaction equation."},
        
            {"PN_ENZYME_REACTION", "Reaction"},
            {"PD_ENZYME_REACTION", "A textual description of the chemical reaction equation. If there are more than one reaction, they are listed separated with the semicolon."},
        
            {"PN_ENZYME_DISEASE", "Disease"},
            {"PD_ENZYME_DISEASE", "Contains the link information to the OMIM (On-line Mendelian Inheritance in Man) database: the MIM number followed by the description."},
        
            {"PN_ENZYME_MOTIF", "Motif"},
            {"PD_ENZYME_MOTIF", "Contains the link information to the PROSITE database."},
        
            {"PN_ENZYME_ORTHOLOG", "Ortholog"},
            {"PD_ENZYME_ORTHOLOG", "Contains the link information to the KEGG/KO database."},
        
            {"PN_ENZYME_PATHWAY", "Pathway"},
            {"PD_ENZYME_PATHWAY", "Contains the link information to the KEGG (Kyoto Encyclopedia of Genes and Genomes) pathway database."},
        
            {"PN_ENZYME_PDB_STRUCTURE", "PDB Structure"},
            {"PD_ENZYME_PDB_STRUCTURE", "Link information to the Protein Data Bank (PDB), which stores the 3-D structure information of proteins."},
        
            {"PN_ENZYME_REFERENCES", "References"},
            {"PD_ENZYME_REFERENCES", "A set of literature references describing enzyme. Each reference consists of MEDLINE UI or PMID (if available) and a text containing author names, title, journal, volume, year and pages."},
        
            //Literature reference
            {"CN_LITERATURE_REFERENCE", "Literature reference"},
            {"CD_LITERATURE_REFERENCE", "Literature reference describing enzyme"},
        
            {"PN_LITERATURE_REFERENCE_MEDLINE_UI", "Medline UI"},
            {"PD_LITERATURE_REFERENCE_MEDLINE_UI", "Medline UI"},
        
            {"PN_LITERATURE_REFERENCE_PMID", "PMID"},
            {"PD_LITERATURE_REFERENCE_PMID", "PMID"},
        
            {"PN_LITERATURE_REFERENCE_REFERENCE", "Reference"},
            {"PD_LITERATURE_REFERENCE_REFERENCE", "A text containing author names, title, journal, volume, year and pages."},
        
            //Kegg Ortholog
            {"CN_ORTHOLOG", "Ortholog"},
            {"CD_ORTHOLOG", "KEGG Ortholog"},
        
            {"PN_ORTHOLOG_CLASSIFICATION", "Classification"},
            {"PD_ORTHOLOG_CLASSIFICATION", "List of ortholog classifications."},
        
            {"PN_ORTHOLOG_GENES", "Genes"},
            {"PD_ORTHOLOG_GENES", "Genes list."}
        };
    }
}
