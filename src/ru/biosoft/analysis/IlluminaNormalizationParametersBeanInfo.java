package ru.biosoft.analysis;

import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.OptionEx;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class IlluminaNormalizationParametersBeanInfo extends BeanInfoEx
{
    public IlluminaNormalizationParametersBeanInfo()
    {
        super(IlluminaNormalizationParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }
    @Override
    public void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInputMulti("illuminaFiles", beanClass, FileDataElement.class),
                getResourceString("PN_ILLNORM_LIST"), getResourceString("PD_ILLNORM_LIST"));
        add(new PropertyDescriptorEx("outputLogarithmBase", beanClass), getResourceString("PN_OUTPUT_LOGARITHM_BASE"),
                getResourceString("PD_OUTPUT_LOGARITHM_BASE"));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputPath", beanClass, TableDataCollection.class,
                ReferenceTypeRegistry.getReferenceType("Probes: Illumina").getClass()), "$illuminaFiles/path$/Illumina normalized"),
                getResourceString("PN_CELNORM_OUTPUTNAME"), getResourceString("PD_CELNORM_OUTPUTNAME"));
    }
}
