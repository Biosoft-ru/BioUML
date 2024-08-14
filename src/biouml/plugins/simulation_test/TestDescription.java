package biouml.plugins.simulation_test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import biouml.standard.simulation.SimulationResult;

public class TestDescription
{
    private double zero = 0.0;
    private double atol = 0.0;
    private double rtol = 0.0;
    private double step = 0.0;
    private String sbmlLevel = null;
    private String solverName = null;

    public TestDescription()
    {
        
    }
    
    public TestDescription(String sbmlLevel, String solverName)
    {
        setSolverName(solverName);
        setSbmlLevel(sbmlLevel);
    }
    
    public static class TestResult
    {
        private Map<String, Integer> names;
        private List<double[]> values;
        private String name;

        public TestResult(String name, Map<String, Integer> varIndex, List<double[]> values) throws Exception
        {
            this.name = name;
            this.names = varIndex;
            this.values = values;
        }

        public Map<String, Integer> getNames()
        {
            return names;
        }

        public List<double[]> getValues()
        {
            return values;
        }

        public int getValuesCount()
        {
            return values.size();
        }

        public int getVariablesCount()
        {
            return names.size();
        }

        public String getName()
        {
            return name;
        }
    }

    public int getVariablesCount()
    {
        return getVariableNames().size();
    }

    Map<String, Integer> variableNames = null;
    public Map<String, Integer> getVariableNames()
    {
        return variableNames;
    }
    public void setVariableNames(Map<String, Integer> variableNames)
    {
        this.variableNames = variableNames;
    }

    public List<double[]> getInterpolatedValues(SimulationResult simulationResult, double[] controlPoints, List<String> variables)
            throws Exception
    {
        setVariableNames(simulationResult.getVariableMap());

        Map<String, Integer> varIndex = simulationResult.getVariableMap();

        double[][] calculatedValues = simulationResult.interpolateLinear(controlPoints);

        List<double[]> result = new ArrayList<>();
        for( int i = 0; i < controlPoints.length; i++ )
        {
            result.add(new double[variables.size()]);
            for( int j = 0; j < variables.size(); j++ )
            {
                Integer index = varIndex.get(variables.get(j));
                if( index == null )
                    continue;

                double val = calculatedValues[i][index];
                result.get(i)[j] = val;
            }
        }
        return result;
    }

    public void setZero(double zero)
    {
        this.zero = zero;
    }

    public double getZero()
    {
        return zero;
    }

    public double getAtol()
    {
        return atol;
    }

    public void setAtol(double atol)
    {
        this.atol = atol;
    }

    public double getRtol()
    {
        return rtol;
    }

    public void setRtol(double rtol)
    {
        this.rtol = rtol;
    }

    public double getStep()
    {
        return step;
    }

    public void setStep(double step)
    {
        this.step = step;
    }
    public String getSbmlLevel()
    {
        return sbmlLevel;
    }
    public void setSbmlLevel(String sbmlLevel)
    {
        this.sbmlLevel = sbmlLevel;
    }
    public String getSolverName()
    {
        return solverName;
    }
    public void setSolverName(String solverName)
    {
        this.solverName = solverName;
    }
}
