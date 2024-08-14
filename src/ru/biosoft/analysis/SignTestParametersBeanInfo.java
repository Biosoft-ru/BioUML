package ru.biosoft.analysis;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SignTestParametersBeanInfo extends BeanInfoEx2<SignTestParameters>
{
    public SignTestParametersBeanInfo()
    {
        super( SignTestParameters.class );
    }
    @Override
    public void initProperties() throws Exception
    {
        property( "inputTablePath" ).inputElement( TableDataCollection.class ).add();
        add( ColumnNameSelector.registerNumericSelector( "sampleCol", beanClass, "inputTablePath" ) );
        property( "adjMethod" ).tags( SignTestParameters.getAvailableMethods() ).add();
        property( "resultTablePath" ).outputElement( TableDataCollection.class )
                .auto( "$inputTablePath/parent$/$sampleCol$ vs. population" ).add();
    }
}
