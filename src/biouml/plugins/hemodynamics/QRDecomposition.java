package biouml.plugins.hemodynamics;

import Jama.Matrix;

/** QR Decomposition.
 <P>
 For an m-by-n matrix A with m >= n, the QR decomposition is an m-by-n
 orthogonal matrix Q and an n-by-n upper triangular matrix R so that
 A = Q*R.
 <P>
 The QR decompostion always exists, even if the matrix does not have
 full rank, so the constructor will never fail.  The primary use of the
 QR decomposition is in the least squares solution of nonsquare systems
 of simultaneous linear equations.  This will fail if isFullRank()
 returns false.
 */

public class QRDecomposition implements java.io.Serializable
{

    /* ------------------------
     Class variables
     * ------------------------ */

    /** Array for internal storage of decomposition.
     @serial internal array storage.
     */
    private double[][] QR;

    /** Row and column dimensions.
     @serial column dimension.
     @serial row dimension.
     */
    private int m, n;

    /** Array for internal storage of diagonal of R.
     @serial diagonal of R.
     */
    private double[] Rdiag;

    /* ------------------------
     Constructor
     * ------------------------ */

    /** QR Decomposition, computed by Householder reflections.
     @param A    Rectangular matrix
     @return     Structure to access R and the Householder vectors and compute Q.
     */

    public QRDecomposition(Matrix A)
    {
        this(A.getArray());
    }
    
    public QRDecomposition(double[][] matrix)
    {
        // Initialize.
        QR = matrix;
        m = matrix.length;
        n = matrix[0].length;
        Rdiag = new double[n];

        // Main loop.
        for( int k = 0; k < n; k++ )
        {
            // Compute 2-norm of k-th column without under/overflow.
            double nrm = 0;
            for( int i = k; i < m; i++ )
            {
//                nrm += QR[i][k]*QR[i][k];
                nrm = hypot(nrm, QR[i][k]);
            }
//nrm = Math.sqrt( nrm );

            if( nrm != 0.0 )
            {
                // Form k-th Householder vector.
                if( QR[k][k] < 0 )
                {
                    nrm = -nrm;
                }
                for( int i = k; i < m; i++ )
                {
                    QR[i][k] /= nrm;
                }
                QR[k][k] += 1.0;

                // Apply transformation to remaining columns.
                for( int j = k + 1; j < n; j++ )
                {
                    double s = 0.0;
                    for( int i = k; i < m; i++ )
                    {
                        s += QR[i][k] * QR[i][j];
                    }
                    s = -s / QR[k][k];
                    for( int i = k; i < m; i++ )
                    {
                        QR[i][j] += s * QR[i][k];
                    }
                }
            }
            Rdiag[k] = -nrm;
        }
    }
    
    private double hypot(double a, double b)
    {
        return Math.sqrt(a*a + b*b);
    }
    
   

    /* ------------------------
     Public Methods
     * ------------------------ */

    /** Is the matrix full rank?
     @return     true if R, and hence A, has full rank.
     */

    public boolean isFullRank()
    {
        for( int j = 0; j < n; j++ )
        {
            if( Rdiag[j] == 0 )
                return false;
        }
        return true;
    }

    /** Return the Householder vectors
     @return     Lower trapezoidal matrix whose columns define the reflections
     */

    public Matrix getH()
    {
        Matrix X = new Matrix(m, n);
        double[][] H = X.getArray();
        for( int i = 0; i < m; i++ )
        {
            for( int j = 0; j < n; j++ )
            {
                if( i >= j )
                {
                    H[i][j] = QR[i][j];
                }
                else
                {
                    H[i][j] = 0.0;
                }
            }
        }
        return X;
    }

    /** Return the upper triangular factor
     @return     R
     */

    public Matrix getRMatrix()
    {
        return new Matrix(getR());
    }
    
    public double[][] getR()
    {
        double[][] r = new double[n][n];
        
        for( int i = 0; i < n; i++ )
        {
            for( int j = i+1; j < n; j++ )
            {
                    r[i][j] = QR[i][j];
            }
            r[i][i] = Rdiag[i];
        }
        return r;
    }

    /** Generate and return the (economy-sized) orthogonal factor
     @return     Q
     */

    public Matrix getQ()
    {
//        Matrix X = new Matrix(m, n);
        double[][] Q = new double[m][n];//X.getArray();
        for( int k = n - 1; k >= 0; k-- )
        {
//            for( int i = 0; i < m; i++ )
//            {
//                Q[i][k] = 0.0;
//            }
            Q[k][k] = 1.0;
            for( int j = k; j < n; j++ )
            {
                if( QR[k][k] != 0 )
                {
                    double s = 0.0;
                    for( int i = k; i < m; i++ )
                    {
                        s += QR[i][k] * Q[i][j];
                    }
                    s = -s / QR[k][k];
                    for( int i = k; i < m; i++ )
                    {
                        Q[i][j] += s * QR[i][k];
                    }
                }
            }
        }
        return new Matrix(Q);
    }

    /** Generate and return the (economy-sized) orthogonal factor
    @return     Q
    */

   public Matrix getQFullMatrix()
   {
       return new Matrix(getQFull());
   }
   
   public double[][] getQFull()
   {
       double[][] q = new double[m][m];
       for( int k = n - 1; k >= 0; k-- )
       {
//           for( int i = 0; i < m; i++ )
//           {
//               q[i][k] = 0.0;
//           }
           q[k][k] = 1.0;
           for( int j = k; j < m; j++ )
           {
               if( QR[k][k] != 0 )
               {
                   double s = 0.0;
                   for( int i = k; i < m; i++ )
                   {
                       s += QR[i][k] * q[i][j];
                   }
                   s = -s / QR[k][k];
                   for( int i = k; i < m; i++ )
                   {
                       q[i][j] += s * QR[i][k];
                   }
               }
           }
       }
//       for (int j=0; j<m; j++)
//       {
//           Q[0][j]= (-1.0)*Q[0][j];
//       }
       return q;
   }
    
    
    /** Least squares solution of A*X = B
     @param B    A Matrix with as many rows as A and any number of columns.
     @return     X that minimizes the two norm of Q*R*X-B.
     @exception  IllegalArgumentException  Matrix row dimensions must agree.
     @exception  RuntimeException  Matrix is rank deficient.
     */

    public Matrix solve(Matrix B)
    {
        if( B.getRowDimension() != m )
        {
            throw new IllegalArgumentException("Matrix row dimensions must agree.");
        }
        if( !this.isFullRank() )
        {
            throw new RuntimeException("Matrix is rank deficient.");
        }

        // Copy right hand side
        int nx = B.getColumnDimension();
        double[][] X = B.getArrayCopy();

        // Compute Y = transpose(Q)*B
        for( int k = 0; k < n; k++ )
        {
            for( int j = 0; j < nx; j++ )
            {
                double s = 0.0;
                for( int i = k; i < m; i++ )
                {
                    s += QR[i][k] * X[i][j];
                }
                s = -s / QR[k][k];
                for( int i = k; i < m; i++ )
                {
                    X[i][j] += s * QR[i][k];
                }
            }
        }
        // Solve R*X = Y;
        for( int k = n - 1; k >= 0; k-- )
        {
            for( int j = 0; j < nx; j++ )
            {
                X[k][j] /= Rdiag[k];
            }
            for( int i = 0; i < k; i++ )
            {
                for( int j = 0; j < nx; j++ )
                {
                    X[i][j] -= X[k][j] * QR[i][k];
                }
            }
        }
        return ( new Matrix(X, n, nx).getMatrix(0, n - 1, 0, nx - 1) );
    }
}