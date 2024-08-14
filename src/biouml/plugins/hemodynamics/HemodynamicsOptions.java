package biouml.plugins.hemodynamics;

import java.util.HashMap;
import java.util.Map;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.simulation.Options;

public class HemodynamicsOptions extends Options
{

    public static final int FLUX_INITIAL_CONDITION = 0;
    public static final int PRESSURE_INITIAL_CONDITION = 1;
    public static final int FILTRATION_INITIAL_CONDITION = 2;
    
    public static final String FLUX_INITIAL_CONDITION_STRING = "Flow";
    public static final String AREA_INITIAL_CONDITION_STRING = "Area";
    public static final String PRESSURE_INITIAL_CONDITION_STRING = "Pressure";
    public static final String FILTRATION_INITIAL_CONDITION_STRING = "Filtration";
    
    private static Map<String, Integer> conditionToCode = new HashMap<String, Integer>()
    {
        {
            put(FLUX_INITIAL_CONDITION_STRING, FLUX_INITIAL_CONDITION);
            put(PRESSURE_INITIAL_CONDITION_STRING, PRESSURE_INITIAL_CONDITION);
            put(FILTRATION_INITIAL_CONDITION_STRING, FILTRATION_INITIAL_CONDITION);
            
        }
    };
    
    public int getInputConditionCode()
    {
        return conditionToCode.get(inputCondition );
    }
    
    public int getOutputConditionCode()
    {
        return conditionToCode.get(outputCondition );
    }
    
    public static final String[] getInputConditions()
    {
        return new String[] {FLUX_INITIAL_CONDITION_STRING, PRESSURE_INITIAL_CONDITION_STRING, FILTRATION_INITIAL_CONDITION_STRING};
    }

    public static final String[] getOutputConditions()
    {
        return new String[] {FLUX_INITIAL_CONDITION_STRING, PRESSURE_INITIAL_CONDITION_STRING, FILTRATION_INITIAL_CONDITION_STRING};
    }

    protected String outputCondition = FILTRATION_INITIAL_CONDITION_STRING;
    protected String inputCondition = FLUX_INITIAL_CONDITION_STRING;
    protected boolean fromZero = false;
    protected boolean oldLinearisation = false;
    protected boolean testMode = false;
    private boolean useFullPressureConservation;
    protected boolean modelArteriols;
    
    public boolean isModelArteriols()
    {
        return modelArteriols;
    }

    public void setModelArteriols(boolean modelArteriols)
    {
        boolean oldValue = this.modelArteriols;
        this.modelArteriols = modelArteriols;
        firePropertyChange( "modelArteriols", oldValue, modelArteriols );
    }

    public boolean isTestMode()
    {
        return testMode;
    }
    public void setTestMode(boolean testMode)
    {
        this.testMode = testMode;
    }
    
    @PropertyName ( "Output condition" )
    public String getOutputCondition()
    {
        return outputCondition;
    }
    public void setOutputCondition(String outputCondition)
    {
        String oldValue = this.outputCondition;
        this.outputCondition = outputCondition;
        firePropertyChange( "outputCondition", oldValue, outputCondition );
    }
    
    @PropertyName ( "Input condition" )
    public String getInputCondition()
    {
        return inputCondition;
    }
    public void setInputCondition(String inputCondition)
    {
        String oldValue = this.inputCondition;
        this.inputCondition = inputCondition;
        firePropertyChange( "inputCondition", oldValue, inputCondition );
    }

    public boolean isFromZero()
    {
        return fromZero;
    }

    public void setFromZero(boolean fromZero)
    {
        boolean oldValue = this.fromZero;
        this.fromZero = fromZero;
        firePropertyChange( "setFromZero", oldValue, fromZero );
    }

    public boolean isOldLinearisation()
    {
        return oldLinearisation;
    }

    public void setOldLinearisation(boolean oldLinearisation)
    {
        boolean oldValue = this.oldLinearisation;
        this.oldLinearisation = oldLinearisation;
        firePropertyChange( "oldLinearisation", oldValue, oldLinearisation );
    }

    @PropertyName("Full pressure conservation law")
    @PropertyDescription("If false then pressure conservation law will be used on branching, otherwise - full pressure.")
    public boolean isUseFullPressureConservation()
    {
        return useFullPressureConservation;
    }

    public void setUseFullPressureConservation(boolean useFullPressureConservation)
    {
        boolean oldValue = this.useFullPressureConservation;
        this.useFullPressureConservation = useFullPressureConservation;
        firePropertyChange( "useFullPressureConservation", oldValue, useFullPressureConservation );
    }
}
