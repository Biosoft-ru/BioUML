package biouml.plugins.test.tests;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import biouml.plugins.optimization.ExperimentalTableSupport.WeightMethod;
import biouml.plugins.optimization.document.editors.TimePointsEditor;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;

public class ExperimentValueTestBeanInfo extends BeanInfoEx
{
    public ExperimentValueTestBeanInfo()
    {
        super(ExperimentValueTest.class, "biouml.plugins.test.tests.MessageBundle");
        beanDescriptor.setDisplayName(getResourceString("CN_EXPERIMENT"));
        beanDescriptor.setShortDescription(getResourceString("CD_EXPERIMENT"));
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pd = new PropertyDescriptorEx("experimentPath", beanClass, "getExperimentPath", "setExperimentPath");
        add(pd, getResourceString("PN_EXPERIMENT_PATH"), getResourceString("PD_EXPERIMENT_PATH"));

        pd = new PropertyDescriptorEx("experimentVariable", beanClass, "getExperimentVariable", "setExperimentVariable");
        add(pd, getResourceString("PN_EXPERIMENT_EXP_VAR"), getResourceString("PD_EXPERIMENT_EXP_VAR"));

        pd = new PropertyDescriptorEx("resultVariable", beanClass, "getResultVariable", "setResultVariable");
        add(pd, getResourceString("PN_EXPERIMENT_RESULT_VAR"), getResourceString("PD_EXPERIMENT_RESULT_VAR"));

        pd = new PropertyDescriptorEx("relativeTo", beanClass, "getRelativeTo", "setRelativeTo");
        pd.setPropertyEditorClass(TestTimePointsEditor.class);
        add(pd, getResourceString("PN_EXPERIMENT_RELATIVE_TO"), getResourceString("PD_EXPERIMENT_RELATIVE_TO"));

        pd = new PropertyDescriptorEx("weightMethod", beanClass, "getWeightMethod", "setWeightMethod");
        pd.setPropertyEditorClass(WeightMethodEditor.class);
        add(pd, getResourceString("PN_EXPERIMENT_WEIGHT_METHOD"), getResourceString("PD_WEIGHT_METHOD"));

        pd = new PropertyDescriptorEx("maxDeviation", beanClass, "getMaxDeviation", "setMaxDeviation");
        pd.setNumberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE);
        add(pd, getResourceString("PN_EXPERIMENT_MAX_DEVIATION"), getResourceString("PD_EXPERIMENT_MAX_DEVIATION"));
    }

    public static class TestTimePointsEditor extends TimePointsEditor
    {
        @Override
        public String[] getTags()
        {
            StreamEx<String> result = StreamEx.of("unspecified");

            TableDataCollection tdc = ( (ExperimentValueTest)getBean() ).getTable();
            if( tdc != null )
            {
                double[] times = TableDataCollectionUtils.getColumn(tdc, "time");
                result = DoubleStreamEx.of(times).mapToObj( Double::toString ).prepend( result );
            }
            return result.toArray( String[]::new );
        }
    }

    public static class WeightMethodEditor extends StringTagEditorSupport
    {
        public WeightMethodEditor()
        {
            super(WeightMethod.getWeightMethods().toArray(new String[WeightMethod.getWeightMethods().size()]));
        }
    }
}
