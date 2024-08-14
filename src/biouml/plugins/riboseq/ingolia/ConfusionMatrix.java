package biouml.plugins.riboseq.ingolia;

public class ConfusionMatrix
{
    private int tp,fp,tn,fn;
    
    public ConfusionMatrix(int tp, int fp, int tn, int fn)
    {
        this.tp = tp;
        this.fp = fp;
        this.tn = tn;
        this.fn = fn;
    }
    
    public ConfusionMatrix(boolean[] expected, boolean[] predicted)
    {
        if( expected.length != predicted.length )
            throw new IllegalArgumentException();
        for( int i = 0; i < expected.length; i++ )
            if( predicted[i] )
            {
                if( expected[i] )
                    tp++;
                else
                    fp++;
            }
            else
            {
                if( expected[i] )
                    fn++;
                else
                    tn++;
            }
    }

    
    public int getTP() { return tp; }
    public int getFP() { return fp; }
    public int getTN() { return tn; }
    public int getFN() { return fn; }
    
    public double getSensitivity()
    {
        return (double)tp/(tp+fn);
    }
    
    public double getSpecificity()
    {
        return (double)tn/(tn + fp);
    }
}
