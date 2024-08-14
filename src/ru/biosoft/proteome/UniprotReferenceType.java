package ru.biosoft.proteome;

import ru.biosoft.access.biohub.ReferenceTypeSupport;

public class UniprotReferenceType extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "Proteins";
    }

    @Override
    public String getSource()
    {
        return "PDB";
    }
}
