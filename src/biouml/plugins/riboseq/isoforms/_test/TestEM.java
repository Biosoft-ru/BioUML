package biouml.plugins.riboseq.isoforms._test;

import java.util.Arrays;

import biouml.plugins.riboseq.isoforms.EM;
import biouml.plugins.riboseq.isoforms.ExpressionMeasures;
import biouml.plugins.riboseq.isoforms.Gibbs;
import biouml.plugins.riboseq.isoforms.HitContainer;
import biouml.plugins.riboseq.isoforms.Read;
import biouml.plugins.riboseq.isoforms.ReadContainer;
import biouml.plugins.riboseq.isoforms.ReadReader;
import biouml.plugins.riboseq.isoforms.ReadsInMemory;
import biouml.plugins.riboseq.isoforms.Hit;
import biouml.plugins.riboseq.isoforms.TranscriptSequence;
import junit.framework.TestCase;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;

public class TestEM extends TestCase
{
    public void test1()
    {
        Read r1 = new Read( "ACGTG" );
        Read r2 = new Read( "ACGTT" );
        ReadReader<Read> mapped = new ReadsInMemory<>( new Read[] {r1, r2} );
        ReadContainer readContainer = new ReadContainer( mapped, ReadReader.EMPTY, ReadReader.EMPTY );
        
        HitContainer hits = new HitContainer();
        hits.addHit( new Hit( 0, 0, true ) );
        hits.addHit( new Hit( 5, 0, true ) );
        hits.finishReadBucket();
        hits.addHit( new Hit( 10, 0, true ) );
        hits.finishReadBucket();
        
        
        TranscriptSequence t1 = new TranscriptSequence("T1", lettersToCodes( "ACGTGACGTGACGTT" ) );
        TranscriptSequence[] transcripts = new TranscriptSequence[]{t1};
        
        EM em = new EM( hits, readContainer, transcripts, true );
        em.run();
        assertTrue( em.isConverged() );
        double[] theta = em.getTheta();
        assertEquals( 2, theta.length );
        assertCloseValues( 0, theta[0], 1e-18 );
        assertCloseValues( 1.0d, theta[1], 1e-18 );
        double[] tpm = ExpressionMeasures.calcTPM( theta, em.getModel().getLenDist(), transcripts );
        assertEquals(1, tpm.length);
        assertEquals( 1e6, tpm[0] );
        
        Gibbs gibbs = new Gibbs( hits, transcripts.length, 0, em.getHitConProb(), em.getNoiseConProb() );
        gibbs.setSeed( 1 );
        double[][] samples = gibbs.sampleTheta( 2 );
        assertEquals( 2, samples.length );
        assertEquals( 1.0, samples[0][0]+samples[0][1] );
        assertEquals( 1.0, samples[1][0]+samples[1][1] );
    }
    
    public void test2()
    {
        Read r1 = new Read( "ACGTG" );
        Read r2 = new Read( "ACGTT" );
        Read r3 = new Read( "ACGTA" );
        ReadReader<Read> mapped = new ReadsInMemory<>( new Read[] {r1, r2, r3} );
        ReadContainer readContainer = new ReadContainer( mapped, ReadReader.EMPTY, ReadReader.EMPTY );
        
        HitContainer hits = new HitContainer();
        hits.addHit( new Hit( 0, 0, true ) );
        hits.finishReadBucket();
        hits.addHit( new Hit( 5, 0, true ) );
        hits.finishReadBucket();
        hits.addHit( new Hit( 0, 1, true ) );
        hits.finishReadBucket();
        
        TranscriptSequence t1 = new TranscriptSequence("T1", lettersToCodes( "ACGTGACGTT" ) );
        TranscriptSequence t2 = new TranscriptSequence("T2", lettersToCodes( "ACGTA" ));
        TranscriptSequence[] transcripts = new TranscriptSequence[]{t1, t2};
        
        EM em = new EM( hits, readContainer, transcripts, true );
        em.run();
        assertTrue( em.isConverged() );
        double[] theta = em.getTheta();
        assertEquals( 3, theta.length );
        
        assertCloseValues( 0, theta[0], 1e-18 );
        assertCloseValues( 2.0/3, theta[1], 1e-18 );
        assertCloseValues( 1.0/3, theta[2], 1e-18 );
        
        double[] tpm = ExpressionMeasures.calcTPM( theta, em.getModel().getLenDist(), transcripts );
        assertEquals( 2, tpm.length );
        assertEquals( 250000.0, tpm[0] );
        assertEquals( 750000.0, tpm[1] );
    }
    
