package ru.biosoft.bsastats;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 *
 */
public class SequenceStatisticsParametersBeanInfo extends BeanInfoEx2<SequenceStatisticsParameters>
{
    public SequenceStatisticsParametersBeanInfo()
    {
        super(SequenceStatisticsParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "source" ).tags( AbstractReadsSourceSelectorParameters.SOURCES ).add();
        property( "track" ).inputElement( Track.class ).hidden( "isTrackHidden" ).add();
        property( "fastq" ).inputElement( FileDataElement.class ).hidden( "isFastqHidden" ).add();
        property( "encoding" ).editor( EncodingSelector.class ).hidden( "isFastqHidden" ).add();
        property( "csfasta" ).inputElement( FileDataElement.class ).hidden( "isSolidHidden" ).add();
        property( "qual" ).inputElement( FileDataElement.class ).hidden( "isSolidHidden" ).add();
        property( "alignment" ).tags( AbstractReadsSourceSelectorParameters.ALIGNMENTS ).add();
        add("processorsDPS");
        addHidden(new PropertyDescriptorEx("sourcePath", beanClass, "getSourcePath", null));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("output", beanClass, FolderCollection.class), "$sourcePath$ stats"));
    }
}
