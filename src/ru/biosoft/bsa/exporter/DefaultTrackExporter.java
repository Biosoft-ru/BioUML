package ru.biosoft.bsa.exporter;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.MessageBundle;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.SubSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackRegion;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

/**
 * Exports tracks to BED and GFF formats
 * (currently under development)
 * @author lan
 *
 */
public class DefaultTrackExporter extends TrackExporter
{
    private String suffix;

    @SuppressWarnings ( "serial" )
    public static class TrackExporterProperties extends BaseParameters
    {
        public TrackExporterProperties(Track track)
        {
            super(track);
        }
        /**
         * Whether to include header line
         */
        private boolean includeHeader = true;
        private boolean headerOptionHidden = false;
        
        private boolean prependChrPrefix = false;

        private boolean hideUCSC = true;
        private boolean useUCSC = false;

        public boolean isIncludeHeader()
        {
            return includeHeader;
        }

        public void setIncludeHeader(boolean includeHeader)
        {
            Object oldValue = this.includeHeader;
            this.includeHeader = includeHeader;
            firePropertyChange("includeHeader", oldValue, includeHeader);
        }
        
        public boolean isHeaderOptionHidden()
        {
            return headerOptionHidden;
        }
        
        public void setHeaderOptionHidden(boolean value)
        {
            this.headerOptionHidden = value;
        }

        public boolean isPrependChrPrefix()
        {
            return prependChrPrefix;
        }

        public void setPrependChrPrefix(boolean prependChrPrefix)
        {
            Object oldValue = this.prependChrPrefix;
            this.prependChrPrefix = prependChrPrefix;
            firePropertyChange( "prependChrPrefix", oldValue, prependChrPrefix );
        }
        
        public boolean hideUCSC()
        {
            return hideUCSC;
        }
        public void setHideUCSC(boolean hideUCSC)
        {
            this.hideUCSC = hideUCSC;
        }
        
        public boolean isUseUCSC()
        {
            return useUCSC;
        }
        public void setUseUCSC(boolean useUCSC)
        {
            boolean oldValue = this.useUCSC;
            this.useUCSC = useUCSC;
            firePropertyChange( "useUCSC", oldValue, useUCSC );
        }
    }

    public static class TrackExporterPropertiesBeanInfo extends BaseParametersBeanInfo
    {
        public TrackExporterPropertiesBeanInfo()
        {
            super(TrackExporterProperties.class, MessageBundle.class.getName());
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            PropertyDescriptorEx pde = new PropertyDescriptorEx("includeHeader", beanClass);
            pde.setHidden( beanClass.getMethod( "isHeaderOptionHidden" ) );
            add( pde, getResourceString( "PN_EXPORT_INCLUDE_HEADER" ), getResourceString( "PD_EXPORT_INCLUDE_HEADER" ) );
            add( new PropertyDescriptorEx( "prependChrPrefix", beanClass ), getResourceString( "PN_EXPORT_PREPEND_CHR_PREFIX" ),
                    getResourceString( "PD_EXPORT_PREPEND_CHR_PREFIX" ) );
            pde = new PropertyDescriptorEx( "useUCSC", beanClass );
            pde.setHidden( beanClass.getMethod( "hideUCSC" ) );
            add( pde, "Conform UCSC", "Make options conform UCSC. This may modify, e.g. 'score' values" );
        }
    }

    @Override
    public boolean accept(TrackRegion trackRegion)
    {
        if( trackRegion.getSequence() == null )
        {
            DataCollection<Site> dc = null;
            try
            {
                dc = trackRegion.getTrack().getAllSites();
            }
            catch( Exception e )
            {
            }
            return dc != null;
        }
        return true;
    }

    protected double checkScoreByUCSC(double score)
    {
        if( parameters instanceof TrackExporterProperties && ( ( (TrackExporterProperties)parameters ).isUseUCSC() ) )
            score = Math.max( 0, Math.min( 1000, score ) );
        return score;
    }

