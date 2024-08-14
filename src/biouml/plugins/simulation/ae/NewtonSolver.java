
package biouml.plugins.simulation.ae;

/*
 * WARNING: READ BEFORE EDITING THIS CLASS!
 * 
 * This class and some others constitutes separate jar which is used during model simulation in BioUML,
 * therefore all class on which it depends should also added to this jar 
 * jar file used for simulation is specified by SimulationEngine
 * 
 * Before adding any new dependencies here - please think twice.
 * 
 * If you add dependency - add this class (and all classes from which it depends) to build_bdk.xml
 * (see biouml.plugins.simulation building)
 * @see SimualtionEngine
 */

public class NewtonSolver
{
    private final static double STPMX = 100.0;

    public static boolean solve(double[] initialGuess, AeModel model) throws Exception
    {
        double MAXITS = 20000;
        double TOLF = 1.0e-10;
        double TOLMIN = 1.0e-12;
        double TOLX = 1.0e-13;

        return solve(initialGuess, model, MAXITS, TOLF, TOLMIN, TOLX);
    }

    public static boolean solve(double[] initialGuess, AeModel model, double MAXITS, double TOLF, double TOLMIN, double TOLX)
            throws Exception
    {
        int n = initialGuess.length;

        double fNorm = calculateHalfNorm(initialGuess, model);
        double fOldNorm;
        double[] currentFunctionValues = model.solveAlgebraic(initialGuess);
        double[] currentArgumentValues = initialGuess;
        double[] xOld = new double[n];
        double[] directions = new double[n];
        double test = 0;

        for( int i = 0; i < n; i++ )
        {
            if( Math.abs(currentFunctionValues[i]) > test )
                test = Math.abs(currentFunctionValues[i]);
        }

        if( test < 0.01 * TOLF )
            return false;

        double sum = 0;
        for( int i = 0; i < n; i++ )
            sum += currentArgumentValues[i] * currentArgumentValues[i];

        double maxStep = STPMX * Math.max(Math.sqrt(sum), n);

        for( int itearations = 0; itearations < MAXITS; itearations++ )
        {
            double[][] yacobian = jacobian(currentArgumentValues, currentFunctionValues, model);
            double gradient[] = new double[n];
            for( int i = 0; i < n; i++ )
            {
                double sum1 = 0;
                for( int j = 0; j < n; j++ )
                    sum1 += yacobian[j][i] * currentFunctionValues[j];
                gradient[i] = sum1;
            }
            for( int i = 0; i < n; i++ )
                xOld[i] = currentArgumentValues[i];

            fOldNorm = fNorm;
            for( int i = 0; i < n; i++ )
                directions[i] = -currentFunctionValues[i];

            int[] permutations = luDecomposition(yacobian);

            luBackSubstitution(yacobian, permutations, directions);

            //            System.out.println("!!!directions!!!:"+directions[0]);

            boolean[] check = new boolean[1];
            fNorm = lineSearch(xOld, fOldNorm, gradient, directions, maxStep, currentArgumentValues, model, check);
            currentFunctionValues = model.solveAlgebraic(currentArgumentValues);
            test = 0;
            for( int i = 0; i < n; i++ )
            {
                if( Math.abs(currentFunctionValues[i]) > test )
                    test = Math.abs(currentFunctionValues[i]);
            }
            if( test < TOLF )
                return false;
            if( check[0] )
            {
                test = 0;
                double den = Math.max(fNorm, 0.5 * n);
                double temp = 0;
                for( int i = 0; i < n; i++ )
                {
                    temp = Math.abs(gradient[i]) * Math.max(Math.abs(currentArgumentValues[i]), 1) / den;
                    if( temp > test )
                        test = temp;
                }
                return test < TOLMIN ? true : false;
            }
            test = 0;
            for( int i = 0; i < n; i++ )
            {
                double temp = ( Math.abs(currentArgumentValues[i] - xOld[i]) ) / Math.max(Math.abs(currentArgumentValues[i]), 1);
                if( temp > test )
                    test = temp;
            }
            if( test < TOLX )
                return false;
        }
        throw new Exception("Error during solving algebraic system: max iteration number exceeded");

    }

