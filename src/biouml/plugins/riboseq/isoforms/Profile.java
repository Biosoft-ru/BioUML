package biouml.plugins.riboseq.isoforms;

import ru.biosoft.bsa.Nucleotide5LetterAlphabet;

public class Profile
{
    double[][][] m;

    public Profile(int maxReadLength)
    {
        m = new double[maxReadLength][Const.ALPHABET_SIZE][Const.ALPHABET_SIZE];

        final int N = Const.ALPHABET_SIZE - 1;
        double probN = 1e-5, portionC = 0.99; //portionC, among ACGT, the portion of probability mass the correct base takes   
        double probC, probO;

        for( int i = 0; i < maxReadLength; i++ )
        {
            for( int j = 0; j < N; j++ )
            {
                m[i][j][N] = probN;
                probC = portionC * ( 1.0 - probN );
                probO = ( 1.0 - portionC ) / ( N - 1 ) * ( 1.0 - probN );
                for( int k = 0; k < N; k++ )
                    m[i][j][k] = ( j == k ? probC : probO );
            }
            m[i][N][N] = probN;
            for( int k = 0; k < N; k++ )
                m[i][N][k] = ( 1.0 - probN ) / N;//why m[i] not symmetric? and rows didn't sum to 1
        }

    }

    public double getProb(byte[] readSeq, byte[] refSeq, int pos, boolean forward)
    {
        double prob = 1;
        for(int readPos = 0; readPos < readSeq.length; readPos++)
            prob *= m[readPos][getCodeAt(refSeq, pos, forward, readPos)][readSeq[readPos]];
        return prob;
    }

    public void init()
    {
        for( int i = 0; i < m.length; i++ )
            for( int j = 0; j < m[0].length; j++ )
                for( int k = 0; k < m[0][0].length; k++ )
                    m[i][j][k] = 0;
    }

    //readSeq and refSeq in codes, pos zero based offset to 5' read end
    //when !forward readSeq is reverse complemented
    public void update(byte[] readSeq, byte[] refSeq, int pos, boolean forward, double prob)
    {
        for(int readPos = 0; readPos < readSeq.length; readPos++)
            m[readPos][getCodeAt(refSeq, pos, forward, readPos)][readSeq[readPos]] += prob;
    }
    
    private byte getCodeAt(byte[] ref, int pos5prime, boolean forward, int offset)
    {
        if(forward)
            return ref[pos5prime + offset];
        
        byte[] complement = Nucleotide5LetterAlphabet.getInstance().codeComplementMatrix();
        return complement[ref[pos5prime - offset]];
    }
    
    public void collect(Profile p)
    {
        for( int i = 0; i < m.length; i++ )
            for( int j = 0; j < m[0].length; j++ )
                for( int k = 0; k < m[0][0].length; k++ )
                    m[i][j][k] += p.m[i][j][k];
    }

    public void finish()
    {
        for( int i = 0; i < m.length; i++ )
        {
            for( int j = 0; j < Const.ALPHABET_SIZE; j++ )
            {
                double sum = 0.0;
                for( int k = 0; k < Const.ALPHABET_SIZE; k++ )
                    sum += m[i][j][k];
                if( sum < Const.EPSILON )
                {
                    for( int k = 0; k < Const.ALPHABET_SIZE; k++ )
                        m[i][j][k] = 0.0;
                    continue;
                }
                for( int k = 0; k < Const.ALPHABET_SIZE; k++ )
                    m[i][j][k] /= sum;
            }
        }

    }
}