    protected void writeBEDSite(Site s, PrintWriter pw)
    {
        DynamicPropertySet p = s.getProperties();
        double score = 0;
        String type = s.getType();
        try
        {
            score = s.getScore();
        }
        catch( Exception e )
        {
        }
        try
        {
            if( type.equals( SiteType.TYPE_TRANSCRIPTION_FACTOR) )
                type = ( (DataElement)p.getProperty("siteModel").getValue() ).getName();
        }
        catch( Exception e )
        {
            type = type.replaceAll( "\\s+", "_" );
        }
        int strand = s.getStrand();
        score = checkScoreByUCSC( score );
        pw.printf( Locale.ENGLISH, "%s\t%d\t%d\t%s\t%g", getChromosomeName( s ), s.getFrom() - 1, s.getTo(), type, score );
        if( strand == StrandType.STRAND_PLUS )
            pw.write("\t+");
        else if( strand == StrandType.STRAND_MINUS )
            pw.write("\t-");
        else
            pw.write( "\t." );
        String[] fields = {"thickStart", "thickEnd", "itemRgb", "blockCount", "blockSizes", "blockStarts"};
        StreamEx.of( fields ).map( p::getProperty ).takeWhile( Objects::nonNull )
                .forEach( fieldVal -> pw.printf( Locale.ENGLISH, "\t%s", fieldVal.getValue() ) );
        pw.print("\n");
    }

    protected void writeGFFSite(Site s, PrintWriter pw)
    {
        DynamicPropertySet p = s.getProperties();
        double score = 0;
        String source = Application.getGlobalValue("ApplicationName");
        String frame = ".";
        String group = ".";
        String type = s.getType();
        try
        {
            score = s.getScore();
        }
        catch( Exception e )
        {
        }
        try
        {
            source = p.getProperty("source").getValue().toString();
        }
        catch( Exception e )
        {
        }
        try
        {
            frame = p.getProperty("frame").getValue().toString();
        }
        catch( Exception e )
        {
        }
        try
        {
            group = p.getProperty("group").getValue().toString();
        }
        catch( Exception e )
        {
        }
        try
        {
            if( type.equals(SiteType.TYPE_TRANSCRIPTION_FACTOR) )
                type = ( (DataElement)p.getProperty("siteModel").getValue() ).getName();
        }
        catch( Exception e )
        {
        }
        score = checkScoreByUCSC( score );
        int strand = s.getStrand();
        pw.printf( Locale.ENGLISH, "%s\t%s\t%s\t%d\t%d\t%g\t%s\t%s\t%s\n", getChromosomeName( s ), source, type, s.getFrom(), s.getTo(),
                score, strand == StrandType.STRAND_PLUS ? "+" : strand == StrandType.STRAND_MINUS ? "-" : ".", frame, group);
    }

    protected void writeGTFSite(Site s, PrintWriter pw)
    {
        DynamicPropertySet p = s.getProperties();
        double score = 0;
        String source = Application.getGlobalValue("ApplicationName");
        String frame = ".";
        String type = s.getType();
        try
        {
            score = s.getScore();
        }
        catch( Exception e )
        {
        }
        try
        {
            source = p.getProperty("source").getValue().toString();
        }
        catch( Exception e )
        {
        }
        try
        {
            frame = p.getProperty("frame").getValue().toString();
        }
        catch( Exception e )
        {
        }
        Set<String> propertyNames = new TreeSet<>(p.asMap().keySet());
        StringBuilder properties = new StringBuilder();
        if( propertyNames.contains("gene_id") )
            properties.append("gene_id \"").append(p.getValue("gene_id")).append("\"; ");
        if( propertyNames.contains("transcript_id") )
            properties.append("transcript_id \"").append(p.getValue("transcript_id")).append("\"; ");
        propertyNames.remove(Site.SCORE_PROPERTY);
        propertyNames.remove("source");
        propertyNames.remove("frame");
        propertyNames.remove("gene_id");
        propertyNames.remove("transcript_id");
        for( String propertyName : propertyNames )
        {
            properties.append(propertyName).append(" \"").append(p.getValue(propertyName)).append("\"; ");
        }
        score = checkScoreByUCSC( score );
        int strand = s.getStrand();
        pw.printf( Locale.ENGLISH, "%s\t%s\t%s\t%d\t%d\t%g\t%s\t%s\t%s", getChromosomeName( s ), source, type, s.getFrom(), s.getTo(),
                score, strand == StrandType.STRAND_PLUS ? "+" : strand == StrandType.STRAND_MINUS ? "-" : ".", frame, properties );
        if( s.getComment() != null && !s.getComment().equals("") )
            pw.print("\t" + s.getComment());
        pw.print("\n");
    }

    protected void writeWiggleSite(Site s, PrintWriter pw)
    {
        Object profileObj = s.getProperties().getValue( "profile" );
        double[] profile;
        if( profileObj instanceof double[] )
        {
            profile = (double[])profileObj;
        }
        else
        {
            profile = new double[s.getLength()];
            Arrays.fill( profile, 1 );
        }
        
        pw.println("fixedStep chrom=" + getChromosomeName( s ) + " start=" + ( s.getFrom() - s.getOriginalSequence().getStart() + 1 ) + " step=1");
        for( double height : profile )
            pw.println(height);
    }
    
