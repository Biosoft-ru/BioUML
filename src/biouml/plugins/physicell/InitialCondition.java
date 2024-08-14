package biouml.plugins.physicell;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;

public class InitialCondition extends Option
{
    private boolean customCondition = false;
    private DataElementPath customConditionCode = null;
    private DataElementPath customConditionTable = null;

    @PropertyName ( "Custom condition" )
    public boolean isCustomCondition()
    {
        return customCondition;
    }
    public void setCustomCondition(boolean customCondition)
    {
        Object oldValue = this.customCondition;
        this.customCondition = customCondition;
        firePropertyChange( "customCondition", oldValue, customCondition );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Custom Java code" )
    public DataElementPath getCustomConditionCode()
    {
        return customConditionCode;
    }
    public void setCustomConditionCode(DataElementPath customConditionCode)
    {
        this.customConditionCode = customConditionCode;
    }

    @PropertyName ( "Custom table" )
    public DataElementPath getCustomConditionTable()
    {
        return customConditionTable;
    }
    public void setCustomConditionTable(DataElementPath customConditionTable)
    {
        this.customConditionTable = customConditionTable;
    }

    public boolean isDefaultCondition()
    {
        return !isCustomCondition();
    }
    
    @Override
    public InitialCondition clone()
    {
        InitialCondition result = new InitialCondition();
        result.customCondition = customCondition;
        result.customConditionCode = DataElementPath.create( customConditionCode.toString() );
        result.customConditionTable = DataElementPath.create( customConditionTable.toString() );
        return result;
    }
}