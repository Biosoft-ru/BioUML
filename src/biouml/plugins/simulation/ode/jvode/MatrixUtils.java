package biouml.plugins.simulation.ode.jvode;

public class MatrixUtils
{

    /*
     * -----------------------------------------------------------------
     * Functions: DenseGETRF and DenseGETRS
     * -----------------------------------------------------------------
     * DenseGETRF performs the LU factorization of the M by N dense
     * matrix A. This is done using standard Gaussian elimination
     * with partial (row) pivoting. Note that this applies only
     * to matrices with M >= N and full column rank.
     *
     * A successful LU factorization leaves the matrix A and the
     * pivot array p with the following information:
     *
     * (1) p[k] contains the row number of the pivot element chosen
     *     at the beginning of elimination step k, k=0, 1, ..., N-1.
     *
     * (2) If the unique LU factorization of A is given by PA = LU,
     *     where P is a permutation matrix, L is a lower trapezoidal
     *     matrix with all 1's on the diagonal, and U is an upper
     *     triangular matrix, then the upper triangular part of A
     *     (including its diagonal) contains U and the strictly lower
     *     trapezoidal part of A contains the multipliers, I-L.
     *
     * For square matrices (M=N), L is unit lower triangular.
     *
     * DenseGETRF returns 0 if successful. Otherwise it encountered
     * a zero diagonal element during the factorization. In this case
     * it returns the column index (numbered from one) at which
     * it encountered the zero.
     *
     * DenseGETRS solves the N-dimensional system A x = b using
     * the LU factorization in A and the pivot information in p
     * computed in DenseGETRF. The solution x is returned in b. This
     * routine cannot fail if the corresponding call to DenseGETRF
     * did not fail.
     * DenseGETRS does NOT check for a square matrix!
     *
     * -----------------------------------------------------------------
     * DenseGETRF and DenseGETRS are simply wrappers around denseGETRF
     * and denseGETRS, respectively, which perform all the work by
     * directly accessing the data in the DlsMat A (i.e. the field cols)
     * -----------------------------------------------------------------
     */
    public static int DenseGETRF(Matrix A, int[] p)
    {
        return denseGETRF(A.cols, A.M, A.N, p);
    }

    public static void DenseGETRS(Matrix A, int[] p, double[] b)
    {
        denseGETRS(A.cols, A.N, p, b);
    }

    public static int denseGETRF(double[][] a, int m, int n, int[] p)
    {
        int i, j, k, l;

        /* k-th elimination step number */
        for( k = 0; k < n; k++ )
        {
            double[] ak = a[k];
            /* find l = pivot row number */
            l = k;
            for( i = k + 1; i < m; i++ )
            {
                double aki = ak[i];
                double akl = ak[l];
                if(aki < 0) aki = -aki;
                if(akl < 0) akl = -akl;
                if( aki > akl )
                    l = i;
            }
            p[k] = l;

            /* check for zero pivot element */
            if( ak[l] == 0.0 )
                return ( k + 1 );

            /* swap a(k,1:n) and a(l,1:n) if necessary */
            if( l != k )
            {
                for( i = 0; i < n; i++ )
                {
                    double temp = a[i][l];
                    a[i][l] = a[i][k];
                    a[i][k] = temp;
                }
            }

            /* Scale the elements below the diagonal in
             * column k by 1.0/a(k,k). After the above swap
             * a(k,k) holds the pivot element. This scaling
             * stores the pivot row multipliers a(i,k)/a(k,k)
             * in a(i,k), i=k+1, ..., m-1.
             */
            double mult = 1.0 / ak[k];
            for( i = k + 1; i < m; i++ )
                ak[i] *= mult;

            /* row_i = row_i - [a(i,k)/a(k,k)] row_k, i=k+1, ..., m-1 */
            /* row k is the pivot row after swapping with row l.      */
            /* The computation is done one column at a time,          */
            /* column j=k+1, ..., n-1.                                */

            for( j = k + 1; j < n; j++ )
            {
                /* a(i,j) = a(i,j) - [a(i,k)/a(k,k)]*a(k,j)  */
                /* a_kj = a(k,j), col_k[i] = - a(i,k)/a(k,k) */
                double[] aj = a[j];
                double ajk = aj[k];
                if( ajk != 0.0 )
                {
                    for( i = k + 1; i < m; i++ )
                        aj[i] -= ajk * ak[i];
                }
            }
        }
        /* return 0 to indicate success */
        return 0;
    }

