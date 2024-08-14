package biouml.standard.type;

/**
 * General interface for species specific entities.
 *
 * Examples of species specific entities are cell, gene and protein.
 */
public interface SpeciesSpecific
{
    public biouml.standard.type.Species getSpecies();
}
