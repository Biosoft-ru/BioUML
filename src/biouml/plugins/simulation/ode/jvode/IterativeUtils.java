package biouml.plugins.simulation.ode.jvode;

public class IterativeUtils
{
    public final static int PREC_NONE = 0; //The iterative linear solver should not use preconditioning.
    public final static int PREC_LEFT = 1; //The iterative linear solver uses preconditioning on the left only.
    public final static int PREC_RIGHT = 2; //The iterative linear solver uses preconditioning on the right only.
    public final static int PREC_BOTH = 3; //The iterative linear solver uses preconditioning on both the left and the right.

    public final static int MODIFIED_GS = 1; //The iterative solver uses the modified Gram-Schmidt routine ModifiedGS listed in this file.
    public final static int CLASSICAL_GS = 2; //The iterative solver uses the classical Gram-Schmidt routine ClassicalGS listed in this file.


    /**
     * ModifiedGS performs a modified Gram-Schmidt orthogonalization
     * of the N_Vector v[k] against the p unit N_Vectors at
     * v[k-1], v[k-2], ..., v[k-p].
     * 
     * v is an array of (k+1) N_Vectors v[i], i=0, 1, ..., k.
     * v[k-1], v[k-2], ..., v[k-p] are assumed to have L2-norm
     * equal to 1.
     * 
     * h is the output k by k Hessenberg matrix of inner products.
     * This matrix must be allocated row-wise so that the (i,j)th
     * entry is h[i][j]. The inner products (v[i],v[k]),
     * i=i0, i0+1, ..., k-1, are stored at h[i][k-1]. Here
     * i0=MAX(0,k-p).
     * 
     * k is the index of the vector in the v array that needs to be
     * orthogonalized against previous vectors in the v array.
     * 
     * p is the number of previous vectors in the v array against
     * which v[k] is to be orthogonalized.
     * 
     * new_vk_norm is a pointer to memory allocated by the caller to
     * hold the Euclidean norm of the orthogonalized vector v[k].
     * 
     * If (k-p) < 0, then ModifiedGS uses p=k. The orthogonalized
     * v[k] is NOT normalized and is stored over the old v[k]. Once
     * the orthogonalization has been performed, the Euclidean norm
     * of v[k] is stored in (*new_vk_norm).
     * 
     * ModifiedGS returns 0 to indicate success. It cannot fail.
     * -----------------------------------------------------------------
     */
    public static double ModifiedGS(double[][] v, double[][] h, int k, int p)
    {

        double vk_norm = Math.sqrt(VectorUtils.dotProd(v[k], v[k]));
        int k_minus_1 = k - 1;
        int i0 = Math.max(k - p, 0);

        /* Perform modified Gram-Schmidt */

        for( int i = i0; i < k; i++ )
        {
            h[i][k_minus_1] = VectorUtils.dotProd(v[i], v[k]);
            VectorUtils.linearSum( -h[i][k_minus_1], v[i], v[k]);
        }

        /* Compute the norm of the new vector at v[k] */

        double new_vk_norm = Math.sqrt(VectorUtils.dotProd(v[k], v[k]));

        /* If the norm of the new vector at v[k] is less than
           FACTOR (== 1000) times unit roundoff times the norm of the
           input vector v[k], then the vector will be reorthogonalized
           in order to ensure that nonorthogonality is not being masked
           by a very small vector length. */

        double temp = 1000 * vk_norm;
        if( ( temp + new_vk_norm ) != temp )
            return 0;

        double new_norm_2 = 0;
        double new_product;
        for( int i = i0; i < k; i++ )
        {
            new_product = VectorUtils.dotProd(v[i], v[k]);
            temp = 1000 * h[i][k_minus_1];
            if( ( temp + new_product ) == temp )
                continue;
            h[i][k_minus_1] += new_product;
            VectorUtils.linearSum(-new_product, v[i], v[k]);
            new_norm_2 += Math.pow(new_product, 2);
        }

        if( new_norm_2 != 0 )
        {
            new_product = Math.pow(new_vk_norm, 2) - new_norm_2;
            new_vk_norm = ( new_product > 0 ) ? Math.sqrt(new_product) : 0;
        }

        return new_vk_norm;
    }

