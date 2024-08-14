package biouml.plugins.sedml.analyses;

import com.developmentontheedge.beans.Option;

import ru.biosoft.util.bean.JSONBean;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName("Column")
public class Column extends Option implements JSONBean
{
    private String name;
    private String expression;

    @PropertyName("Name")
    @PropertyDescription("Column name")
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        String oldValue = this.name;
        this.name = name;
        firePropertyChange( "name", oldValue, name );
    }

    @PropertyName("Expression")
    @PropertyDescription("Expression")
    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        String oldValue = this.expression;
        this.expression = expression;
        firePropertyChange( "expression", oldValue, expression );
    }
}
