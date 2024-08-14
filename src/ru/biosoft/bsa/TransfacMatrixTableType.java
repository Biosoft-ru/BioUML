package ru.biosoft.bsa;

public class TransfacMatrixTableType extends MatrixTableType
{
    @Override
    public String getSource()
    {
        return "TRANSFAC";
    }

    @Override
    public int getIdScore(String id)
    {
        if(id.matches("[BIFVNP]\\$[A-Z0-9_]{2,}")) return SCORE_HIGH_SPECIFIC;
        if(id.matches("VS\\$[A-Z0-9_]{2,}")) return SCORE_MEDIUM_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
}