    public static void denseGETRS(double[][] a, int n, int[] p, double[] b)
    {
        /* Permute b, based on pivot information in p */
        for(int k = 0; k < n; k++ )
        {
            int pk = p[k];
            if( pk != k )
            {
                double tmp = b[k];
                b[k] = b[pk];
                b[pk] = tmp;
            }
        }

        /* Solve Ly = b, store solution y in b */
        for(int k = 0; k < n - 1; k++ )
        {
            double[] ak = a[k];
            double bk = b[k];
            for(int i = k + 1; i < n; i++ )
                b[i] -= ak[i] * bk;
        }

        /* Solve Ux = y, store solution x in b */
        for(int k = n - 1; k > 0; k-- )
        {
            double[] ak = a[k];
            double bk = b[k] /= ak[k];
            for(int i = 0; i < k; i++ )
                b[i] -= ak[i] * bk;
        }
        b[0] /= a[0][0];
    }

    /*
     * -----------------------------------------------------------------
     * Functions : DensePOTRF and DensePOTRS
     * -----------------------------------------------------------------
     * DensePOTRF computes the Cholesky factorization of a real symmetric
     * positive definite matrix A.
     * -----------------------------------------------------------------
     * DensePOTRS solves a system of linear equations A*X = B with a
     * symmetric positive definite matrix A using the Cholesky factorization
     * A = L*L**T computed by DensePOTRF.
     *
     * -----------------------------------------------------------------
     * DensePOTRF and DensePOTRS are simply wrappers around densePOTRF
     * and densePOTRS, respectively, which perform all the work by
     * directly accessing the data in the DlsMat A (i.e. the field cols)
     * -----------------------------------------------------------------
     */

    public static int DensePOTRF(Matrix A)
    {
        return ( densePOTRF(A.cols, A.M) );
    }

    public static void DensePOTRS(Matrix A, double[] b)
    {
        densePOTRS2(A.cols, A.M, b);
    }

    /*
     * Cholesky decomposition of a symmetric positive-definite matrix
     * A = C^T*C: gaxpy version.
     * Only the lower triangle of A is accessed and it is overwritten with
     * the lower triangle of C.
     */
    static int densePOTRF(double[][] a, int m)
    {
        double a_diag;

        for( int j = 0; j < m; j++ )
        {
            if( j > 0 )
            {
                for( int i = j; i < m; i++ )
                {
                    for( int k = 0; k < j; k++ )
                    {
                        a[j][i] -= a[k][i] * a[k][j];
                    }
                }
            }

            a_diag = a[j][j];
            if( a_diag <= 0.0 )
                return ( j );
            a_diag = Math.sqrt(a_diag);

            for( int i = j; i < m; i++ )
                a[j][i] /= a_diag;

        }

        return ( 0 );
    }

    /**
     * Solution of Ax=b, with A s.p.d., based on the Cholesky decomposition
     * obtained with denPOTRF.; A = C*C^T, C lower triangular
     *
     */
    static void densePOTRS(double[][] a, int m, double[] b)
    {
        double[] aj, ai;

        /* Solve C y = b, forward substitution - column version.
           Store solution y in b */
        for( int j = 0; j < m - 1; j++ )
        {
            aj = a[j];
            b[j] /= aj[j];
            for( int i = j + 1; i < m; i++ )
                b[i] -= b[j] * aj[i];
        }
        aj = a[m - 1];
        b[m - 1] /= aj[m - 1];

        /* Solve C^T x = y, backward substitution - row version.
           Store solution x in b */
        aj = a[m - 1];
        b[m - 1] /= aj[m - 1];
        for( int i = m - 2; i >= 0; i-- )
        {
            ai = a[i];
            for( int j = i + 1; j < m; j++ )
                b[i] -= ai[j] * b[j];
            b[i] /= ai[i];
        }

    }

    static void densePOTRS2(double[][] a, int m, double[] b)
    {
        /* Solve C y = b, forward substitution - column version.
           Store solution y in b */
        for( int j = 0; j < m - 1; j++ )
        {
            b[j] /= a[j][j];
            for( int i = j + 1; i < m; i++ )
                b[i] -= b[j] * a[j][i];
        }
        b[m - 1] /= a[m - 1][m - 1];

        /* Solve C^T x = y, backward substitution - row version.
           Store solution x in b */
        b[m - 1] /= a[m - 1][m - 1];
        for( int i = m - 2; i >= 0; i-- )
        {
            for( int j = i + 1; j < m; j++ )
                b[i] -= a[i][j] * b[j];
            b[i] /= a[i][i];
        }

    }


