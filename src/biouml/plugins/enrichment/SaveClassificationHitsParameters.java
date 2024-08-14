package biouml.plugins.enrichment;

import java.util.stream.Stream;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.type.Species;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;

@SuppressWarnings ( "serial" )
public class SaveClassificationHitsParameters extends AbstractAnalysisParameters
{
    private DataElementPath sourcePath;
    private Species species;
    private int maxGroupSize = 1000;
    private int minHits = 10;
    private double pValueThreshold = 0.1;
    private String pValueColumn = ColumnNameSelector.NONE_COLUMN;
    private int minCategories = 10, maxCategories = 500;
    private DataElementPath outputPath;
    private String[] obligateCategories;

    @PropertyName ( "Functional classification" )
    @PropertyDescription ( "Result of Functional classification analysis." )
    public DataElementPath getSourcePath()
    {
        return sourcePath;
    }
    public void setSourcePath(DataElementPath sourcePath)
    {
        Object oldValue = this.sourcePath;
        this.sourcePath = sourcePath;
        firePropertyChange( "this.sourcePath", oldValue, sourcePath );
    }

    @PropertyName ( "Maximal group size" )
    @PropertyDescription ( "Maximal size of the classification group." )
    public int getMaxGroupSize()
    {
        return maxGroupSize;
    }
    public void setMaxGroupSize(int maxGroupSize)
    {
        Object oldValue = this.maxGroupSize;
        this.maxGroupSize = maxGroupSize;
        firePropertyChange( "this.maxGroupSize", oldValue, maxGroupSize );
    }

    @PropertyName ( "Minimal hits" )
    @PropertyDescription ( "Minimal number of hits to the group." )
    public int getMinHits()
    {
        return minHits;
    }
    public void setMinHits(int minHits)
    {
        Object oldValue = this.minHits;
        this.minHits = minHits;
        firePropertyChange( "this.minHits", oldValue, minHits );
    }

    @PropertyName ( "FDR threshold" )
    @PropertyDescription ( "Adjusted P-Value threshold." )
    public double getPValueThreshold()
    {
        return pValueThreshold;
    }
    public void setPValueThreshold(double pValueThreshold)
    {
        Object oldValue = this.pValueThreshold;
        this.pValueThreshold = pValueThreshold;
        firePropertyChange( "this.pValueThreshold", oldValue, pValueThreshold );
    }

    @PropertyName ( "Species" )
    @PropertyDescription ( "Species to which analysis should be confined." )
    public Species getSpecies()
    {
        return species;
    }
    public void setSpecies(Species species)
    {
        Object oldValue = this.species;
        this.species = species;
        firePropertyChange( "this.species", oldValue, species );
    }

    @PropertyName ( "P-value column" )
    @PropertyDescription ( "The result will use selected column in filtering process." )
    public String getPValueColumn()
    {
        return pValueColumn;
    }
    public void setPValueColumn(String pValueColumn)
    {
        Object oldValue = this.pValueColumn;
        this.pValueColumn = pValueColumn;
        firePropertyChange( "this.pValueColumn", oldValue, pValueColumn );
    }

    @PropertyName ( "Output path" )
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
    @PropertyName ( "Minimal number of categories" )
    public int getMinCategories()
    {
        return minCategories;
    }
    public void setMinCategories(int minCategories)
    {
        Object oldValue = this.minCategories;
        this.minCategories = minCategories;
        firePropertyChange( "minCategories", oldValue, minCategories );
    }
    @PropertyName ( "Maximal number of categories" )
    public int getMaxCategories()
    {
        return maxCategories;
    }
    public void setMaxCategories(int maxCategories)
    {
        Object oldValue = this.maxCategories;
        this.maxCategories = maxCategories;
        firePropertyChange( "maxCategories", oldValue, maxCategories );
    }

    @PropertyName ( "Obligate categories" )
    @PropertyDescription ( "Categories that are requred to be taken as subsets" )
    public String[] getObligateCategories()
    {
        return obligateCategories;
    }
    public void setObligateCategories(String[] obligateCategories)
    {
        Object oldValue = this.obligateCategories;
        this.obligateCategories = obligateCategories;
        firePropertyChange( "obligateCategories", oldValue, obligateCategories );
    }
}
