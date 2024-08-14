package ru.biosoft.bsa.snp;

import ru.biosoft.access.biohub.ReferenceTypeSupport;
import ru.biosoft.access.core.ClassIcon;

@ClassIcon("resources/snp.gif")
public class SNPTableType extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        if(id.matches("rs\\d+")) return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "SNP";
    }
}