    /*
     * -----------------------------------------------------------------
     * Functions : DenseGEQRF and DenseORMQR
     * -----------------------------------------------------------------
     * DenseGEQRF computes a QR factorization of a real M-by-N matrix A:
     * A = Q * R (with M>= N).
     * 
     * DenseGEQRF requires a temporary work vector wrk of length M.
     * -----------------------------------------------------------------
     * DenseORMQR computes the product w = Q * v where Q is a real
     * orthogonal matrix defined as the product of k elementary reflectors
     *
     *        Q = H(1) H(2) . . . H(k)
     *
     * as returned by DenseGEQRF. Q is an M-by-N matrix, v is a vector
     * of length N and w is a vector of length M (with M>=N).
     *
     * DenseORMQR requires a temporary work vector wrk of length M.
     *
     * -----------------------------------------------------------------
     * DenseGEQRF and DenseORMQR are simply wrappers around denseGEQRF
     * and denseORMQR, respectively, which perform all the work by
     * directly accessing the data in the DlsMat A (i.e. the field cols)
     * -----------------------------------------------------------------
     */
    public static int DenseGEQRF(Matrix A, double[] beta, double[] wrk)
    {
        return denseGEQRF(A.cols, A.M, A.N, beta, wrk);
    }
    public static int DenseORMQR(Matrix A, double[] beta, double[] vn, double[] vm, double[] wrk)
    {
        return ( denseORMQR(A.cols, A.M, A.N, beta, vn, vm, wrk) );
    }

    /**
     * QR factorization of a rectangular matrix A of size m by n (m >= n)
     * using Householder reflections.
     *
     * On exit, the elements on and above the diagonal of A contain the n by n
     * upper triangular matrix R; the elements below the diagonal, with the array beta,
     * represent the orthogonal matrix Q as a product of elementary reflectors .
     *
     * v (of length m) must be provided as workspace.
     *
     */
    static int denseGEQRF(double[][] a, int m, int n, double[] beta, double[] v)
    {
        double ajj, s, mu, v1, v1_2;
        double[] col_j, col_k;
        int i, j, k;

        /* For each column...*/
        for( j = 0; j < n; j++ )
        {

            col_j = a[j];

            ajj = col_j[j];

            /* Compute the j-th Householder vector (of length m-j) */
            v[0] = 1.0;
            s = 0.0;
            for( i = 1; i < m - j; i++ )
            {
                v[i] = col_j[i + j];
                s += v[i] * v[i];
            }

            if( s != 0.0 )
            {
                mu = Math.sqrt(ajj * ajj + s);
                v1 = ( ajj <= 0.0 ) ? ajj - mu : -s / ( ajj + mu );
                v1_2 = v1 * v1;
                beta[j] = 2.0 * v1_2 / ( s + v1_2 );
                for( i = 1; i < m - j; i++ )
                    v[i] /= v1;
            }
            else
            {
                beta[j] = 0.0;
            }

            /* Update upper triangle of A (load R) */
            for( k = j; k < n; k++ )
            {
                col_k = a[k];
                s = 0.0;
                for( i = 0; i < m - j; i++ )
                    s += col_k[i + j] * v[i];
                s *= beta[j];
                for( i = 0; i < m - j; i++ )
                    col_k[i + j] -= s * v[i];
            }

            /* Update A (load Householder vector) */
            if( j < m - 1 )
            {
                for( i = 1; i < m - j; i++ )
                    col_j[i + j] = v[i];
            }

        }
        return 0;
    }

    /**
     * Computes vm = Q * vn, where the orthogonal matrix Q is stored as
     * elementary reflectors in the m by n matrix A and in the vector beta.
     * (NOTE: It is assumed that an QR factorization has been previously
     * computed with denGEQRF).
     *
     * vn (IN) has length n, vm (OUT) has length m, and it's assumed that m >= n.
     *
     * v (of length m) must be provided as workspace.
     */
    static int denseORMQR(double[][] a, int m, int n, double[] beta, double[] vn, double[] vm, double[] v)
    {
        double[] col_j;
        double s;
        /* Initialize vm */
        for( int i = 0; i < n; i++ )
            vm[i] = vn[i];
        for( int i = n; i < m; i++ )
            vm[i] = 0.0;

        /* Accumulate (backwards) corrections into vm */
        for( int j = n - 1; j >= 0; j-- )
        {
            col_j = a[j];
            v[0] = 1.0;
            s = vm[j];
            for( int i = 1; i < m - j; i++ )
            {
                v[i] = col_j[i + j];
                s += v[i] * vm[i + j];
            }
            s *= beta[j];
            for( int i = 0; i < m - j; i++ )
                vm[i + j] -= s * v[i];
        }
        return 0;
    }



