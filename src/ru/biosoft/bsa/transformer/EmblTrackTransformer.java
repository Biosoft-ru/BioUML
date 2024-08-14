package ru.biosoft.bsa.transformer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.Entry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackImpl;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

/**
 * Converts {@link Entry} to the {@link Track} and back.
 */
public class EmblTrackTransformer extends AbstractTransformer<Entry, Track>
{
    protected static final Logger log = Logger.getLogger( EmblTrackTransformer.class.getName() );

    public static final String EMBL_FT_TAG = "FT";
    public static final String EMBL_FH_TAG = "FH";
    public static final String GENBANK_FT_TAG = "  ";
    public static final String GENBANK_FEATURES_TAG = "FEATURES";
    public static final String EMBL_FT_HEADER = "FH   Key             Location/Qualifiers\nFH";
    public static final String GENBANK_FT_HEADER = "FEATURES             Location/Qualifiers";

    public static final int EMBL_FORMAT = 0;
    public static final int GENBANK_FORMAT = 1;

    public final static int CONST = 21;

    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    /** @return Track.class */
    @Override
    public Class<Track> getOutputType()
    {
        return Track.class;
    }

    @Override
    public boolean isOutputType(Class<?> type)
    {
        return Track.class.isAssignableFrom(type);
    }

    protected int format = EMBL_FORMAT;
    public void setFormat(int format)
    {
        this.format = format;
    }

    /**
     * Converts Entry to the Track
     */
    @Override
    public Track transformInput(Entry entry) throws Exception
    {
        BufferedReader reader = new BufferedReader(entry.getReader());

        String propName = null;
        StringBuffer propValue = null;
        StringBuffer locationString = null;
        String curName = null;

        String line;
        List<LocationParser.Site> resultSites = new ArrayList<>();
        boolean trackBlockStarted = false;
        DataCollection collection = getTransformedCollection();
        Sequence sequence = (collection instanceof AnnotatedSequence)?((AnnotatedSequence)collection).getSequence():null;
        while( ( line = reader.readLine() ) != null )
        {
            if( format == GENBANK_FORMAT && line.startsWith(GENBANK_FEATURES_TAG) )
            {
                trackBlockStarted = true;
                continue;
            }

            if( ( format == EMBL_FORMAT && !line.startsWith(EMBL_FT_TAG) ) || ( format == GENBANK_FORMAT && !trackBlockStarted ) )
                continue;

            if( format == GENBANK_FORMAT && !line.startsWith(GENBANK_FT_TAG) )
            {
                trackBlockStarted = false;
                continue;
            }

            String rightTag = line.substring(CONST);
            String leftTag = line.substring(4, CONST).trim();

            if( leftTag.length() != 0 ) //name not null !!!
            {
                add(propName, propValue, curName, locationString, resultSites);
                locationString = new StringBuffer(rightTag);
                propName = null;
                curName = leftTag;
            }
            else if( rightTag.charAt(0) == '/' ) //.startsWith("/"))
            {
                add(propName, propValue, curName, locationString, resultSites);
                int idx = rightTag.indexOf("=");
                if( idx > 0 )
                {
                    propName = rightTag.substring(1, idx);
                    propValue = new StringBuffer(rightTag.substring(idx + 1));
                    locationString = null;
                }
            }
            else if( propName != null )
            {
                propValue.append(" ");
                propValue.append(rightTag);
            }
            else
            {
                locationString.append(rightTag);
            }
        }
        add(propName, propValue, curName, locationString, resultSites);

        addToResult(curName, resultSites);

        TrackImpl track = new TrackImpl(entry.getName(), getTransformedCollection());

        for( int i = 0; i < resultSites.size(); i++ )
        {
            LocationParser.Site strSite = resultSites.get(i);
            int precision = Precision.PRECISION_EXACTLY;

            if( strSite.fBetween )
                precision = Precision.PRECISION_NOT_KNOWN;
            else if( strSite.startBefore )
                precision = strSite.fEndsAfter ? Precision.PRECISION_CUT_BOTH : Precision.PRECISION_CUT_LEFT;
            else if( strSite.fEndsAfter )
                precision = Precision.PRECISION_CUT_RIGHT;

            int id = 1;
            String newName = strSite.type;
            StringBuffer strBuf = new StringBuffer(48);
            while( track.contains(newName) )
            {
                int pos = strSite.name.lastIndexOf('_');
                strBuf.delete(0, strBuf.length());
                if( pos != -1 )
                    strBuf.append(newName.substring(0, pos));
                else
                    strBuf.append(newName);
                strBuf.append('_').append(id++);
                newName = strBuf.toString();
            }

            /**@todo: This is a fixing for the following buck, the position of
             *  imported features where not given correctly.
             *  To fix this at this position in the code is not the optimal solution.
             *  It should be fixed when filling the results table. (ela)
             *  To be improved.
             */
            if( strSite.strand == ru.biosoft.bsa.Site.STRAND_MINUS )
            {
                strSite.start = strSite.length + strSite.start - 1;
            }
            ru.biosoft.bsa.Site site = new ru.biosoft.bsa.SiteImpl(null, newName, strSite.type, ru.biosoft.bsa.Site.BASIS_ANNOTATED,
                    strSite.start, strSite.length, precision, strSite.strand, sequence, strSite.siteProperties);

            track.addSite(site);
        }
        return track;
    }
    List<LocationParser.Site> sites = new ArrayList<>();

