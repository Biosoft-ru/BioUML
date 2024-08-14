
package ru.biosoft.analysis.diagram;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.util.OptionEx;
import biouml.model.Diagram;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * @author anna
 *
 */
public class DiagramExtensionAnalysisParametersBeanInfo extends BeanInfoEx
{
    public DiagramExtensionAnalysisParametersBeanInfo()
    {
        super(DiagramExtensionAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(MessageBundle.CN_CLASS);
        beanDescriptor.setShortDescription(MessageBundle.CD_CLASS);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add(DataElementPathEditor.registerInput("inputDiagramPath", beanClass, Diagram.class), MessageBundle.PN_INPUT_DIAGRAM,
                MessageBundle.PD_INPUT_DIAGRAM);
        add(new PropertyDescriptorEx("stepNumber", beanClass), MessageBundle.PN_ITERATION, MessageBundle.PD_ITERATION);
        add(new PropertyDescriptorEx("reactionsOnly", beanClass), MessageBundle.PN_REACTIONS_ONLY, MessageBundle.PD_REACTIONS_ONLY);
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputDiagramPath", beanClass, Diagram.class),
                "$inputDiagramPath$ extend step $stepNumber$"), MessageBundle.PN_OUTPUT_DIAGRAM, MessageBundle.PD_OUTPUT_DIAGRAM);
    }
}
