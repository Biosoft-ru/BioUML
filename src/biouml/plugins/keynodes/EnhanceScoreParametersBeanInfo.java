package biouml.plugins.keynodes;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author Ilya
 */
public class EnhanceScoreParametersBeanInfo extends BeanInfoEx2<EnhanceScoreParameters>
{
    public EnhanceScoreParametersBeanInfo()
    {
        super( EnhanceScoreParameters.class );
    }
    
    @Override
    public void initProperties() throws Exception
    {
        property("keyNodeTable").inputElement(TableDataCollection.class).add();
        property("selectedProteins").inputElement(TableDataCollection.class).add();
        add(ColumnNameSelector.registerNumericSelector("column", beanClass, "keyNodeTable"));
        add("enhancement");
        property("result").outputElement(TableDataCollection.class).auto( "$keyNodeTable$ enhanced$suffix" ).add();
    }
}