    public static double lineSearch(double[] xOld, double fOld, double[] gradient, double[] direction, double maxStep, double[] x,
            AeModel model, boolean[] check) throws Exception
    {
        double f;
        double alam2 = 0;
        double f2 = 0;
        int n = xOld.length;
        double sum = 0;
        for( int i = 0; i < n; i++ )
        {
            //            System.out.println("directions" + i+":" +direction[i]);
            //            System.out.println("gradient" + i+":" +gradient[i]);
            sum += direction[i] * direction[i];
        }
        sum = Math.sqrt(sum);
        if( sum > maxStep )
        {
            for( int i = 0; i < n; i++ )
            {
                direction[i] *= maxStep / sum;
            }
        }
        double slope = 0;
        for( int i = 0; i < n; i++ )
            slope += gradient[i] * direction[i];
        if( slope >= 0 )
            throw new Exception("roundoff problem in lineSearch");
        double test = 0;
        for( int i = 0; i < n; i++ )
        {
            double temp = Math.abs(direction[i]) / Math.max(Math.abs(xOld[i]), 1);
            if( temp > test )
                test = temp;
        }
        double alamin = TOLERANCE_X / test;
        double alam = 1;
        //
        double fTemp = fOld;
        //
        for( ;; )
        {
            double tmpAlam;
            for( int i = 0; i < n; i++ )
            {
                x[i] = xOld[i] + alam * direction[i];
            }
            f = calculateHalfNorm(x, model);
            //
            if( f == fTemp )
                return f;
            fTemp = f;
            //
            if( alam < alamin )
            {
                for( int i = 0; i < n; i++ )
                {
                    x[i] = xOld[i];
                }
                check[0] = true;
                return f;
            }
            else if( f <= fOld + ALF * alam * slope )
            {
                check[0] = false;
                return f;
            }
            else
            {
                if( alam == 1 )
                {
                    tmpAlam = -slope / ( 1 * ( f - fOld - slope ) );
                }
                else
                {
                    double rhs1 = f - fOld - alam * slope;
                    double rhs2 = f2 - fOld - alam2 * slope;
                    double a = ( rhs1 / ( alam * alam ) - rhs2 / ( alam2 * alam2 ) ) / ( alam - alam2 );
                    double b = ( alam2 * rhs1 / ( alam * alam ) + alam * rhs2 / ( alam2 * alam2 ) ) / ( alam - alam2 );
                    if( a == 0 )
                    {
                        tmpAlam = -slope / ( 2 * b );
                    }
                    else
                    {
                        double disc = b * b - 3 * a * slope;
                        if( disc < 0 )
                            tmpAlam = 0.5 * alam;
                        else if( b <= 0 )
                            tmpAlam = ( -b + Math.sqrt(disc) ) / ( 3 * a );
                        else
                            tmpAlam = -slope / ( b + Math.sqrt(disc) );
                    }
                    if( tmpAlam > 0.5 * alam )
                        tmpAlam = 0.5 * alam;

                }
            }
            alam2 = alam;
            f2 = f;
            alam = Math.max(tmpAlam, 0.1 * alam);
        }
    }

    public static int[] luDecomposition(double[][] matrix) throws Exception
    {
        int n = matrix.length;
        int maxI;
        if( n == 0 || n != matrix[0].length )
        {
            throw new Exception("Wrong matrix on input");
        }

        double[] scalingVector = new double[n];
        int[] rowPermutation = new int[n];
        int parity = 1;
        for( int i = 0; i < n; i++ )
        {
            double max = 0;
            double temp = 0;
            for( int j = 0; j < n; j++ )
            {
                if( ( temp = Math.abs(matrix[i][j]) ) > max )
                    max = temp;
            }
            if( max == 0 )
            {
                //                throw new Exception("Singular matrix in LU decomposition");
            }

            scalingVector[i] = 1 / max;
        }

        double sum;
        for( int j = 0; j < n; j++ )
        {
            for( int i = 0; i < j; i++ )
            {
                sum = matrix[i][j];
                for( int k = 0; k < i; k++ )
                    sum -= matrix[i][k] * matrix[k][j];
                matrix[i][j] = sum;
            }
            double max = 0;
            maxI = j;
            for( int i = j; i < n; i++ )
            {
                sum = matrix[i][j];
                for( int k = 0; k < j; k++ )
                    sum -= matrix[i][k] * matrix[k][j];
                matrix[i][j] = sum;
                double dum;
                if( ( dum = Math.abs(sum) ) >= max )
                {
                    max = dum;
                    maxI = i;
                }
            }
            if( j != maxI )
            {
                double t;
                for( int k = 0; k < n; k++ )
                {
                    t = matrix[maxI][k];
                    matrix[maxI][k] = matrix[j][k];
                    matrix[j][k] = t;
                }
                parity = -parity;
                scalingVector[maxI] = scalingVector[j];
            }
            rowPermutation[j] = maxI;
            if( j != n - 1 )
            {
                double t = 1 / matrix[j][j];
                for( int i = j + 1; i < n; i++ )
                    matrix[i][j] *= t;
            }
        }
        return rowPermutation;
    }

