package ru.biosoft.access.biohub;


public class DefaultReferenceType extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        return SCORE_LOW_SPECIFIC;
    }

    @Override
    public String getObjectType()
    {
        return "Unspecified";
    }

    @Override
    public String getDescriptionHTML()
    {
        return "This reference type is set for collections where ID format is unknown for BioUML.";
    }
}
