package ru.biosoft.analysis.optimization.methods;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.OptimizationMethodParameters;
import ru.biosoft.analysis.optimization.OptimizationMethodParametersBeanInfo;
import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.util.ByteMatrix;
import ru.biosoft.util.DoubleMatrix;
import Jama.Matrix;

/*
* This algorithm was created based on the paper
* Mattias Bjorkman and Kenneth Holmstrom
* "Global Optimization Using the DIRECT Algorithm in Matlab"
* (AMO - Advanced Modeling and Optimization, vol. 1, #2, 1999)
* 
* Notation used in the glbSolve algorithm.
* Appropriate variables in the article are given in the second column.
* 
* C       Matrix with all rectangle center-points.
* D       List with distances from center-point to the vertices.
* F       List with function values.
* I       Set of dimensions with maximum side length for the current rectangle.
* L       Matrix with all rectangle side lengths in each dimension.
* invL    = log3(1/(2L)) (more compact representation of L)
* S       Index set of potentially optimal rectangles.
* T       The number of iteration to be performed.
* f_min   The current minimum function value.
* i_min   Rectangle index.
* e       Global/local search weight parameter.
* delta   New side length in the current divided dimension.
*/

public class GlbSolveOptMethod extends OptimizationMethod<GlbSolveOptMethod.GlbSolveOptMethodParameters>
{
    private static final double[] deltaL = new double[255];
    private static final double[] invSquareL = new double[255];

    static
    {
        double invL = 2;
        for( int i = 0; i < deltaL.length; i++ )
        {
            deltaL[i] = 2.0 / invL;
            invSquareL[i] = 1.0 / ( invL * invL );
            invL *= 3;
        }
    }

    public GlbSolveOptMethod(DataCollection<?> origin, String name)
    {
        super(origin, name, null);
        parameters = new GlbSolveOptMethodParameters();
    }

    @PropertyName("Method parameters")
    @PropertyDescription("Method parameters.")
    public class GlbSolveOptMethodParameters extends OptimizationMethodParameters
    {
        private int numOfIterations;

        public GlbSolveOptMethodParameters()
        {
            this.numOfIterations = 300;
            random = null;
        }

        @PropertyName("Number if iterations")
        @PropertyDescription("The number of iterations after which the optimization process will be stopped.")
        public int getNumOfIterations()
        {
            return this.numOfIterations;
        }

        public void setNumOfIterations(int numOfIterations)
        {
            int oldValue = this.numOfIterations;
            this.numOfIterations = numOfIterations;
            firePropertyChange("numberOfIterations", oldValue, numOfIterations);
        }

        @Override
        public void read(Properties properties, String prefix)
        {
            super.read(properties, prefix);
            try
            {
                numOfIterations = Integer.parseInt(properties.getProperty(prefix + "numOfIterations"));
            }
            catch( Exception e )
            {
            }
        }

        @Override
        public void write(Properties properties, String prefix)
        {
            super.write(properties, prefix);
            properties.put(prefix + "numOfIterations", Integer.toString(numOfIterations));
        }
    }

