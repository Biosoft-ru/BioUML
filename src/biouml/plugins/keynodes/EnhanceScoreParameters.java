package biouml.plugins.keynodes;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

/**
 * @author Ilya
 */
@SuppressWarnings ( "serial" )
@PropertyName ( "Enhance score parameters" )
public class EnhanceScoreParameters extends AbstractAnalysisParameters
{
    
    private DataElementPath keyNodeTable;
    private DataElementPath selectedProteins;
    private DataElementPath result;
    private double enhancement = 5;
    private String column;
    
    @PropertyName ( "Key Node result" )
    @PropertyDescription ( "Key Node result." )
    public DataElementPath getKeyNodeTable()
    {
        return keyNodeTable;
    }
    public void setKeyNodeTable(DataElementPath keyNodeTable)
    {
        DataElementPath oldValue = this.keyNodeTable;
        this.keyNodeTable = keyNodeTable;
        firePropertyChange( "keyNodeTable", oldValue, keyNodeTable );
    }
    
    @PropertyName ( "Selected proteins" )
    @PropertyDescription ( "Selected proteins." )
    public DataElementPath getSelectedProteins()
    {
        return selectedProteins;
    }
    public void setSelectedProteins(DataElementPath selectedProteins)
    {
        DataElementPath oldValue = this.selectedProteins;
        this.selectedProteins = selectedProteins;
        firePropertyChange( "selectedProteins", oldValue, selectedProteins );
    }
    
    @PropertyName ( "Enhancement value" )
    @PropertyDescription ( "Enhancement value." )
    public double getEnhancement()
    {
        return enhancement;
    }
    public void setEnhancement(double enhancement)
    {
        double oldValue = this.enhancement;
        this.enhancement = enhancement;
        firePropertyChange( "enhancement", oldValue, enhancement );
    }
    
    @PropertyName ( "Result" )
    @PropertyDescription ( "Result." )
    public DataElementPath getResult()
    {
        return result;
    }
    public void setResult(DataElementPath result)
    {
        DataElementPath oldValue = this.result;
        this.result = result;
        firePropertyChange( "result", oldValue, result );
    }
    
    @PropertyName ( "Column" )
    @PropertyDescription ( "Column." )
    public String getColumn()
    {
        return column;
    }
    public void setColumn(String column)
    {
        String oldValue = this.column;
        this.column = column;
        firePropertyChange( "column", oldValue, column );
    }
}
