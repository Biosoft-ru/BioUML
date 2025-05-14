package biouml.plugins.physicell;

import java.util.stream.Stream;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.physicell.core.SignalBehavior;

/**
 * Bean corresponds to Hypothesis 
 */
public class RuleProperties
{
    public static String DIRECTION_INCREASES = "increases";
    public static String DIRECTION_DECREASES = "decreases";

    private MulticellEModel model;
    private CellDefinitionProperties cellDefinition;
    private String[] directions = new String[] {DIRECTION_INCREASES, DIRECTION_DECREASES};

    private String signal = "";
    private String direction = DIRECTION_INCREASES;
    private String behavior = "";
    private double baseValue = 0.0;
    private double saturationValue = 0.0;
    private double halfMax = 0.5;
    private double hillPower = 4;
    private boolean applyToDead = false;

    public String[] getAvailableDirections()
    {
        return directions;
    }

    public Stream<String> getAvailableSignals()
    {
        return getAvailableSignals( model, cellDefinition );
    }
    
    public Stream<String> getAvailableBehaviors()
    {
        return getAvailableBehaviors( model, cellDefinition );
    }
    
    public static Stream<String> getAvailableSignals(MulticellEModel model,  CellDefinitionProperties cellDefinition)
    {
        String[] substrates = new String[0];
        String[] cellTypes = new String[0];
        String[] custom = new String[0];
        if( model != null )
        {
            substrates = model.getSubstrates().stream().map( s -> s.getName() ).toArray( String[]::new );
            cellTypes = model.getCellDefinitions().stream().map( s -> s.getName() ).toArray( String[]::new );
        }
        if( cellDefinition != null )
            custom = Stream.of( cellDefinition.getCustomDataProperties().getVariables() ).map( s -> s.getName() ).toArray( String[]::new );
        return SignalBehavior.getSignals( substrates, cellTypes, custom ).stream().sorted();
    }

    public static Stream<String> getAvailableBehaviors(MulticellEModel model,  CellDefinitionProperties cellDefinition)
    {
        String[] substrates = new String[0];
        String[] cellTypes = new String[0];
        String[] custom = new String[0];
        if( model != null )
        {
            substrates = model.getSubstrates().stream().map( s -> s.getName() ).toArray( String[]::new );
            cellTypes = model.getCellDefinitions().stream().map( s -> s.getName() ).toArray( String[]::new );
        }
        if( cellDefinition != null )
            custom = Stream.of( cellDefinition.getCustomDataProperties().getVariables() ).map( s -> s.getName() ).toArray( String[]::new );
        return SignalBehavior.getBehaviors( substrates, cellTypes, custom ).stream().sorted();
    }

    public void setEModel(MulticellEModel emodel)
    {
        this.model = emodel;
    }

    public void setCellDefinition(CellDefinitionProperties cellDefinition)
    {
        this.cellDefinition = cellDefinition;
    }

    @PropertyName ( "Signal" )
    public String getSignal()
    {
        return signal;
    }
    public void setSignal(String signal)
    {
        this.signal = signal;
    }

    @PropertyName ( "Direction" )
    public String getDirection()
    {
        return direction;
    }
    public void setDirection(String direction)
    {
        this.direction = direction;
    }

    @PropertyName ( "Behavior" )
    public String getBehavior()
    {
        return behavior;
    }
    public void setBehavior(String behavior)
    {
        this.behavior = behavior;
    }

    @PropertyName ( "Base value" )
    public double getBaseValue()
    {
        return baseValue;
    }
    public void setBaseValue(double baseValue)
    {
        this.baseValue = baseValue;
    }

    @PropertyName ( "Saturation value" )
    public double getSaturationValue()
    {
        return saturationValue;
    }
    public void setSaturationValue(double sturationValue)
    {
        this.saturationValue = sturationValue;
    }

    @PropertyName ( "Half-max" )
    public double getHalfMax()
    {
        return halfMax;
    }
    public void setHalfMax(double halfMax)
    {
        this.halfMax = halfMax;
    }

    @PropertyName ( "Hill power" )
    public double getHillPower()
    {
        return hillPower;
    }
    public void setHillPower(double hillPower)
    {
        this.hillPower = hillPower;
    }

    @PropertyName ( "Apply to dead" )
    public boolean isApplyToDead()
    {
        return applyToDead;
    }
    public void setApplyToDead(boolean applyToDead)
    {
        this.applyToDead = applyToDead;
    }

    @Override
    public RuleProperties clone()
    {
        RuleProperties rule = new RuleProperties();
        rule.setHalfMax( halfMax );
        rule.setDirection( direction );
        rule.setHillPower( hillPower );
        rule.setSignal( signal );
        rule.setBaseValue( baseValue );
        rule.setBehavior( behavior );
        rule.setApplyToDead( applyToDead );
        rule.setSaturationValue( saturationValue );
        return rule;
    }

    @Override
    public String toString()
    {
        return signal + " " + direction + " " + behavior;
    }
}