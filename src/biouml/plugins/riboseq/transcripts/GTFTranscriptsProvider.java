package biouml.plugins.riboseq.transcripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import one.util.streamex.StreamEx;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.bsa.DiscontinuousCoordinateSystem;
import ru.biosoft.bsa.Interval;
import ru.biosoft.util.TextUtil;

public class GTFTranscriptsProvider extends TranscriptsProvider
{
    private File gtfFile;
    public GTFTranscriptsProvider(File gtfFile)
    {
        this.gtfFile = gtfFile;
    }

    @Override
    public List<Transcript> getTranscripts()
    {
        Map<String, List<Feature>> exons = new HashMap<>();
        Map<String, List<Feature>> cdsSet = new HashMap<>();

        try (BufferedReader reader = new BufferedReader( new FileReader( gtfFile ) ))
        {
            String line;
            while( ( line = reader.readLine() ) != null && line.startsWith( "#" ) )
                ;
            Pattern pattern = Pattern.compile( "transcript_id\\s+\"(.+)\"" );
            while( line != null )
            {
                String[] fields = TextUtil.split( line, '\t' );
                if( fields.length != 9 )
                    throw new Exception( "Invalid line " + line );
                String featureType = fields[2];
                Feature feature = new Feature();
                feature.chr = fields[0];
                feature.location = new Interval( Integer.parseInt( fields[3] ), Integer.parseInt( fields[4] ) ).shift( -1 );
                feature.isOnPositiveStrand = fields[6].equals( "+" );

                String transcriptId = null;
                String[] attributes = fields[8].split(";\\s+");
                for(String attribute: attributes)
                {
                    Matcher matcher = pattern.matcher(attribute);
                    if(matcher.find())
                        transcriptId = matcher.group(1);
                }
                if(transcriptId == null)
                    throw new Exception("transcript_id not set in the line " + line);

                Map<String, List<Feature>> container = null;
                if( featureType.equals( "exon" ) )
                    container = exons;
                else if( featureType.equals( "cds" ) )
                    container = cdsSet;
                if(container != null)
                {
                    List<Feature> features = container.get( transcriptId );
                    if(features == null)
                        container.put( transcriptId, features = new ArrayList<>() );
                    features.add( feature );
                }

                line = reader.readLine();
            }
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }

        HashSet<String> transcripts = getSubset() != null ? new HashSet<>(getSubset()) : null;
        List<Transcript> result = new ArrayList<>();
        exons.forEach( (transcriptId, exonList)->{
            if(transcripts != null && !transcripts.contains( transcriptId ))
                return;


            List<Interval> exonLocations = StreamEx.of( exonList ).map( f->f.location ).sorted().toList();
            Interval firstExon = exonLocations.get( 0 );
            Interval lastExon = exonLocations.get( exonLocations.size() - 1 );
            String chr = exonList.get( 0 ).chr;
            boolean isOnPositiveStrand = exonList.get( 0 ).isOnPositiveStrand;


            List<Feature> cdsFeatures = cdsSet.get( transcriptId );
            if(isOnlyProteinCoding() && cdsFeatures == null)
                return;
            List<Interval> cdsLocations = Collections.emptyList();
            if(cdsFeatures != null)
            {
                DiscontinuousCoordinateSystem cs = new DiscontinuousCoordinateSystem( exonLocations, !isOnPositiveStrand );
                cdsLocations = new ArrayList<>();
                List<Interval> cdsParts = StreamEx.of( cdsFeatures ).map( f->f.location ).sorted().toList();
                int from = cdsParts.get( 0 ).getFrom();
                int to = cdsParts.get( cdsParts.size() - 1 ).getTo();
                from = cs.translateCoordinate( from );
                to = cs.translateCoordinate( to );
                if(!isOnPositiveStrand)
                {
                    int tmp = from;
                    from = to;
                    to = tmp;
                }
                cdsLocations.add( new Interval(from, to) );
            }

            Transcript t = new Transcript( transcriptId, chr, new Interval( firstExon.getFrom(), lastExon.getTo() ),
                    isOnPositiveStrand, exonLocations, cdsLocations );
            result.add( t );
        } );
        return result;
    }

    private static class Feature
    {
        Interval location;
        String chr;
        boolean isOnPositiveStrand;
    }

}
