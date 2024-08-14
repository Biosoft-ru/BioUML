package biouml.plugins.expression;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.util.OptionEx;

import biouml.model.Diagram;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * @author lan
 */
public class ExpressionFilterAnalysisParametersBeanInfo extends BeanInfoEx
{
    public ExpressionFilterAnalysisParametersBeanInfo()
    {
        super(ExpressionFilterAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("inputDiagram", beanClass, Diagram.class), getResourceString("PN_INPUT_DIAGRAM"),
                getResourceString("PD_INPUT_DIAGRAM"));
        add(new PropertyDescriptorEx("filterProperties", beanClass), getResourceString("PN_FILTER_PROPERTIES"),
                getResourceString("PD_FILTER_PROPERTIES"));
        add(OptionEx
                .makeAutoProperty(DataElementPathEditor.registerOutput("outputDiagram", beanClass, Diagram.class), "$inputDiagram$ expr"),
                getResourceString("PN_OUTPUT_DIAGRAM"), getResourceString("PD_OUTPUT_DIAGRAM"));
    }
}
