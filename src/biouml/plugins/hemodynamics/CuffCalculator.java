package biouml.plugins.hemodynamics;

public class CuffCalculator
{
    

//    private int cuffStart = 3;
//    private int cuffEnd = 7;
//    private double baseCuffPressure = 40;
    
//    if( enableCuff && cuff[j] && (i == cuffStart || i == cuffEnd))
//        ff[0] += calculateFCuff( i, j, k );
//
//    private double[] getCuffPressure()
//    {
//        double[] result = new double[integrationSegments];
//        for (int i=cuffStart; i<cuffEnd; i++)
//            result[i] = baseCuffPressure;
//        return result;
////        double h = dels[i] * this.solverStep;
////        return baseCuffPressure * Math.cos( Math.PI * ( x + h ) / 2 * h );
//    }
//
//   private boolean enableCuff = false;
//   private boolean[] cuff;
//    private double getCuffPressureDerivative(double x, int i, boolean start)
//    {
//        double h = dels[i] * integrationSegments;
//        return -baseCuffPressure *Math.PI/(2*h)*Math.sin( Math.PI * ( start? x + h: x ) / 2 * h );
//    }
//
//    private double calculateFCuff(int i, int j, int k)
//    {
//        boolean start = j == cuffStart;
//
//        double aDelta = ( area[i][j + 1] - area[i][j] ) / vesselSegments;
//        double qDelta = ( flow[i][j + 1] - flow[i][j] ) / vesselSegments;
//
//        double aj = area[i][j] + ( k - 1 / 2 ) * aDelta;
//        double qj = flow[i][j + 1] + ( k - 1 / 2 ) * qDelta;
//
//        return aj * getCuffPressureDerivative( ( j - 0.5 ) * dels[i], i, start )
//                / ( ( qj / aj ) * ( qj / aj ) - beta[i] * Math.sqrt( aj ) / 2 * area0[i] );
//    }
}
