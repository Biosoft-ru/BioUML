package biouml.plugins.sedml.analyses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.Maps;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.standard.simulation.SimulationResult;

import com.developmentontheedge.beans.Option;

public class MergeSimulationResults extends AnalysisMethodSupport<MergeSimulationResults.Parameters>
{

    public MergeSimulationResults(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        if( parameters.getInputs().length == 0 )
            throw new IllegalArgumentException( "No inputs given" );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataCollectionUtils.createFoldersForPath( parameters.getOutputPath() );
        
        SimulationResult result;
        if( parameters.isMergeToExistingOutput() && parameters.getOutputPath().exists() )
            result = parameters.getOutputPath().getDataElement( SimulationResult.class );
        else
            result = new SimulationResult( parameters.getOutputPath().getParentCollection(), parameters.getOutputPath().getName() );

        for( SimulationResultReference p : parameters.getInputs() )
        {
            String prefix = p.getNamePrefix();
            if(prefix == null || prefix.isEmpty())
                prefix = p.getPath().getName();
            if( parameters.getMergeType().equals( Parameters.MERGE_BY_TIME ) )
                prefix = "";
            merge( result, p.getPath().getDataElement( SimulationResult.class ), prefix );
        }

        
        parameters.getOutputPath().save( result );

        return result;
    }

    private void merge(SimulationResult left, SimulationResult right, String prefix)
    {
        if(left.getVariableMap() == null || left.getVariableMap().isEmpty())
            copy(left, right, prefix);
        else if( parameters.getMergeType().equals( Parameters.MERGE_BY_TIME ) )
            mergeByTime( left, right );
        else
            mergeByVars( left, right, prefix );
    }

    private void copy(SimulationResult dst, SimulationResult src, String prefix)
    {
        dst.setTimes( src.getTimes() );
        dst.setValues( src.getValues() );
        dst.setCompletionTime( src.getCompletionTime() );
        dst.setDescription( src.getDescription() );
        dst.setDiagramPath( src.getDiagramPath() );
        dst.setInitialTime( src.getInitialTime() );
        dst.setSimulatorName( src.getSimulatorName() );
        dst.setTitle( src.getTitle() );
        Map<String, Integer> variableMap = Maps.transformKeys(src.getVariableMap(),
                key -> key.equals("time") ? key : prefix + key);
        dst.setVariableMap( variableMap );
        for(biouml.model.dynamics.Variable var : src.getInitialValues())
            dst.addInitialValue( var );
    }

    private void mergeByVars(SimulationResult left, SimulationResult right, String prefix)
    {
        if(left.getCount() != right.getCount())
            throw new IllegalArgumentException("Can not merge simulation results with distinct number of time points");
        
        int leftSize = left.getVariableMap().size();
        int rightSize = right.getVariableMap().size();
        
        int timeIndex = Integer.MAX_VALUE;
        if(right.getVariableMap().containsKey( "time" ))
        {
            timeIndex = right.getVariableMap().get( "time" );
            rightSize--;
        }
        Map<String, Integer> variableMap = new HashMap<>(left.getVariableMap());
        for(Map.Entry<String, Integer> e : right.getVariableMap().entrySet())
            if(!e.getKey().equals( "time" ))
            {
                int index = e.getValue();
                if(index > timeIndex)
                    index--;
                variableMap.put( prefix + e.getKey(), index + leftSize );
            }
        
        double[][] values = new double[left.getCount()][];
        for(int i = 0; i < left.getCount(); i++)
        {
            values[i] = new double[leftSize + rightSize];
            
            double[] leftValues = left.getValue(i);
            System.arraycopy( leftValues, 0, values[i], 0, leftSize );
        
            
            double[] rightValues = right.getValue(i);
            for(int j = 0; j < rightValues.length; j++)
            {
                if(j == timeIndex)
                    continue;
                int index = j;
                if(j > timeIndex)
                    index--;
                values[i][leftSize + index] = rightValues[j];
            }
        }
        
        left.setVariableMap( variableMap );
        left.setValues( values );
    }

