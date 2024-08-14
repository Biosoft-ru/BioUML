package biouml.plugins.sedml.analyses;

import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.standard.simulation.SimulationResult;

public class SetInitialValuesFromSimulationResult extends AnalysisMethodSupport<SetInitialValuesFromSimulationResult.Parameters>
{

    public SetInitialValuesFromSimulationResult(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Diagram inputDiagram = parameters.getInputDiagram().getDataElement( Diagram.class );
        SimulationResult simulationResult = parameters.getSimulationResult().getDataElement( SimulationResult.class );
        DataElementPath outPath = parameters.getOutputDiagram();
        Diagram outputDiagram = inputDiagram.clone( outPath.getParentCollection(), outPath.getName() );

        EModel emodel = outputDiagram.getRole( EModel.class );
        double[] lastValues = simulationResult.getValues()[simulationResult.getCount() - 1];
        for( Map.Entry<String, Integer> var : simulationResult.getVariableMap().entrySet() )
        {
            double newValue = lastValues[var.getValue()];
            Variable variable = emodel.getVariable( var.getKey() );
            if( variable == null )
                throw new IllegalArgumentException( "Can not find " + var.getKey() + " in " + parameters.getInputDiagram() );
            variable.setInitialValue( newValue );
        }

        outPath.save( outputDiagram );
        return outputDiagram;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath simulationResult, inputDiagram, outputDiagram;

        public DataElementPath getSimulationResult()
        {
            return simulationResult;
        }

        public void setSimulationResult(DataElementPath simulationResult)
        {
            this.simulationResult = simulationResult;
        }

        public DataElementPath getInputDiagram()
        {
            return inputDiagram;
        }

        public void setInputDiagram(DataElementPath inputDiagram)
        {
            this.inputDiagram = inputDiagram;
        }

        public DataElementPath getOutputDiagram()
        {
            return outputDiagram;
        }

        public void setOutputDiagram(DataElementPath outputDiagram)
        {
            this.outputDiagram = outputDiagram;
        }
    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "inputDiagram" ).inputElement( Diagram.class ).add();
            property( "simulationResult" ).inputElement( SimulationResult.class ).add();
            property( "outputDiagram" ).outputElement( Diagram.class ).add();
        }
    }

}
