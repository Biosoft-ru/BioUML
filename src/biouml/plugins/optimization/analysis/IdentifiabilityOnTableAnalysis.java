package biouml.plugins.optimization.analysis;

import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.optimization.ExperimentalTableSupport;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.OptimizationExperiment.ExperimentType;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.optimization.ParameterConnection;
import biouml.plugins.optimization.ExperimentalTableSupport.WeightMethod;
import biouml.plugins.simulation.SimulationTaskParameters;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

public class IdentifiabilityOnTableAnalysis
        extends AbstractIdentifiabilityAnalysis<IdentifiabilityOnTableAnalysis.IdentifiabilityOnTableParameters>
{
    public IdentifiabilityOnTableAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new IdentifiabilityOnTableParameters() );
    }

    @Override
    public void validateParameters()
    {
        super.validateParameters();

        checkNotEmpty( "timeColumnName" );

        TableDataCollection paramsTable = parameters.getParametersTablePath().getDataElement( TableDataCollection.class );
        ColumnModel cm = paramsTable.getColumnModel();
        int minIndex = cm.optColumnIndex( "Min" );
        if( minIndex == -1 )
            throw new IllegalArgumentException( "Parameters table must contain 'Min' column" );
        int maxIndex = cm.optColumnIndex( "Max" );
        if( maxIndex == -1 )
            throw new IllegalArgumentException( "Parameters table must contain 'Max' column" );
        int valueIndex = cm.optColumnIndex( "Val" );
        if( valueIndex == -1 )
            throw new IllegalArgumentException( "Parameters table must contain 'Val' column" );
    }

    @Override
    protected OptimizationParameters prepareOptimizationParameters()
    {
        OptimizationParameters optParams = new OptimizationParameters();
        optParams.setDiagram( parameters.getDiagram() );
        optParams.setFittingParameters( getFittingParameters() );
        optParams.setOptimizationExperiments( getExperiments() );
        Map<String, SimulationTaskParameters> stp = optParams.getSimulationTaskParameters();
        for( SimulationTaskParameters stParam : stp.values() )
        {
            initSimulationEngine( (JavaSimulationEngine)stParam.getSimulationEngine() );
        }
        return optParams;
    }

    @Override
    protected List<Parameter> selectParameters(List<Parameter> allParameters)
    {
        return allParameters;
    }

    @Override
    protected List<Parameter> getFittingParameters()
    {
        List<Parameter> params = new ArrayList<>();
        Diagram diagram = parameters.getDiagram();
        DataCollection<Variable> availableVariables = diagram.getRole( EModel.class ).getVariables();
        TableDataCollection paramsTable = parameters.getParametersTablePath().getDataElement( TableDataCollection.class );
        ColumnModel cm = paramsTable.getColumnModel();
        int minIndex = cm.optColumnIndex( "Min" );
        int maxIndex = cm.optColumnIndex( "Max" );
        int valueIndex = cm.optColumnIndex( "Val" );
        for( RowDataElement rde : paramsTable )
        {
            String paramName = rde.getName();
            if( !availableVariables.contains( paramName ) )
            {
                if( !availableVariables.contains( "$" + paramName ) )
                {
                    log.warning( "Cannot find parameter '" + paramName
                            + "' in a given diagram. Check that diagram and parameters table are correct" );
                    continue;
                }
                else
                    paramName = "$" + paramName;
            }
            params.add( new Parameter( paramName, (Double)rde.getValues()[valueIndex], (Double)rde.getValues()[minIndex],
                    (Double)rde.getValues()[maxIndex] ) );
        }
        return params;
    }

    protected List<OptimizationExperiment> getExperiments()
    {
        TableDataCollection original = parameters.getObservablesTablePath().getDataElement( TableDataCollection.class );
        TableDataCollection tdc = new StandardTableDataCollection( null, "tc_exp" );

        original.getColumnModel().forEach( tc -> tdc.getColumnModel().addColumn( tc.getName(), tc.getValueClass() ) );
        original.stream().forEachOrdered( rde -> TableDataCollectionUtils.addRow( tdc, rde.getName(), rde.getValues() ) );

        if( parameters.getTimeColumnName() != null && !parameters.getTimeColumnName().isEmpty() )
        {
            int timeIndex = tdc.getColumnModel().optColumnIndex( parameters.getTimeColumnName() );
            tdc.getColumnModel().renameColumn( timeIndex, "time" );
        }

        OptimizationExperiment experiment = new OptimizationExperiment( "exp", tdc );
        experiment.setExperimentType( ExperimentType.toString( ExperimentType.TIME_COURSE ) );
        experiment.setWeightMethod( parameters.getWeightMethod() );
        experiment.setDiagram(parameters.getDiagram());
        experiment.setDiagramStateName("no state");
        List<ParameterConnection> conn = experiment.getParameterConnections();
        for(int i = 0; i < conn.size(); i++)
            conn.get(i).setNameInDiagram(conn.get(i).getNameInFile());

        if( parameters.resetWeights() )
            resetWeights( experiment );

        return StreamEx.of( experiment ).toList();
    }

    protected void initSimulationEngine(JavaSimulationEngine jse)
    {
        jse.setRelTolerance( 1E-5 );
        jse.setAbsTolerance( 1E-6 );
        jse.setLogLevel( Level.SEVERE );
    }

    private void resetWeights(OptimizationExperiment experiment)
    {
        TableDataCollection weights = parameters.getWeightsPath().getDataElement( TableDataCollection.class );
        int weightsColumn = weights.getColumnModel().optColumnIndex( parameters.getWeightColumnName() );
        for( ParameterConnection pc : experiment.getParameterConnections() )
        {
            try
            {
                RowDataElement rde = weights.get( pc.getName() );
                if( rde != null )
                    pc.setWeight( (Double)rde.getValues()[weightsColumn] );
            }
            catch( Exception e )
            {
                //do nothing
                log.log(Level.WARNING, "Cannot find parameter's '" + pc.getName() + "' weight in table", e );
            }
        }
    }

    @SuppressWarnings ( "serial" )
    @PropertyName ( "Parameters" )
    @PropertyDescription ( "Identifiability analysis parameters" )
    public static class IdentifiabilityOnTableParameters extends AbstractIdentifiabilityAnalysisParameters
    {
        private static final String NONE_COLUMN = "(none)";

        private DataElementPath diagramPath;
        private DataElementPath parametersTablePath;
        private DataElementPath observablesTablePath;
        private String timeColumnName = "";
        private String weightMethod = WeightMethod.toString( WeightMethod.MEAN_SQUARE );
        private DataElementPath weightsPath;
        private String weightColumnName = NONE_COLUMN;

        private final OptMethodWrapper optMethodWrapper = new OptMethodWrapper( this );

        @PropertyName ( "Diagram" )
        @PropertyDescription ( "Path to input diagram" )
        public DataElementPath getDiagramPath()
        {
            return diagramPath;
        }
        public void setDiagramPath(DataElementPath diagramPath)
        {
            Object oldValue = this.diagramPath;
            this.diagramPath = diagramPath;
            firePropertyChange( "diagramPath", oldValue, this.diagramPath );
        }

        @Override
        public Diagram getDiagram()
        {
            return diagramPath.getDataElement( Diagram.class );
        }

        @PropertyName ( "Parameters table" )
        @PropertyDescription ( "Table with list of parameters to check identifiability (must contain \"Min\", \"Max\", \"Val\" columns)" )
        public DataElementPath getParametersTablePath()
        {
            return parametersTablePath;
        }
        public void setParametersTablePath(DataElementPath parametersTablePath)
        {
            Object oldValue = this.parametersTablePath;
            this.parametersTablePath = parametersTablePath;
            firePropertyChange( "parametersTablePath", oldValue, this.parametersTablePath );
        }

        @PropertyName ( "Observables table" )
        @PropertyDescription ( "Table with data about observables values and time" )
        public DataElementPath getObservablesTablePath()
        {
            return observablesTablePath;
        }
        public void setObservablesTablePath(DataElementPath observablesTablePath)
        {
            Object oldValue = this.observablesTablePath;
            this.observablesTablePath = observablesTablePath;
            firePropertyChange( "observablesTablePath", oldValue, this.observablesTablePath );
        }

        @PropertyName ( "Time column" )
        @PropertyDescription ( "Name of the column with time values from the \"Observbles table\" "
                + "(will search for \"time\" if not selected)" )
        public String getTimeColumnName()
        {
            return timeColumnName;
        }
        public void setTimeColumnName(String timeColumnName)
        {
            String oldValue = this.timeColumnName;
            this.timeColumnName = timeColumnName;
            firePropertyChange( "timeColumnName", oldValue, this.timeColumnName );
        }

        @PropertyName ( "Weight method" )
        @PropertyDescription ( "Method which will be used to evaluate weights for objective function" )
        public String getWeightMethod()
        {
            return weightMethod;
        }
        public void setWeightMethod(String weightMethod)
        {
            String oldValue = this.weightMethod;
            this.weightMethod = weightMethod;
            firePropertyChange( "weightMethod", oldValue, this.weightMethod );
            firePropertyChange( "*", null, null );
        }

        @PropertyName ( "Weights table" )
        @PropertyDescription ( "Table with data about weights for observable values" )
        public DataElementPath getWeightsPath()
        {
            return weightsPath;
        }
        public void setWeightsPath(DataElementPath weightsPath)
        {
            Object oldValue = this.weightsPath;
            this.weightsPath = weightsPath;
            firePropertyChange( "weightsPath", oldValue, this.weightsPath );
        }
        public boolean hideWeightsPath()
        {
            return !WeightMethod.toString( WeightMethod.EDITED ).equals( weightMethod );
        }
        public boolean resetWeights()
        {
            return !hideWeightsPath() && weightsPath != null && !NONE_COLUMN.equals( weightColumnName );
        }

        @PropertyName ( "Weight column" )
        @PropertyDescription ( "Name of the column with weights from the \"Weights table\" (will set weights to 1.0 if not selected)" )
        public String getWeightColumnName()
        {
            return weightColumnName;
        }
        public void setWeightColumnName(String weightColumnName)
        {
            String oldValue = this.weightColumnName;
            this.weightColumnName = weightColumnName;
            firePropertyChange( "weightColumnName", oldValue, this.weightColumnName );
        }

        public OptMethodWrapper getOptMethodWrapper()
        {
            return optMethodWrapper;
        }
        @SuppressWarnings ( "unused" )
        public void setOptMethodWrapper(OptMethodWrapper optMethodWrapper)
        {
            //do nothing
        }
        @Override
        protected OptimizationMethod<?> getOptimizationMethod()
        {
            return optMethodWrapper.getOptMethod();
        }
    }

    public static class IdentifiabilityOnTableParametersBeanInfo extends BeanInfoEx2<IdentifiabilityOnTableParameters>
    {
        public IdentifiabilityOnTableParametersBeanInfo()
        {
            super( IdentifiabilityOnTableParameters.class );
        }

        @Override
        public void initProperties() throws IntrospectionException, NoSuchMethodException
        {
            property( "diagramPath" ).inputElement( Diagram.class ).add();
            property( "parametersTablePath" ).inputElement( TableDataCollection.class ).add();
            property( "observablesTablePath" ).inputElement( TableDataCollection.class ).add();
            property( ColumnNameSelector.registerSelector( "timeColumnName", beanClass, "observablesTablePath", true ) ).add();
            addHidden( "maxStepsNumber", "isManualSteps" );
            addHidden( "maxStepSize", "isManualSteps" );
            addExpert( "manualSteps" );
            addHidden( "stepsLeft", "isAutoSteps" );
            addHidden( "stepsRight", "isAutoSteps" );
            addExpert( "logY" );
            addExpert( "logX" );
            addExpert( "manualBound" );
            property( "delta" ).expert().hidden( "isAutoBound" ).add();
            property( "confidenceLevel" ).hidden( "isManualBound" ).add();
            add( "saveSolutions" );
            property( "plotType" ).expert().tags( IdentifiabilityHelper.getAvailablePlotTypes() ).add();
            add( "optMethodWrapper" );
            property( "weightMethod" ).tags( ExperimentalTableSupport.WeightMethod.getWeightMethods().toArray( new String[0] ) ).expert()
                    .add();
            property( "weightsPath" ).hidden( "hideWeightsPath" ).inputElement( TableDataCollection.class ).canBeNull().expert().add();
            property( ColumnNameSelector.registerSelector( "weightColumnName", beanClass, "weightsPath" ) ).hidden( "hideWeightsPath" )
                    .expert().add();
            property( "outputPath" ).outputElement( FolderCollection.class )
                    .auto( "$diagramPath/parent$/Identifiability results (for $diagramPath/name$)" ).add();
        }

    }

}
