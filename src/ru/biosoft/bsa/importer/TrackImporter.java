package ru.biosoft.bsa.importer;

import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.bean.StaticDescriptor;

public abstract class TrackImporter implements DataElementImporter
{
    protected String format;

    protected TrackImportProperties importerProperties;

    public static final String SEQUENCE_COLLECTION_PROPERTY = "sequencePath";

    private static Map<String, PropertyDescriptor> descriptors = new ConcurrentHashMap<>();

    protected static PropertyDescriptor getDescriptor(String name)
    {
        PropertyDescriptor pd = descriptors.get(name);
        if(pd != null) return pd;
        pd = StaticDescriptor.create(name);
        descriptors.put(name, pd);
        return pd;
    }

    protected static String normalizeChromosome(String name)
    {
        return normalizeChromosome( name, false );
    }

    protected static String normalizeChromosome(String name, boolean normalizeChromosome)
    {
        if( normalizeChromosome )
        {
        String smallName = name.toLowerCase();
        if( smallName.startsWith( "chr" ) )
            name = name.substring("chr".length());
        return name.equals("M") ? "MT" : name;
    }
    return name;
    }

    @Override
    public int accept(DataCollection<?> parent, File file)
    {
        if( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable(parent, getResultType()) )
            return ACCEPT_UNSUPPORTED;
        if( file == null )
            return ACCEPT_HIGH_PRIORITY;
        if( Pattern.compile(".+\\." + format, Pattern.CASE_INSENSITIVE).matcher(file.getName()).matches() )
            return ACCEPT_HIGH_PRIORITY;
        //Try to read 20 first lines from file
        try(BufferedReader input = ApplicationUtils.utfReader( file ))
        {
            int totalLines = 0;
            int goodLines = 0;
            int commentLines = 0;

            String line;
            beforeParse();
            while( ( line = input.readLine() ) != null && totalLines < 20 && (totalLines + commentLines) < 100)
            {
                if( isComment(line) )
                {
                    commentLines++;
                    continue;
                }
                totalLines++;
                Site[] s = parseLineToMultipleSites(line);
                if( s == null )
                    continue;
                goodLines++;
            }
            if( totalLines > 5 && goodLines == totalLines )
                return ACCEPT_MEDIUM_PRIORITY;
            if( totalLines > 0 && (float)goodLines / totalLines > 0.9 )
                return ACCEPT_BELOW_MEDIUM_PRIORITY;
            if( totalLines > 0 && (float)goodLines / totalLines > 0.5 )
                return ACCEPT_LOW_PRIORITY;
            if( totalLines > 0 && (float)goodLines / totalLines > 0.3 )
                return ACCEPT_BELOW_MEDIUM_PRIORITY;
        }
        catch( Exception e )
        {
        }
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public TrackImportProperties getProperties(DataCollection<?> parent, File file, String elementName)
    {
        return getProperties();
    }

    protected synchronized TrackImportProperties getProperties()
    {
        if(importerProperties == null) importerProperties = new TrackImportProperties();
        return importerProperties;
    }

    private final Map<String, Sequence> sequenceCache = new HashMap<>();
    private DataCollection<AnnotatedSequence> seqBase;

    private Sequence getSequence(String name)
    {
        Sequence result = sequenceCache.get(name);
        if(result != null)
            return result;

        if(seqBase != null)
        {
            try
            {
                result = seqBase.get(name).getSequence();
            }
            catch( Exception e )
            {
            }
            if(result != null)
            {
                sequenceCache.put(name, result);
                return result;
            }
        }

        result = new LinearSequence(name, new byte[0], Nucleotide15LetterAlphabet.getInstance());
        sequenceCache.put(name, result);

        return result;
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String elementName, FunctionJobControl jobControl, Logger log) throws Exception
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
        }

        String name = elementName == null || elementName.equals("") ? file.getName().replaceFirst("\\.[^\\.]+$", "") : elementName;
        if( parent.contains(name) )
            parent.remove(name);
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, name);
        if( getProperties().getSequenceCollectionPath() != null )
        {
            properties.put(Track.SEQUENCES_COLLECTION_PROPERTY, getProperties().getSequenceCollectionPath().toString());
            seqBase = getProperties().getSequenceCollection();
        }
        if( !TextUtil2.isEmpty(getProperties().getGenomeId()) )
            properties.put(Track.GENOME_ID_PROPERTY, getProperties().getGenomeId());
        properties.putAll(getProperties().getTrackProperties());
        WritableTrack track = TrackUtils.createTrack( parent, properties, getTrackClass() );
        int j = 0;
        boolean invalidRows = false;
        int skip = getProperties().getSkipLines();
        try (FileInputStream is = new FileInputStream( file );
                BufferedReader input = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) );
                FileChannel ch = is.getChannel())
        {
            String line;
            beforeParse();

            while( ( line = input.readLine() ) != null )
            {
                if(skip > 0)
                {
                    skip--;
                    continue;
                }
                if( isComment( line ) )
                    continue;
                Site[] sites = parseLineToMultipleSites(line);
                if( sites == null )
                {
                    invalidRows = true;
                    continue;
                }
                for(Site site: sites)
                {
                    if(site.getOriginalSequence() == null)
                    {
                        site = new SiteImpl(site.getOrigin(), null, site.getType(), site.getBasis(), site.getStart(), site.getLength(),
                                site.getPrecision(), site.getStrand(), getSequence(site.getName()), site.getComment(), site.getProperties());
                    }
                    track.addSite(site);
                }
                j++;
                if( jobControl != null && j % 100 == 0 )
                {
                    jobControl.setPreparedness((int) ( 100 * ch.position() / ch.size() ));
                    if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    {
                        input.close();
                        parent.remove(name);
                        return null;
                    }
                }
            }
            track.finalizeAddition();

            if(track instanceof DataCollection)
            {
                ((DataCollection<?>)track).getInfo().getProperties().putAll(importerProperties.getTrackProperties());
            }

            parent.put(track);
        }
        catch( Exception e )
        {
            parent.remove(name);
            throw e;
        }
        if( jobControl != null && jobControl.getStatus() != JobControl.TERMINATED_BY_REQUEST
                && jobControl.getStatus() != JobControl.TERMINATED_BY_ERROR )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
        return parent.get(name);
    }

    /**
     * Parses single line
     * @param line line to parse
     * @return Site created by line or null in case of error
     */
    protected Site parseLine(String line)
    {
        return null;
    }

    protected Site[] parseLineToMultipleSites(String line)
    {
        Site site = parseLine(line);
        return site == null ? null:new Site[] {site};
    }

    protected boolean isComment(String line)
    {
        return line.startsWith("#");
    }

    /**
     * This method can be used to set up initial importer state
     */
    protected void beforeParse()
    {

    }

    @Override
    public boolean init(Properties properties)
    {
        String sequenceCollection = properties.getProperty(SEQUENCE_COLLECTION_PROPERTY);
        if(sequenceCollection != null)
            getProperties().setSequenceCollectionPath(DataElementPath.create(sequenceCollection));
        return true;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return WritableTrack.class;
    }

    protected Class<? extends WritableTrack> getTrackClass()
    {
        return SqlTrack.class;
    }
}
