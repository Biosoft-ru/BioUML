
package ru.biosoft.bsa._test;

import junit.framework.TestCase;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.TrackImpl;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;

/**
 * @author yura
 *
 */
public class TestIPSSiteModel extends TestCase
{

    public void testFindAllSites() throws Exception
    {
        double[][] weights = new double[][] {
                {0.06382978723404255, 0.0851063829787234, 0.19148936170212766, 0.6595744680851063},
                {0.723404255319149, 0, 0.1276595744680851, 0.14893617021276595},
                { 0, 0, 0, 1},
                { 1, 0, 0, 0},
                { 1, 0, 0, 0},
                {0.851063829787234, 0.02127659574468085, 0.0851063829787234, 0.0425531914893617},
                {0.06382978723404255, 0.06382978723404255, 0.1276595744680851, 0.7446808510638298},
                {0.44680851063829785, 0.0851063829787234, 0.0851063829787234, 0.3829787234042553}
        };
        FrequencyMatrix frequencyMatrix = new FrequencyMatrix(null, "TBP", Nucleotide5LetterAlphabet.getInstance(), null, weights , false);
        double threshold = 4.0;
        IPSSiteModel siteModel = new IPSSiteModel("", null, new FrequencyMatrix[] {frequencyMatrix}, threshold);
        
        String flanks10 = "CCCCCCCCCC";
        String flanks60 = flanks10 + flanks10 + flanks10 + flanks10 + flanks10 + flanks10;
        String sequenceStr = flanks60 + "TATAAATACCCCTATAAATA" + flanks60;
        LinearSequence sequence = new LinearSequence(sequenceStr, Nucleotide5LetterAlphabet.getInstance());
        
        WritableTrack track = new TrackImpl("", null);
        siteModel.findAllSites(sequence, track);
        DataCollection<Site> sites = track.getAllSites();
        assertEquals(2, sites.getSize());
    }
}
