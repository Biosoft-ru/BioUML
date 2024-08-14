package biouml.plugins.expression;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 *
 */
public class ExpressionFilterPropertiesBeanInfo extends BeanInfoEx2<ExpressionFilterProperties>
{
    public ExpressionFilterPropertiesBeanInfo()
    {
        super(ExpressionFilterProperties.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "table" ).inputElement( TableDataCollection.class ).add();
        add("useOutsideFill");
        addHidden("outsideOptions", "isOutsideOptionsHidden");
        add("useInsideFill");
        addHidden("insideOptions", "isInsideOptionsHidden");
        add("usePval");
        addHidden("pvalOptions", "isPvalOptionsHidden");
        add("useFlux");
        addHidden("fluxOptions", "isFluxOptionsHidden");
    }
}
