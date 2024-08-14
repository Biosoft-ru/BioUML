package biouml.plugins.simulation.ode.jvode;

/*
 * -----------------------------------------------------------------
 * Type : DlsMat
 * -----------------------------------------------------------------
 * The type DlsMat is defined to be a pointer to a structure
 * with various sizes, a data field, and an array of pointers to
 * the columns which defines a dense or band matrix for use in
 * direct linear solvers. The M and N fields indicates the number
 * of rows and columns, respectively. The data field is a one
 * dimensional array used for component storage. The cols field
 * stores the pointers in data for the beginning of each column.
 * -----------------------------------------------------------------
 * For DENSE matrices, the relevant fields in DlsMat are:
 *    type  = SUNDIALS_DENSE
 *    M     - number of rows
 *    N     - number of columns
 *    ldim  - leading dimension (ldim >= M)
 *    data  - pointer to a contiguous block of realtype variables
 *    ldata - length of the data array =ldim*N
 *    cols  - array of pointers. cols[j] points to the first element
 *            of the j-th column of the matrix in the array data.
 *
 * The elements of a dense matrix are stored columnwise (i.e columns
 * are stored one on top of the other in memory).
 * If A is of type DlsMat, then the (i,j)th element of A (with
 * 0 <= i < M and 0 <= j < N) is given by (A->data)[j*n+i].
 *
 * The DENSE_COL and DENSE_ELEM macros below allow a user to access
 * efficiently individual matrix elements without writing out explicit
 * data structure references and without knowing too much about the
 * underlying element storage. The only storage assumption needed is
 * that elements are stored columnwise and that a pointer to the
 * jth column of elements can be obtained via the DENSE_COL macro.
 * -----------------------------------------------------------------
 * For BAND matrices, the relevant fields in DlsMat are:
 *    type  = SUNDIALS_BAND
 *    M     - number of rows
 *    N     - number of columns
 *    mu    - upper bandwidth, 0 <= mu <= min(M,N)
 *    ml    - lower bandwidth, 0 <= ml <= min(M,N)
 *    s_mu  - storage upper bandwidth, mu <= s_mu <= N-1.
 *            The dgbtrf routine writes the LU factors into the storage
 *            for A. The upper triangular factor U, however, may have
 *            an upper bandwidth as big as MIN(N-1,mu+ml) because of
 *            partial pivoting. The s_mu field holds the upper
 *            bandwidth allocated for A.
 *    ldim  - leading dimension (ldim >= s_mu)
 *    data  - pointer to a contiguous block of realtype variables
 *    ldata - length of the data array =ldim*(s_mu+ml+1)
 *    cols  - array of pointers. cols[j] points to the first element
 *            of the j-th column of the matrix in the array data.
 *
 * The BAND_COL, BAND_COL_ELEM, and BAND_ELEM macros below allow a
 * user to access individual matrix elements without writing out
 * explicit data structure references and without knowing too much
 * about the underlying element storage. The only storage assumption
 * needed is that elements are stored columnwise and that a pointer
 * into the jth column of elements can be obtained via the BAND_COL
 * macro. The BAND_COL_ELEM macro selects an element from a column
 * which has already been isolated via BAND_COL. The macro
 * BAND_COL_ELEM allows the user to avoid the translation
 * from the matrix location (i,j) to the index in the array returned
 * by BAND_COL at which the (i,j)th element is stored.
 * -----------------------------------------------------------------
 */

public class Matrix
{

    public final static int SUNDIALS_DENSE = 1;
    public final static int SUNDIALS_BAND = 2;

    private int type;
    public int M;
    public int N;
    private int ldim;
    int mu;
    int ml;
    int smu;
    public int ldata;
    public double[][] cols;



    /*
     * -----------------------------------------------------------------
     * DENSE_COL and DENSE_ELEM
     * -----------------------------------------------------------------
     *
     * DENSE_COL(A,j) references the jth column of the M-by-N dense
     * matrix A, 0 <= j < N. The type of the expression DENSE_COL(A,j)
     * is (realtype *). After the assignment in the usage above, col_j
     * may be treated as an array indexed from 0 to M-1. The (i,j)-th
     * element of A is thus referenced by col_j[i].
     *
     * DENSE_ELEM(A,i,j) references the (i,j)th element of the dense
     * M-by-N matrix A, 0 <= i < M ; 0 <= j < N.
     *
     * -----------------------------------------------------------------
     */