    /*
     * -----------------------------------------------------------------
     * Function : BandGBTRF
     * -----------------------------------------------------------------
     * Usage : ier = BandGBTRF(A, p);
     *         if (ier != 0) ... A is singular
     * -----------------------------------------------------------------
     * BandGBTRF performs the LU factorization of the N by N band
     * matrix A. This is done using standard Gaussian elimination
     * with partial pivoting.
     *
     * A successful LU factorization leaves the "matrix" A and the
     * pivot array p with the following information:
     *
     * (1) p[k] contains the row number of the pivot element chosen
     *     at the beginning of elimination step k, k=0, 1, ..., N-1.
     *
     * (2) If the unique LU factorization of A is given by PA = LU,
     *     where P is a permutation matrix, L is a lower triangular
     *     matrix with all 1's on the diagonal, and U is an upper
     *     triangular matrix, then the upper triangular part of A
     *     (including its diagonal) contains U and the strictly lower
     *     triangular part of A contains the multipliers, I-L.
     *
     * BandGBTRF returns 0 if successful. Otherwise it encountered
     * a zero diagonal element during the factorization. In this case
     * it returns the column index (numbered from one) at which
     * it encountered the zero.
     *
     * Important Note: A must be allocated to accommodate the increase
     * in upper bandwidth that occurs during factorization. If
     * mathematically, A is a band matrix with upper bandwidth mu and
     * lower bandwidth ml, then the upper triangular factor U can
     * have upper bandwidth as big as smu = MIN(n-1,mu+ml). The lower
     * triangular factor L has lower bandwidth ml. Allocate A with
     * call A = BandAllocMat(N,mu,ml,smu), where mu, ml, and smu are
     * as defined above. The user does not have to zero the "extra"
     * storage allocated for the purpose of factorization. This will
     * handled by the BandGBTRF routine.
     *
     * BandGBTRF is only a wrapper around bandGBTRF. All work is done
     * in bandGBTRF works directly on the data in the DlsMat A (i.e.,
     * the field cols).
     * -----------------------------------------------------------------
     */
    public static int BandGBTRF(Matrix A, int[] p)
    {
        return bandGBTRF(A.cols, A.N, A.mu, A.ml, A.smu, p);
    }
    static int bandGBTRF(double[][] a, int n, int mu, int ml, int smu, int[] p)
    {
        int l, storage_l, storage_k, last_col_k, last_row_k;
        double max, temp, mult, a_kj;
        boolean swap;
        double[] col_k, col_j;
        
        /* zero out the first smu - mu rows of the rectangular array a */
        int num_rows = smu - mu;
        if( num_rows > 0 )
        {
            for( int c = 0; c < n; c++ )
            {
                double[] ac = a[c];
                for( int r = 0; r < num_rows; r++ )
                {
                    ac[r] = 0.0;
                }
            }
        }

        /* k = elimination step number */
        for( int k = 0; k < n - 1; k++)
        {
            last_row_k = Math.min(n - 1, k + ml);
            col_k = a[k];
               /* find l = pivot row number */
            l = k;
            max = Math.abs(col_k[smu]);
            for( int i = k + 1, j = 1; i <= last_row_k; i++, j++ )
            {
                if( Math.abs(col_k[j + smu]) > max )
                {
                    l = i;
                    max = Math.abs(col_k[j + smu]);
                }
            }
            storage_l = l - k + smu;
            p[k] = l;

            /* check for zero pivot element */
            if( col_k[storage_l]== 0.0 )
            {
                return ( k + 1 );
            }
            /* swap a(l,k) and a(k,k) if necessary */
            if( ( swap = ( l != k ) ) )
            {
                temp = col_k[storage_l];
                col_k[storage_l] =  col_k[smu];
                col_k[smu] = temp;
            }

            /* Scale the elements below the diagonal in         */
            /* column k by -1.0 / a(k,k). After the above swap, */
            /* a(k,k) holds the pivot element. This scaling     */
            /* stores the pivot row multipliers -a(i,k)/a(k,k)  */
            /* in a(i,k), i=k+1, ..., MIN(n-1,k+ml).            */

            mult = -1.0 /  col_k[smu];
            for( int i = k + 1, j = 1; i <= last_row_k; i++, j++ )
            {
                col_k[j + smu] *= mult;
            }

            /* row_i = row_i - [a(i,k)/a(k,k)] row_k, i=k+1, ..., MIN(n-1,k+ml) */
            /* row k is the pivot row after swapping with row l.                */
            /* The computation is done one column at a time,                    */
            /* column j=k+1, ..., MIN(k+smu,n-1).                               */

            last_col_k = Math.min(k + smu, n - 1);
            for( int j = k + 1; j <= last_col_k; j++ )
            {
                col_j = a[j];
                storage_l = l - j + smu;
                storage_k = k - j + smu;
                a_kj = col_j[storage_l];

                /* Swap the elements a(k,j) and a(k,l) if l!=k. */
                if( swap )
                {
                    col_j[storage_l] = col_j[storage_k];
                    col_j[storage_k] = a_kj;
                }

                /* a(i,j) = a(i,j) - [a(i,k)/a(k,k)]*a(k,j) */
                /* a_kj = a(k,j), *kptr = - a(i,k)/a(k,k), *jptr = a(i,j) */
                if( a_kj != 0.0 )
                {
                    for( int i = k + 1, s = 0; i <= last_row_k; i++, s++ )
                    {
                        col_j[i - j + smu] += col_j[storage_l] * col_k[s + 1 + smu];
                    }
                }
            }
        }

        /* set the last pivot row to be n-1 and check for a zero pivot */

        p[n - 1] = n - 1;
        if( a[n - 1][smu] == 0.0 )
            return ( n );

        /* return 0 to indicate success */

        return ( 0 );
    }
    /*
     * -----------------------------------------------------------------
     * Function : BandGBTRS
     * -----------------------------------------------------------------
     * Usage : BandGBTRS(A, p, b);
     * -----------------------------------------------------------------
     * BandGBTRS solves the N-dimensional system A x = b using
     * the LU factorization in A and the pivot information in p
     * computed in BandGBTRF. The solution x is returned in b. This
     * routine cannot fail if the corresponding call to BandGBTRF
     * did not fail.
     *
     * BandGBTRS is only a wrapper around bandGBTRS which does all the
     * work directly on the data in the DlsMat A (i.e., the field cols).
     * -----------------------------------------------------------------
     */

