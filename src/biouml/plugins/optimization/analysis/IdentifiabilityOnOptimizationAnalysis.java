package biouml.plugins.optimization.analysis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.workbench.diagram.SetInitialValuesAction;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class IdentifiabilityOnOptimizationAnalysis
        extends AbstractIdentifiabilityAnalysis<IdentifiabilityOnOptimizationAnalysis.IdentifiabilityOnOptimizationParameters>
{
    public IdentifiabilityOnOptimizationAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new IdentifiabilityOnOptimizationParameters() );
    }

    @Override
    public void validateParameters()
    {
        super.validateParameters();

        IdentifiabilityOnOptimizationParameters params = parameters;
        Optimization opt = params.getOptimization();
        if( opt == null )
            throw new IllegalArgumentException( "Selected optimization document can not be read." );
        if( opt.getParameters().getOptimizationExperiments().size() == 0 )
            throw new IllegalArgumentException( "Optimization document should contain at least one experiment. "
                    + "Please add experiments in selected optimization or specify anlother one." );
    }

    @Override
    protected List<Parameter> selectParameters(List<Parameter> allParameters)
    {
        String[] selectedParams = parameters.getSelectedParams();
        if( selectedParams.length == 0 )
            return allParameters;
        Set<String> selectedSet = new HashSet<>( Arrays.asList( selectedParams ) );
        return StreamEx.of( allParameters ).filter( p -> selectedSet.contains( p.getName() ) ).toList();
    }

    @Override
    protected OptimizationParameters prepareOptimizationParameters()
    {
        return parameters.getOptimization().getParameters();
    }

    @Override
    protected List<Parameter> getFittingParameters()
    {
        Optimization opt = parameters.getOptimization();
        try
        {
            if( parameters.getValuesPath() != null )
            {
                TableDataCollection values = (TableDataCollection)parameters.getValuesPath().getDataElement();
                SetInitialValuesAction setValuesAction = new SetInitialValuesAction(log)
                {
                    @Override
                    protected void setValue(DataElement de, double value)
                    {
                        ( (Parameter)de ).setValue( value );
                    }

                    @Override
                    protected Iterator<DataElement> getElementsIterator()
                    {
                        return StreamEx.of( opt.getParameters().getFittingParameters() ).map( p -> (DataElement)p ).iterator();
                    }
                };
                setValuesAction.setValues( values );
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not set initial values of the fitting parameters: " + e);
        }
        return opt.getParameters().getFittingParameters().stream().map( Parameter::copy ).collect( Collectors.toList() );
    }

    public static int index = 1;

    @SuppressWarnings ( "serial" )
    @PropertyName ( "Parameters" )
    @PropertyDescription ( "Identifiability analysis parameters" )
    public static class IdentifiabilityOnOptimizationParameters extends AbstractIdentifiabilityAnalysisParameters
    {
        private DataElementPath optimizationPath;
        private DataElementPath valuesPath;
        private String[] selectedParams = new String[0];
        private Optimization optimization;

        public Optimization getOptimization()
        {
            if(optimizationPath == null)
                return null;
            if( optimization == null )
            {
                Optimization baseOptimization = optimizationPath.optDataElement( Optimization.class );
                if( baseOptimization != null )
                {
                    optimization = baseOptimization.clone( baseOptimization.getOrigin(),
                            baseOptimization.getName() + "_identifiability_" + index );
                    index++;
                }
            }
            return optimization;
        }

        @Override
        public Diagram getDiagram()
        {
            return getOptimization().getDiagram();
        }

        @PropertyName ( "Optimization" )
        @PropertyDescription ( "A path to the optimization document." )
        public DataElementPath getOptimizationPath()
        {
            return optimizationPath;
        }
        public void setOptimizationPath(DataElementPath optimizationPath)
        {
            Object oldValue = this.optimizationPath;
            this.optimizationPath = optimizationPath;
            optimization = null;
            firePropertyChange( "optimizationPath", oldValue, this.optimizationPath );
        }

        @PropertyName ( "Parameter values" )
        @PropertyDescription ( "A path to the table containing the fitted parameter values."
                + " If this path is skipped, the values stored in the optimization document will be used as the start values of the analysis." )
        public DataElementPath getValuesPath()
        {
            return valuesPath;
        }
        public void setValuesPath(DataElementPath valuesPath)
        {
            Object oldValue = this.valuesPath;
            this.valuesPath = valuesPath;
            firePropertyChange( "valuesPath", oldValue, this.valuesPath );
        }

        @PropertyName ( "Parameters" )
        @PropertyDescription ( "Parameters to check identifiability." )
        public String[] getSelectedParams()
        {
            return selectedParams;
        }
        public void setSelectedParams(String[] selectedParams)
        {
            if( selectedParams == null )
                return;
            String[] oldValue = this.selectedParams;
            this.selectedParams = selectedParams;
            firePropertyChange( "selectedParams", oldValue, selectedParams );
        }

        @Override
        protected OptimizationMethod<?> getOptimizationMethod()
        {
            return getOptimization().getOptimizationMethod();
        }
    }

    public static class IdentifiabilityOnOptimizationParametersBeanInfo extends BeanInfoEx2<IdentifiabilityOnOptimizationParameters>
    {
        public IdentifiabilityOnOptimizationParametersBeanInfo()
        {
            super( IdentifiabilityOnOptimizationParameters.class );
        }
        @Override
        public void initProperties() throws Exception
        {
            property( "optimizationPath" ).inputElement( Optimization.class ).add();
            property( "valuesPath" ).inputElement( TableDataCollection.class ).canBeNull().add();
            property( "selectedParams" ).editor( ParametersNameEditor.class ).simple().hideChildren().expert().add();
            addHidden("maxStepsNumber", "isManualSteps");
            addHidden("maxStepSize", "isManualSteps");
            addExpert("manualSteps");
            addHidden( "stepsLeft", "isAutoSteps" );
            addHidden( "stepsRight", "isAutoSteps" );
            addExpert("logY");
            addExpert("logX");
            addExpert( "manualBound" );
            property( "delta" ).expert().hidden( "isAutoBound" ).add();
            property( "confidenceLevel" ).hidden( "isManualBound" ).add();
            add( "saveSolutions" );
            property( "plotType" ).expert().tags( IdentifiabilityHelper.getAvailablePlotTypes() ).add();
            property( "outputPath" ).outputElement( FolderCollection.class )
                    .auto( "$optimizationPath/parent$/Identifiability results (for $optimizationPath/name$)" ).add();
        }
    }

    public static abstract class NameEditor extends GenericMultiSelectEditor
    {
        @Override
        public String[] getAvailableValues()
        {
            return getValues( ( (IdentifiabilityOnOptimizationParameters)getBean() ).getOptimization() );
        }
        protected abstract String[] getValues(Optimization optimization);
    }

    public static class ParametersNameEditor extends NameEditor
    {
        @Override
        protected String[] getValues(Optimization optimization)
        {
            return Optional.ofNullable( optimization )
                    .map( opt -> opt.getParameters().getFittingParameters().stream().map( Parameter::getName ).toArray( String[]::new ) )
                    .orElse( new String[0] );
        }
    }
}