    public static void luBackSubstitution(double[][] luDecomposition, int[] rowPermutation, double[] rightHandSide) throws Exception
    {
        int n = luDecomposition.length;
        if( n == 0 || n != luDecomposition[0].length || n != rightHandSide.length || n != rowPermutation.length )
            throw new Exception("Wrong matrix on input");
        double sum;
        int ip;
        int ii = -1;
        for( int i = 0; i < n; i++ )
        {
            ip = rowPermutation[i];
            sum = rightHandSide[ip];
            rightHandSide[ip] = rightHandSide[i];
            if( ii >= 0 )
            {
                for( int j = ii; j < i; j++ )
                    sum -= luDecomposition[i][j] * rightHandSide[j];
            }
            else if( sum > 0 )
                ii = i;
            rightHandSide[i] = sum;
        }
        for( int i = n - 1; i >= 0; i-- )
        {
            sum = rightHandSide[i];
            for( int j = i + 1; j < n; j++ )
                sum -= luDecomposition[i][j] * rightHandSide[j];
            rightHandSide[i] = sum / luDecomposition[i][i];
        }
    }

    private final static double EPS = 1.0e-4;
    public static double[][] jacobian(double[] x, double[] function, AeModel model) throws Exception
    {
        int n = x.length;
        if( n == 0 || function.length != n )
            throw new Exception("invalid format");

        double[][] yacoby = new double[n][n];
        for( int j = 0; j < n; j++ )
        {
            double temp = x[j];
            double h = EPS * Math.abs(temp);

            if( h < EPS )
                h = EPS;

            x[j] = temp + h;

            h = x[j] - temp;

            double[] fResult = model.solveAlgebraic(x);
            x[j] = temp;

            for( int i = 0; i < n; i++ )
            {
                yacoby[i][j] = ( fResult[i] - function[i] ) / h;
                /*
                                System.out.println("h=" + h + "J=" + yacoby[i][j]);
                                System.out.println("    fResult[i]=" + fResult[i]
                                + ", function[i]=" + function[i]);
                */
            }
        }

        return yacoby;
    }

    private final static double TOLERANCE_X = 1.0e-7;
    private final static double ALF = 1.0e-4;

    public static double calculateHalfNorm(double[] x, AeModel m) throws Exception
    {
        double[] result = m.solveAlgebraic(x);
        
        if (result.length < x.length )
            throw new Exception("Algebraic system is undeterminated: "+result.length+" equations and "+x.length+" variables");
        double sum = 0;
        for( int i = 0; i < x.length; i++ )
        {
            sum += result[i] * result[i];
        }
        return 0.5 * sum;
    }

    public static void solveLinearEquationsSet(double[][] a, double[] rightHandSide) throws Exception
    {
        int[] rowPermuiatations = luDecomposition(a);
        StringBuilder row;
        for( int i = 0; i < a.length; i++ )
        {
            row = new StringBuilder();
            for( int j = 0; j < a.length; j++ )
            {
                row.append(a[i][j]).append(" ");
            }
            System.out.println(row.toString());
        }
        row = new StringBuilder();
        for( int i = 0; i < rowPermuiatations.length; i++ )
        {
            row.append(rowPermuiatations[i]).append(" ");
        }
        System.out.println("ROW:" + row.toString());
        luBackSubstitution(a, rowPermuiatations /*new int[]{1,0}*/, rightHandSide);
    }
}