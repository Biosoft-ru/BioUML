package biouml.model.dynamics;

import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.BeanInfoConstants;

import biouml.model.dynamics.VariableRoleBeanInfo.SubstanceUnitsEditor;

public class VariableBeanInfo extends BeanInfoEx2<Variable>
{
    protected VariableBeanInfo(Class<? extends Variable> c)
    {
        super(c);
    }

    public VariableBeanInfo()
    {
        this(Variable.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        add("title");
        addReadOnly("type");
        property("initialValue").numberFormat( BeanInfoConstants.NUMBER_FORMAT_NONE ).add();
        add("constant");
        add("units", SubstanceUnitsEditor.class);
        add("comment");
    }
}
