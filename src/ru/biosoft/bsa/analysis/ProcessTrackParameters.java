package ru.biosoft.bsa.analysis;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;

public class ProcessTrackParameters extends AbstractAnalysisParameters
{
    public static final String NO_SHRINK = "No shrink";
    public static final String SHRINK_TO_START = "Shrink to start";
    public static final String SHRINK_TO_CENTER = "Shrink to center";
    public static final String SHRINK_TO_END = "Shrink to end";
    public static final String SHRINK_TO_SUMMIT = "Shrink to summit";
    
    private static final long serialVersionUID = 1L;
    private DataElementPath sourcePath, destPath;
    private int enlargeStart = 100, enlargeEnd = 100;
    private int minimalSize;
    private boolean mergeOverlapping;
    private boolean removeSmallSites = true;
    private DataElementPath sequences;
    private String shrinkMode = NO_SHRINK;
    
    public Track getSource()
    {
        DataElement de = getSourcePath() == null?null:getSourcePath().optDataElement();
        return de instanceof Track?(Track)de:null;
    }
    
    @PropertyName("Source track")
    @PropertyDescription("Track you want to process")
    public DataElementPath getSourcePath()
    {
        return sourcePath;
    }
    public void setSourcePath(DataElementPath sourcePath)
    {
        Object oldValue = this.sourcePath;
        this.sourcePath = sourcePath;
        if(sourcePath.optDataElement() instanceof SqlTrack)
        {
            SqlTrack t = sourcePath.getDataElement(SqlTrack.class);
            DataElementPath chromosomesPath = t.getChromosomesPath();
            if(chromosomesPath != null)
                setSequences( chromosomesPath );
        }
        firePropertyChange("sourcePath", oldValue, this.sourcePath);
    }
    
    @PropertyName("Output track")
    @PropertyDescription("Path for processed track")
    public DataElementPath getDestPath()
    {
        return destPath;
    }
    public void setDestPath(DataElementPath destPath)
    {
        Object oldValue = this.destPath;
        this.destPath = destPath;
        firePropertyChange("destPath", oldValue, this.destPath);
    }
    
    @PropertyName("Shrink sites to zero length?")
    @PropertyDescription("When shrink mode is selected, the sites are shrinked to zero length before further processing")
    public String getShrinkMode()
    {
        return shrinkMode;
    }

    public void setShrinkMode(String shrinkMode)
    {
        Object oldValue = this.shrinkMode;
        this.shrinkMode = shrinkMode;
        firePropertyChange( "shrinkMode", oldValue, this.shrinkMode );
    }

    @PropertyName("Enlarge sites at start")
    @PropertyDescription("Use positive numbers to enlarge and negative to shrink")
    public int getEnlargeStart()
    {
        return enlargeStart;
    }
    public void setEnlargeStart(int enlargeStart)
    {
        Object oldValue = this.enlargeStart;
        this.enlargeStart = enlargeStart;
        firePropertyChange("enlargeStart", oldValue, this.enlargeStart);
    }

    @PropertyName("Enlarge sites at end")
    @PropertyDescription("Use positive numbers to enlarge and negative to shrink")
    public int getEnlargeEnd()
    {
        return enlargeEnd;
    }
    public void setEnlargeEnd(int enlargeEnd)
    {
        Object oldValue = this.enlargeEnd;
        this.enlargeEnd = enlargeEnd;
        firePropertyChange("enlargeEnd", oldValue, this.enlargeEnd);
    }
    
    @PropertyName("Minimal site size")
    @PropertyDescription("Sites shorter than specified size will be removed from output")
    public int getMinimalSize()
    {
        return minimalSize;
    }
    public void setMinimalSize(int minimalSize)
    {
        Object oldValue = this.minimalSize;
        this.minimalSize = minimalSize;
        firePropertyChange("minimalSize", oldValue, this.minimalSize);
    }
    
    @PropertyName("Merge overlapping")
    @PropertyDescription("Merges overlapping sites into a single site. Site annotations will be lost!")
    public boolean isMergeOverlapping()
    {
        return mergeOverlapping;
    }
    public void setMergeOverlapping(boolean mergeOverlapping)
    {
        Object oldValue = this.mergeOverlapping;
        this.mergeOverlapping = mergeOverlapping;
        firePropertyChange("mergeOverlapping", oldValue, this.mergeOverlapping);
    }
    
    @PropertyName("Remove small sites")
    @PropertyDescription("If checked, sites smaller then 'Minimal site size' will be removed, otherwise they will be expanded to 'Minimal site size'")
    public boolean isRemoveSmallSites()
    {
        return removeSmallSites;
    }
    
    public void setRemoveSmallSites(boolean removeSmallSites)
    {
        Object oldValue = this.removeSmallSites;
        this.removeSmallSites = removeSmallSites;
        firePropertyChange("removeSmallSites", oldValue, this.removeSmallSites);
    }

    @PropertyName("Sequences")
    @PropertyDescription("Sequences to use")
    public DataElementPath getSequences()
    {
        return sequences;
    }

    public void setSequences(DataElementPath sequences)
    {
        Object oldValue = this.sequences;
        this.sequences = sequences;
        firePropertyChange("sequences", oldValue, this.sequences);
    }

}