    public double getDenseElement(int i, int j)
    {
        return cols[j][i];
    }
    public void setDenseElement(int i, int j, double val)
    {
        cols[j][i] = val;
    }

    /*
     * -----------------------------------------------------------------
     * BAND_COL, BAND_COL_ELEM, and BAND_ELEM
     * -----------------------------------------------------------------
     * 
     * BAND_COL(A,j) references the diagonal element of the jth column
     * of the N by N band matrix A, 0 <= j <= N-1. The type of the
     * expression BAND_COL(A,j) is realtype *. The pointer returned by
     * the call BAND_COL(A,j) can be treated as an array which is
     * indexed from -(A->mu) to (A->ml).
     * 
     * BAND_COL_ELEM references the (i,j)th entry of the band matrix A
     * when used in conjunction with BAND_COL. The index (i,j) should
     * satisfy j-(A->mu) <= i <= j+(A->ml).
     *
     * BAND_ELEM(A,i,j) references the (i,j)th element of the M-by-N
     * band matrix A, where 0 <= i,j <= N-1. The location (i,j) should
     * further satisfy j-(A->mu) <= i <= j+(A->ml).
     *
     * -----------------------------------------------------------------
     */
    public double getBandElement(int i, int j)
    {
        return cols[j][i - j + smu];
    }
    public void setBandElement(int i, int j, double val)
    {
        cols[j][i - j + smu] = val;
    }

    /*
     * -----------------------------------------------------------------
     * Function: NewDenseMat
     * -----------------------------------------------------------------
     * NewDenseMat allocates memory for an M-by-N dense matrix and
     * returns the storage allocated (type DlsMat). NewDenseMat
     * returns NULL if the request for matrix storage cannot be
     * satisfied. See the above documentation for the type DlsMat
     * for matrix storage details.
     * -----------------------------------------------------------------
     */
    public Matrix(int M, int N)
    {
        if( ( M < 0 ) || ( N < 0 ) )
            throw new IllegalArgumentException("Matrix dimensions must be positive");

        this.cols = new double[N][];
        for( int j = 0; j < N; j++ )
            cols[j] = new double[M];

        this.M = M;
        this.N = N;

        this.ldim = M;
        this.ldata = M * N;
        this.type = SUNDIALS_DENSE;
    }

    /*
     * -----------------------------------------------------------------
     * Function: NewBandMat
     * -----------------------------------------------------------------
     * NewBandMat allocates memory for an M-by-N band matrix
     * with upper bandwidth mu, lower bandwidth ml, and storage upper
     * bandwidth smu. Pass smu as follows depending on whether A will
     * be LU factored:
     *
     * (1) Pass smu = mu if A will not be factored.
     *
     * (2) Pass smu = MIN(N-1,mu+ml) if A will be factored.
     *
     * NewBandMat returns the storage allocated (type DlsMat) or
     * NULL if the request for matrix storage cannot be satisfied.
     * See the documentation for the type DlsMat for matrix storage
     * details.
     * -----------------------------------------------------------------
     */
    public Matrix(int N, int mu, int ml, int smu)
    {
        if( N <= 0 )
            throw new IllegalArgumentException("Matrix dimenxions must be positive");

        this.ldim = smu + ml + 1;

        this.cols = new double[N][];
        for( int j = 0; j < N; j++ )
            cols[j] = new double[ldim];

        this.M = N;
        this.N = N;
        this.mu = mu;
        this.ml = ml;
        this.smu = smu;

        this.ldata = N * ldim;
        this.type = SUNDIALS_BAND;
    }


    /*
     * -----------------------------------------------------------------
     * Function : AddIdentity
     * -----------------------------------------------------------------
     * AddIdentity adds 1.0 to the main diagonal (A_ii, i=1,2,...,N-1) of
     * the M-by-N matrix A (M>= N) and stores the result back in A.
     * AddIdentity is typically used with square matrices.
     * AddIdentity does not check for M >= N and therefore a segmentation
     * fault will occur if M < N!
     * -----------------------------------------------------------------
     */
    public void addIdentity()
    {
        switch( type )
        {
            case SUNDIALS_DENSE:
            {
                for( int i = 0; i < N; i++ )
                    cols[i][i] += 1.0;
                break;
            }
            case SUNDIALS_BAND:
            {
                for( int i = 0; i < M; i++ )
                    cols[i][smu] += 1.0;
                break;
            }
        }

    }

