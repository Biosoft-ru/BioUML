package biouml.plugins.chipmunk;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.autosome.ChIPAct;
import ru.autosome.ChIPAct.Parameters;
import ru.autosome.ChIPMunk;
import ru.autosome.assist.Conductor;
import ru.autosome.ytilib.MunkResult;
import ru.biosoft.access.security.SessionThreadFactory;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.analysis.FrequencyMatrix;

public class ChipMunkAPI
{
    private static Logger log = Logger.getLogger( ChipMunkAPI.class.getName() );

    public static FrequencyMatrix[] chipmunkMotifDiscovery(Sequence[] sequences) throws Exception
    {
        Conductor conductor = createConductor( log );
        Parameters actParams = createActParameters( conductor, sequences );
        ChIPMunk.Parameters munkParams = createMunkParameters();
        
        
        ChIPMunk chipmunk = new ChIPMunk(actParams, munkParams);
        MunkResult result = (MunkResult)chipmunk.launchViaConductor();
        
        if(conductor.getStatus() == Conductor.Status.ERROR)
            throw conductor.getError();
        
        if(conductor.getStatus() == Conductor.Status.FAIL)
            throw new Exception("Cannot find motif");
        
        if(result == null)
            throw new Exception("No result");

        FrequencyMatrix weightMatrix = new FrequencyMatrix( null, "matrix",
              Nucleotide15LetterAlphabet.getInstance(), null, transposeMatrix( result.getWPCM().getMatrix() ), false );
        return new FrequencyMatrix[] {weightMatrix};
    }
    
    static Conductor createConductor(Logger log)
    {
        Conductor conductor = new Conductor();
        conductor.setOutputPrinter(new LogPrinter(log, Level.INFO));
        conductor.setMessagePrinter(new LogPrinter(log, Level.FINE));
        conductor.setThreadFactory(new SessionThreadFactory());
        return conductor;
    }
    
    static ChIPAct.Parameters createActParameters(Conductor conductor, Sequence[] sequences)
    {
        ArrayList<ru.autosome.ytilib.Sequence[]> sequenceSets = new ArrayList<>();
        sequenceSets.add( convertSequences( sequences ) );
        ChIPAct.Parameters actParameters = new ChIPAct.Parameters( conductor, sequenceSets );
        actParameters.setThreadCount( 1 );
        //actParameters.setStepLimit( getParameters().getStepLimit() );
        //actParameters.setTryLimit( getParameters().getTryLimit() );
        //if( getParameters().getGcPercent() >= 0 )
        //    actParameters.setGCPercent( getParameters().getGcPercent() );
        return actParameters;
    }
    
    static ChIPMunk.Parameters createMunkParameters()
    {
        int startLength = 16;
        int stopLength = 6;
        double zoopsFactor = 1;
        ChIPMunk.Parameters munkParameters = new ChIPMunk.Parameters(startLength, stopLength, false, zoopsFactor);
        //munkParameters.setShapeProvider(getParameters().getShapeProvider());
        return munkParameters;
    }
    
    static ru.autosome.ytilib.Sequence[] convertSequences(Sequence[] biosoftSequences)
    {
        ru.autosome.ytilib.Sequence[] result = new  ru.autosome.ytilib.Sequence[biosoftSequences.length];
        for(int i = 0; i < biosoftSequences.length; i++)
        {
            byte[] bytes = biosoftSequences[i].getBytes();
            String str = new String(bytes, StandardCharsets.ISO_8859_1);
            result[i] = new ru.autosome.ytilib.Sequence( str );
        }
        return result;
    }
    
    protected static double[][] transposeMatrix(double[][] matrix)
    {
        int length = matrix[0].length;
        double[][] result = new double[length][];
        for(int i=0; i<length; i++)
        {
            result[i] = new double[matrix.length];
            for(int j=0; j<matrix.length; j++)
                result[i][j] = matrix[j][i];
        }
        return result;
    }
    
    public static void main(String[] args) throws Exception
    {
        Sequence[] sequences = new Sequence[5];
        Nucleotide5LetterAlphabet alphabet = Nucleotide5LetterAlphabet.getInstance();
        sequences[0] = new LinearSequence( "ACCAACGTACGTTTGGTATTATATGATAGGGGATAGAT".getBytes(), alphabet );
        sequences[1] = new LinearSequence( "TTTACGTACGTTACGTCGTAGCTAGCTAGTACGTAGTCAGCTAG".getBytes(), alphabet );
        sequences[2] = new LinearSequence( "TGCTGCTGCTGCTGCTCGTCACGTACGTACCGATCGATCGTAGC".getBytes(), alphabet );
        sequences[3] = new LinearSequence( "TTTTTTttGTGTGTGTGGTGTGTGTGGTGTGTGACGTACGTGTG".getBytes(), alphabet );
        sequences[4] = new LinearSequence( "TGGGGGGGGGGGACGTACGTGGGGGGGGGGGGGGGGGGGGGGGG".getBytes(), alphabet );
        FrequencyMatrix[] res = chipmunkMotifDiscovery( sequences );
        for(int i = 0; i < res.length; i++)
        {
            System.out.println("Frequency matrix " + i );
            FrequencyMatrix m = res[i];
            for(int j = 0; j < m.getLength(); j++)
            {
                System.out.print( j + ":" );
                for(byte code : alphabet.basicCodes())
                {
                    double freq = m.getFrequency( j, code );
                    System.out.print("\t" + String.format( "%04f", freq ));
                }
                System.out.println();
            }
            
        }
    }
    
}
