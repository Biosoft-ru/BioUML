package biouml.workbench.diagram.viewpart;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.type.Reaction;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class ReactionSimple implements DataElement
{
    private Reaction r;

    public ReactionSimple()
    {
    }

    public ReactionSimple(@Nonnull Reaction r)
    {
        this.r = r;
    }

    @PropertyName ( "Name" )
    public String getName()
    {
        return r == null ? null : r.getName();
    }
    public void setName(String name)
    {

    }

    @PropertyName ( "Title" )
    public String getTitle()
    {
        return r == null ? null : r.getTitle();
    }
    public void setTitle(String title)
    {
        if( r != null )
            r.setTitle( title );
    }

    @PropertyName ( "Formula" )
    public String getFormula()
    {
        return r == null ? null : r.getFormula();
    }
    public void setFormula(String formula)
    {
        if( r != null )
            r.setFormula( formula );
    }

    @PropertyName ( "Reversible" )
    public boolean isReversible()
    {
        if (r != null)
            return r.isReversible();
        return false;
//        return r == null ? null : r.isReversible();
    }
    public void setReversible(boolean reversible)
    {
        if( r != null )
        r.setReversible( reversible );
    }

    @PropertyName ( "Fast" )
    public boolean isFast()
    {
        if (r!=null)
            return r.isFast();
        return false;
    }
    public void setFast(boolean fast)
    {
        if( r != null )
            r.setFast( fast );
    }

    @PropertyName ( "Comment" )
    public String getComment()
    {
        return r == null ? null : r.getComment();
    }
    public void setComment(String comment)
    {
        if( r != null )
            this.r.setComment( comment );
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Reaction getReaction()
    {
        return r;
    }
}