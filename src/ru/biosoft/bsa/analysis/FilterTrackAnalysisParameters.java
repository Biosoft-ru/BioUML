package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.aggregate.NumericAggregator;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public class FilterTrackAnalysisParameters extends AbstractAnalysisParameters
{
    public static final String MODE_INTERSECT = "Intersect";
    public static final String MODE_INTERSECT_BOTH_SITES = "Intersect (leave both sites)";
    public static final String MODE_SUBTRACT = "Subtract";

    static final String[] MODES = {MODE_INTERSECT, MODE_INTERSECT_BOTH_SITES, MODE_SUBTRACT};
    
    private DataElementPath inputTrack, filterTrack, outputTrack;
    private int maxDistance = 0;

    private String[] fieldNames;
    private String[] selectedFilterFieldNames;
    private boolean ignoreNaNInAggregator = true;
    private NumericAggregator aggregator = NumericAggregator.getAggregators()[0];
    private String mode = MODES[0];

    @PropertyName("Input track")
    @PropertyDescription("Track which you want to filter")
    public DataElementPath getInputTrack()
    {
        return inputTrack;
    }

    public void setInputTrack(DataElementPath inputTrack)
    {
        Object oldValue = this.inputTrack;
        this.inputTrack = inputTrack;
        //drop selection if input track was changed
        if( inputTrack == null || !inputTrack.equals( oldValue ) )
        {
            setFieldNames( null );
            setSelectedFilterFieldNames( null );
        }
        firePropertyChange("inputTrack", oldValue, inputTrack);
    }

    @PropertyName("Filter track")
    @PropertyDescription("Track to use as a filter")
    public DataElementPath getFilterTrack()
    {
        return filterTrack;
    }

    public void setFilterTrack(DataElementPath filterTrack)
    {
        Object oldValue = this.filterTrack;
        this.filterTrack = filterTrack;
        if( filterTrack == null || !filterTrack.equals( oldValue ) )
            setSelectedFilterFieldNames( null );
        firePropertyChange("filterTrack", oldValue, filterTrack);
    }

    @PropertyName("Output track")
    @PropertyDescription("Specify the location where to store results")
    public DataElementPath getOutputTrack()
    {
        return outputTrack;
    }

    public void setOutputTrack(DataElementPath outputTrack)
    {
        Object oldValue = this.outputTrack;
        this.outputTrack = outputTrack;
        firePropertyChange("outputTrack", oldValue, outputTrack);
    }

    @PropertyName("Max distance")
    @PropertyDescription("Maximal difference between site starts and ends to consider sites equal")
    public int getMaxDistance()
    {
        return maxDistance;
    }

    public void setMaxDistance(int maxDistance)
    {
        Object oldValue = this.maxDistance;
        this.maxDistance = maxDistance;
        firePropertyChange("maxDistance", oldValue, maxDistance);
    }

    @PropertyName("Fields to compare")
    @PropertyDescription("Consider sites equal when these fields values match aside from sites position and strand")
    public String[] getFieldNames()
    {
        return fieldNames == null ? new String[0] : fieldNames;
    }

    public void setFieldNames(String[] fieldNames)
    {
        Object oldValue = this.fieldNames;
        this.fieldNames = fieldNames;
        firePropertyChange("fieldNames", oldValue, fieldNames);
    }

    @PropertyName ( "Fields to copy from filter track" )
    public String[] getSelectedFilterFieldNames()
    {
        return selectedFilterFieldNames == null ? new String[0] : selectedFilterFieldNames;
    }
    public void setSelectedFilterFieldNames(String[] selectedFilterFieldNames)
    {
        Object oldValue = this.selectedFilterFieldNames;
        this.selectedFilterFieldNames = selectedFilterFieldNames;
        firePropertyChange( "selectedFilterFieldNames", oldValue, selectedFilterFieldNames );
    }

    @PropertyName ( "Ignore empty values" )
    @PropertyDescription ( "Ignore empty values during aggregator work" )
    public boolean isIgnoreNaNInAggregator()
    {
        return ignoreNaNInAggregator;
    }
    public void setIgnoreNaNInAggregator(boolean ignoreNaNInAggregator)
    {
        boolean oldValue = this.ignoreNaNInAggregator;
        this.ignoreNaNInAggregator = ignoreNaNInAggregator;
        firePropertyChange( "ignoreNaNInAggregator", oldValue, ignoreNaNInAggregator );
        if( aggregator != null )
            aggregator.setIgnoreNaNs( ignoreNaNInAggregator );
    }

    @PropertyName ( "Aggregator for numbers" )
    @PropertyDescription ( "Function to be used for numerical properties when sites intervals intersects" )
    public NumericAggregator getAggregator()
    {
        return aggregator;
    }
    public void setAggregator(NumericAggregator aggregator)
    {
        if( aggregator != null )
            aggregator.setIgnoreNaNs( ignoreNaNInAggregator );
        Object oldValue = this.aggregator;
        this.aggregator = aggregator;
        firePropertyChange( "aggregator", oldValue, aggregator );
    }

    @PropertyName("Filtering mode")
    @PropertyDescription("Specify how to perform the filtering")
    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        Object oldValue = this.mode;
        this.mode = mode;
        firePropertyChange("mode", oldValue, mode);
    }
}
