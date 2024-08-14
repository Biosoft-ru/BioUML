package ru.biosoft.bsa.exporter;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.SubSequence;
import ru.biosoft.bsa.TrackRegion;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

public class IntervalTrackExporter extends TrackExporter
{
    private static final String DELIMETER = "\t";

    private boolean hasStrand;
    private List<String> columnNames;

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

    private String getChromosomeName(Site s)
    {
        String chr = s.getOriginalSequence().getName();
        if( !chr.startsWith( "chr" ) )
        {
            chr = "chr" + chr;
        }

        return chr;
    }

    /**
     * Write single site into output
     * @param pw output file writer
     * @param site site to write
     */
    protected void writeSite(PrintWriter pw, Site site)
    {
        final DynamicPropertySet properties = site.getProperties();

        pw.printf( "%s" + DELIMETER + "%d" + DELIMETER + "%d", getChromosomeName( site ), site.getFrom() - 1, site.getTo() );

        if( hasStrand )
        {
            String strand = "";
            if( site.getStrand() == StrandType.STRAND_PLUS )
            {
                strand = "+";
            }
            else if( site.getStrand() == StrandType.STRAND_MINUS )
            {
                strand = "-";
            }

            pw.printf( DELIMETER + "%s", strand );
        }

        for( String columnName : columnNames )
        {
            final DynamicProperty property = properties.getProperty( columnName );

            if( property != null )
            {
                pw.printf( DELIMETER + "%s", properties.getValue( columnName ) );
            }
            else
            {
                pw.printf( DELIMETER );
            }
        }
        pw.println();
    }

    @Override
    public void doExport(TrackRegion trackRegion, File file, FunctionJobControl jobControl) throws Exception
    {
        hasStrand = false;
        columnNames = new ArrayList<>();

        SubSequence subSequence = new SubSequence( trackRegion.getSequenceObject(), trackRegion.getFrom(), trackRegion.getTo() );
        trackRegion = new TrackRegion( trackRegion.getTrack(), subSequence.getCompletePath(), subSequence.getInterval() );
        
        final DataCollection<Site> siteDataCollection = trackRegion.getTrack().getAllSites();
        Iterator<Site> it = parameters.getIterator( trackRegion );

        try (PrintWriter pw = new PrintWriter( file ))
        {
            writeHeader( pw, siteDataCollection );
            if( trackRegion.getSequenceObject() == null )
            {
                int nSites = siteDataCollection.getSize();
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
            jobControl.setPreparedness( 100 );
            jobControl.functionFinished();
        }
    }

    private void writeHeader(PrintWriter pw, DataCollection<Site> sites)
    {
        final String DEFAULT_HEADER = "#CHROM" + DELIMETER + "START" + DELIMETER + "END";
        final String STRAND = "STRAND";

        boolean needHeader = false;
        StringBuilder header = new StringBuilder(DEFAULT_HEADER);

        for( Site site : sites )
        {
            if( !hasStrand )
            {
                final int strand = site.getStrand();
                if( strand != StrandType.STRAND_NOT_KNOWN )
                {
                    needHeader = true;
                    header.append( DELIMETER ).append( STRAND );
                    hasStrand = true;
                }
            }

            final DynamicPropertySet properties = site.getProperties();
            if( properties.size() != 0 )
            {
                if( !needHeader )
                {
                    needHeader = true;
                }

                List<String> siteColumnNames = new ArrayList<>( properties.asMap().keySet() );
                if( !columnNames.containsAll( siteColumnNames ) )
                {
                    for( String columnName : siteColumnNames )
                    {
                        if( !columnNames.contains( columnName ) )
                        {
                            columnNames.add( columnName );
                        }
                    }
                }
            }
        }

        if( !columnNames.isEmpty() )
        {
            Collections.sort( columnNames );
            for( String columnName : columnNames )
            {
                header.append( DELIMETER ).append( columnName );
            }
        }

        if( needHeader )
        {
            pw.println( header );
        }
    }

    @Override
    public boolean init(String format, String suffix)
    {
        return true;
    }
    
}