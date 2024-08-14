package ru.biosoft.bsastats;

import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.html.ZipHtmlDataCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 */
public class MergeStatsParametersBeanInfo extends BeanInfoEx2<MergeStatsParameters>
{
    public MergeStatsParametersBeanInfo()
    {
        super(MergeStatsParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInputMulti("inputStatistics", beanClass, FolderCollection.class));
        property( "output" ).outputElement( ZipHtmlDataCollection.class ).auto( "$inputStatistics/path$/Report" ).add();
    }
}
