package ru.biosoft.bsa.macs;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.gui.MessageBundle;
import ru.biosoft.util.OptionEx;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class MACSAnalysisParametersBeanInfo extends BeanInfoEx
{
    public MACSAnalysisParametersBeanInfo()
    {
        super(MACSAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add(DataElementPathEditor.registerInput("trackPath", beanClass, Track.class),
                getResourceString("PN_SITESEARCH_TRACK"), getResourceString("PD_SITESEARCH_TRACK"));
        add(DataElementPathEditor.registerInput("controlPath", beanClass, Track.class, true),
                getResourceString("PN_MACS_CONTROL"), getResourceString("PD_MACS_CONTROL"));
        //MACS algorithm parameters
        add(new PropertyDescriptorEx("nolambda", beanClass), getResourceString("PN_MACS_NOLAMBDA"), getResourceString("PD_MACS_NOLAMBDA"));
        add(new PropertyDescriptorEx("lambdaSet", beanClass), getResourceString("PN_MACS_LAMBDA_SET"), getResourceString("PD_MACS_LAMBDA_SET"));
        
        add(new PropertyDescriptorEx("nomodel", beanClass), getResourceString("PN_MACS_NOMODEL"), getResourceString("PD_MACS_NOMODEL"));
        add(new PropertyDescriptorEx("shiftsize", beanClass), getResourceString("PN_MACS_SHIFTSIZE"),
                getResourceString("PD_MACS_SHIFTSIZE"));
        add(new PropertyDescriptorEx("bw", beanClass), getResourceString("PN_MACS_BW"), getResourceString("PD_MACS_BW"));
        
        add(new PropertyDescriptorEx("gsize", beanClass), getResourceString("PN_MACS_GSIZE"), getResourceString("PD_MACS_GSIZE"));
        add(new PropertyDescriptorEx("mfold", beanClass), getResourceString("PN_MACS_MFOLD"), getResourceString("PD_MACS_MFOLD"));
        add(new PropertyDescriptorEx("tsize", beanClass), getResourceString("PN_MACS_TSIZE"), getResourceString("PD_MACS_TSIZE"));
        add(new PropertyDescriptorEx("pvalue", beanClass), getResourceString("PN_MACS_PVALUE"), getResourceString("PD_MACS_PVALUE"));
        add(new PropertyDescriptorEx("futureFDR", beanClass), getResourceString("PN_MACS_FUTURE_FDR"), getResourceString("PD_MACS_FUTURE_FDR"));

        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputPath", beanClass, SqlTrack.class), "$trackPath$ peaks"),
                getResourceString("PN_SITESEARCH_OUTPUTNAME"), getResourceString("PD_SITESEARCH_OUTPUTNAME"));
    }
}
