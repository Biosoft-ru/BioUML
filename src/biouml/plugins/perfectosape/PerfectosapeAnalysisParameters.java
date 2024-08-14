package biouml.plugins.perfectosape;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
public class PerfectosapeAnalysisParameters extends AbstractAnalysisParameters
{
    protected static final String SEQUENCES_MODE = "RAW sequences";
    protected static final String SNP_MODE = "SNP list";
    private String mode = SEQUENCES_MODE;
    private DataElementPath matrixLib;
    private DataElementPath seqTable;
    private Species species = Species.getDefaultSpecies(null);
    private DataElementPath snpTable;
    private DataElementPath outTable;

    @PropertyName("Input matrix library")
    @PropertyDescription("Collection of input matrices (must be single-nucleotide matrices!)")
    public DataElementPath getMatrixLib()
    {
        return matrixLib;
    }
    public void setMatrixLib(DataElementPath matrixLib)
    {
        Object oldValue = this.matrixLib;
        this.matrixLib = matrixLib;
        firePropertyChange("matrixLib", oldValue, matrixLib);
    }
    
    @PropertyName("Sequences table")
    @PropertyDescription("Table containing sequences information (three columns: left flank, alleles, right flank)")
    public DataElementPath getSeqTable()
    {
        return seqTable;
    }
    public void setSeqTable(DataElementPath seqTable)
    {
        Object oldValue = this.seqTable;
        this.seqTable = seqTable;
        firePropertyChange("seqTable", oldValue, seqTable);
    }
    
    @PropertyName("Output table")
    @PropertyDescription("Path to store output")
    public DataElementPath getOutTable()
    {
        return outTable;
    }
    public void setOutTable(DataElementPath outTable)
    {
        Object oldValue = this.outTable;
        this.outTable = outTable;
        firePropertyChange("outTable", oldValue, outTable);
    }
    
    @PropertyName("Input data")
    @PropertyDescription("Select input data mode")
    public String getMode()
    {
        return mode;
    }
    public void setMode(String mode)
    {
        Object oldValue = this.mode;
        this.mode = mode;
        firePropertyChange("mode", oldValue, mode);
    }
    
    public boolean isSequencesModeHidden()
    {
        return !SEQUENCES_MODE.equals(mode);
    }
    
    public boolean isSNPModeHidden()
    {
        return !SNP_MODE.equals(mode);
    }
    
    @PropertyName("Species")
    @PropertyDescription("Species")
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
    
    @PropertyName("SNP table")
    @PropertyDescription("Table containing SNP identifiers (like 'rs111111')")
    public DataElementPath getSnpTable()
    {
        return snpTable;
    }
    public void setSnpTable(DataElementPath snpTable)
    {
        Object oldValue = this.snpTable;
        this.snpTable = snpTable;
        firePropertyChange("snpTable", oldValue, snpTable);
    }
    
    public DataElementPath getTable()
    {
        return isSequencesModeHidden() ? snpTable : seqTable;
    }
}
