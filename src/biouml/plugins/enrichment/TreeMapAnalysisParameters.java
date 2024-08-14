package biouml.plugins.enrichment;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;

@SuppressWarnings ( "serial" )
public class TreeMapAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath sourcePath;
    private String pvalueColumn = ColumnNameSelector.NONE_COLUMN;
    private double similarity = 0.7;
    private Species species;
    private DataElementPath outputPath;
    private int displayLimit = 30;
    private boolean representativeOnly = false;


    @PropertyName ( "Functional classification" )
    @PropertyDescription ( "Result of Functional classification analysis." )
    public DataElementPath getSourcePath()
    {
        return sourcePath;
    }

    public void setSourcePath(DataElementPath sourcePath)
    {
        DataElementPath oldValue = this.sourcePath;
        this.sourcePath = sourcePath;

        if( sourcePath != null && sourcePath.optDataElement() instanceof TableDataCollection )
            setPvalueColumn( ColumnNameSelector.NONE_COLUMN );
        else
            setPvalueColumn( "" );
        firePropertyChange( "sourcePath", oldValue, this.sourcePath );
        if( sourcePath != null )
            setSpecies( Species.getDefaultSpecies( sourcePath.optDataCollection() ) );
    }

    public TableDataCollection getSource()
    {
        DataElement de = sourcePath == null ? null : sourcePath.optDataElement();
        return de instanceof TableDataCollection ? (TableDataCollection)de : null;
    }

    @PropertyName ( "P-value column" )
    @PropertyDescription ( "The result will use selected column in treemap building process." )
    public String getPvalueColumn()
    {
        return pvalueColumn;
    }
    public void setPvalueColumn(String pvalColumn)
    {
        Object oldValue = this.pvalueColumn;
        this.pvalueColumn = pvalColumn;
        firePropertyChange( "pvalueColumn", oldValue, pvalColumn );
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

    @PropertyName ( "Species" )
    @PropertyDescription ( "Species to which analysis should be confined" )
    public Species getSpecies()
    {
        return species;
    }
    public void setSpecies(Species species)
    {
        Object oldValue = this.species;
        this.species = species;
        firePropertyChange( "species", oldValue, species );
    }

    @PropertyName ( "Similarity" )
    @PropertyDescription ( "A lower value will result in shorter and semantically diverse list" )
    public double getSimilarity()
    {
        return similarity;
    }

    public void setSimilarity(double similarity)
    {
        Object oldValue = this.similarity;
        this.similarity = similarity;
        firePropertyChange( "similarity", oldValue, similarity );
    }

    @PropertyName ( "Max number to display" )
    @PropertyDescription ( "Maximal number of groups shown on treemap picture" )
    public int getDisplayLimit()
    {
        return displayLimit;
    }

    public void setDisplayLimit(int displayLimit)
    {
        this.displayLimit = displayLimit;
    }

    @PropertyName ( "Only representative" )
    @PropertyDescription ( "Only display parent ontologies for clusters" )
    public boolean isRepresentativeOnly()
    {
        return representativeOnly; 
    }
    
    public void setRepresentativeOnly(boolean representativeOnly)
    {
        Object oldValue = this.representativeOnly;
        this.representativeOnly = representativeOnly;
        firePropertyChange( "representativeOnly", oldValue, representativeOnly );
    }


}
