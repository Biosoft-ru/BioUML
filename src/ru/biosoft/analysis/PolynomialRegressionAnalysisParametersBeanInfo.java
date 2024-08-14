package ru.biosoft.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.BeanUtil;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class PolynomialRegressionAnalysisParametersBeanInfo extends BeanInfoEx
{
    public PolynomialRegressionAnalysisParametersBeanInfo()
    {
        super(PolynomialRegressionAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("experimentData", beanClass), getResourceString("PN_EXPERIMENT"), getResourceString("PD_EXPERIMENT"));
        add(new PropertyDescriptorEx("regressionPower", beanClass), getResourceString("PN_REGRESSION_POWER"), getResourceString("PD_REGRESSION_POWER"));
        add(new PropertyDescriptorEx("pvalue", beanClass), getResourceString("PN_PVALUE"), getResourceString("PD_PVALUE"));
        add(BeanUtil.createExpertDescriptor("threshold", beanClass), getResourceString("PN_THRESHOLD"), getResourceString("PD_THRESHOLD"));
        add(new PropertyDescriptorEx("fdr", beanClass), getResourceString("PN_CALCULATING_FDR"),
                getResourceString("PD_CALCULATING_FDR"));

        add(DataElementPathEditor.registerOutput("outputTablePath", beanClass, TableDataCollection.class),
                getResourceString("PN_OUTPUT_TABLE"), getResourceString("PD_OUTPUT_TABLE"));
        
       
    }

}
