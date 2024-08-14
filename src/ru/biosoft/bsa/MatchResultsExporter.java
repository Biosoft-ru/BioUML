package ru.biosoft.bsa;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.analysis.MatchSiteModel;
import ru.biosoft.bsa.analysis.SiteSearchAnalysis;
import ru.biosoft.bsa.analysis.WeightMatrixModel;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class MatchResultsExporter implements DataElementExporter
{

    @Override
    public int accept(DataElement de)
    {
        if( de instanceof SqlTrack || ( de instanceof TrackRegion && ( (TrackRegion)de ).getTrack() instanceof SqlTrack ) )
            return DataElementExporter.ACCEPT_HIGH_PRIORITY;
        return DataElementExporter.ACCEPT_UNSUPPORTED;
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( SqlTrack.class, TrackRegion.class );
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file) throws Exception
    {
        doExport(de, file, null);
    }

    protected void writeHead(SqlTrack track, PrintWriter writer)
    {
        Properties properties = track.getInfo().getProperties();
        String models = properties.getProperty(SqlTrack.DE_PROPERTY_COLLECTION_PREFIX + SiteModel.SITE_MODEL_PROPERTY);
        if(models != null)
        {
            try
            {
                WeightMatrixModel model = DataElementPath.create(models).getDataCollection(WeightMatrixModel.class).iterator().next();
                DataElementPath matrices = model.getFrequencyMatrix().getOrigin().getCompletePath();
                writer.println("Search for sites by WeightMatrix library: " + matrices);
            }
            catch( Exception e )
            {
            }
        }
        String sequenceCollection = properties.getProperty(Track.SEQUENCES_COLLECTION_PROPERTY);
        if(sequenceCollection == null )
            sequenceCollection = properties.getProperty(SiteSearchAnalysis.SEQUENCES_COLLECTION_PROPERTY);
        if( sequenceCollection != null )
            writer.println("Sequence file: " + sequenceCollection);
        if( models != null )
            writer.println("Site selection profile: " + models);
    }

    private String formatSequence(Sequence seq, int coreStart, int coreLength)
    {
        char[] formatted = new char[seq.getLength()];
        for( int i = 0; i < formatted.length; i++ )
        {
            char letter = (char)seq.getLetterAt(i + seq.getStart());
            letter = ( i >= coreStart && i < coreStart + coreLength ) ? Character.toUpperCase(letter) : Character.toLowerCase(letter);
            formatted[i] = letter;
        }
        return new String(formatted);
    }

    protected void writeSite(Site s, PrintWriter writer)
    {
        try
        {
            DynamicPropertySet properties = s.getProperties();
            MatchSiteModel model = (MatchSiteModel)properties.getValue(SiteModel.SITE_MODEL_PROPERTY);
            String modelName = model.getName();

            int coreStart = model.getCoreStart();
            int coreLength = model.getCoreLength();
            Sequence sequence = s.getSequence();
            String sequenceString = formatSequence(sequence, coreStart, coreLength);

            double score = s.getScore();

            Object value = properties.getValue(MatchSiteModel.CORE_SCORE_PROPERTY);
            double coreScore = ( value instanceof Float ) ? ( (Float)value ).doubleValue() : ( (Double)value ).doubleValue();

            String strand = ( s.getStrand() == StrandType.STRAND_PLUS ) ? "+" : ( s.getStrand() == StrandType.STRAND_MINUS ? "-" : "?" );

            writer.printf(Locale.ENGLISH, " %-23s| %9d (%s) | %5.3f | %5.3f | %s\n", new Object[] {modelName, s.getFrom(), strand,
                    coreScore, score, sequenceString});
        }
        catch( Exception e )
        {
        }
    }

    protected void writeTail(SqlTrack track, PrintWriter writer)
    {
        int totalSequenceLength = -1;
        try
        {
            totalSequenceLength = Integer.parseInt(track.getInfo().getProperty(SiteSearchAnalysis.TOTAL_LENGTH_PROPERTY));
        }
        catch( Exception e )
        {
            return;
        }
        int nSites = track.getAllSites().getSize();

        writer.println();
        writer.println();
        writer.println("Total sequences length=" + totalSequenceLength);
        writer.println();
        writer.println("Total number of found sites=" + nSites);
        writer.println();
        writer.println("Frequency of sites per nucleotide=" + ( (double)nSites / totalSequenceLength ));
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file, FunctionJobControl jobControl) throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();
        SqlTrack track = ( de instanceof TrackRegion ) ? (SqlTrack) ( (TrackRegion)de ).getTrack() : (SqlTrack)de;

        DataCollection<?> sequences = null;
        String sequencesPath = track.getInfo().getProperty(SiteSearchAnalysis.INTERVALS_COLLECTION_PROPERTY);
        if( sequencesPath != null )
            sequences = CollectionFactory.getDataCollection(sequencesPath);
        if( sequences == null )
        {
            sequencesPath = track.getInfo().getProperty(Track.SEQUENCES_COLLECTION_PROPERTY);
            if( sequencesPath != null )
                sequences = CollectionFactory.getDataCollection(sequencesPath);
        }
        if( sequences == null )
        {
            sequencesPath = track.getInfo().getProperty(SiteSearchAnalysis.SEQUENCES_COLLECTION_PROPERTY);
            if( sequencesPath != null )
                sequences = CollectionFactory.getDataCollection(sequencesPath);
        }
        if(sequences == null)
            throw new Exception("Can not export '" + de.getName() + "'");
        
        try (PrintWriter writer = new PrintWriter( file ))
        {
            writeHead( track, writer );

            for( Object seqObj : sequences )
            {
                if( seqObj instanceof ru.biosoft.bsa.AnnotatedSequence )
                    seqObj = ( (ru.biosoft.bsa.AnnotatedSequence)seqObj ).getSequence();
                if( ! ( seqObj instanceof Sequence ) )
                    continue;
                Sequence seq = (Sequence)seqObj;
                String seqId = DataElementPath.create( seq ).toString();
                int from = seq.getStart();
                int to = seq.getStart() + seq.getLength() - 1;
                if( seq instanceof SequenceRegion )
                {
                    SequenceRegion seqRegion = (SequenceRegion)seq;
                    seqId = DataElementPath.create( seqRegion.getParentSequence() ).toString();
                    from = seqRegion.translatePosition( seq.getStart() );
                    to = seqRegion.translatePosition( seq.getStart() + seq.getLength() - 1 );
                }
                SubSequence subSeq = new SubSequence( seq, from, to );

                writer.println();
                writer.println( "Inspecting sequence ID   " + seq.getName() );
                writer.println();
                DataCollection<Site> sites = track.getSites( seqId, from, to );
                for( Site s : sites )
                {
                    writeSite( subSeq.translateSite( s ), writer );
                }

            }

            writeTail( track, writer );
        }
        
        if( jobControl != null )
            jobControl.functionFinished();
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

}
