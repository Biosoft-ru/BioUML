package biouml.plugins.sedml.analyses;

import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author axec
 *
 */
public class CurveBeanInfo extends BeanInfoEx2<Curve>
{
    public CurveBeanInfo()
    {
        super(Curve.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("title");
        add("logX");
        add("logY");
        add("expressionX");
        add("expressionY");
    }
}
