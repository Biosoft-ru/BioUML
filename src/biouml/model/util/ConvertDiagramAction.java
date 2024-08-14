
package biouml.model.util;

import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import biouml.model.Diagram;
import biouml.model.DiagramTypeConverter;
import biouml.workbench.diagram.DiagramTypeConverterRegistry;
import biouml.workbench.diagram.DiagramTypeConverterRegistry.Conversion;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

/**
 * @author anna
 *
 */
public class ConvertDiagramAction extends BackgroundDynamicAction
{
    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        return new ActionProperties(model);
    }

    @Override
    public boolean isApplicable(Object model)
    {
        if( model instanceof Diagram )
        {
            Conversion[] conversions = DiagramTypeConverterRegistry.getPossibleConversions( ( (Diagram)model ).getType().getClass()
                    .getName());
            return conversions != null && conversions.length > 0;
        }
        return false;
    }

    @Override
    public JobControl getJobControl(final Object model, List<DataElement> selectedItems, final Object properties) throws Exception
    {
        return new AbstractJobControl(log)
        {
            @Override
            protected void doRun() throws JobControlException
            {
                ActionProperties actionProperties = (ActionProperties)properties;
                Conversion conversion = actionProperties.getConversion();
                DataElementPath path = actionProperties.getOutputDiagramPath();
                try
                {
                    DiagramTypeConverter converter = conversion.getConverter().newInstance();
                    DataCollection origin = path.getParentCollection();
                    Diagram newDiagram = ( (Diagram)model ).clone(origin, path.getName());
                    newDiagram = converter.convert(newDiagram, conversion.getDiagramType());
                    origin.put(newDiagram);
                    setPreparedness(100);
                    resultsAreReady(new Object[] {newDiagram});
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }

            }
        };
    }

    public static class ActionProperties
    {
        private DataElementPath inputDiagramPath, outputDiagramPath;
        private Conversion conversion;
        private Conversion[] conversions;

        public ActionProperties(Object model)
        {
            if( model instanceof Diagram )
            {
                DataElementPath modelPath = ((Diagram)model).getCompletePath();
                setInputDiagramPath(modelPath);
                Conversion[] availableConversions = DiagramTypeConverterRegistry.getPossibleConversions( ( (Diagram)model ).getType()
                        .getClass().getName());
                setConversions(availableConversions);
                if( availableConversions.length > 0 )
                    setConversion(availableConversions[0]);
            }
        }


        public DataElementPath getInputDiagramPath()
        {
            return inputDiagramPath;
        }


        public void setInputDiagramPath(DataElementPath inputDiagramPath)
        {
            this.inputDiagramPath = inputDiagramPath;
        }


        public Conversion getConversion()
        {
            return conversion;
        }


        public void setConversion(Conversion conversion)
        {
            this.conversion = conversion;
        }


        public DataElementPath getOutputDiagramPath()
        {
            return outputDiagramPath;
        }


        public void setOutputDiagramPath(DataElementPath outputDiagramPath)
        {
            this.outputDiagramPath = outputDiagramPath;
        }


        public Conversion[] getConversions()
        {
            return conversions;
        }


        public void setConversions(Conversion[] conversions)
        {
            this.conversions = conversions;
        }
    }

    public static class ConversionSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return ( (ActionProperties)getBean() ).getConversions();
        }
    }

    public static class ActionPropertiesBeanInfo extends BeanInfoEx
    {
        public ActionPropertiesBeanInfo()
        {
            super(ActionProperties.class, MessageBundle.class.getName());
            beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
            beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            add(DataElementPathEditor.registerOutput("outputDiagramPath", beanClass, Diagram.class),
                    getResourceString("PN_CONVERTED_DIAGRAM_NAME"), getResourceString("PD_CONVERTED_DIAGRAM_NAME"));
            PropertyDescriptorEx pde = new PropertyDescriptorEx("conversion", beanClass);
            pde.setSimple(true);
            pde.setPropertyEditorClass(ConversionSelector.class);
            add(pde, getResourceString("PN_CONVERTED_DIAGRAM_TYPE"), getResourceString("PD_CONVERTED_DIAGRAM_TYPE"));
        }
    }
}
