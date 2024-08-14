package ru.biosoft.analysis.type;

import ru.biosoft.access.biohub.ReferenceTypeSupport;
import ru.biosoft.access.core.ClassIcon;

@ClassIcon("resources/genes.gif")
public class GeneTableType extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "Genes";
    }
}