    public void test3()
    {
        Read r1 = new Read( "ACGTG" );
        ReadReader<Read> mapped = new ReadsInMemory<>( new Read[] {r1} );
        ReadContainer readContainer = new ReadContainer( mapped, ReadReader.EMPTY, ReadReader.EMPTY );

        TranscriptSequence t1 = new TranscriptSequence("T1", lettersToCodes( "ACGTGC" ) );
        TranscriptSequence t2 = new TranscriptSequence("T2", lettersToCodes( "ACGTG" ));
        TranscriptSequence[] transcripts = new TranscriptSequence[]{t1, t2};

        HitContainer hits = new HitContainer();
        hits.addHit( new Hit( 0, 0, true ) );
        hits.addHit( new Hit( 0, 1, true ) );
        hits.finishReadBucket();
        
        EM em = new EM( hits, readContainer, transcripts, true );
        em.run();
        assertTrue( em.isConverged() );
        double[] theta = em.getTheta();
        assertEquals( 3, theta.length );
        
        assertCloseValues(0, theta[0], 1e-18);
        assertCloseValues(0, theta[1], 1e-18);
        assertCloseValues(1, theta[2], 1e-18);
        double[] tpm = ExpressionMeasures.calcTPM( theta, em.getModel().getLenDist(), transcripts );
        assertEquals( 2, tpm.length );
        assertCloseValues( 0, tpm[0], 1e-12 );
        assertEquals( 1e6, tpm[1] );
    }
    
    public void testGibbsTE()
    {
        TranscriptSequence t1 = new TranscriptSequence("T1", lettersToCodes( "ACGTGACGTT" ) );
        TranscriptSequence t2 = new TranscriptSequence("T2", lettersToCodes( "ACGTA" ));
        TranscriptSequence[] transcripts = new TranscriptSequence[]{t1, t2};

        Read mrnaR1 = new Read( "ACGTG" );
        Read mrnaR2 = new Read( "ACGTT" );
        Read mrnaR3 = new Read( "ACGTA" );
        ReadReader<Read> mapped = new ReadsInMemory<>( new Read[] {mrnaR1, mrnaR2, mrnaR3} );
        ReadContainer readContainer = new ReadContainer( mapped, ReadReader.EMPTY, ReadReader.EMPTY );
        
        HitContainer mrnaHits = new HitContainer();
        mrnaHits.addHit( new Hit( 0, 0, true ) );
        mrnaHits.finishReadBucket();
        mrnaHits.addHit( new Hit( 5, 0, true ) );
        mrnaHits.finishReadBucket();
        mrnaHits.addHit( new Hit( 0, 1, true ) );
        mrnaHits.finishReadBucket();
        
        EM em = new EM( mrnaHits, readContainer, transcripts, true );
        em.run();
        assertTrue( em.isConverged() );
        em.getTheta();
        
        int nSamples = 50000;
        
        Gibbs gibbs = new Gibbs( mrnaHits, transcripts.length, 0, em.getHitConProb(), em.getNoiseConProb() );
        //gibbs.setSeed( 1 );
        double[][] mRNAThetaSamples = gibbs.sampleTheta( nSamples );
        double[][] mRNATPMSamples = new double[nSamples][];
        for(int i = 0; i < nSamples; i++)
            mRNATPMSamples[i] = ExpressionMeasures.calcTPM( mRNAThetaSamples[i], em.getModel().getLenDist(), transcripts );

        TranscriptSequence cds1 = new TranscriptSequence( "CDS1", lettersToCodes( "ACGTG" ) );
        TranscriptSequence cds2 = new TranscriptSequence( "CDS2", lettersToCodes( "ACGTA" ) );
        TranscriptSequence[] cdsSet = new TranscriptSequence[]{cds1, cds2};
        
        Read riboR1 = new Read("ACGTG");
        Read riboR2 = new Read("ACGTA");
        ReadReader<Read> riboMapped = new ReadsInMemory<>( new Read[] {riboR1, riboR2} );
        ReadContainer riboReadContainer = new ReadContainer( riboMapped, ReadReader.EMPTY, ReadReader.EMPTY );
        
        HitContainer riboHits = new HitContainer();
        riboHits.addHit( new Hit(0, 0, true) );
        riboHits.finishReadBucket();
        riboHits.addHit( new Hit(0, 1, true) );
        riboHits.finishReadBucket();

        em = new EM( riboHits, riboReadContainer, cdsSet, true );
        em.run();
        assertTrue( em.isConverged() );
        em.getTheta();
        
        gibbs = new Gibbs( riboHits, cdsSet.length, 0, em.getHitConProb(), em.getNoiseConProb() );
        gibbs.setSeed( 1 );
        double[][] riboThetaSamples = gibbs.sampleTheta( nSamples );
        double[][] riboTPMSamples = new double[nSamples][];
        for(int i = 0; i < nSamples; i++)
            riboTPMSamples[i] = ExpressionMeasures.calcTPM( riboThetaSamples[i], em.getModel().getLenDist(), cdsSet );

        double[][] teSamples = new double[transcripts.length][nSamples];
        for(int i = 0; i < nSamples; i++)
        {
            double sum = 0;
            for(int j = 0; j < transcripts.length; j++)
            {
                teSamples[j][i] = riboTPMSamples[i][j] / mRNATPMSamples[i][j];
                sum += teSamples[j][i];
            }
            for(int j = 0; j < transcripts.length; j++)
                teSamples[j][i] /= sum;
        }

        
        for(int i = 0; i < transcripts.length; i++)
        {
            Arrays.sort( teSamples[i] );
            double median = teSamples[i][nSamples / 2];
            double lb95 = teSamples[i][nSamples*25/1000];
            double ub95 = teSamples[i][nSamples*975/1000];
            System.out.println( median + " " + lb95 + " " + ub95 );
        }
        
    }
    
