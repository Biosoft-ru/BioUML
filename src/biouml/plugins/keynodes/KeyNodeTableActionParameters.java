package biouml.plugins.keynodes;

import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;

/**
 * @author anna
 *
 */
@SuppressWarnings ( "serial" )
public class KeyNodeTableActionParameters extends AbstractAnalysisParameters
{
    protected DataElementPath knResultPath;
    protected DataElementPath outputPath;
    protected double score;
    protected List<DataElement> selectedItems = null;
    protected String rankColumn;
    protected boolean lowestRanking = false;
    protected int numTopRanking = 1;
    protected boolean separateResults = false;

    public KeyNodeTableActionParameters()
    {
    }
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }
    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange("outputPath", oldValue, outputPath);
    }

    @PropertyName ( "Score" )
    @PropertyDescription ( "Number of top ranking molecules" )
    public double getScore()
    {
        return score;
    }
    public void setScore(double score)
    {
        Object oldValue = this.score;
        this.score = score;
        firePropertyChange("score", oldValue, score);
    }

    @PropertyName ( "Analysis result" )
    @PropertyDescription ( "Result or target or effector search analysis" )
    public DataElementPath getKnResultPath()
    {
        return knResultPath;
    }
    public void setKnResultPath(DataElementPath knResultPath)
    {
        Object oldValue = this.knResultPath;
        this.knResultPath = knResultPath;
        if( knResultPath != null && ( knResultPath.optDataElement() instanceof TableDataCollection ))
            setRankColumn(ColumnNameSelector.NONE_COLUMN);
        else
            setRankColumn("");
        firePropertyChange("knResultPath", oldValue, knResultPath);
    }

    public List<DataElement> getSelectedItems()
    {
        return selectedItems;
    }

    public void setSelectedItems(List<DataElement> selectedItems)
    {
        this.selectedItems = selectedItems;
    }

    @PropertyName ( "Rank column" )
    @PropertyDescription ( "The result will be sorted by selected column in descending order and top ranking molecules will be taken" )
    public String getRankColumn()
    {
        return rankColumn;
    }
    public void setRankColumn(String rankColumn)
    {
        Object oldValue = this.rankColumn;
        this.rankColumn = rankColumn;
        firePropertyChange("rankColumn", oldValue, rankColumn);
    }

    @PropertyName ( "Number of top ranking molecules" )
    @PropertyDescription ( "Number of top ranking molecules" )
    public int getNumTopRanking()
    {
        return numTopRanking;
    }
    public void setNumTopRanking(int numTopRanking)
    {
        Object oldValue = this.numTopRanking;
        this.numTopRanking = numTopRanking;
        firePropertyChange("numTopRanking", oldValue, numTopRanking);
    }

    public boolean isSeparateResults()
    {
        return separateResults;
    }
    public void setSeparateResults(boolean separateResults)
    {
        Object oldValue = this.separateResults;
        this.separateResults = separateResults;
        firePropertyChange("separateResults", oldValue, separateResults);
    }

    /**
     * @return the lowestRanking
     */
    @PropertyName ( "Take lowest-ranking" )
    @PropertyDescription ( "Use molecules with lowest values in given column instead" )
    public boolean isLowestRanking()
    {
        return lowestRanking;
    }
    /**
     * @param lowestRanking the lowestRanking to set
     */
    public void setLowestRanking(boolean lowestRanking)
    {
        Object oldValue = this.lowestRanking;
        this.lowestRanking = lowestRanking;
        firePropertyChange("lowestRanking", oldValue, lowestRanking);
    }
}
