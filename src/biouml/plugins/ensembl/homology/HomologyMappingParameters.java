package biouml.plugins.ensembl.homology;

import biouml.standard.type.Species;
import ru.biosoft.analysis.TableConverterParameters;

/**
 * @author lan
 *
 */
public class HomologyMappingParameters extends TableConverterParameters
{
    private Species targetSpecies;

    public HomologyMappingParameters()
    {
        super();
        setTargetSpecies(Species.getDefaultSpecies(null));
    }
    
    /**
     * @return the targetSpecies
     */
    public Species getTargetSpecies()
    {
        return targetSpecies;
    }

    /**
     * @param targetSpecies the targetSpecies to set
     */
    public void setTargetSpecies(Species targetSpecies)
    {
        Object oldValue = this.targetSpecies;
        this.targetSpecies = targetSpecies;
        firePropertyChange("targetSpecies", oldValue, targetSpecies);
    }

    @Override
    public String getShortTargetType()
    {
        if(getSpecies() == getTargetSpecies())
            return super.getShortTargetType();
        return "(" + getTargetSpecies().getCommonName() + ")";
    }
}
