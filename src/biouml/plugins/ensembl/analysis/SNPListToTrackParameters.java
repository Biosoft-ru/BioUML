package biouml.plugins.ensembl.analysis;

import java.util.Properties;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.aggregate.NumericAggregator;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;

@SuppressWarnings ( "serial" )
public class SNPListToTrackParameters extends AbstractAnalysisParameters
{
    protected DataElementPath sourcePath, destPath, annotatedPath, outputGenes;
    protected Species species;
    protected int threePrimeSize = 0, fivePrimeSize = 1000;
    protected boolean outputNonMatched;
    protected String column = ColumnNameSelector.NONE_COLUMN;
    protected boolean ignoreNaNInAggregator = true;
    protected NumericAggregator aggregator = NumericAggregator.getAggregators()[0];

    public SNPListToTrackParameters()
    {
    }

    public TableDataCollection getSource()
    {
        DataCollection<?> source = sourcePath == null ? null : sourcePath.optDataCollection();
        return source instanceof TableDataCollection ? (TableDataCollection)source : null;
    }

    public void setSource(DataCollection<?> source)
    {
        setSourcePath(DataElementPath.create(source));
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
        if( oldValue == null || sourcePath == null || getSource() == null
                || ( !oldValue.equals(sourcePath) && !getSource().getColumnModel().hasColumn(getColumn()) ) )
            setColumn(ColumnNameSelector.NONE_COLUMN);
        Species species = Species.getDefaultSpecies(sourcePath == null ? null : sourcePath.optDataCollection());
        setEnsembl( EnsemblDatabaseSelector.getDefaultEnsembl( species ) );
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

    private EnsemblDatabase ensembl = EnsemblDatabaseSelector.getDefaultEnsembl();
    @PropertyName("Ensembl")
    @PropertyDescription("Ensembl database version")
    public EnsemblDatabase getEnsembl()
    {
        return ensembl;
    }
    public void setEnsembl(EnsemblDatabase ensembl)
    {
        Object oldValue = this.ensembl;
        this.ensembl = ensembl;
        firePropertyChange( "ensembl", oldValue, ensembl );
    }

    public DataElementPath getAnnotatedPath()
    {
        return annotatedPath;
    }

    public void setAnnotatedPath(DataElementPath annotatedPath)
    {
        Object oldValue = this.annotatedPath;
        this.annotatedPath = annotatedPath;
        firePropertyChange("annotatedPath", oldValue, this.annotatedPath);
    }

    public int getThreePrimeSize()
    {
        return threePrimeSize;
    }

    public void setThreePrimeSize(int threePrimeSize)
    {
        Object oldValue = this.threePrimeSize;
        this.threePrimeSize = threePrimeSize;
        firePropertyChange("threePrimeSize", oldValue, this.threePrimeSize);
    }

    public int getFivePrimeSize()
    {
        return fivePrimeSize;
    }

    public void setFivePrimeSize(int fivePrimeSize)
    {
        Object oldValue = this.fivePrimeSize;
        this.fivePrimeSize = fivePrimeSize;
        firePropertyChange("fivePrimeSize", oldValue, this.fivePrimeSize);
    }

    public boolean isOutputNonMatched()
    {
        return outputNonMatched;
    }

    public void setOutputNonMatched(boolean outputNonMatched)
    {
        Object oldValue = this.outputNonMatched;
        this.outputNonMatched = outputNonMatched;
        firePropertyChange("outputNonMatched", oldValue, this.outputNonMatched);
    }

    public DataElementPath getOutputGenes()
    {
        return outputGenes;
    }

    public void setOutputGenes(DataElementPath outputGenes)
    {
        Object oldValue = this.outputGenes;
        this.outputGenes = outputGenes;
        firePropertyChange("outputGenes", oldValue, this.outputGenes);
    }

    public String getColumn()
    {
        return column;
    }

    public void setColumn(String column)
    {
        Object oldValue = this.column;
        this.column = column;
        firePropertyChange("column", oldValue, this.column);
    }

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
        firePropertyChange("aggregator", oldValue, this.aggregator);
    }

    @Override
    public void read(Properties properties, String prefix)
    {
        super.read(properties, prefix);
        String column = properties.getProperty(prefix + "column");
        setColumn(column);
    }
}
