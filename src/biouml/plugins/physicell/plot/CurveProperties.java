package biouml.plugins.physicell.plot;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.physicell.CellDefinitionProperties;
import biouml.plugins.physicell.MulticellEModel;
import biouml.plugins.physicell.PhysicellUtil;
import biouml.plugins.physicell.RuleProperties;
import one.util.streamex.StreamEx;

public class CurveProperties extends Option
{
    public static final int LT = 1;
    public static final int LEQ = 2;
    public static final int GEQ = 3;
    public static final int GT = 4;
    
    private static final String LT_STRING = "<";
    private static final String LEQ_STRING = "<=";
    private static final String GEQ_STRING = ">=";
    private static final String GT_STRING = ">";
    
    private static final String NO_SIGNAL = "<<No signal>>";
    
    private static Map<String, Integer> relationMap = new HashMap<>();
    {
        relationMap.put( LT_STRING, LT );
        relationMap.put( LEQ_STRING, LEQ );
        relationMap.put( GEQ_STRING, GEQ );
        relationMap.put( GT_STRING, GT );
    }
    
    private boolean isNoSignal = true;
    private String name;
    private String cellType;
    private String signal = NO_SIGNAL;
    private String relation = LT_STRING;
    private double value;
    private int relationInt = LT;
    private MulticellEModel model;
    private CellDefinitionProperties cellDefinition;
    
    public void setModel(MulticellEModel model)
    {
        this.model = model;
    }

    public Stream<String> getCellTypes()
    {
        return model.getCellDefinitions().stream().map( cd -> cd.getName() );
    }

    public Stream<String> getSignals()
    {
        if( cellDefinition == null )
            return Stream.empty();
        return StreamEx.of( RuleProperties.getAvailableSignals( model, cellDefinition ) ).prepend( NO_SIGNAL );
    }

    public static String[] getRelations()
    {
        return new String[] {LT_STRING, LEQ_STRING, GEQ_STRING, GT_STRING};
    }
    
    @PropertyName("Name")
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @PropertyName("Cell type")
    public String getCellType()
    {
        return cellType;
    }
    public void setCellType(String cellType)
    {
        String oldValue = this.cellType;
        this.cellType = cellType;
        cellDefinition = PhysicellUtil.findCellDefinition( cellType, model );
        firePropertyChange( "cellType", oldValue, cellType );
    }
    
    @PropertyName("Signal")
    public String getSignal()
    {
        return signal;
    }
    public void setSignal(String signal)
    {
        String oldValue = this.signal;
        this.signal = signal;
        isNoSignal = signal.equals( NO_SIGNAL );            
        firePropertyChange( "signal", oldValue, signal );
    }
    
    public boolean isNoSignal()
    {
        return isNoSignal;
    }
    
    @PropertyName("Relation")
    public String getRelation()
    {
        return relation;
    }
    public void setRelation(String relation)
    {
        String oldValue = this.relation;
        this.relation = relation;
        relationInt = relationMap.get( relation );
        firePropertyChange( "relation", oldValue, relation );
    }
    
    public int getRelationInt()
    {
        return relationInt;
    }
    
    @PropertyName("Value")
    public double getValue()
    {
        return value;
    }
    public void setValue(double value)
    {
        double oldValue = this.value;
        this.value = value;
        firePropertyChange( "value", oldValue, value );
    }
}