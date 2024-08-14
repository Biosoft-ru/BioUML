package biouml.plugins.modelreduction;

import com.developmentontheedge.beans.BeanInfoConstants;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.Diagram;

public class QuasiSteadyStateAnalysisParametersBeanInfo extends BeanInfoEx2<QuasiSteadyStateAnalysisParameters>
{
    public QuasiSteadyStateAnalysisParametersBeanInfo()
    {
        super(QuasiSteadyStateAnalysisParameters.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        property("input").inputElement(Diagram.class).add();
        property("output").outputElement( TableDataCollection.class).add();
        property("initialTime").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).add();
        property("completionTime").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).add();
        property("timeIncrement").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).add();
        property("dEpsilon").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).expert().add();
        property("timeEpsilon").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).expert().add();
        property("ratioEpsilon").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).expert().add();
    }
}
