package ru.biosoft.bsastats;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsastats.processors.QualityPerBaseProcessor;
import ru.biosoft.bsastats.processors.QualityPerSequenceProcessor;
import ru.biosoft.bsastats.processors.StatisticsProcessor;
import ru.biosoft.bsastats.processors.StatisticsProcessorsRegistry;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public class SequenceStatisticsParameters extends AbstractReadsSourceSelectorParameters
{
    protected DataElementPath output;
    protected DynamicPropertySet processorsDPS;
    
    public SequenceStatisticsParameters()
    {
        DynamicPropertySet processorsDPS = new DynamicPropertySetSupport();
        for(String processor: StatisticsProcessorsRegistry.getValues())
        {
            DynamicProperty property;
            property = new DynamicProperty(processor, Boolean.class, true);

            Class<? extends StatisticsProcessor> processorClass = StatisticsProcessorsRegistry.getProcessorClass(processor);
            PropertyDescription description = processorClass.getAnnotation(PropertyDescription.class);
            if(description != null)
                property.setShortDescription(description.value());
            processorsDPS.add(property);
        }
        setProcessorsDPS( processorsDPS );
        setSource( SOURCE_TRACK );
    }

    @PropertyName("Output path")
    @PropertyDescription("Path to the output folder (will be created)")
    public DataElementPath getOutput()
    {
        return output;
    }

    public void setOutput(DataElementPath output)
    {
        Object oldValue = this.output;
        this.output = output;
        firePropertyChange("output", oldValue, output);
    }
    
    public String[] getProcessors()
    {
        List<String> result = new ArrayList<>();
        for(DynamicProperty property: processorsDPS)
        {
            if((Boolean)property.getValue()) result.add(property.getName());
        }
        return result.toArray(new String[result.size()]);
    }

    @PropertyName("Processors")
    @PropertyDescription("List of methods to gather the statistics")
    public DynamicPropertySet getProcessorsDPS()
    {
        return processorsDPS;
    }

    private PropertyChangeListener dpsListener = e->firePropertyChange( "*", null, null );
    
    public void setProcessorsDPS(DynamicPropertySet processorsDPS)
    {
        DynamicPropertySet oldValue = this.processorsDPS;
        this.processorsDPS = processorsDPS;
        if(oldValue != null)
            oldValue.removePropertyChangeListener( dpsListener );
        if(processorsDPS != null)
            processorsDPS.addPropertyChangeListener( dpsListener );
        firePropertyChange("processorsDPS", oldValue, processorsDPS);
    }
    
    @Override
    public void setSource(String source)
    {
        super.setSource( source );
        for(DynamicProperty dp : getProcessorsDPS())
            dp.setValue( true );
        if(source.equals( SOURCE_TRACK ))
        {
            StatisticsProcessor[] qualityProcessors = new StatisticsProcessor[]{new QualityPerBaseProcessor(), new QualityPerSequenceProcessor() };
            for(StatisticsProcessor p : qualityProcessors )
                getProcessorsDPS().getProperty( p.getName() ).setValue( false );
        }
        firePropertyChange( "*", null, null );
    }
}
