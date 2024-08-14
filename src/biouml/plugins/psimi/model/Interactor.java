package biouml.plugins.psimi.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

@SuppressWarnings ( "serial" )
public class Interactor extends Concept
{
    public Interactor(DataCollection<?> origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }
    
    protected Concept interactorType;
    protected Organism organism;
    protected String sequence;
    
    public Concept getInteractorType()
    {
        return interactorType;
    }

    public void setInteractorType(Concept interactorType)
    {
        this.interactorType = interactorType;
    }

    public Organism getOrganism()
    {
        return organism;
    }

    public void setOrganism(Organism organism)
    {
        this.organism = organism;
    }

    public String getSequence()
    {
        return sequence;
    }

    public void setSequence(String sequence)
    {
        this.sequence = sequence;
    }
}
