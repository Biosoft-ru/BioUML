package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public class TableToTrackParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputTable, outputTrack;
    private String chromosomeColumn, fromColumn, toColumn, strandColumn = ColumnNameSelector.NONE_COLUMN;
    private DataElementPath sequencePath;
    private String genomeId;

    @PropertyName("Input table")
    @PropertyDescription("Table to convert to the track")
    public DataElementPath getInputTable()
    {
        return inputTable;
    }

    public void setInputTable(DataElementPath inputTable)
    {
        Object oldValue = this.inputTable;
        this.inputTable = inputTable;
        firePropertyChange("inputTable", oldValue, inputTable);
        if(inputTable != null && inputTable.optDataElement() instanceof TableDataCollection)
        {
            TableDataCollection table = inputTable.getDataElement(TableDataCollection.class);
            String columnName = table.getColumnModel().getColumn(0).getName();
            if(!table.getColumnModel().hasColumn(getChromosomeColumn())) setChromosomeColumn(columnName);
            if(!table.getColumnModel().hasColumn(getFromColumn())) setFromColumn(columnName);
            if(!table.getColumnModel().hasColumn(getToColumn())) setToColumn(columnName);
        } else
        {
            setChromosomeColumn("");
            setFromColumn("");
            setToColumn("");
        }
    }

    @PropertyName("Output track")
    @PropertyDescription("Path where to store the result")
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

    @PropertyName("Chromosome (sequence) column")
    @PropertyDescription("Column where chromosome number is located (either like 'chr1', 'chr:1', 'chr.1' or simply '1')")
    public String getChromosomeColumn()
    {
        return chromosomeColumn;
    }

    public void setChromosomeColumn(String chromosomeColumn)
    {
        Object oldValue = this.chromosomeColumn;
        this.chromosomeColumn = chromosomeColumn;
        firePropertyChange("chromosomeColumn", oldValue, chromosomeColumn);
    }

    @PropertyName("From column")
    @PropertyDescription("Column where site start coordinate is located")
    public String getFromColumn()
    {
        return fromColumn;
    }

    public void setFromColumn(String fromColumn)
    {
        Object oldValue = this.fromColumn;
        this.fromColumn = fromColumn;
        firePropertyChange("fromColumn", oldValue, fromColumn);
    }

    @PropertyName("To column")
    @PropertyDescription("Column where site end coordinate is located")
    public String getToColumn()
    {
        return toColumn;
    }

    public void setToColumn(String toColumn)
    {
        Object oldValue = this.toColumn;
        this.toColumn = toColumn;
        firePropertyChange("toColumn", oldValue, toColumn);
    }

    @PropertyName("Strand column")
    @PropertyDescription("Column where site strand is located (+ or -)")
    public String getStrandColumn()
    {
        return strandColumn;
    }

    public void setStrandColumn(String strandColumn)
    {
        Object oldValue = this.strandColumn;
        this.strandColumn = strandColumn;
        firePropertyChange( "strandColumn", oldValue, strandColumn );
    }

    @PropertyName("Sequence collection")
    @PropertyDescription("Sequences to bind the track to")
    public DataElementPath getSequenceCollectionPath()
    {
        return sequencePath;
    }

    public void setSequenceCollectionPath(DataElementPath sequencePath)
    {
        Object oldValue = this.sequencePath;
        this.sequencePath = sequencePath;
        firePropertyChange("sequencePath", oldValue, sequencePath);
    }

    public DataCollection getSequenceCollection()
    {
        return sequencePath == null ? null : sequencePath.optDataCollection();
    }

    @PropertyName("Genome ID string")
    @PropertyDescription("Something like 'hg18' or 'mm6'")
    public String getGenomeId()
    {
        return genomeId;
    }

    public void setGenomeId(String genomeId)
    {
        Object oldValue = this.genomeId;
        this.genomeId = genomeId;
        firePropertyChange("genomeId", oldValue, genomeId);
    }
}
