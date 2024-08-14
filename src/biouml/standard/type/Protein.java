package biouml.standard.type;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;

@ClassIcon ( "resources/protein.gif" )
@PropertyName("Protein")
public class Protein extends Biopolymer
{
    public static final String[] proteinFunctionalStates = {"active", "inactive", "unknown"};
    public static final String[] proteinStructures = {"monomer", "homodimer", "heterodimer", "multimer", "unknown"};
    public static final String[] proteinModifications = {"none", "phosphorylated", "fatty_acylation", "prenylation", "cholesterolation", "ubiquitination",
            "sumolation", "glycation", "gpi_anchor", "unknown"};
    
    private String gene;
    private String functionalState;
    private String structure;
    private String modification;

    public Protein(DataCollection origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_PROTEIN;
    }

    @PropertyName ( "Gene ID" )
    @PropertyDescription ( "Identifier of  gene (in the given database) encoded this protein or RNA." )
    public String getGene()
    {
        return gene;
    }
    public void setGene(String gene)
    {
        String oldValue = this.gene;
        this.gene = gene;
        firePropertyChange("gene", oldValue, gene);
    }

    @PropertyName ( "Functional state" )
    @PropertyDescription ( "Functional state of the protein." )
    public String getFunctionalState()
    {
        return functionalState;
    }
    public void setFunctionalState(String functionalState)
    {
        String oldValue = this.functionalState;
        this.functionalState = functionalState;
        firePropertyChange("functionalState", oldValue, functionalState);
    }

    @PropertyName ( "Structure" )
    @PropertyDescription ( "Top level structure descripton of the protein." + "Possible values are:<ul>" + "<li>unknown</li>"
            + "<li>monomer</li>" + "<li>homodimer</li>" + "<li>heterodimer</li>" + "<li>multimer - this value specifies proteins "
            + "consisted of three or more components.</li>" + "</ul>" )
    public String getStructure()
    {
        return structure;
    }
    public void setStructure(String structure)
    {
        String oldValue = this.structure;
        this.structure = structure;
        firePropertyChange("structure", oldValue, structure);
    }

    @PropertyName ( "Modification" )
    @PropertyDescription ( "Modefication of the protein." )
    public String getModification()
    {
        return modification;
    }
    public void setModification(String modification)
    {
        String oldValue = this.modification;
        this.modification = modification;
        firePropertyChange("modification", oldValue, modification);
    }
}