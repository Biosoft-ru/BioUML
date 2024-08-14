package ru.biosoft.bsa.analysis.chipseqprofile;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.OptionEx;

public class ChIPSeqProfileParametersBeanInfo extends BeanInfoEx
{
    public ChIPSeqProfileParametersBeanInfo()
    {
        super(ChIPSeqProfileParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(MessageBundle.CN_CHIPSEQ_PROFILE_ANALYSIS);
        beanDescriptor.setShortDescription(MessageBundle.CD_CHIPSEQ_PROFILE_ANALYSIS);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("peakTrackPath", beanClass, Track.class), MessageBundle.PN_PEAK_TRACK,
                MessageBundle.PD_PEAK_TRACK);
        add(DataElementPathEditor.registerInput("tagTrackPath", beanClass, Track.class), MessageBundle.PN_TAG_TRACK,
                MessageBundle.PD_TAG_TRACK);

        add(new PropertyDescriptorEx("fragmentSize", beanClass), MessageBundle.PN_FRAGMENT_SIZE, MessageBundle.PD_FRAGMENT_SIZE);
        add(new PropertyDescriptorEx("sigma", beanClass), MessageBundle.PN_SIGMA, MessageBundle.PD_SIGMA);
        add(new PropertyDescriptorEx("errorRate", beanClass), MessageBundle.PN_ERROR_RATE, MessageBundle.PD_ERROR_RATE);

        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("profileTrackPath", beanClass, Track.class),
                "$peakTrackPath$ profiled"), MessageBundle.PN_PROFILE_TRACK, MessageBundle.PD_PROFILE_TRACK);
    }
}
