package ru.biosoft.bsa.analysis;

import java.util.Objects;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Track;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
public class TrackCoverageAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath track, output;
    private BasicGenomeSelector sequences;
    private int step=100000, window=100000;
    private boolean outputEmptyIntervals = false;
    
    public TrackCoverageAnalysisParameters()
    {
        setSequences(new BasicGenomeSelector());
    }
    
    @PropertyName("Input track")
    @PropertyDescription("Track to calculate coverage for")
    public DataElementPath getTrack()
    {
        return track;
    }
    
    public void setTrack(DataElementPath track)
    {
        Object oldValue = this.track;
        this.track = track;
        if(!Objects.equals( oldValue, track ))
        {
            Track t = track.optDataElement(Track.class);
            if(t != null)
            {
                setSequences(new BasicGenomeSelector(t));
            }
        }
        firePropertyChange("track", oldValue, track);
    }
    
    @PropertyName("Output table")
    @PropertyDescription("Path to the output table")
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
    
    @PropertyName("Sequences")
    @PropertyDescription("Collection of sequences")
    public BasicGenomeSelector getSequences()
    {
        return sequences;
    }
    public void setSequences(BasicGenomeSelector sequences)
    {
        Object oldValue = this.sequences;
        this.sequences = sequences;
        sequences.setParent(this);
        firePropertyChange("sequences", oldValue, sequences);
    }
    
    @PropertyName("Step")
    @PropertyDescription("Number of bp to step (at least 1000)")
    public int getStep()
    {
        return step;
    }
    
    public void setStep(int step)
    {
        Object oldValue = this.step;
        this.step = step;
        firePropertyChange("step", oldValue, step);
    }
    
    @PropertyName("Window size")
    @PropertyDescription("Size of the window to average the coverage")
    public int getWindow()
    {
        return window;
    }
    
    public void setWindow(int window)
    {
        Object oldValue = this.window;
        this.window = window;
        firePropertyChange("window", oldValue, window);
    }

    @PropertyName("Output empty intervals")
    @PropertyDescription("When checked, intervals without single site will appear in the output table as well")
    public boolean isOutputEmptyIntervals()
    {
        return outputEmptyIntervals;
    }

    public void setOutputEmptyIntervals(boolean outputEmptyIntervals)
    {
        Object oldValue = this.outputEmptyIntervals;
        this.outputEmptyIntervals = outputEmptyIntervals;
        firePropertyChange("outputEmptyIntervals", oldValue, outputEmptyIntervals);
    }
}
