package ru.biosoft.proteome;

import java.util.Properties;

import ru.biosoft.access.biohub.SQLBasedHub;

/**
 * Proteome specific hub
 * - uniprot to PDB
 */
public class ProteomeHub extends SQLBasedHub
{
    public ProteomeHub(Properties properties)
    {
        super(properties);
    }

    private final Matching[] matchings = new Matching[] {new Matching(UniprotReferenceType.class, PDBReferenceType.class, true, 0.99)};

    @Override
    protected Matching[] getMatchings()
    {
        return matchings;
    }
}
