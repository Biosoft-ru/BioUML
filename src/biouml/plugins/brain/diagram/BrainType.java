package biouml.plugins.brain.diagram;

public class BrainType
{
	// diagram element types
    public static final String TYPE_CONNECTIVITY_MATRIX = "connectivityMatrix";
    public static final String TYPE_DELAY_MATRIX = "delayMatrix";
    public static final String TYPE_REGIONAL_MODEL = "regionalModel";
    public static final String TYPE_CELLULAR_MODEL = "cellularModel";
    public static final String TYPE_RECEPTOR_MODEL = "receptorModel";
    
    // regional epilepsy model types (describes region dynamics)
    public static final String TYPE_REGIONAL_ROSSLER = "regionalRossler";
    public static final String TYPE_REGIONAL_EPILEPTOR = "regionalEpileptor";
    
    // cellular epilepsy model types (describes cellular dynamics)
    public static final String TYPE_CELLULAR_EPILEPTOR2 = "cellularEpileptor2";
    public static final String TYPE_CELLULAR_OXYGEN = "cellularOxygen";
    public static final String TYPE_CELLULAR_MINIMAL = "cellularMinimal";
    public static final String TYPE_CELLULAR_EPILEPTOR2_WITH_OXYGEN = "cellularEpileptor2Oxygen"; // Epileptor-2 cellular model with oxygen dynamic
    
    // receptor model types (describes receptors dynamics)
    public static final String TYPE_RECEPTOR_AMPA = "receptorAMPA";
}
