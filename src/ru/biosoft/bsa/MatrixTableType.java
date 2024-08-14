package ru.biosoft.bsa;

import ru.biosoft.access.biohub.ReferenceTypeSupport;
import ru.biosoft.access.core.ClassIcon;

@ClassIcon("analysis/resources/sitessummary.gif")
public class MatrixTableType extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "Matrices";
    }
}
