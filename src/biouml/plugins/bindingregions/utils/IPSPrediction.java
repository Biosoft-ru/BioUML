package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.List;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;

/***
 * remark: strand = '+' or '-'
 */
public class IPSPrediction implements Comparable<IPSPrediction>
{
    public static final String MATRIX_NAME_PROPERTY = "matrixName";

    String matrixName;
    String chromosome;
    Interval interval;
    float ipsScore;
    byte strand;
    
    public IPSPrediction(String matrixName, String chromosome, Interval interval, float ipsScore, byte strand)
    {
        this.matrixName = matrixName;
        this.chromosome = chromosome;
        this.interval = interval;
        this.ipsScore = ipsScore;
        this.strand = strand;
    }
    
    public String getMatrixName()
    {
        return matrixName;
    }
    
    public String getChromosome()
    {
        return chromosome;
    }
    
    public Interval getInterval()
    {
        return interval;
    }

    public float getIpsScore()
    {
        return ipsScore;
    }

    public byte getStrand()
    {
        return strand;
    }

    @Override
    public int compareTo(IPSPrediction o)
    {
        return interval.compareTo(o.getInterval());
    }

    //////////////////////////////O.K.
    public static double getMaximalIpsScore(Sequence sequence, IPSSiteModel ipsSiteModel)
    {
        return SequenceRegion.withReversed( sequence ).map( ipsSiteModel::findBestSite )
                .mapToDouble( Site::getScore ).append( 0.0 ).max().getAsDouble();
    }

    /////////////////////// O.K.
    public static List<IPSSiteModel> getIpsSiteModels(DataElementPath pathToMatrices)
    {
        List<IPSSiteModel> result = new ArrayList<>();
        for( DataElementPath matrixPath : pathToMatrices.getChildren())
        {
            result.add(getIpsSiteModel(matrixPath, 3.3));
        }
        return result;
    }
    
    ////////////////////////////O.K.
    public static IPSSiteModel getIpsSiteModel(DataElementPath pathToMatrix, double ipsThreshold)
    {
        FrequencyMatrix matrix = pathToMatrix.getDataElement(FrequencyMatrix.class);
        return new IPSSiteModel(matrix.getName(), null, new FrequencyMatrix[] {matrix}, ipsThreshold, IPSSiteModel.DEFAULT_DIST_MIN,
                IPSSiteModel.DEFAULT_WINDOW);
    }
    
    ////////////////////////////O.K.
    public static IPSSiteModel getIpsSiteModel(DataElementPath pathToMatrix)
    {
        return getIpsSiteModel(pathToMatrix, 0.01);
    }
}