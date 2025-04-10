package ru.biosoft.bsastats;

import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.repository.DataElementPathEditor;

public class FilterByLengthBeanInfo extends TaskProcessorBeanInfo
{

    public FilterByLengthBeanInfo()
    {
        super( FilterByLength.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        add("minLength");
        add(DataElementPathEditor.registerOutput( "shortSequencesPath", beanClass, FileDataElement.class, true ));
        add("maxLength");
        add(DataElementPathEditor.registerOutput( "longSequencesPath", beanClass, FileDataElement.class, true ));
    }
}