    private void mergeByTime(SimulationResult left, SimulationResult right)
    {
        if( !left.getVariableMap().equals( right.getVariableMap() ) )
            throw new IllegalArgumentException( "Can not merge simulation results with distinct set of variables" );

        boolean skipFirst = right.getTimes()[0] == 0 && right.getCount() > 1 && Arrays.equals( right.getValue( 0 ), left.getValue( left.getCount() - 1 ) );
        double[] times = new double[left.getCount() + right.getCount() - (skipFirst ? 1 : 0)];
        System.arraycopy( left.getTimes(), 0, times, 0, left.getCount() );
        double rightStartTime = times[left.getCount() - 1];
        for(int i = 0; i < right.getCount() - (skipFirst ? 1 : 0); i++)
            times[i + left.getCount()] = right.getTimes()[i + (skipFirst ? 1 : 0)] + rightStartTime;

        double[][] values = new double[times.length][];
        System.arraycopy( left.getValues(), 0, values, 0, left.getCount() );
        System.arraycopy( right.getValues(), skipFirst ? 1 : 0, values, left.getCount(), right.getCount() - (skipFirst ? 1 : 0) );
        
        if(left.getVariableMap().containsKey( "time" ))
        {
            int timeColumn = left.getVariableMap().get( "time" );
            for(int i = 0; i < values.length; i++)
                values[i][timeColumn] = times[i];
        }
        
        left.setTimes( times );
        left.setValues( values );
    }

    @PropertyName ( "SimulationResult" )
    public static class SimulationResultReference extends Option implements JSONBean
    {
        private DataElementPath path;
        private String namePrefix = "";

        public DataElementPath getPath()
        {
            return path;
        }

        @PropertyName ( "Path" )
        @PropertyDescription("Path to simulation result")
        public void setPath(DataElementPath path)
        {
            Object oldValue = this.path;
            this.path = path;
            firePropertyChange( "path", oldValue, path );
        }

        @PropertyName("Name prefix")
        @PropertyDescription("Prefix for names of variables")
        public String getNamePrefix()
        {
            return namePrefix;
        }

        public void setNamePrefix(String namePrefix)
        {
            Object oldValue = this.namePrefix;
            this.namePrefix = namePrefix;
            firePropertyChange( "namePrefix", oldValue, namePrefix );
        }
    }

    public static class SimulationResultReferenceBeanInfo extends BeanInfoEx2<SimulationResultReference>
    {
        public SimulationResultReferenceBeanInfo()
        {
            super( SimulationResultReference.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "path" ).inputElement( SimulationResult.class ).add();
            add( "namePrefix" );
        }
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private SimulationResultReference[] inputs;

        public static final String MERGE_BY_TIME = "By time";
        public static final String MERGE_BY_VARS = "By variables";
        static final String[] MERGE_TYPES = {MERGE_BY_TIME, MERGE_BY_VARS};
        private String mergeType = MERGE_BY_VARS;

        private boolean mergeToExistingOutput;

        private DataElementPath outputPath;
        
        public Parameters()
        {
            setInputs( new SimulationResultReference[] {new SimulationResultReference()} );
        }

        public SimulationResultReference[] getInputs()
        {
            return inputs;
        }

        public void setInputs(SimulationResultReference[] inputs)
        {
            for( SimulationResultReference e : inputs )
                if(e != null)
                    e.setParent( this );
            Object oldValue = this.inputs;
            this.inputs = inputs;
            firePropertyChange( "inputs", oldValue, inputs );
        }

        public String getMergeType()
        {
            return mergeType;
        }

        public void setMergeType(String mergeType)
        {
            Object oldValue = this.mergeType;
            this.mergeType = mergeType;
            firePropertyChange( "mergeType", oldValue, mergeType );
        }

        public boolean isMergeToExistingOutput()
        {
            return mergeToExistingOutput;
        }

        public void setMergeToExistingOutput(boolean mergeToExistingOutput)
        {
            boolean oldValue = this.mergeToExistingOutput;
            this.mergeToExistingOutput = mergeToExistingOutput;
            firePropertyChange( "mergeToExistingOutput", oldValue, mergeToExistingOutput );
        }

        public DataElementPath getOutputPath()
        {
            return outputPath;
        }

        public void setOutputPath(DataElementPath outputPath)
        {
            Object oldValue = this.outputPath;
            this.outputPath = outputPath;
            firePropertyChange( "outputPath", oldValue, outputPath );
        }
        
        @Override
        public @Nonnull String[] getInputNames()
        {
            String[] result = new String[inputs.length];
            for(int i = 0; i < inputs.length; i++)
                result[i] = "inputs/[" + i + "]/path";
            return result;
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
            add( "inputs" );
            addWithTags( "mergeType", Parameters.MERGE_TYPES );
            add( "mergeToExistingOutput" );
            property( "outputPath" ).outputElement( SimulationResult.class ).add();
        }
    }
}
