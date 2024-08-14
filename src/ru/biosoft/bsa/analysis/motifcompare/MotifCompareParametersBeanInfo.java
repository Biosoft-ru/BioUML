package ru.biosoft.bsa.analysis.motifcompare;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class MotifCompareParametersBeanInfo extends BeanInfoEx
{
    public MotifCompareParametersBeanInfo()
    {
        super(MotifCompareParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(MessageBundle.CN_MOTIF_COMPARE);
        beanDescriptor.setShortDescription(MessageBundle.CD_MOTIF_COMPARE);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInputMulti("siteModels", beanClass, SiteModel.class), getResourceString("PN_SITE_MODELS"), getResourceString("PD_SITE_MODELS"));
        
        PropertyDescriptorEx pde = DataElementPathEditor.registerInput("sequences", beanClass, DataCollection.class);
        pde.setValue(DataElementPathEditor.CHILD_CLASS, AnnotatedSequence.class);
        add(pde, getResourceString("PN_SEQUENCES"), getResourceString("PD_SEQUENCES"));
        
        pde = DataElementPathEditor.registerInput("backgroundSequences", beanClass, DataCollection.class, true);
        pde.setValue(DataElementPathEditor.CHILD_CLASS, AnnotatedSequence.class);
        add(pde, getResourceString("PN_BACKGROUND_SEQUENCES"), getResourceString("PN_BACKGROUND_SEQUENCES"));

        pde = new PropertyDescriptorEx("numberOfPermutations", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_NUMBER_OF_PERMUTATIONS"), getResourceString("PD_NUMBER_OF_PERMUTATIONS"));
        
        pde = new PropertyDescriptorEx("seed", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_SEED"), getResourceString("PD_SEED"));
        
        pde = new PropertyDescriptorEx("modelFDR", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_MODEL_FDR"), getResourceString("PD_MODEL_FDR"));

        add(DataElementPathEditor.registerOutput("output", beanClass, TableDataCollection.class), getResourceString("PN_OUTPUT"), getResourceString("PD_OUTPUT"));

    }

}
