package biouml.standard.type;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;

abstract public class Biopolymer extends Molecule
{
    private String species;
    private String source;
    private String regulation;

    protected Biopolymer(DataCollection<?> origin, String name)
    {
        super(origin, name);
    }

    @PropertyName ( "Species" )
    @PropertyDescription ( "Organism Species.<br>" + "In most cases this is done by giving the Latin genus and "
            + "species designations, followed (in parentheses) by the common " + "name in English where known." + "<p>The format is:"
            + "<pre>Genus species (name)</pre>" + "Example:" + "<pre>Homo sapiens (human)</pre>" )
    public String getSpecies()
    {
        return species;
    }
    public void setSpecies(String species)
    {
        String oldValue = this.species;
        this.species = species;
        firePropertyChange("species", oldValue, species);
    }

    @PropertyName ( "Source" )
    @PropertyDescription ( "Cell line (it also may be cell, tissue or organ) where an expression "
            + "of the gene, RNA or protein was registered." + "<p>The format is:" + "<pre>cell identifier</pre>" )
    public String getSource()
    {
        return source;
    }
    public void setSource(String source)
    {
        String oldValue = this.source;
        this.source = source;
        firePropertyChange("source", oldValue, source);
    }

    @PropertyName ( "Regulation" )
    @PropertyDescription ( "List of factors regulating this component." )
    public String getRegulation()
    {
        return regulation;
    }
    public void setRegulation(String regulation)
    {
        String oldValue = this.regulation;
        this.regulation = regulation;
        firePropertyChange("regulation", oldValue, regulation);
    }
}
