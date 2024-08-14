package biouml.plugins.optimization;

import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.Diagram;


public class OptimizationBeanInfo extends BeanInfoEx2<Optimization>
{
    public OptimizationBeanInfo()
    {
        super(Optimization.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }
    
    @Override
    public void initProperties() throws Exception
    {
        property("optimizationDiagramPath").outputElement(Diagram.class).title("CN_OPT_DIAGRAM").description("CD_OPT_DIAGRAM").add();
        addHidden("optimizationMethod");
    }
}
