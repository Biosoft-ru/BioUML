package biouml.plugins.riboseq.svm;

import biouml.plugins.riboseq.util.SiteUtil;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.AnalysisFailException;
import ru.biosoft.access.exception.BiosoftParseException;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.exporter.IntervalTrackExporter;
import ru.biosoft.bsa.exporter.TrackExporter;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

public class SVMAnalysis extends AnalysisMethodSupport<SVMParameters>
{
    public SVMAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new SVMParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        File dir = TempFiles.dir( "svmAnalysis" );
        try
        {
            final Track inputYesTrack = getInputYesTrack();
            final Track inputNoTrack = getInputNoTrack();
            final Track inputUndefinedTrack = getInputUndefinedTrack();
    
            exportTracks( inputYesTrack, inputNoTrack, inputUndefinedTrack, dir );
            Map<String, Site> clusterMap = createClusterMap( inputUndefinedTrack );
    
            runRScriptForClassification(dir);
    
            return filterTrack( inputUndefinedTrack, clusterMap, dir );
        }
        finally
        {
            ApplicationUtils.removeDir( dir );
        }
    }

    private @Nonnull Track getInputYesTrack()
    {
        DataElementPath inputYesTrack = parameters.getInputYesTrack();

        return inputYesTrack.getDataElement( Track.class );
    }

    private @Nonnull Track getInputNoTrack()
    {
        DataElementPath inputNoTrack = parameters.getInputNoTrack();

        return inputNoTrack.getDataElement( Track.class );
    }

    private @Nonnull Track getInputUndefinedTrack()
    {
        DataElementPath inputundefinedTrack = parameters.getInputUndefinedTrack();

        return inputundefinedTrack.getDataElement( Track.class );
    }

    private void exportTracks(@Nonnull Track inputYesTrack, @Nonnull Track inputNoTrack, @Nonnull Track inputUndefinedTrack, File dir)
            throws Exception
    {
        final TrackExporter trackExporter = new IntervalTrackExporter();
        trackExporter.init( null, "interval" );

        File file = new File( dir, "yesFile.interval" );
        trackExporter.doExport( inputYesTrack, file );

        file = new File( dir, "noFile.interval" );
        trackExporter.doExport( inputNoTrack, file );

        file = new File( dir, "unFile.interval" );
        trackExporter.doExport( inputUndefinedTrack, file );
    }

    private Map<String, Site> createClusterMap(Track track)
    {
        Map<String, Site> clusterMap = new HashMap<>();

        final DataCollection<Site> siteDataCollection = track.getAllSites();
        for( Site site : siteDataCollection )
        {
            final String placeDescription = SiteUtil.isSiteReversed( site ) ? "_rev" : "";
            final String chrName = site.getSequence().getName();
            final int start = site.getStart();

            final String idName = chrName + "chr_" + start + placeDescription;

            clusterMap.put( idName, site );
        }

        return clusterMap;
    }

    private void runRScriptForClassification(File dir) throws Exception
    {
        String script = com.developmentontheedge.application.ApplicationUtils.readAsString( SVMAnalysis.class
                .getResourceAsStream( "svmAnalyze.R" ) );
        try(TempFile scriptFile = TempFiles.file( "svm.R", script ))
        {
            final String rScriptName = scriptFile.toString();
            final ProcessBuilder processBuilder = new ProcessBuilder( "Rscript", rScriptName, dir.toString().replace( '\\', '/' )+'/' );
            processBuilder.redirectErrorStream( true );

            final Process process = processBuilder.start();

            InputStream stdout = process.getInputStream();
            InputStreamReader stdoutReader = new InputStreamReader( stdout, Charset.defaultCharset() );
            StringBuilder message = new StringBuilder();
            try (BufferedReader stdoutBufferedReader = new BufferedReader( stdoutReader ))
            {
                String line;
                while( ( line = stdoutBufferedReader.readLine() ) != null )
                {
                    message.append( line ).append( '\n' );
                }
            }

            final int NORMAL_EXIT_VAL = 0;
            final int exitVal = process.waitFor();
            if( exitVal != NORMAL_EXIT_VAL )
            {
                throw new AnalysisFailException( null, message.toString() );
            }
        }
    }

    private SqlTrack filterTrack(Track undefinedTrack, Map<String, Site> clusterMap, File dir) throws Exception
    {
        final String TRUST_STR = "TRUE";

        SqlTrack outputClassifiedTrack = createOutputTrack( undefinedTrack );

        final String analysisResultFileName = "outputAnalysis.txt";
        final File resultFile = new File( dir, analysisResultFileName );
        try(final Scanner scanner = new Scanner( resultFile, StandardCharsets.UTF_8.name() ))
        {
            // skip header
            if( scanner.hasNextLine() )
            {
                scanner.nextLine();
            }

            while( scanner.hasNext() )
            {
                final String idName = scanner.next();
                if( clusterMap.containsKey( idName ) )
                {
                    final Site site = clusterMap.get( idName );
                    final String prediction = scanner.next();
                    if( prediction.equals( TRUST_STR ) )
                    {
                        outputClassifiedTrack.addSite( site );
                    }
                }
                else
                {
                    throw new BiosoftParseException( null, "not found the specified cluster " + idName );
                }

            }
        }

        outputClassifiedTrack.finalizeAddition();
        parameters.getOutputClassifiedYesTrack().save( outputClassifiedTrack );

        return outputClassifiedTrack;
    }

    private SqlTrack createOutputTrack(Track referenceTrack) throws Exception
    {
        DataElementPath outputTrack = parameters.getOutputClassifiedYesTrack();

        return SqlTrack.createTrack( outputTrack, referenceTrack );
    }
}