package ru.biosoft.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.FilterTableParameters.ModeSelector;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class FilterTableParametersBeanInfo extends BeanInfoEx2<FilterTableParameters>
{
    public FilterTableParametersBeanInfo()
    {
        super(FilterTableParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        property( "inputPath" ).inputElement( TableDataCollection.class ).add();
        add("filterExpression");
        add("filteringMode", ModeSelector.class);
        addHidden("valuesCount", "isValuesCountHidden");
        property( "outputPath" ).outputElement( TableDataCollection.class ).auto( "$inputPath$ filtered" )
                .value( DataElementPathEditor.ICON_ID, FilterTableParameters.class.getMethod( "getIcon" ) ).add();
    }
}
