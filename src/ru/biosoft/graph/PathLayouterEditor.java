package ru.biosoft.graph;

import ru.biosoft.graph.DiagonalPathLayouter;
import ru.biosoft.graph.HierarchicPathLayouter;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.OrthogonalPathLayouter;

import com.developmentontheedge.beans.editors.TagEditorSupport;

public class PathLayouterEditor extends TagEditorSupport
{
    private static String ORTHOGONAL = "Orthogonal layouter";
    private static String DIAGONAL = "Diagonal Layouter";
    private static String HIERARCHIC = "Hierarchic Layouter";

    @Override
    public String[] getTags()
    {
        return new String[] {ORTHOGONAL, DIAGONAL, HIERARCHIC};
    }

    @Override
    public String getAsText()
    {
        Layouter layouter = (Layouter)getValue();

        if( layouter instanceof OrthogonalPathLayouter )
        {
            return ORTHOGONAL;
        }
        else if( layouter instanceof HierarchicPathLayouter )
        {
            return HIERARCHIC;
        }
        else if( layouter instanceof DiagonalPathLayouter )
        {
            return DIAGONAL;
        }
        else
        {
            return null;
        }
    }

    @Override
    public void setAsText(String text)
    {
        if( text.equals(ORTHOGONAL) )
        {
            setValue(new OrthogonalPathLayouter());
        }
        else if( text.equals(DIAGONAL) )
        {
            setValue(new DiagonalPathLayouter());
        }
        else if( text.equals(HIERARCHIC) )
        {
            setValue(new HierarchicPathLayouter());
        }
        this.firePropertyChange();
    }
}
