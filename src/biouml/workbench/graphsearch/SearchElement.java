package biouml.workbench.graphsearch;

import java.util.Objects;

import biouml.standard.type.Base;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Reaction;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.Element;

/**
 * Element for graph search
 */
public class SearchElement extends Element
{
    protected Base base;
    protected boolean add;
    protected boolean use;

    public SearchElement(Base base)
    {
        super( createDataElementPath(base) );
        this.base = base;
        this.add = true;
        this.use = false;
    }

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("NodeInfo, ");
        sb.append("Base id: ");
        sb.append(base.getName());
        return sb.toString();
    }

    public Base getBase()
    {
        return base;
    }

    public String getBaseName()
    {
        return base.getName();
    }

    public String getBaseTitle()
    {
        return base.getTitle();
    }

    public String getBaseType()
    {
        return base.getType();
    }


    public boolean isAdd()
    {
        return add;
    }

    public void setAdd(boolean add)
    {
        this.add = add;
    }

    public boolean isUse()
    {
        return use;
    }

    public void setUse(boolean use)
    {
        this.use = use;
    }
    
    public boolean sameLinkedFromPath(SearchElement other)
    {
        return Objects.equals( getLinkedFromPath(), other.getLinkedFromPath() );
    }
    
    private static DataElementPath createDataElementPath(Base base)
    {
        if (base.getOrigin() == null && base instanceof Reaction) //workaround for reactions from biohub which are not presented in data collection
            return DatabaseReference.STUB_PATH.getChildPath("reaction", base.getName() );
        return DataElementPath.create(base);
    }
}
