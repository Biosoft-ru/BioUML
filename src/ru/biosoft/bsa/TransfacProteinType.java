package ru.biosoft.bsa;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.ProteinTableType;

@ClassIcon("resources/proteins-transfac.gif")
public class TransfacProteinType extends ProteinTableType
{
    @Override
    public int getIdScore(String id)
    {
        if(id.matches("T\\d{5,7}"))
            return SCORE_HIGH_SPECIFIC+1;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSource()
    {
        return "Transfac";
    }
}
