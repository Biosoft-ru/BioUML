package ru.biosoft.analysis.type;

import ru.biosoft.access.biohub.ReferenceTypeSupport;
import ru.biosoft.access.core.ClassIcon;

@ClassIcon ( "resources/reactions.gif" )
public class ReactionType extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "Reactions";
    }
}