    /**
     * ClassicalGS performs a classical Gram-Schmidt
     * orthogonalization of the N_Vector v[k] against the p unit
     * N_Vectors at v[k-1], v[k-2], ..., v[k-p]. The parameters v, h,
     * k, p, and new_vk_norm are as described in the documentation
     * for ModifiedGS.
     * 
     * temp is an N_Vector which can be used as workspace by the
     * ClassicalGS routine.
     * 
     * s is a length k array of realtype which can be used as
     * workspace by the ClassicalGS routine.
     *
     * ClassicalGS returns 0 to indicate success. It cannot fail.
     * -----------------------------------------------------------------
     */
    public static double ClassicalGS(double[][] v, double[][] h, int k, int p, double[] temp, double[] s)
    {
        int k_minus_1 = k - 1;

        /* Perform Classical Gram-Schmidt */
        double vk_norm = Math.sqrt(VectorUtils.dotProd(v[k], v[k]));

        int i0 = Math.max(k - p, 0);
        for( int i = i0; i < k; i++ )
        {
            h[i][k_minus_1] = VectorUtils.dotProd(v[i], v[k]);
        }
        for( int i = i0; i < k; i++ )
        {
            VectorUtils.linearSum(-h[i][k_minus_1], v[i], v[k]);
        }

        /* Compute the norm of the new vector at v[k] */
        double new_vk_norm = Math.sqrt(VectorUtils.dotProd(v[k], v[k]));

        /* Reorthogonalize if necessary */
        if( ( 1000 * new_vk_norm ) < vk_norm )
        {
            for( int i = i0; i < k; i++ )
            {
                s[i] = VectorUtils.dotProd(v[i], v[k]);
            }
            if( i0 < k )
            {
                VectorUtils.scale(s[i0], v[i0], temp);
                h[i0][k_minus_1] += s[i0];
            }
            for( int i = i0 + 1; i < k; i++ )
            {
                VectorUtils.linearSum(s[i], v[i], temp);
                h[i][k_minus_1] += s[i];
            }
            VectorUtils.linearDiff(v[k], temp, v[k]);

            new_vk_norm = Math.sqrt(VectorUtils.dotProd(v[k], v[k]));
        }
        return new_vk_norm;
    }

