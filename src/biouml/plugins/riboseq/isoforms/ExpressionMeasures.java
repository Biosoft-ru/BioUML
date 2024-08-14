package biouml.plugins.riboseq.isoforms;

public class ExpressionMeasures
{
    public static double[] calcTPM(double[] theta, LenDist lenDist, TranscriptSequence[] transcripts)
    {
        double[] tpm = new double[theta.length - 1];
        double sum = 0;
        for(int i = 0; i < tpm.length; i++)
        {
            double effLen = lenDist.calcEffectiveLength( transcripts[i].seq.length );
            sum += (tpm[i] = theta[i+1] / effLen);
        }
        for(int i = 0; i < tpm.length; i++)
            tpm[i] = tpm[i] * 1e6 / sum;
        return tpm;
    }
}
