package ru.biosoft.analysis;

import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class GenerateTableFromJSONParametersBeanInfo extends BeanInfoEx2<GenerateTableFromJSONParameters>
{
    public GenerateTableFromJSONParametersBeanInfo()
    {
        super( GenerateTableFromJSONParameters.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput( "input", beanClass, TextDataElement.class ));
        add(DataElementPathEditor.registerOutput( "output", beanClass, TableDataCollection.class ));
    }
}