    /**
     * QRfact performs a QR factorization of the Hessenberg matrix H.
     * 
     * n is the problem size; the matrix H is (n+1) by n.
     * 
     * h is the (n+1) by n Hessenberg matrix H to be factored. It is
     * stored row-wise.
     * 
     * q is an array of length 2*n containing the Givens rotations
     * computed by this function. A Givens rotation has the form:
     * | c  -s |
     * | s   c |.
     * The components of the Givens rotations are stored in q as
     * (c, s, c, s, ..., c, s).
     * 
     * job is a control flag. If job==0, then a new QR factorization
     * is performed. If job!=0, then it is assumed that the first
     * n-1 columns of h have already been factored and only the last
     * column needs to be updated.
     * 
     * QRfact returns 0 if successful. If a zero is encountered on
     * the diagonal of the triangular factor R, then QRfact returns
     * the equation number of the zero entry, where the equations are
     * numbered from 1, not 0. If QRsol is subsequently called in
     * this situation, it will return an error because it could not
     * divide by the zero diagonal entry.
     * -----------------------------------------------------------------
     */
    public static int QRfact(int n, double[][] h, double[] q, int job)
    {
        double c, s, temp1, temp2, temp3;
        int code = 0;
        switch( job )
        {
            case 0:
                /* Compute a new factorization of H */
                code = 0;
                for( int k = 0; k < n; k++ )
                {
                    /* Multiply column k by the previous k-1 Givens rotations */
                    for( int j = 0; j < k - 1; j++ )
                    {
                        int i = 2 * j;
                        temp1 = h[j][k];
                        temp2 = h[j + 1][k];
                        c = q[i];
                        s = q[i + 1];
                        h[j][k] = c * temp1 - s * temp2;
                        h[j + 1][k] = s * temp1 + c * temp2;
                    }
                    /* Compute the Givens rotation components c and s */
                    int q_ptr = 2 * k;
                    temp1 = h[k][k];
                    temp2 = h[k + 1][k];
                    if( temp2 == 0 )
                    {
                        c = 1;
                        s = 0;
                    }
                    else if( Math.abs(temp2) >= Math.abs(temp1) )
                    {
                        temp3 = temp1 / temp2;
                        s = -1.0 / Math.sqrt(1 + temp3*temp3);
                        c = -s * temp3;
                    }
                    else
                    {
                        temp3 = temp2 / temp1;
                        c = 1.0 / Math.sqrt(1 + temp3*temp3);
                        s = -c * temp3;
                    }
                    q[q_ptr] = c;
                    q[q_ptr + 1] = s;
                    if( ( h[k][k] = c * temp1 - s * temp2 ) == 0 )
                        code = k + 1;
                }
                break;
            default:
                /* Update the factored H to which a new column has been added */
                int n_minus_1 = n - 1;
                code = 0;
                /* Multiply the new column by the previous n-1 Givens rotations */
                for( int k = 0; k < n_minus_1; k++ )
                {
                    int i = 2 * k;
                    temp1 = h[k][n_minus_1];
                    temp2 = h[k + 1][n_minus_1];
                    c = q[i];
                    s = q[i + 1];
                    h[k][n_minus_1] = c * temp1 - s * temp2;
                    h[k + 1][n_minus_1] = s * temp1 + c * temp2;
                }

                /* Compute new Givens rotation and multiply it times the last two
                   entries in the new column of H.  Note that the second entry of
                   this product will be 0, so it is not necessary to compute it. */

                temp1 = h[n_minus_1][n_minus_1];
                temp2 = h[n][n_minus_1];
                if( temp2 == 0 )
                {
                    c = 1;
                    s = 0;
                }
                else if( Math.abs(temp2) >= Math.abs(temp1) )
                {
                    temp3 = temp1 / temp2;
                    s = -1.0 / Math.sqrt(1 +temp3*temp3);
                    c = -s * temp3;
                }
                else
                {
                    temp3 = temp2 / temp1;
                    c = 1.0 / Math.sqrt(1 + temp3*temp3);
                    s = -c * temp3;
                }
                int q_ptr = 2 * n_minus_1;
                q[q_ptr] = c;
                q[q_ptr + 1] = s;
                if( ( h[n_minus_1][n_minus_1] = c * temp1 - s * temp2 ) == 0 )
                    code = n;
        }

        return code;
    }

    /**
     * This implementation of QRsol is a slight modification of a
     * previous routine (called qrsol) written by Milo Dorr.
     * -----------------------------------------------------------------
     */
    public static int QRsol(int n, double[][] h, double[] q, double[] b)
    {
        double c, s, temp1, temp2;
        int q_ptr, code = 0;
        /* Compute Q*b */
        for(int k = 0; k < n; k++ )
        {
            q_ptr = 2 * k;
            c = q[q_ptr];
            s = q[q_ptr + 1];
            temp1 = b[k];
            temp2 = b[k + 1];
            b[k] = c * temp1 - s * temp2;
            b[k + 1] = s * temp1 + c * temp2;
        }
        /* Solve  R*x = Q*b */
        for(int k = n - 1; k >= 0; k-- )
        {
            if( h[k][k] == 0 )
            {
                code = k + 1;
                break;
            }
            b[k] /= h[k][k];
            for(int i = 0; i < k; i++ )
                b[i] -= b[k] * h[i][k];
        }
        return ( code );
    }

}
