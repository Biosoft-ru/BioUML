package biouml.plugins.riboseq.ingolia.svmlight;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import biouml.plugins.riboseq.ingolia.ObservationList;

@CodePrivilege(CodePrivilegeType.LAUNCH)
public class SVMLightPredict extends SVMLight
{

    public double[] predict(ObservationList observations, File modelFile, Logger log) throws Exception
    {
        try (TempFile observationsFile = TempFiles.file( ".txt" ); TempFile scoresFile = TempFiles.file( ".txt" ))
        {
            writeObservations(observations, observationsFile);
            
            List<String> options = new ArrayList<>();
            options.add( observationsFile.getAbsolutePath() );
            options.add( modelFile.getAbsolutePath() );
            options.add( scoresFile.getAbsolutePath() );
            runCommand( "svm_classify", options, log );
            
            double[] scores = new double[observations.getObservations().size()];
            parseScores( scores, scoresFile );
            return scores;
        }
    }

    private void parseScores(double[] scores, File scoresFile) throws Exception
    {
        try(BufferedReader reader = ApplicationUtils.utfReader( scoresFile ))
        {
            int n = 0;
            String line;
            while((line = reader.readLine()) != null)
            {
                if(n >= scores.length)
                    throw new Exception("Invalid svm_classify output: too many lines");
                try
                {
                    scores[n++] = Double.parseDouble( line );
                }
                catch( NumberFormatException e )
                {
                    throw new Exception("Invalid svm_classify output: can not parse line " + n +" '" + line + "'");
                }
            }
            if(n != scores.length)
                throw new Exception("Invalid svm_classify output: not enough lines");
        }
    }
}
