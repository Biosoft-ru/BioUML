package ru.biosoft.bsa.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.MatrixTableType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.OptionEx;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SiteSearchSummaryParametersBeanInfo extends BeanInfoEx
{
    public SiteSearchSummaryParametersBeanInfo()
    {
        super(SiteSearchSummaryParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add(DataElementPathEditor.registerInput("yesTrackPath", beanClass, Track.class),
                getResourceString("PN_SUMMARY_YESTRACK"), getResourceString("PD_SUMMARY_YESTRACK"));
        add(DataElementPathEditor.registerInput("noTrackPath", beanClass, Track.class, true),
                getResourceString("PN_SUMMARY_NOTRACK"), getResourceString("PD_SUMMARY_NOTRACK"));
        add(new PropertyDescriptorEx("overrepresentedOnly", beanClass),
                getResourceString("PN_SUMMARY_OVERREPRESENTED_ONLY"), getResourceString("PD_SUMMARY_OVERREPRESENTED_ONLY"));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputPath", beanClass, TableDataCollection.class, MatrixTableType.class), "$yesTrackPath$ summary"),
                getResourceString("PN_SUMMARY_OUTPUTNAME"), getResourceString("PD_SUMMARY_OUTPUTNAME"));
    }
}
