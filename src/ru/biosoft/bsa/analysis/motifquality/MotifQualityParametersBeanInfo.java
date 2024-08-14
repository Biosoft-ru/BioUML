package ru.biosoft.bsa.analysis.motifquality;

import java.beans.PropertyDescriptor;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.OptionEx;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class MotifQualityParametersBeanInfo extends BeanInfoEx
{
    public MotifQualityParametersBeanInfo()
    {
        super(MotifQualityParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(MessageBundle.CN_MOTIF_QUALITY_ANALYSIS);
        beanDescriptor.setShortDescription(MessageBundle.CD_MOTIF_QUALITY_ANALYSIS);
    }

    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = DataElementPathEditor.registerInput("sequences", beanClass, Track.class);
        pde.setValue(DataElementPathEditor.CHILD_CLASS, AnnotatedSequence.class);
        add(pde, MessageBundle.PN_SEQUENCES, MessageBundle.PD_SEQUENCES);
        add(DataElementPathEditor.registerInput("siteModel", beanClass, SiteModel.class), MessageBundle.PN_SITE_MODEL, MessageBundle.PD_SITE_MODEL);
        add(new PropertyDescriptor("numberOfPoints", beanClass), MessageBundle.PN_NUMBER_OF_POINTS, MessageBundle.PD_NUMBER_OF_POINTS);
        add(new PropertyDescriptor("shufflesCount", beanClass), MessageBundle.PN_SHUFFLES_COUNT, MessageBundle.PD_SHUFFLES_COUNT);
        add(new PropertyDescriptorEx("seed", beanClass), MessageBundle.PN_SEED, MessageBundle.PD_SEED);
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("output", beanClass, TableDataCollection.class), "$siteModel/parent$ $siteModel/name$ roc"), MessageBundle.PN_OUTPUT, MessageBundle.PD_OUTPUT);
    }
}
