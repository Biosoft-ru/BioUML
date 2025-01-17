package ru.biosoft.analysis.type;

import ru.biosoft.access.biohub.ReferenceTypeSupport;

public class PathwayType extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "Pathways";
    }
}