    /*
     * -----------------------------------------------------------------
     * Function : SetToZero
     * -----------------------------------------------------------------
     * SetToZero sets all the elements of the M-by-N matrix A to 0.0.
     * -----------------------------------------------------------------
     */
    public void setToZero()
    {
        switch( type )
        {
            case SUNDIALS_DENSE:
            {
                for( int j = 0; j < N; j++ )
                {
                    for( int i = 0; i < M; i++ )
                        cols[j][i] = 0.0;
                }
                break;
            }
            case SUNDIALS_BAND:
            {
                int colSize = mu + ml + 1;
                for( int j = 0; j < M; j++ )
                {
                    for( int i = 0; i < colSize; i++ )
                        cols[j][i + smu - mu] = 0.0;
                }
                break;
            }
        }

    }

    public void scale(double c)
    {
        switch( type )
        {
            case SUNDIALS_DENSE:
            {
                denseScale(c, cols, M, N);
                break;
            }
            case SUNDIALS_BAND:
            {
                bandScale(c, cols, N, mu, ml, smu);
                break;
            }
        }
    }


    public static void denseScale(double c, double[][] a, int m, int n)
    {
        {
            for( int j = 0; j < n; j++ )
            {
                for( int i = 0; i < m; i++ )
                {
                    a[j][i] *= c;
                }
            }
        }
    }

    static void bandScale(double c, double[][] a, int n, int mu, int ml, int smu)
    {
        int colSize = mu + ml + 1;

        for( int j = 0; j < n; j++ )
        {
            for( int i = 0; i < colSize; i++ )
            {
                a[j][i + smu - mu] *= c;
            }
        }
    }

    /*
     * -----------------------------------------------------------------
     * Function : DenseCopy
     * -----------------------------------------------------------------
     * DenseCopy copies the contents of the M-by-N matrix A into the
     * M-by-N matrix B.
     * 
     * DenseCopy is a wrapper around denseCopy which accesses the data
     * in the DlsMat A and B (i.e. the fields cols)
     * -----------------------------------------------------------------
     */
    public void denseCopy(Matrix b)
    {

        denseCopy(cols, b.cols, M, N);
    }

    public void bandCopy(Matrix b, int copymu, int copyml)
    {

        bandCopy(cols, b.cols, N, smu, b.smu, copymu, copyml);
    }

    public static void denseCopy(double[][] a, double[][] b, int m, int n)
    {
        for( int j = 0; j < n; j++ )
        {
            for( int i = 0; i < m; i++ )
            {
                b[j][i] = a[j][i];
            }
        }

    }

    static void bandCopy(double[][] a, double[][] b, int n, int asmu, int bsmu, int copymu, int copyml)
    {
        int copySize = copymu + copyml + 1;
        for( int j = 0; j < n; j++ )
        {
            for( int i = 0; i < copySize; i++ )
            {
                b[j][i + bsmu - copymu] = a[j][i + asmu - copymu];
            }
        }
    }


    /*
     * -----------------------------------------------------------------
     * Functions: PrintMat
     * -----------------------------------------------------------------
     * This function prints the M-by-N (dense or band) matrix A to
     * standard output as it would normally appear on paper.
     * It is intended as debugging tools with small values of M and N.
     * The elements are printed using the %g/%lg/%Lg option.
     * A blank line is printed before and after the matrix.
     * -----------------------------------------------------------------
     */
    @Override
    public String toString()
    {
        int start, finish;

        StringBuffer sb = new StringBuffer("\n");
        switch( type )
        {

            case SUNDIALS_DENSE:
            {
                for( int i = 0; i < M; i++ )
                {
                    for( int j = 0; j < N; j++ )
                    {
                        sb.append(getDenseElement(i, j));
                        sb.append(" ");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
                break;

            case SUNDIALS_BAND:
            {
                for( int i = 0; i < N; i++ )
                {
                    start = Math.max(0, i - ml);
                    finish = Math.min(N - 1, i + mu);
                    for( int j = 0; j < start; j++ )
                        sb.append(" ");
                    for( int j = start; j <= finish; j++ )
                    {

                        sb.append(getBandElement(i, j));
                        sb.append(" ");
                    }
                    sb.append("\n");
                }
                sb.append("\n");

                break;
            }
        }
        return sb.toString();
    }
}