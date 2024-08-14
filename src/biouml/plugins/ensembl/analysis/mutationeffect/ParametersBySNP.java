package biouml.plugins.ensembl.analysis.mutationeffect;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.maos.Parameters;

@SuppressWarnings ( "serial" )
public class ParametersBySNP extends Parameters
{

    private DataElementPath snpTable;
    private String riskColumn;

    public ParametersBySNP()
    {
        super();
    }

    @Override
    public Track getVcfTrackDataElement()
    {
        return null;
    }

    @PropertyName ( "Input SNP table" )
    @PropertyDescription ( "Input table with SNP identifiers" )
    public DataElementPath getSnpTable()
    {
        return snpTable;
    }
    public void setSnpTable(DataElementPath snpTable)
    {
        Object oldValue = this.snpTable;
        this.snpTable = snpTable;
        firePropertyChange( "snpTable", oldValue, snpTable );
    }

    @PropertyName ( "Risk column" )
    @PropertyDescription ( "Column with risk allele. If absent or not set, alternative allele of SNP will be taken " )
    public String getRiskColumn()
    {
        return riskColumn;
    }

    public void setRiskColumn(String riskColumn)
    {
        Object oldValue = this.riskColumn;
        this.riskColumn = riskColumn;
        firePropertyChange( "this.riskColumn", oldValue, riskColumn );
    }

}
