package biouml.plugins.kegg.type;

import ru.biosoft.access.core.DataCollection;
import biouml.standard.type.DiagramReference;
import biouml.standard.type.Substance;

public class Glycan extends Substance
{
    public Glycan(DataCollection origin, String name)
    {
        super(origin, name);
    }

    private String composition;

    public void setComposition(String composition)
    {
        this.composition = composition;
    }

    public String getComposition()
    {
        return composition;
    }

    private float mass;

    public void setMass(float mass)
    {
        this.mass = mass;
    }

    public float getMass()
    {
        return mass;
    }

    private String glycanClass;

    public void setGlycanClass(String glycanClass)
    {
        this.glycanClass = glycanClass;
    }

    public String getGlycanClass()
    {
        return glycanClass;
    }

    private String binding;

    public void setBinding(String binding)
    {
        this.binding = binding;
    }

    public String getBinding()
    {
        return binding;
    }

    private String compound;

    public void setCompound(String compound)
    {
        this.compound = compound;
    }

    public String getCompound()
    {
        return compound;
    }

    private DiagramReference[] pathways;

    public void setPathways(DiagramReference[] pathways)
    {
        this.pathways = pathways;
    }

    public DiagramReference[] getPathways()
    {
        return pathways;
    }

    private String reaction;

    public void setReaction(String reaction)
    {
        this.reaction = reaction;
    }

    public String getReaction()
    {
        return reaction;
    }

    private String enzyme;

    public void setEnzyme(String enzyme)
    {
        this.enzyme = enzyme;
    }

    public String getEnzyme()
    {
        return enzyme;
    }

    private String ortholog;

    public void setOrtholog(String ortholog)
    {
        this.ortholog = ortholog;
    }

    public String getOrtholog()
    {
        return ortholog;
    }
}
