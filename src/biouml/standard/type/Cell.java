package biouml.standard.type;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;

@ClassIcon ( "resources/cell.gif" )
public class Cell extends Compartment
{
    private String species;

    public Cell(DataCollection parent, String name)
    {
        super(parent, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CELL;
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
}
