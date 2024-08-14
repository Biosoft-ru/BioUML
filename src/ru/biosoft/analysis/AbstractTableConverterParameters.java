package ru.biosoft.analysis;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.analysis.aggregate.NumericAggregator;
import ru.biosoft.analysis.aggregate.NumericSelector;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
public abstract class AbstractTableConverterParameters extends AbstractAnalysisParameters
{
    public static final String SPECIES_LIST = Species.formatSpeciesNames();

    public AbstractTableConverterParameters()
    {
        setSpecies(Species.getDefaultSpecies(null));
        setAggregator(NumericAggregator.getAggregators()[0]);
    }

    private String columnName = ColumnNameSelector.NONE_COLUMN;
    private boolean ignoreNaNInAggregator = true;
    private NumericAggregator aggregator;
    private Species species;
    private String targetType;
    private DataElementPath outputTable;

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
    @PropertyName ( "Numerical value treatment rule" )
    @PropertyDescription ( "Select one of the rules to treat values in the numerical columns of the table when several rows are merged into a single one.\n" +
            "In cases of \"average\", \"average w/o 20% outliers\" and \"sum\", the selected rule is applied to all numerical columns of the table. " +
            "In cases of \"minimum\", \"maximum\" and \"extreme\" a new option appears bellow which request user to select a \"Leading column\". " +
            "The chosen rule is applied then to the values in the selected Leading column " +
            "(e.g. in the Leading column the maximum value is computed among all the merged rows). " +
            "All other numerical values of the table will be taken from that row which corresponds to the selected value in the leading column.")
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
        firePropertyChange("aggregator", oldValue, aggregator);
    }

    @PropertyName ( "Leading column" )
    @PropertyDescription ( "Select the column with numerical values to apply one of the rules described above" )
    public String getColumnName()
    {
        return columnName;
    }

    public boolean isAggregatorColumnHidden()
    {
        return !(getAggregator() instanceof NumericSelector);
    }

    public boolean isColumnSpecified()
    {
        return ( getAggregator() instanceof NumericSelector ) && columnName != null && !columnName.equals(ColumnNameSelector.NONE_COLUMN)
                && !columnName.equals("");
    }

    public void setColumnName(String columnName)
    {
        String oldValue = this.columnName;
        this.columnName = columnName;
        firePropertyChange("columnName", oldValue, columnName);
    }

    @PropertyName ( "Species" )
    @PropertyDescription ( "Select $SPECIES_LIST$ species" )
    public Species getSpecies()
    {
        return species;
    }

    public void setSpecies(Species species)
    {
        Object oldValue = this.species;
        this.species = species;
        firePropertyChange("species", oldValue, species);
    }

    @PropertyName ( "Output type" )
    @PropertyDescription ( "Select type of identifiers for the resulting table" )
    public String getTargetType()
    {
        return targetType;
    }

    public String getShortTargetType()
    {
        return targetType == null?null:targetType.replaceAll(":", "");
    }

    public ReferenceType getTargetTypeObject()
    {
        return targetType == null?null:ReferenceTypeRegistry.optReferenceType(targetType);
    }

    public void setTargetType(String targetType)
    {
        Object oldValue = this.targetType;
        this.targetType = targetType;
        firePropertyChange("targetType", oldValue, targetType);
    }

    public String getIcon()
    {
        return IconFactory.getClassIconId(getTargetTypeObject().getClass());
    }

    @PropertyName ( "Output table" )
    @PropertyDescription ( "Path to store the resulting table in the tree" )
    public DataElementPath getOutputTable()
    {
        return outputTable;
    }

    public void setOutputTable(DataElementPath outputTable)
    {
        Object oldValue = this.outputTable;
        this.outputTable = outputTable;
        firePropertyChange("outputTable", oldValue, outputTable);
    }

    private boolean outputSourceIds = true;
    @PropertyName( "Output source ids" )
    @PropertyDescription( "Add column with source ids to output table" )
    public boolean isOutputSourceIds()
    {
        return outputSourceIds;
    }
    public void setOutputSourceIds(boolean outputSourceIds)
    {
        boolean oldValue = this.outputSourceIds;
        this.outputSourceIds = outputSourceIds;
        firePropertyChange( "outputSourceIds", oldValue, outputSourceIds );
    }
}
