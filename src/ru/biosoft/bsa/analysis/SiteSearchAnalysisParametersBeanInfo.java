package ru.biosoft.bsa.analysis;

import java.beans.PropertyDescriptor;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.gui.MessageBundle;
import ru.biosoft.util.OptionEx;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SiteSearchAnalysisParametersBeanInfo extends BeanInfoEx
{
    public SiteSearchAnalysisParametersBeanInfo()
    {
        super(SiteSearchAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add(DataElementPathEditor.registerInput("trackPath", beanClass, Track.class, true),
                getResourceString("PN_SITESEARCH_TRACK"), getResourceString("PD_SITESEARCH_TRACK"));
        add(new PropertyDescriptorEx("dbSelector", beanClass), getResourceString("PN_SITESEARCH_SEQDATABASE"), getResourceString("PD_SITESEARCH_SEQDATABASE"));
        addHidden(new PropertyDescriptor("defaultProfile", beanClass, "getDefaultProfile", null));
        PropertyDescriptorEx pde = DataElementPathEditor.registerInput("seqCollectionPath", beanClass, SequenceCollection.class);
        pde.setHidden(beanClass.getMethod("isSeqCollectionPathHidden"));
        add(pde,
                getResourceString("PN_SITESEARCH_SEQCOLLECTION"), getResourceString("PD_SITESEARCH_SEQCOLLECTION"));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerInput("profilePath", beanClass, SiteModelCollection.class),
                "$defaultProfile$"), getResourceString("PN_SITESEARCH_PROFILE"), getResourceString("PD_SITESEARCH_PROFILE"));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("output", beanClass, SqlTrack.class), "$trackPath$ sites"),
                getResourceString("PN_SITESEARCH_OUTPUTNAME"), getResourceString("PD_SITESEARCH_OUTPUTNAME"));
    }
}
