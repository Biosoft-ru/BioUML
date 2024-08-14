package biouml.plugins.sbml._test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;

import ru.biosoft.util.Maps;
import ru.biosoft.util.Util;
import biouml.plugins.sbml._test.TestStatistics.ResultComparison.Statistics;
import biouml.plugins.sbml._test.TestStatistics.ResultRelativeErrors.RelativeErrors;
import biouml.standard.simulation.MathUtils;
import biouml.standard.simulation.SimulationResult;

public class TestStatistics
{
    private static int NO_DIFFERENCE = 0;
    private static int SMALL_DIFFERENCE = 1;
    private static int SIGNIFICANT_DIFFERENCE = 2;

    private double zero = 0.0;

    public static class TestResult
    {
        private final Map<String, Integer> names;
        private final List<double[]> values;
        private final String name;

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

    static final double TIME_EPSILON = 1e-6;

    private final List<TestResult> results = new ArrayList<>();
    private double[] controlPoints;

    public void addResult(String name, List<String> variableNames, List<double[]> variableValues) throws Exception
    {
        if( variableValues.size() == 0 )
        {
            throw new Exception("Zero sized values array");
        }

        // interpolate result in control points
        double[] times = null;
        for( int i = 0; i < variableNames.size(); i++ )
        {
            if( "time".equals(variableNames.get(i)) )
            {
                // interpolate result in control points
                times = new double[variableValues.size()];
                for( int j = 0, s = variableValues.size(); j < s; j++ )
                    times[j] = variableValues.get(j)[i];
                break;
            }
        }

        if( times == null )
        {
            throw new Exception("No 'time' variable found");
        }

        List<double[]> interpolatedValues = new ArrayList<>(controlPoints.length);
        for( double controlPoint : controlPoints )
        {
            interpolatedValues.add(new double[variableNames.size() - 1]);
        }

        Map<String, Integer> varIndex = new HashMap<>();
        int[] indexes = MathUtils.multiBinarySearch(times, controlPoints);
        for( int i = 0, count = 0; i < variableNames.size(); i++ )
        {
            if( !"time".equals(variableNames.get(i)) )
            {
                varIndex.put(variableNames.get(i), count);

                double[] values = new double[variableValues.size()];
                for( int j = 0, s = variableValues.size(); j < s; j++ )
                    values[j] = variableValues.get(j)[i];

                double[] interpolated = MathUtils.interpolateLinear(times, values, controlPoints, indexes);

                for( int j = 0; j < interpolated.length; j++ )
                    interpolatedValues.get(j)[count] = interpolated[j];

                count++;
            }
        }

        results.add(new TestResult(name, varIndex, interpolatedValues));
    }

