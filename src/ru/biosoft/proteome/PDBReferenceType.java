package ru.biosoft.proteome;

import ru.biosoft.access.biohub.ReferenceTypeSupport;

public class PDBReferenceType extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "Structures";
    }

    @Override
    public String getSource()
    {
        return "PDB";
    }
}
