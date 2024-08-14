package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.standard.type.Species;

@SuppressWarnings ( "serial" )
public class ExtractPromotersParameters extends AbstractAnalysisParameters
{
    public static final String MODE_DO_NOT_MERGE_OVERLAPPING = "Leave all";
    public static final String MODE_SELECT_ONE_MAX = "Select one with maximal value";
    public static final String MODE_SELECT_ONE_MIN = "Select one with minimal value";
    public static final String MODE_SELECT_ONE_EXTREME = "Select one with extreme value";
    
    protected Integer from, to;
    protected DataElementPath sourcePath, destPath;
    protected Species species;
    
    protected String overlapMergingMode = MODE_DO_NOT_MERGE_OVERLAPPING;
    protected int minDistance = 100;
    protected String leadingColumn = ColumnNameSelector.NONE_COLUMN;
    

    public ExtractPromotersParameters()
    {
        from = -1000;
        to = 100;
        setSpecies(Species.getDefaultSpecies(null));
    }

    public Integer getFrom()
    {
        return from;
    }

    public void setFrom(Integer from)
    {
        Integer oldValue = this.from;
        this.from = from;
        firePropertyChange("from", oldValue, from);
    }

    public Integer getTo()
    {
        return to;
    }

    public void setTo(Integer to)
    {
        Integer oldValue = this.to;
        this.to = to;
        firePropertyChange("to", oldValue, to);
    }

    public DataElementPath getSourcePath()
    {
        return sourcePath;
    }

    public void setSourcePath(DataElementPath sourcePath)
    {
        DataElementPath oldValue = this.sourcePath;
        this.sourcePath = sourcePath;
        firePropertyChange("sourcePath", oldValue, this.sourcePath);
        setSpecies(Species.getDefaultSpecies(sourcePath == null ? null : sourcePath.optDataCollection()));
    }

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

    public Species getSpecies()
    {
        return species;
    }

    public void setSpecies(Species species)
    {
        Object oldValue = this.species;
        this.species = species;
        firePropertyChange("species", oldValue, this.species);
    }
    
    @PropertyName("Close TSS handling")
    @PropertyDescription("How to handle TSS located close to each other")
    public String getOverlapMergingMode()
    {
        return overlapMergingMode;
    }

    public void setOverlapMergingMode(String overlapMergingMode)
    {
        Object oldValue = this.overlapMergingMode;
        this.overlapMergingMode = overlapMergingMode;
        firePropertyChange( "overlapMergingMode", oldValue, overlapMergingMode );
    }
    
    @PropertyName("Minimal distance")
    @PropertyDescription("Minimal distance between adjacent TSS")
    public int getMinDistance()
    {
        return minDistance;
    }

    public void setMinDistance(int minDistance)
    {
        Object oldValue = this.minDistance;
        this.minDistance = minDistance;
        firePropertyChange( "minDistance", oldValue, minDistance );
    }
    
    public boolean isIgnoreOverlaping()
    {
        return overlapMergingMode.equals( MODE_DO_NOT_MERGE_OVERLAPPING );
    }

    @PropertyName("Leading column")
    @PropertyDescription("Numeric column used to select one TSS from adjacent")
    public String getLeadingColumn()
    {
        return leadingColumn;
    }

    public void setLeadingColumn(String leadingColumn)
    {
        Object oldValue = this.leadingColumn;
        this.leadingColumn = leadingColumn;
        firePropertyChange( "leadingColumn", oldValue, leadingColumn );
    }
}