    public int getValuesCount()
    {
        return controlPoints.length;
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

    public List<double[]> getAverages()
    {
        int n = getValuesCount();
        int m = getVariablesCount();
        List<double[]> averages = new ArrayList<>(n);
        List<int[]> resultsPerVar = new ArrayList<>(n);

        for( int i = 0; i < n; i++ )
        {
            double[] av = new double[m];
            Arrays.fill(av, 0.);
            averages.add(av);

            int[] rpv = new int[m];
            Arrays.fill(rpv, 0);
            resultsPerVar.add(rpv);
        }

        Map<String, Integer> globalIndexMap = getVariableNames();
        Set<Map.Entry<String, Integer>> globalIndexMapEntrySet = globalIndexMap.entrySet();

        for( TestResult testResult : results )
        {
            List<double[]> variableValues = testResult.getValues();
            Map<String, Integer> varIndex = testResult.getNames();
            for( Map.Entry<String, Integer> entry : globalIndexMapEntrySet )
            {
                Integer index = varIndex.get(entry.getKey());
                if( index == null )
                    continue;

                Integer gIndex = entry.getValue();
                for( int i = 0; i < n; i++ )
                {
                    double[] varValues = variableValues.get(i);
                    double[] av = averages.get(i);
                    av[gIndex] += varValues[index];
                    int[] rpv = resultsPerVar.get(i);
                    rpv[gIndex]++;
                }
            }
        }

        for( int i = 0; i < n; i++ )
        {
            double[] av = averages.get(i);
            int[] rpv = resultsPerVar.get(i);
            for( int j = 0; j < m; j++ )
            {
                av[j] = av[j] / rpv[j];
            }
        }

        return averages;
    }

    public List<double[]> getMedianes()
    {
        int n = getValuesCount();
        int m = getVariablesCount();
        List<double[]> medianes = new ArrayList<>(n);
        List<List<Double>[]> values = new ArrayList<>(n);

        for( int i = 0; i < n; i++ )
        {
            double[] av = new double[m];
            Arrays.fill(av, 0.);
            medianes.add(av);

            List<Double>[] v = new List[m];
            for( int j = 0; j < m; j++ )
            {
                v[j] = new ArrayList<>();
            }
            values.add(v);
        }

        Map<String, Integer> globalIndexMap = getVariableNames();
        Set<Map.Entry<String, Integer>> globalIndexMapEntrySet = globalIndexMap.entrySet();

        for( TestResult testResult : results )
        {
            List<double[]> variableValues = testResult.getValues();
            Map<String, Integer> varIndex = testResult.getNames();
            for( Map.Entry<String, Integer> entry : globalIndexMapEntrySet )
            {
                Integer index = varIndex.get(entry.getKey());
                if( index == null )
                    continue;

                Integer gIndex = entry.getValue();
                for( int i = 0; i < n; i++ )
                {
                    double[] varValues = variableValues.get(i);
                    List<Double>[] av = values.get(i);
                    av[gIndex].add(varValues[index]);
                }
            }
        }

        for( int i = 0; i < n; i++ )
        {
            double[] av = medianes.get(i);
            List<Double>[] rpv = values.get(i);
            for( int j = 0; j < m; j++ )
            {
                av[j] = DoubleStreamEx.of( rpv[j] ).collect( Util.median() ).orElse( Double.NaN );
            }
        }

        return medianes;
    }

    public List<double[]> getMin()
    {
        int n = getValuesCount();
        int m = getVariablesCount();

        List<double[]> min = new ArrayList<>(n);
        for( int i = 0; i < n; i++ )
        {
            double[] mm = new double[m];
            Arrays.fill(mm, Double.MAX_VALUE);
            min.add(mm);
        }

        Map<String, Integer> globalIndexMap = getVariableNames();
        Set<Map.Entry<String, Integer>> globalIndexMapEntrySet = globalIndexMap.entrySet();

        for( TestResult testResult : results )
        {
            List<double[]> variableValues = testResult.getValues();
            Map<String, Integer> varIndex = testResult.getNames();
            for( Map.Entry<String, Integer> entry : globalIndexMapEntrySet )
            {
                Integer index = varIndex.get(entry.getKey());
                if( index == null )
                    continue;

                Integer gIndex = entry.getValue();
                for( int i = 0; i < n; i++ )
                {
                    double[] varValues = variableValues.get(i);
                    double[] mm = min.get(i);
                    if( mm[gIndex] > varValues[index] )
                        mm[gIndex] = varValues[index];
                }
            }
        }

        return min;
    }

    public List<double[]> getMax()
    {
        int n = getValuesCount();
        int m = getVariablesCount();

        List<double[]> max = new ArrayList<>(n);
        for( int i = 0; i < n; i++ )
        {
            double[] mm = new double[m];
            Arrays.fill(mm, -Double.MAX_VALUE);
            max.add(mm);
        }

        Map<String, Integer> globalIndexMap = getVariableNames();
        Set<Map.Entry<String, Integer>> globalIndexMapEntrySet = globalIndexMap.entrySet();

        for( TestResult testResult : results )
        {
            List<double[]> variableValues = testResult.getValues();
            Map<String, Integer> varIndex = testResult.getNames();
            for( Map.Entry<String, Integer> entry : globalIndexMapEntrySet )
            {
                Integer index = varIndex.get(entry.getKey());
                if( index == null )
                    continue;

                Integer gIndex = entry.getValue();
                for( int i = 0; i < n; i++ )
                {
                    double[] varValues = variableValues.get(i);
                    double[] mm = max.get(i);
                    if( mm[gIndex] < varValues[index] )
                        mm[gIndex] = varValues[index];
                }
            }
        }

        return max;
    }

    public double[] getControlPoints()
    {
        return controlPoints;
    }

    public static class ResultComparison
    {
        public static class Statistics
        {
            public double time;
            public double mean;
            public double relativeError;
            public Map<String, Double> values = new HashMap<>();
            public double bioumlValue;
            public double min;
            public double max;
        }

        private final Map<String, List<Statistics>> statistics = new HashMap<>();
        private double maxRelativeError;

        public void addStatistics(String var, Statistics s)
        {
            List<Statistics> st = statistics.get(var);
            if( st == null )
            {
                st = new ArrayList<>();
                st.add(s);
                statistics.put(var, st);
            }
            else
            {
                st.add(s);
            }
        }

        public Map<String, List<Statistics>> getStatistics()
        {
            return statistics;
        }

        public int getDimension()
        {
            if( statistics.size() > 0 )
            {
                List<Statistics> ls = statistics.values().iterator().next();
                return ls.size();
            }
            return 0;
        }

        public Map<String, Statistics> getRow(int i)
        {
            return Maps.transformValues( statistics, val -> val.get( i ) );
        }

        public void setMaxRelativeError(double maxRelativeError)
        {
            this.maxRelativeError = maxRelativeError;
        }

        public double getMaxRelativeError()
        {
            return maxRelativeError;
        }
    }

    public static class ResultRelativeErrors
    {
        public static class Values
        {
            public double value = 0.0;
            public double relativeError = 0.0;
            public double relativeMedianesError = 0.0;

            public Values(Double value)
            {
                this.value = value;
            }
        }

        public static class RelativeErrors
        {
            public double time;
            public Hashtable<String, Values> relativeErrors;

            public RelativeErrors()
            {
                this.relativeErrors = new Hashtable<>();
            }
        }

        private final Map<String, List<RelativeErrors>> relativeErrors = new HashMap<>();
        private final Map<String, Double> maxRelativeErrorsByVariables = new HashMap<>();
        private final Map<String, Double> maxRelativeErrorsBySolvers = new HashMap<>();

        private final Map<String, Double> maxRelativeMedianesErrorsBySolvers = new HashMap<>();

        public void addRelativeErrors(String var, RelativeErrors re)
        {
            List<RelativeErrors> st = relativeErrors.get(var);
            if( st == null )
            {
                st = new ArrayList<>();
                st.add(re);
                relativeErrors.put(var, st);
            }
            else
            {
                st.add(re);
            }
        }

        public Map<String, List<RelativeErrors>> getRelativeErrors()
        {
            return relativeErrors;
        }

        public int getDimension()
        {
            if( relativeErrors.size() > 0 )
            {
                List<RelativeErrors> ls = relativeErrors.values().iterator().next();
                return ls.size();
            }
            return 0;
        }

        public Map<String, RelativeErrors> getRow(int i)
        {
            return Maps.transformValues( relativeErrors, val -> val.get( i ) );
        }

        public void generateMaxValues()
        {
            for( Map.Entry<String, List<RelativeErrors>> entry : relativeErrors.entrySet() )
            {
                double max = StreamEx.of( entry.getValue() ).flatCollection( errors -> errors.relativeErrors.values() )
                        .mapToDouble( val -> val.relativeError ).max().orElse( -Double.MAX_VALUE );
                maxRelativeErrorsByVariables.put(entry.getKey(), max);
            }

            for( List<RelativeErrors> errorsList : relativeErrors.values() )
            {
                for( RelativeErrors errors : errorsList )
                {
                    for( Map.Entry<String, Values> entry : errors.relativeErrors.entrySet() )
                    {
                        String solverName = entry.getKey();
                        Values value = entry.getValue();
                        Double maxValue = maxRelativeErrorsBySolvers.get(solverName);
                        if( maxValue == null )
                        {
                            maxRelativeErrorsBySolvers.put(solverName, value.relativeError);
                        }
                        else if( maxValue.doubleValue() < value.relativeError )
                        {
                            maxRelativeErrorsBySolvers.put(solverName, value.relativeError);
                        }

                        Double avValue = maxRelativeMedianesErrorsBySolvers.get(solverName);
                        if( maxValue == null )
                        {
                            maxRelativeMedianesErrorsBySolvers.put(solverName, value.relativeMedianesError);
                        }
                        else if( avValue.doubleValue() < value.relativeMedianesError )
                        {
                            maxRelativeMedianesErrorsBySolvers.put(solverName, value.relativeMedianesError);
                        }
                    }
                }
            }
        }

        public Map<String, Double> getMaxByVariables()
        {
            return maxRelativeErrorsByVariables;
        }

        public Map<String, Double> getMaxBySolvers()
        {
            return maxRelativeErrorsBySolvers;
        }
        public Map<String, Double> getMaxByMedianesBySolvers()
        {
            return maxRelativeMedianesErrorsBySolvers;
        }
    }


    public ResultComparison getComparison(SimulationResult simulationResult, TestLogger logger) throws Exception
    {
        ResultComparison resultComparison = new ResultComparison();
        setVariableNames(simulationResult.getVariableMap());

        List<double[]> averages = getMedianes();//getAverages();
        List<double[]> mins = getMin();
        List<double[]> maxs = getMax();

        Map<String, Integer> varIndex = simulationResult.getVariableMap();

        double[][] calculatedValues = simulationResult.interpolateLinear(controlPoints);
        double[] times = getControlPoints();

        Map<String, Integer> globalIndexMap = varIndex;
        Set<Map.Entry<String, Integer>> globalIndexMapEntrySet = globalIndexMap.entrySet();

        double maxRelativeError = -Double.MAX_VALUE;

        for( int i = 0; i < times.length; i++ )
        {
            double[] av = averages.get(i);
            double[] min = mins.get(i);
            double[] max = maxs.get(i);

            for( Map.Entry<String, Integer> entry : globalIndexMapEntrySet )
            {
                String var = entry.getKey();
                Integer gIndex = entry.getValue();
                if( gIndex == null )
                    continue;

                double val = calculatedValues[i][gIndex];

                ResultComparison.Statistics s = new ResultComparison.Statistics();
                s.bioumlValue = val;
                s.mean = av[gIndex];
                s.min = min[gIndex];
                s.max = max[gIndex];

                int resultType = getResultType(s);
                if( resultType == NO_DIFFERENCE )
                {
                    s.relativeError = 0.;
                }
                else
                {
                    if( Math.abs(min[gIndex]) < zero && Math.abs(max[gIndex]) < zero )
                    {
                        s.relativeError = 100.0;
                    }
                    else if( Math.abs(max[gIndex]) < zero )
                    {
                        s.relativeError = ( Math.abs( ( val - min[gIndex] ) / min[gIndex]) ) * 100.0;
                    }
                    else if( Math.abs(min[gIndex]) < zero )
                    {
                        s.relativeError = ( Math.abs( ( max[gIndex] - val ) / max[gIndex]) ) * 100.0;
                    }
                    else
                    {
                        s.relativeError = Math.min(Math.abs( ( min[gIndex] - val ) / min[gIndex]), Math.abs( ( val - max[gIndex] )
                                / max[gIndex])) * 100.0;
                    }
                }

                s.time = times[i];

                for( TestResult result : results )
                {
                    List<double[]> values = result.getValues();
                    Map<String, Integer> indexMap = result.getNames();
                    Integer vIndex = indexMap.get(var);
                    if( vIndex == null )
                        continue;

                    s.values.put(result.getName(), values.get(i)[vIndex]);
                }

                resultComparison.addStatistics(var, s);
            }
        }

        Set<String> vars = globalIndexMap.keySet();
        //HACK: try to hide bad points of function
        for( String var : vars )
        {
            List<Statistics> column = resultComparison.getStatistics().get(var);
            for( int i = 1; i < column.size() - 1; i++ )
            {
                Statistics cell = column.get(i);
                if( cell.relativeError != 0.0 )
                {
                    if( ( column.get(i - 1).relativeError == 0.0 ) && ( column.get(i + 1).relativeError == 0.0 ) )
                    {
                        double p1 = column.get(i).bioumlValue - column.get(i - 1).bioumlValue;
                        double p2 = column.get(i + 1).bioumlValue - column.get(i).bioumlValue;
                        if( ( p1 == 0.0 && p2 != 0.0 ) || ( p1 != 0.0 && p2 == 0.0 )
                                || ( Math.abs(p1 / p2) > 20.0 || Math.abs(p2 / p1) > 20.0 ) )
                        {
                            cell.relativeError = 0.0;
                        }
                    }
                }
            }
        }

        //find max relative error
        for( String var : vars )
        {
            List<Statistics> column = resultComparison.getStatistics().get(var);
            for( int i = 1; i < column.size() - 1; i++ )
            {
                if( column.get(i).relativeError > maxRelativeError )
                    maxRelativeError = column.get(i).relativeError;
            }
        }

        resultComparison.setMaxRelativeError(maxRelativeError);
        return resultComparison;
    }

    public ResultRelativeErrors getRelativeErrors(SimulationResult simulationResult, TestLogger logger) throws Exception
    {
        ResultRelativeErrors resultComparison = new ResultRelativeErrors();

        Map<String, Integer> varIndex;
        if( simulationResult != null )
        {
            varIndex = simulationResult.getVariableMap();
        }
        else
        {
            if( results.size() > 0 )
            {
                varIndex = results.get(0).getNames();
            }
            else
            {
                return null;
            }
        }

        setVariableNames(varIndex);

        double[][] calculatedValues = null;
        if( simulationResult != null )
        {
            calculatedValues = simulationResult.interpolateLinear(controlPoints);
        }
        double[] times = getControlPoints();

        Map<String, Integer> globalIndexMap = varIndex;
        Set<String> vars = globalIndexMap.keySet();

        for( int i = 0; i < times.length; i++ )
        {
            for( String var : vars )
            {
                Integer index = varIndex.get(var);
                if( index == null )
                    continue;

                ResultRelativeErrors.RelativeErrors re = new ResultRelativeErrors.RelativeErrors();

                if( calculatedValues != null )
                {
                    double val = calculatedValues[i][index];
                    re.relativeErrors.put("BioUML", new ResultRelativeErrors.Values(val));
                }

                re.time = times[i];

                for( TestResult result : results )
                {
                    List<double[]> values = result.getValues();
                    Map<String, Integer> indexMap = result.getNames();
                    Integer vIndex = indexMap.get(var);
                    if( vIndex == null )
                        continue;

                    re.relativeErrors.put(result.getName(), new ResultRelativeErrors.Values(values.get(i)[vIndex]));
                }
                calculateRelativeErrors(re);

                resultComparison.addRelativeErrors(var, re);
            }
        }

        //HACK: try to hide bad points of function
        for( String var : vars )
        {
            List<ResultRelativeErrors.RelativeErrors> column = resultComparison.getRelativeErrors().get(var);
            for( int i = 1; i < column.size() - 1; i++ )
            {
                final RelativeErrors relativeErrors = column.get(i);
                ResultRelativeErrors.RelativeErrors cellGroup = relativeErrors;
                for( Map.Entry<String, ResultRelativeErrors.Values> entry : cellGroup.relativeErrors.entrySet() )
                {
                    String solver = entry.getKey();
                    ResultRelativeErrors.Values cell = entry.getValue();

                    final RelativeErrors relativeErrorsPlus1 = column.get(i + 1);
                    final RelativeErrors relativeErrorsMinus1 = column.get(i - 1);
                    if( cell.relativeError != 0.0 )
                    {
                        if( ( relativeErrorsMinus1.relativeErrors.get(solver).relativeError == 0.0 )
                                && ( relativeErrorsPlus1.relativeErrors.get(solver).relativeError == 0.0 ) )
                        {
                            double p1 = relativeErrors.relativeErrors.get(solver).value - relativeErrorsMinus1.relativeErrors.get(solver).value;
                            double p2 = relativeErrorsPlus1.relativeErrors.get(solver).value - relativeErrors.relativeErrors.get(solver).value;
                            if( ( p1 == 0.0 && p2 != 0.0 ) || ( p1 != 0.0 && p2 == 0.0 )
                                    || ( Math.abs(p1 / p2) > 20.0 || Math.abs(p2 / p1) > 20.0 ) )
                            {
                                cell.relativeError = 0.0;
                            }
                        }
                    }

                    if( cell.relativeMedianesError != 0.0 )
                    {
                        if( ( relativeErrorsMinus1.relativeErrors.get(solver).relativeMedianesError == 0.0 )
                                && ( relativeErrorsPlus1.relativeErrors.get(solver).relativeMedianesError == 0.0 ) )
                        {
                            double p1 = relativeErrors.relativeErrors.get(solver).value - relativeErrorsMinus1.relativeErrors.get(solver).value;
                            double p2 = relativeErrorsPlus1.relativeErrors.get(solver).value - relativeErrors.relativeErrors.get(solver).value;
                            if( ( p1 == 0.0 && p2 != 0.0 ) || ( p1 != 0.0 && p2 == 0.0 )
                                    || ( Math.abs(p1 / p2) > 20.0 || Math.abs(p2 / p1) > 20.0 ) )
                            {
                                cell.relativeMedianesError = 0.0;
                            }
                        }
                    }
                }
            }
        }

        resultComparison.generateMaxValues();

        return resultComparison;
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

    private void calculateRelativeErrors(ResultRelativeErrors.RelativeErrors re)
    {
        for( Map.Entry<String, ResultRelativeErrors.Values> entry : re.relativeErrors.entrySet() )
        {
            String mainName = entry.getKey();
            ResultRelativeErrors.Values mainElement = entry.getValue();
            double val = mainElement.value;
            double[] sortedArray = new double[re.relativeErrors.keySet().size()];
            int currentSize = 0;
            for( Map.Entry<String, ResultRelativeErrors.Values> entry2 : re.relativeErrors.entrySet() )
            {
                String name = entry2.getKey();
                if( !name.equals(mainName) )
                {
                    int currentPos = currentSize;
                    double value = entry2.getValue().value;
                    for( int i = 0; i < currentSize; i++ )
                    {
                        if( value < sortedArray[i] )
                        {
                            currentPos = i;
                            break;
                        }
                    }
                    for( int i = currentSize; i > currentPos; i-- )
                    {
                        sortedArray[i] = sortedArray[i - 1];
                    }
                    sortedArray[currentPos] = value;
                    currentSize++;
                }
            }
            double min;
            double max;
            double av;
            if( currentSize == 0 )
            {
                min = val;
                max = val;
                av = val;
            }
            else
            {
                min = sortedArray[0];
                max = sortedArray[currentSize - 1];
                av = sortedArray[currentSize / 2];
            }

            int resultType = getResultType(val, min, max);
            double relativeError = 0.;
            if( resultType != NO_DIFFERENCE )
            {
                if( Math.abs(min) < zero && Math.abs(max) < zero )
                {
                    relativeError = 100.0;
                }
                else if( Math.abs(max) < zero )
                {
                    relativeError = ( Math.abs( ( val - min ) / min) ) * 100.0;
                }
                else if( Math.abs(min) < zero )
                {
                    relativeError = ( Math.abs( ( max - val ) / max) ) * 100.0;
                }
                else
                {
                    relativeError = Math.min(Math.abs( ( min - val ) / min), Math.abs( ( val - max ) / max)) * 100.0;
                }
            }
            mainElement.relativeError = relativeError;

            resultType = getResultType(val, av);
            double relativeMedianesError = 0.;
            if( resultType != NO_DIFFERENCE )
            {
                if( Math.abs(av) < zero )
                {
                    if( Math.abs(av) < zero )
                    {
                        relativeMedianesError = 0.0;
                    }
                    else
                    {
                        relativeMedianesError = 100.0;
                    }
                }
                else
                {
                    relativeMedianesError = Math.abs( ( val - av ) / av) * 100.0;
                }
            }
            mainElement.relativeMedianesError = relativeMedianesError;
        }
    }
    public void setControlPoints(double[] controlPoints)
    {
        this.controlPoints = controlPoints;
    }

    public int getResultType(Statistics s)
    {
        return getResultType(s.bioumlValue, s.min, s.max);
    }

    public int getResultType(double value, double min, double max)
    {
        if( ( ( Math.abs(min) <= zero || Math.abs(max) <= zero ) && Math.abs(value) <= zero )
                || ( ( value >= ( min * 0.999 ) ) && ( value <= max * 1.001 ) ) )
        {
            return NO_DIFFERENCE;
        }
        else if( ( value >= min * 0.5 ) && ( value <= max * 1.5 ) )
        {
            return SMALL_DIFFERENCE;
        }
        return SIGNIFICANT_DIFFERENCE;
    }

    public int getResultType(double value, double av)
    {
        if( av == 0.0 )
        {
            if( value > -zero && value < zero )
            {
                return NO_DIFFERENCE;
            }
            else
            {
                return SIGNIFICANT_DIFFERENCE;
            }
        }

        double deflection = Math.abs( ( value - av ) / av);
        if( deflection < 0.01 )
        {
            return NO_DIFFERENCE;
        }
        else if( deflection < 0.5 )
        {
            return SMALL_DIFFERENCE;
        }
        return SIGNIFICANT_DIFFERENCE;
    }

    public void setZero(double zero)
    {
        this.zero = zero;
    }
}
