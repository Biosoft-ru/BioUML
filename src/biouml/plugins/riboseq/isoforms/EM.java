package biouml.plugins.riboseq.isoforms;

import java.util.Arrays;
import biouml.plugins.riboseq.isoforms.Read.Type;
import gnu.trove.list.array.TDoubleArrayList;

public class EM
{
    private static final double NOISE_FRACTION = 1e-8;

    public EM(HitContainer hits, ReadContainer readContainer, TranscriptSequence[] transcripts, boolean strandSpecific)
    {
        this.hits = hits;
        this.readContainer = readContainer;
        this.transcripts = transcripts;
        this.strandSpecific = strandSpecific;
        
        noiseConProb = new double[hits.getReadCount()];
        hitConProb = new double[hits.getHitCount()];
    }

    private TranscriptSequence[] transcripts;
    private boolean strandSpecific;
    
    private HitContainer hits;
    
    private ReadContainer readContainer;

    private double maxAbsErr = 1e-18;
    public double getMaxAbsErr() { return maxAbsErr; }
    public void setMaxAbsErr(double maxAbsErr) { this.maxAbsErr = maxAbsErr; }

    private double[] hitConProb;
    public double[] getHitConProb() { return hitConProb; }
    
    private double[] noiseConProb;
    public double[] getNoiseConProb() { return noiseConProb; }

    private SingleModel model;
    public SingleModel getModel() { return model; }   
    
    private double[] theta;
    public double[] getTheta() { return theta; }
    
    private boolean converged;
    public boolean isConverged() { return converged; }
    
    private int roundsModelUpdate = 10;
    public int getRoundsModelUpdate()
    {
        return roundsModelUpdate;
    }
    public void setRoundsModelUpdate(int roundsModelUpdate)
    {
        this.roundsModelUpdate = roundsModelUpdate;
    }
    
    private int minRound = 20;
    public int getMinRound()
    {
        return minRound;
    }
    public void setMinRound(int minRound)
    {
        this.minRound = minRound;
    }

    private int maxRound = 10000;
    public int getMaxRound()
    {
        return maxRound;
    }
    public void setMaxRound(int maxRound)
    {
        this.maxRound = maxRound;
    }

    public void run()
    {
        ModelParameters modelParameters = new ModelParameters();
        modelParameters.minL = readContainer.getMinReadLen();
        modelParameters.maxL = readContainer.getMaxReadLen();
        modelParameters.transcripts = transcripts;
        modelParameters.probF = strandSpecific ? 1 : 0.5;
        model = new SingleModel(modelParameters);
        model.estimateFromReads(readContainer);
        
        initTheta();
        double[] probv = new double[transcripts.length + 1];
        double[] countv = new double[transcripts.length + 1];

        int round;
        for( round = 1; round <= maxRound; round++ )
        {
            boolean updateModel = round <= roundsModelUpdate;
            
            for( int i = 0; i <= transcripts.length; i++ )
                probv[i] = theta[i];
            
            SingleModel modelHelper = new SingleModel(modelParameters, false);
            eStep(countv, model, updateModel, modelHelper, readContainer.getReader( Type.MAPPED ));
            model.setNeedCalConProb( false );
            if(updateModel)
            {
                model.init();
                model.collect(modelHelper);
                model.finish();
            }
            
            countv[0] += readContainer.getUnmappedCount();
            double sum = 0;
            for(double c : countv) sum += c;

            //M step
            for(int i = 0; i <= transcripts.length; i++)
                theta[i] = countv[i] / sum;
            
            if( round > minRound )
            {
                converged = true;
                for( int i = 0; i <= transcripts.length; i++ )
                {
                    double absErr = Math.abs( probv[i] - theta[i] );
                    if( absErr > maxAbsErr )
                    {
                        converged = false;
                        break;
                    }
                }
                if( converged )
                    break;
            }
        }
    }

    private void eStep(double[] countv, SingleModel model, boolean updateModel, SingleModel modelHelper, ReadReader<Read> readReader)
    {
        if(updateModel)
            modelHelper.init();
        if(model.isNeedCalConProb() || updateModel)
            readReader.reset();
        Arrays.fill( countv, 0 );
        TDoubleArrayList fracs = new TDoubleArrayList( 100 );
        Hit hit = new Hit();
        for(int readId = 0; readId < hits.getReadCount(); readId++)
        {
            Read read = null;
            if(model.isNeedCalConProb() || updateModel)
                read = readReader.read();
            
            fracs.reset();
            
            if(model.isNeedCalConProb())
                noiseConProb[readId] = model.getNoiseConProb(read);
            
            double frac = theta[0] * noiseConProb[readId]; 
            if(frac < Const.EPSILON)
                frac = 0;
            fracs.add( frac );
            double sum = frac;

            int from = hits.getReadBucketFrom( readId );
            int to = hits.getReadBucketTo( readId );
            for(int hitId = from; hitId < to; hitId++)
            {
                hits.getHitAt( hitId, hit );
                if(model.isNeedCalConProb())
                    hitConProb[hitId] = model.getConProb(read, hit);
                
                frac = theta[hit.getTranscriptId()+1] * hitConProb[hitId];
                if(frac < Const.EPSILON)
                    frac = 0;
                fracs.add(frac);
                sum += frac;
            }
            
            if(sum > Const.EPSILON)
            {
                frac = fracs.get( 0 ) / sum;
                countv[0] += frac;
                if(updateModel)
                    modelHelper.updateNoise(read, frac);
                for(int hitId = from; hitId < to; hitId++)
                {
                    hits.getHitAt( hitId, hit );
                    int id = hitId - from + 1;
                    frac = fracs.get( id ) / sum;
                    countv[hit.getTranscriptId()+1] += frac;
                    if(updateModel)
                        modelHelper.update( read, hit, frac);
                }
            }
        }
        if(updateModel)
            modelHelper.finish();
    }

    private void initTheta()
    {
        theta = new double[transcripts.length + 1];
        theta[0] = readContainer.getUnmappedCount() / ((double)readContainer.getUnmappedCount() + readContainer.getMappedCount());
        theta[0] = Math.max( theta[0], NOISE_FRACTION );
        double t = (1-theta[0])/transcripts.length;
        for(int i = 1; i <= transcripts.length; i++)
            theta[i] = t;
    }
    
}
