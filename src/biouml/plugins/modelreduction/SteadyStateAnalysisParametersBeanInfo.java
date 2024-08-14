package biouml.plugins.modelreduction;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

import biouml.model.Diagram;
import biouml.standard.simulation.SimulationResult;

import com.developmentontheedge.beans.BeanInfoConstants;

public class SteadyStateAnalysisParametersBeanInfo extends BeanInfoEx2<SteadyStateAnalysisParameters>
{
    public SteadyStateAnalysisParametersBeanInfo()
    {
        super(SteadyStateAnalysisParameters.class);
    }

    public SteadyStateAnalysisParametersBeanInfo(Class<? extends SteadyStateAnalysisParameters> beanClass)
    {
        super(beanClass);
    }

    @Override
    public void initProperties() throws Exception
    {
        property("input").inputElement(Diagram.class).add();
        property("inputState").tags(bean->bean.getAvailableStates()).add();
        addWithTags("outputType", SteadyStateAnalysisParameters.OUTPUT_DIAGRAM_TYPE, SteadyStateAnalysisParameters.OUTPUT_SIMULATION_RESULT_TYPE, SteadyStateAnalysisParameters.OUTPUT_TABLE_TYPE);
        property( "output" ).hidden("isOutputDiagramHidden").outputElement( Diagram.class ).add();
        property("stateName").hidden("isOutputDiagramHidden").add();
        property( "simulationResult" ).hidden("isOutputSimulationResultHidden").outputElement( SimulationResult.class ).add();
        property( "tableResult" ).hidden("isOutputTableHidden").outputElement( TableDataCollection.class ).add();
        add("variableNames");
        initMethodProperties();
    }

    public void initMethodProperties() throws Exception
    {
        property("relativeTolerance").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).add();
        property("absoluteTolerance").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).add();
        addExpert("validationSize");
        property("startSearchTime").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).expert().add();
        addExpert("engineWrapper");
    }
}