    public void testGibbs()
    {
        Read r1 = new Read( "AAAAA" );
        Read r2 = new Read( "CCCCC" );
        ReadReader<Read> mapped = new ReadsInMemory<>( new Read[] {r1, r2} );
        ReadContainer readContainer = new ReadContainer( mapped, ReadReader.EMPTY, ReadReader.EMPTY );
        
        HitContainer hits = new HitContainer();
        hits.addHit( new Hit( 0, 0, true ) );
        hits.finishReadBucket();
        hits.addHit( new Hit( 0, 1, true ) );
        hits.finishReadBucket();
        
        
        TranscriptSequence t1 = new TranscriptSequence("T1", lettersToCodes( "AAAAA" ) );
        TranscriptSequence t2 = new TranscriptSequence("T2", lettersToCodes( "CCCCC" ) );
        TranscriptSequence[] transcripts = new TranscriptSequence[]{t1, t2};
        
        EM em = new EM( hits, readContainer, transcripts, true );
        em.run();
        assertTrue( em.isConverged() );
        double[] theta = em.getTheta();
        assertEquals( 3, theta.length );
        assertCloseValues( 0.0, theta[0], 1e-18 );
        assertCloseValues( 0.5, theta[1], 1e-18 );
        assertCloseValues( 0.5, theta[2], 1e-18 );
        double[] tpm = ExpressionMeasures.calcTPM( theta, em.getModel().getLenDist(), transcripts );
        assertEquals(2, tpm.length);
        assertEquals( 5e5, tpm[0] );
        assertEquals( 5e5, tpm[1] );
        
        Gibbs gibbs = new Gibbs( hits, transcripts.length, 0, em.getHitConProb(), em.getNoiseConProb() );
        gibbs.setSeed( 0 );
        int nSamples = 50000;
        double[][] samples = gibbs.sampleTheta( nSamples );

        double[][] tpmSamples = new double[transcripts.length][nSamples];
        for( int i = 0; i < nSamples; i++ )
        {
            double[] curTpm = ExpressionMeasures.calcTPM( samples[i], em.getModel().getLenDist(), transcripts );
            for(int j = 0; j < transcripts.length; j++)
                tpmSamples[j][i] = curTpm[j];
        }
        
        for(int i = 0; i < transcripts.length; i++)
        {
            Arrays.sort( tpmSamples[i] );
            double pme = 0;
            for(int j = 0; j < nSamples; j++)
                pme += tpmSamples[i][j];
            pme /= nSamples;
            double median = tpmSamples[i][nSamples / 2];
            double lb95 = tpmSamples[i][nSamples*25/1000];
            double ub95 = tpmSamples[i][nSamples*975/1000];
            if( i == 0 )
            {
                assertEquals( 498544.06860307633, pme );
                assertEquals( 497088.7103407975, median );
                assertEquals( 85732.48596812267, lb95 );
                assertEquals( 912700.808056087, ub95 );
            }
            else if( i == 1 )
            {
                assertEquals( 501455.93139692914, pme );
                assertEquals( 502911.5808415342, median );
                assertEquals( 87361.05788889165, lb95 );
                assertEquals( 914293.9138132662, ub95 );
            }
        }
    }
    
    private static byte[] lettersToCodes(String str)
    {
        byte[] l2c = Nucleotide5LetterAlphabet.getInstance().letterToCodeMatrix();
        byte[] res = new byte[str.length()];
        for(int i = 0; i < str.length(); i++)
            res[i] = l2c[str.charAt( i )]; 
        return res;
    }
    
    private void assertCloseValues(double x, double y, double absTol)
    {
        assertTrue( Math.abs( x-y ) <= absTol );
    }
}

