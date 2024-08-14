package biouml.plugins.simulation_test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.util.Maps;
import biouml.model.dynamics.EModel;
import biouml.plugins.simulation.OdeSimulatorOptions;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.DormandPrince;
import biouml.plugins.simulation.ode.ImexSD;
import biouml.standard.simulation.SimulationResult;

abstract public class SimulatorTest
{
    public static final double DEFAULT_INITIAL_STEP = 0.0;

    protected SimulationResult simulationResult;

    protected double zero;
    protected double atol;
    protected double rtol;
    protected double initialStep = DEFAULT_INITIAL_STEP;
    protected int modelType;
    protected String statisticsMode = OdeSimulatorOptions.STATISTICS_INTERMEDIATE;
    protected SimulationEngine engine;

    /**
     * Moved from SemanticSImulatorTest
     * Axec:
     * Normally specie quantity type is defined by field initialAmount or initialConcentration<br>
     * or if non of them is defined by flag hasOnlySubstanceUnits (in BioUML).<br>
     * However in some tests for some species intitialAmount and initialConcentration are not defined<br>
     * And output type is defined NOT by .xml file but -settings.txt file.<br>
     * For example see <b>157 SBML test</b>
     * Next two arrays defines in which units species should be output in current test in that case<br>
     * 
     * @Pendingin BioUML we actually presume that output quantity type is the same as initial quantity type<br>
     * So we need to set initialConcentration or Amount in that case. May be it is reasonable to add new outputQuantityType<b>
     */
    //List of species that should be outputted in amount
    protected List<String> outputInAmount;
    //list of species that should be outputted in concentration
    protected List<String> outputInConcentration;

    protected abstract void executeTest(SimulationEngine engine, TestLogger logger, String testName);

    protected void prepareSimulatorOptions(SimulationEngine engine)
    {
        if( engine instanceof JavaSimulationEngine )
        {
            JavaSimulationEngine javaEngine = (JavaSimulationEngine)engine;
            SimulatorSupport simulatorSupport = (SimulatorSupport)javaEngine.getSolver();
            String solverName = engine.getSolverName();
            System.out.println("Solver name = " + solverName);
            double iStep = ( initialStep == DEFAULT_INITIAL_STEP ) ? engine.getTimeIncrement() : initialStep;
            if( "DormandPrince".equals(solverName) )
            {
                simulatorSupport.setOptions(new DormandPrince.DPOptions(iStep, true, EModel.isOfType(modelType, EModel.EVENT_TYPE),
                        statisticsMode));
            }
            else if( "Imex".equals(solverName) )
            {
                simulatorSupport.setOptions(new ImexSD.ImexOptions(iStep, EModel.isOfType(modelType, EModel.EVENT_TYPE), statisticsMode));

            }
        }
    }

    /////////////////////////
    //utility methods
    /////////////////////////

    public static String getMangledName(String name)
    {
        String prefix = "";
        if (name.contains( "/" ))
        {
            int lastIndex = name.lastIndexOf( "/" )+1;
            prefix = name.substring( 0, lastIndex ).replaceAll( "/", "__" ); 
            name = name.substring( lastIndex );
        }
            
        if (name.startsWith("$$rate_"))
            name = name.substring(7, name.length());
        
        char[] fullName = name.toCharArray();
        
        int len = 0;
        for( int i = fullName.length - 1; i >= 0; i-- )
        {
            if( fullName[i] == '.' || fullName[i] == '$' )
                break;

            len++;
        }

        char[] shortName = new char[len];
        for( int i = fullName.length - 1, j = len - 1; i >= 0; i-- )
        {
            if( fullName[i] == '.' || fullName[i] == '$' )
                break;

            shortName[j--] = fullName[i];
        }        
        return prefix + new String(shortName).replaceAll("\"", "");
    }

    public static Map<String, Integer> getMangledNamesMap(Map<String, Integer> variableMap)
    {
        if( variableMap == null )
            return null;

        return Maps.transformKeys( variableMap, SimulatorTest::getMangledName );
    }

    public SimulationResult prepareValues(String[] requiredVariables, Map mangledMap, TestLogger logger)
    {
        SimulationResult newSimulationResult = new SimulationResult(simulationResult.getOrigin(), simulationResult.getName());
        boolean createInterpolation = modelType != EModel.STATIC_TYPE && modelType != EModel.STATIC_EQUATIONS_TYPE;
        SimulationResult resultToBeUsed = simulationResult;

        if( createInterpolation )
        {
            newSimulationResult = simulationResult.approximate(engine.getInitialTime(), engine.getCompletionTime(), engine
                    .getTimeIncrement());
            resultToBeUsed = newSimulationResult;
        }
        if( resultToBeUsed == null )
            return null;

        double oldValues[][] = resultToBeUsed.getValues();

        int n = (int) ( ( engine.getCompletionTime() - engine.getInitialTime() ) / engine.getTimeIncrement() );
        if( engine.getInitialTime() + n * engine.getTimeIncrement() < engine.getCompletionTime() )
            n += 2;
        else
            n++;

        if( oldValues != null && modelType != EModel.STATIC_EQUATIONS_TYPE )
            n = oldValues.length;

        boolean containsTime = false;
        for( String requiredVariable : requiredVariables )
        {
            if( "time".equals(requiredVariable) || "Time".equals(requiredVariable))
            {
                containsTime = true;
                break;
            }
        }
        double[] newTimes = new double[n];
        double[][] newValues = new double[n][requiredVariables.length - ( containsTime ? 1 : 0 )];
        Map<String, Integer> newVariableMap = new LinkedHashMap<>();

        int counter = 0;
        for( String requiredVariable : requiredVariables )
        {
            if( requiredVariable.equals("time") || requiredVariable.equals("Time") )
                continue;

            if( mangledMap.containsKey(requiredVariable) )
            {
                // fill it in with regular simulated values
                int index = ( (Integer)mangledMap.get(requiredVariable) ).intValue();

                for( int j = 0; j < n; j++ )
                    newValues[j][counter] = oldValues[j][index];
            }
            else
            {
                logger.warn("Variable '" + requiredVariable
                        + "' is not contained in simulation result: will be substituted with initial value");
                // fill it in with initial values
                double value = getVarInitialValue(requiredVariable);
                for( int j = 0; j < n; j++ )
                    newValues[j][counter] = value;
            }

            newVariableMap.put(requiredVariable, counter);
            counter++;
        }

        for( int j = 0; j < n; j++ )
            newTimes[j] = engine.getInitialTime() + j * engine.getTimeIncrement();

        newSimulationResult.setTimes(newTimes);
        newSimulationResult.setValues(newValues);
        newSimulationResult.setVariableMap(newVariableMap);

        return newSimulationResult;
    }
    protected double getVarInitialValue(String varName)
    {
        return engine.getExecutableModel().getVariables().stream().filter( v -> getMangledName( v.getName() ).equals( varName ) ).findAny()
                .map(v -> v.getInitialValue()).orElse(0.0);
    }
}