    public static void BandGBTRS(Matrix A, int[] p, double[] b)
    {
        bandGBTRS(A.cols, A.N, A.smu, A.ml, p, b);
    }
    static void bandGBTRS(double[][] a, int n, int smu, int ml, int[] p, double[] b)
    {
        int l, first_row_k, last_row_k;
        double mult;

        /* Solve Ly = Pb, store solution y in b */
        for( int k = 0; k < n - 1; k++ )
        {
            l = p[k];
            mult = b[l];
            if( l != k )
            {
                b[l] = b[k];
                b[k] = mult;
            }
            last_row_k = Math.min(n - 1, k + ml);
            for( int i = k + 1; i <= last_row_k; i++ )
            {
                b[i] += mult * a[k][i - k + smu];
            }
        }

        /* Solve Ux = y, store solution x in b */

        for( int k = n - 1; k >= 0; k-- )
        {
            first_row_k = Math.max(0, k - smu);
            b[k] /= a[k][smu];
            mult = -b[k];
            for( int i = first_row_k; i <= k - 1; i++ )
            {
                b[i] += mult * a[k][i - k + smu];
            }
        }
    }

    /*
     * -----------------------------------------------------------------
     * Function: denseAddIdentity
     * -----------------------------------------------------------------
     * denseAddIdentity adds the identity matrix to the n-by-n matrix
     * stored in the double** arrays.
     * -----------------------------------------------------------------
     */
    public static void denseAddIdentity(double[][] a, int n)
    {
        for(int i = 0; i < n; i++ )
            a[i][i] += 1;
    }
    
    public static double[][] newDenseMat(int m, int n)
    {
        if( ( n <= 0 ) || ( m <= 0 ) )
            return null;
        double[][] a = new double[n][];
        for( int j = 0; j < n; j++ )
            a[j] = new double[m];
        return a;
    }
    
    public static double[][] newBandMat(int n, int smu, int ml)
    {
        if( n <= 0 )
            return null;
        double[][] a = new double[n][];
        int colSize = smu + ml + 1;
        for( int j = 0; j < n; j++ )
            a[j] = new double[colSize];
        return a;
    }
}