    private String getChromosomeName(Site s)
    {
        String chr = s.getOriginalSequence().getName();
        if( getParameters().isPrependChrPrefix() && !chr.startsWith( "chr" ) )
            chr = "chr" + chr;
        return chr;
    }

    /**
     * Write single site into output
     * @param pw output file writer
     * @param s site to write
     */
    protected void writeSite(PrintWriter pw, Site s)
    {
        if( suffix.equalsIgnoreCase("gff") )
            writeGFFSite(s, pw);
        else if( suffix.equalsIgnoreCase("gtf") )
            writeGTFSite(s, pw);
        else if( suffix.equalsIgnoreCase("wig") )
            writeWiggleSite(s, pw);
        else
            writeBEDSite(s, pw);
    }

    @Override
    public void doExport(TrackRegion trackRegion, File file, FunctionJobControl jobControl) throws Exception
    {
        try (PrintWriter pw = new PrintWriter( file, "UTF-8" ))
        {
            String info = "";
            SubSequence subSequence = new SubSequence( trackRegion.getSequenceObject(), trackRegion.getFrom(), trackRegion.getTo() );
            trackRegion = new TrackRegion( trackRegion.getTrack(), subSequence.getCompletePath(), subSequence.getInterval() );
            boolean isBED = suffix.equalsIgnoreCase( "bed" );
            if( isBED && trackRegion.getSequenceName() != null && getParameters() != null && getParameters().isIncludeHeader() )
            {
                pw.printf( Locale.ENGLISH, "browser position chr%s:%d-%d\n", trackRegion.getSequenceName(), trackRegion.getFrom(),
                        trackRegion.getTo() );
            }
            if( trackRegion.getName() != null && !trackRegion.getName().equals( "" ) && getParameters().isIncludeHeader() )
                info = "track name=" + trackRegion.getName() + "\n";
            pw.print( info );
            if( trackRegion.getSequenceObject() == null )
            {
                DataCollection<Site> dc = trackRegion.getTrack().getAllSites();
                Iterator<Site> it = parameters.getIterator( trackRegion );
                int nSites = dc.getSize();
                int i = 0;
                while(it.hasNext())
                {
                    Site s = it.next();
                    writeSite( pw, s );
                    i++;
                    if( i % 5000 == 0 && jobControl != null )
                    {
                        jobControl.setPreparedness( (int)Math.floor( i * 100. / nSites ) );
                        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                        {
                            file.delete();
                            return;
                        }
                    }
                }
            }
            else
            {
                // Split sequence to regions of like 5000 sites each
                // Assuming that sites are distributed evenly through the sequence
                int nSites = trackRegion.countSites();
                for( Interval interval : trackRegion.getInterval().split( nSites / 5000 ) )
                {
                    for( Site s : trackRegion.getSites( interval ) )
                    {
                        if( s.getFrom() < interval.getFrom() && interval.getFrom() > trackRegion.getFrom() )
                            continue;
                        writeSite( pw, s );
                    }
                    if( jobControl != null )
                    {
                        jobControl.setPreparedness( (int)Math.floor( trackRegion.getInterval().getPointPos( interval.getTo() ) * 100 ) );
                        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                        {
                            file.delete();
                            return;
                        }
                    }
                }
            }
        }
        if( jobControl != null && jobControl.getStatus() != JobControl.TERMINATED_BY_REQUEST
                && jobControl.getStatus() != JobControl.TERMINATED_BY_ERROR )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
    }

    @Override
    public boolean init(String format, String suffix)
    {
        if( suffix.equalsIgnoreCase("bed") || suffix.equalsIgnoreCase("gff") || suffix.equalsIgnoreCase("gtf")
                || suffix.equalsIgnoreCase("wig") )
        {
            this.suffix = suffix;
            return true;
        }
        return false;
    }

    @Override
    protected BaseParameters createParameters(DataElement de, File file)
    {
        TrackExporterProperties trackExporterProperties = new TrackExporterProperties( getTrack( de ) );
        if( "bed".equalsIgnoreCase( suffix ) || "gff".equalsIgnoreCase( suffix ) || "gtf".equalsIgnoreCase( suffix ) )
            trackExporterProperties.setHideUCSC( false );
        return trackExporterProperties;
    }
    
    public TrackExporterProperties getParameters()
    {
        return (TrackExporterProperties)parameters;
    }
}
