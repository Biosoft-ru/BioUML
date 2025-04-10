package ru.biosoft.bsa.track;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.FileTrack;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.importer.VCFTrackImporter;

/**
 * class refactored, @author anna
 */
@ClassIcon ( "resources/trackvcf.png" )
@PropertyName ( "track" )
public class VCFFileTrack extends FileTrack
{
    private static final Logger log = Logger.getLogger(VCFFileTrack.class.getName());
    
    public VCFFileTrack(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
        
    }

    @Override
    protected void readFromFile(File file, DataCollection<Site> sites)
    {
        Map<String, String> formatTypeMap = new HashMap<>();
        Map<String, String> infoTypeMap = new HashMap<>();
        List<String> sampleIdsList = new ArrayList<>();
        Properties properties = new Properties();

        try (FileInputStream is = new FileInputStream(file);
                BufferedReader input = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                FileChannel ch = is.getChannel())
        {
            String line;

            int i = 1;
            while ( (line = input.readLine()) != null )
            {
                if( VCFTrackImporter.isComment(line, properties, infoTypeMap, sampleIdsList, formatTypeMap, file.getName()) )
                    continue;
                String siteName = String.valueOf(i++);
                Site site = VCFTrackImporter.parseLine(line, formatTypeMap, sampleIdsList, infoTypeMap,
                        Boolean.valueOf(getInfo().getProperties().getProperty("normalizeChromosome", "false")));
                if( site.getOriginalSequence() == null )
                {
                    Sequence seq = getSequence(site.getName());
                    site = new SiteImpl(site.getOrigin(), siteName, site.getType(), site.getBasis(), site.getStart(), site.getLength(), site.getPrecision(), site.getStrand(), seq,
                            site.getComment(), site.getProperties());
                }
                sites.put(site);
            }
            sites.getInfo().getProperties().putAll(properties);
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Can not create VCF track from file", e);
        }
    }
}
