package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public class FilterTrackByConditionParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputTrack, outputTrack;
    private String condition;

    @PropertyName("Input track")
    @PropertyDescription("Track you want to filter")
    public DataElementPath getInputTrack()
    {
        return inputTrack;
    }

    public void setInputTrack(DataElementPath inputTrack)
    {
        Object oldValue = this.inputTrack;
        this.inputTrack = inputTrack;
        firePropertyChange("inputTrack", oldValue, inputTrack);
    }

    @PropertyName("Output track")
    @PropertyDescription("Where to store the result")
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

    @PropertyName("Condition")
    @PropertyDescription("JavaScript predicate for sites that should pass the filter")
    public String getCondition()
    {
        return condition;
    }

    public void setCondition(String condition)
    {
        Object oldValue = this.condition;
        this.condition = condition;
        firePropertyChange("condition", oldValue, condition);
    }
}