    private void addToResult(String curName, List<LocationParser.Site> resultSites)
    {
        if( sites == null )
            return;

        if( isSource(curName) )
            return;

        resultSites.addAll(sites);
    }

    void addSite(StringBuffer locationString, String curName, List<LocationParser.Site> resultSites) throws Exception
    {
        if( locationString != null )
        {
            addToResult(curName, resultSites);
            parseSite(locationString.toString(), curName);
        }
    }

    private void add(String propName, StringBuffer propValue, String curName, StringBuffer locationString, List<LocationParser.Site> resultSites)
            throws Exception
    {
        addSite(locationString, curName, resultSites);

        if( propName == null )
            return;

        if( isSource(curName) )
        {
            //siteSetProperties.setValue(propName, propValue.toString());
        }
        else
        {
            for( LocationParser.Site site : sites )
            {
                site.siteProperties.add(new DynamicProperty(propName, String.class, propValue.toString()));
            }
        }

    }

    boolean isSource(String curName)
    {
        return "source".equals(curName);
    }

    void parseSite(String rightTag, String curName) throws Exception
    {
        LocationParser locParser = new LocationParser(curName, rightTag);

        if( !isSource(curName) )
            sites = locParser.parse();
    }

    /**
     * Converts Track  to the Entry
     */
    @Override
    public Entry transformOutput(Track track)
    {
        String header = EMBL_FT_HEADER;
        String ft_tag = EMBL_FT_TAG;

        if( format == GENBANK_FORMAT )
        {
            header = GENBANK_FT_HEADER;
            ft_tag = GENBANK_FT_TAG;
        }

        StringWriter out = new StringWriter();

        try
        {
            DataCollection<Site> sites = track.getAllSites();
            if( sites.getSize() > 0 )
            {
                out.write(header + "\n");
                for(Site site: sites)
                {
                    try
                    {
                        writeSite(out, ft_tag, site);
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, "Can not write site", e);
                    }
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not save sites in EMBL format", e);
        }

        return new Entry(getPrimaryCollection(), track.getName(), out.toString());
    }

    public void writeSite(Writer out, String linePrefix, Site site) throws IOException
    {
        /** @pending - long string should be cut not more */
        String field = makeField(linePrefix, 5) + site.getType();
        out.write(field);
        for( int i = field.length(); i < 21; i++ )
            out.write(" ");

        int start = site.getStart();
        int end = start + site.getLength() - 1;

        if( site.getStrand() == ru.biosoft.bsa.Site.STRAND_MINUS )
            out.write("complement(");
        int precision = site.getPrecision();
        if( precision == ru.biosoft.bsa.Site.PRECISION_CUT_LEFT || precision == ru.biosoft.bsa.Site.PRECISION_CUT_BOTH )
            out.write("<");
        out.write(String.valueOf(start));
        if( precision == ru.biosoft.bsa.Site.PRECISION_NOT_KNOWN )
            out.write("^");
        else
            out.write("..");
        if( precision == ru.biosoft.bsa.Site.PRECISION_CUT_RIGHT || precision == ru.biosoft.bsa.Site.PRECISION_CUT_BOTH )
            out.write(">");
        out.write(String.valueOf(end));
        if( site.getStrand() == ru.biosoft.bsa.Site.STRAND_MINUS )
            out.write(")");
        out.write("\n");
        writeSiteProperties(out, linePrefix, site);
    }

    private String makeField(String linePrefix, int size)
    {
        StringBuffer field = new StringBuffer(32);
        if( linePrefix != null )
            field.append(linePrefix);
        for( int i = field.length(); i < size; i++ )
            field.append(' ');
        return field.toString();
    }

    private void writeSiteProperties(Writer out, String linePrefix, ru.biosoft.bsa.Site site) throws IOException
    {
        DynamicPropertySet siteProperties = site.getProperties();
        if( siteProperties != null )
        {
            for(DynamicProperty property: siteProperties)
            {
                String k = property.getName();
                String v = String.valueOf(property.getValue());
                String start = makeField(linePrefix, 21);
                out.write(start + "/" + k + "=" + v + "\n");
            }
        }
    }
}