    public static class GlbSolveOptMethodParametersBeanInfo extends OptimizationMethodParametersBeanInfo
    {
        public GlbSolveOptMethodParametersBeanInfo()
        {
            super(GlbSolveOptMethodParameters.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            add( "numOfIterations" );
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Optimization problem
    //
    private double[] solution;

    @Override
    public void setOptimizationProblem(OptimizationProblem problem)
    {
        this.problem = problem;
        if( problem != null )
        {
            params = problem.getParameters();
            n = params.size();

            solution = new double[n];

            stepsNumber = parameters.getNumOfIterations();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Realization of the method
    //
    private DoubleMatrix C;
    private ByteMatrix invL;

    private TDoubleArrayList F;
    private TDoubleArrayList D;

    private double e;
    private double f_min = Double.POSITIVE_INFINITY;
    private int i_min;
    private int mSize;

    private double penalty = Double.POSITIVE_INFINITY;
    private int[] sortedD;
    private int Dcount;

    @Override
    public double getDeviation()
    {
        return f_min;
    }

    @Override
    public double getPenalty()
    {
        return penalty;
    }

    @Override
    public double[] getIntermediateSolution()
    {
        return solution.clone();
    }

    private static class MinValue
    {
        double minValue;
        int i_min;

        public MinValue(double minValue, int i_min)
        {
            this.minValue = minValue;
            this.i_min = i_min;
        }
    }

    @Override
    public double[] getSolution() throws IOException, Exception
    {
        getInitialData();

        int nThreads = SecurityManager.getMaximumThreadsNumber();
        List<int[]> tasks = new ArrayList<>( nThreads );
        for( int i = 0; i < nThreads; i++ )
            tasks.add(new int[] {0, 0});

        for( int t = 0; t < parameters.getNumOfIterations() && go; ++t )
        {
            for( int j : getS() )
            {
                TIntSet I = getI(j);
                int[] Iarray = I.toArray();
                Arrays.sort(Iarray);

                byte newInvL = (byte) ( invL.get(j, Iarray[0]) + 1 );
                if( newInvL == deltaL.length )
                {
                    log.warning("Max precision reached at iteration#" + t);
                    return solution;
                }

                individualsNumber = 2 * Iarray.length;
                double[][] population = new double[individualsNumber][];
                distances = new double[individualsNumber];
                penalties = new double[individualsNumber];

                int[] m = new int[n];

                for( int ind = 0; ind < Iarray.length; ind++ )
                {
                    int i = Iarray[ind];
                    m[i] = nextStep(i, j, deltaL[newInvL], population, 2 * ind);
                }

                calculateDistances(population);
                refreshSolution(population, distances, penalties);

                double[] w = new double[n];

                for( int ind = 0; ind < Iarray.length; ind++ )
                {
                    int i = Iarray[ind];
                    F.set(m[i], distances[2 * ind]);
                    F.set(m[i] + 1, distances[2 * ind + 1]);
                    w[i] = Math.min(distances[2 * ind], distances[2 * ind + 1]);
                }

                while( !I.isEmpty() )
                {
                    double minW = Double.POSITIVE_INFINITY;
                    int i = -1;

                    TIntIterator it = I.iterator();
                    while( it.hasNext() && go )
                    {
                        int next = it.next();
                        if( w[next] <= minW )
                        {
                            minW = w[next];
                            i = next;
                        }
                    }

                    I.remove(i);
                    invL.set(j, i, newInvL);

                    for( int k = 0; k < n; ++k )
                    {
                        byte invLjk = invL.get(j, k);
                        invL.set(m[i], k, invLjk);
                        invL.set(m[i] + 1, k, invLjk);
                    }

                    double sum = 0;
                    byte[] ls = invL.get(j);
                    Arrays.sort(ls);
                    for( int k = n - 1; k >= 0; k-- )
                        sum += invSquareL[ls[k]];

                    double dist = Math.sqrt(sum);

                    int distPos = Math.abs(searchInD(dist));
                    System.arraycopy(sortedD, distPos, sortedD, distPos + 2, D.size() - distPos - 2);
                    sortedD[distPos] = m[i];
                    sortedD[distPos + 1] = m[i] + 1;
                    Dcount += 2;
                    D.setQuick(m[i], dist);
                    D.setQuick(m[i] + 1, dist);

                    double oldJ = D.getQuick(j);
                    int oldJPos1 = searchInD(oldJ);
                    int oldJPos;
                    int jDelta = 0;
                    while( true )
                    {
                        if( oldJPos1 - jDelta >= 0 && sortedD[oldJPos1 - jDelta] == j )
                        {
                            oldJPos = oldJPos1 - jDelta;
                            break;
                        }
                        if( oldJPos1 + jDelta < Dcount && sortedD[oldJPos1 + jDelta] == j )
                        {
                            oldJPos = oldJPos1 + jDelta;
                            break;
                        }
                        jDelta++;
                    }
                    if( oldJPos < distPos )
                        System.arraycopy(sortedD, oldJPos + 1, sortedD, oldJPos, distPos - oldJPos);
                    else
                        System.arraycopy(sortedD, distPos, sortedD, distPos + 1, oldJPos - distPos);
                    sortedD[distPos] = j;
                    D.setQuick(j, dist);
                }
            }

            final double variableE = Math.max(e * Math.abs(f_min), 1E-8);

            for( int i = 0; i < nThreads; i++ )
            {
                tasks.get(i)[0] = mSize * i / nThreads;
                tasks.get(i)[1] = mSize * ( i + 1 ) / nThreads;
            }
            final List<MinValue> minValues = Collections.synchronizedList(new ArrayList<MinValue>());

            TaskPool.getInstance().iterate(tasks, task -> {
                double minValue = Double.MAX_VALUE;
                int i_min = 0;
                for( int i = task[0]; i < task[1]; ++i )
                {
                    double val = ( F.getQuick(i) - f_min + variableE ) / D.getQuick(i);
                    if( val < minValue )
                    {
                        minValue = val;
                        i_min = i;
                    }
                }
                minValues.add(new MinValue(minValue, i_min));
            });
            double bestMinValue = Double.MAX_VALUE;
            for( MinValue minValue : minValues )
            {
                if( minValue.minValue < bestMinValue )
                {
                    bestMinValue = minValue.minValue;
                    i_min = minValue.i_min;
                }
            }

            displayInfo();
            incPreparedness(t + 1);
            //System.out.printf("%d\t%f\n", t, f_min);
        }
        return solution;
    }

    private int nextStep(int i, int j, double delta, double[][] population, int nextIndivid) throws Exception
    {
        double[] x1 = new double[n];
        double[] x2 = new double[n];

        double[] c1 = new double[n];
        double[] c2 = new double[n];

        for( int k = 0; k < n; ++k )
        {
            double lower = params.get(k).getLowerBound();
            double upper = params.get(k).getUpperBound();

            double Cjk = C.get(j, k);
            c1[k] = Cjk;
            c2[k] = Cjk;

            if( k == i )
            {
                c1[k] += delta;
                c2[k] -= delta;
            }

            x1[k] = lower + ( upper - lower ) * c1[k];
            x2[k] = lower + ( upper - lower ) * c2[k];
        }

        population[nextIndivid] = x1;
        population[nextIndivid + 1] = x2;

        C.add(c1);
        C.add(c2);

        F.add(0.0);
        F.add(0.0);

        D.add(0.0);
        D.add(0.0);

        if( sortedD.length < mSize + 2 )
        {
            int[] newBeforeSortD = new int[sortedD.length * 2];
            System.arraycopy(sortedD, 0, newBeforeSortD, 0, mSize);
            sortedD = newBeforeSortD;
        }

        invL.add();
        invL.add();

        mSize += 2;
        return mSize - 2;
    }

    private void refreshSolution(double[][] population, double[] distances, double[] penalties)
    {
        for( int i = 0; i < population.length; ++i )
        {
            if( f_min > distances[i] )
            {
                f_min = distances[i];
                penalty = penalties[i];
                for( int k = 0; k < n; ++k )
                    solution[k] = population[i][k];
            }
        }
    }

    /*
     * The following function defines alpha and beta by letting the line (y = alpha * x - beta)
     * pass through the points (D_i_min, F_i_min) and
     * (max(D_j, j = 1...N), min{F_i: D_i = max(D_j, j = 1...N), i = 1...N})
     */
    private double[] defineAlphaBeta(double maxD, double minF)
    {
        double result[] = new double[2];

        //The first point.
        double oneAbs = D.get(i_min);
        double oneOrd = F.get(i_min);

        //The second point.
        double twoAbs = maxD;
        double twoOrd = minF;

        result[0] = ( oneOrd - twoOrd ) / ( oneAbs - twoAbs ); //alpha
        result[1] = oneOrd - result[0] * oneAbs; //beta

        return result;
    }

    private void getInitialData() throws Exception
    {
        e = 0.0001;
        mSize = 1;

        C = new DoubleMatrix(n);
        invL = new ByteMatrix(n);

        F = new TDoubleArrayList();
        D = new TDoubleArrayList();

        double[] first = new double[n];
        for( int i = 0; i < n; ++i )
            first[i] = 0.5;

        C.add(first);

        byte[] invLFirst = new byte[n];
        for( int i = 0; i < n; ++i )
            invLFirst[i] = 0;
        invL.add(invLFirst);

        for( int i = 0; i < n; ++i )
            solution[i] = params.get(i).getValue();

        double[] goodness = problem.testGoodnessOfFit(solution, jobControl);
        F.add(goodness[0]);
        penalty = goodness[1];

        D.add(0.5 * Math.sqrt(n));
        sortedD = new int[10];
        Dcount = 1;

        f_min = F.get(0);
        i_min = 0;
    }

    private TIntSet getI(int j)
    {
        TIntSet I = new TIntHashSet();
        byte minInvL = Byte.MAX_VALUE;
        for( int i = 0; i < n; ++i )
            if( invL.get(j, i) < minInvL )
                minInvL = invL.get(j, i);

        for( int i = 0; i < n; ++i )
            if( invL.get(j, i) == minInvL )
                I.add(i);
        return I;
    }

    private int[] getS()
    {
        TIntSet S = new TIntHashSet();
        TIntList S_up_arrow = new TIntArrayList();

        int j = getJ();

        double minF = 0;

        while( j < mSize )
        {
            minF = F.get(sortedD[j]);

            int i = j + 1;
            int newInd = sortedD[j];

            double Dj = D.getQuick(sortedD[j]);
            while( i < mSize )
            {
                int ind = sortedD[i];
                if( Dj != D.getQuick(ind) )
                    break;
                double Fi = F.get(ind);
                if( minF > Fi )
                {
                    minF = Fi;
                    newInd = ind;
                }
                i++;
            }

            S_up_arrow.add(newInd);

            j = i;
        }

        TIntIterator it = S_up_arrow.iterator();

        if( S_up_arrow.size() > 1 )
        {
            double[] alphaBeta = defineAlphaBeta(D.getQuick(sortedD[mSize - 1]), minF);
            double alpha = alphaBeta[0];
            double beta = alphaBeta[1];

            TDoubleList abs = new TDoubleArrayList();
            TDoubleList ord = new TDoubleArrayList();
            TIntList S_wave = new TIntArrayList();

            while( it.hasNext() )
            {
                int k = it.next();

                if( F.get(k) <= alpha * D.get(k) + beta + Math.pow(10, -12) )
                {
                    S_wave.add(k);
                    abs.add(D.get(k));
                    ord.add(F.get(k));
                }
            }

            TIntList conhullInd = conhull(abs, ord);
            for( int i = 0; i < conhullInd.size(); ++i )
                S.add(S_wave.get(conhullInd.get(i)));
        }
        else if( it.hasNext() )
        {
            S.add(it.next());
        }
        int[] result = S.toArray();
        Arrays.sort(result);
        return result;
    }

    private int getJ()
    {
        int j;
        double Dmin = D.getQuick(i_min);
        int j1 = 0, j2 = mSize - 1;
        while( true )
        {
            j = ( j2 + j1 ) >>> 1;  // unsigned right shift for safe averaging
            double curD = D.getQuick(sortedD[j]);
            if( curD > Dmin )
                j2 = j - 1;
            else if( curD < Dmin )
                j1 = j + 1;
            else
            {
                if( j == 0 )
                    break;
                double prevD = D.getQuick(sortedD[j - 1]);
                if( prevD < Dmin )
                    break;
                j2 = j;
            }
        }
        return j;
    }

    /**
     * Search in D using sortedD index
     * @param value to search
     * @return index (in sortedD) of value or insertion point
     */
    private int searchInD(double value)
    {
        int j;
        int j1 = 0, j2 = Dcount - 1;
        while( true )
        {
            j = ( j2 + j1 ) >>> 1;  // unsigned right shift for safe averaging
            double curD = D.getQuick(sortedD[j]);
            if( curD == value )
                return j;
            if( curD > value )
            {
                j2 = j - 1;
                if( j1 > j2 )
                    return -j;
            }
            else
            {
                j1 = j + 1;
                if( j1 > j2 )
                    return -j - 1;
            }
        }
    }

    private TIntList conhull(TDoubleList abs, TDoubleList ord)
    {
        int size = abs.size();
        TIntList indexes = new TIntArrayList();

        if( size == 1 )
            indexes.add(0);

        if( size == 2 )
        {
            indexes.add(0);
            indexes.add(1);
        }

        if( size > 2 )
        {
            for( int i = 0; i < size; ++i )
                indexes.add(i);

            size--;
            int start = 0, v = start, w = size, flag = 0;

            while( ( next(v, size) != start || flag == 0 ) && go )
            {
                if( next(v, size) == w )
                    flag = 1;

                int[] numbers = new int[3];
                numbers[0] = v;
                numbers[1] = next(v, size);
                numbers[2] = next(next(v, size), size);

                Matrix currMatrix = new Matrix(3, 3);

                for( int i = 0; i < 3; ++i )
                {
                    currMatrix.set(i, 0, abs.get(numbers[i]));
                    currMatrix.set(i, 1, ord.get(numbers[i]));
                    currMatrix.set(i, 2, 1);
                }

                boolean leftturn = ( currMatrix.det() >= 0 ) ? true : false;

                if( leftturn )
                    v = next(v, size);
                else
                {
                    int indToRemove = next(v, size);

                    abs.remove(indToRemove);
                    ord.remove(indToRemove);
                    indexes.remove(indToRemove);

                    size--;
                    w--;
                    v = prev(v, size);
                }
            }
        }
        return indexes;
    }

    private int next(int pNum, int pSize)
    {
        int result = 0;
        if( pNum < pSize )
            result = pNum + 1;
        if( pNum == pSize )
            result = 0;
        return result;
    }

    private int prev(int pNum, int pSize)
    {
        int result = 0;
        if( pNum > 0 )
            result = pNum - 1;
        if( pNum == 0 )
            result = pSize;
        return result;
    }
}
