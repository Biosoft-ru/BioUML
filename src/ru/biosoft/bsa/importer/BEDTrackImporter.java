package ru.biosoft.bsa.importer;

import java.util.Properties;

import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * @author lan
 *
 */
public class BEDTrackImporter extends TrackImporter
{
    @Override
    protected Site parseLine(String line)
    {
        return parseBEDLine( line, true );
    }

    public static Site parseBEDLine(String line, boolean normalizeChromosome)
    {
        String[] fields = line.split("\\s");
        if( fields.length < 3 )
            return null;
        String chrom = normalizeChromosome( fields[0], normalizeChromosome );
        int start, length;
        try
        {
            int zeroBasedStart = Integer.parseInt(fields[1]);
            int end = Integer.parseInt(fields[2]);
            length = end - zeroBasedStart;
            start = zeroBasedStart + 1;
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        
        String name = null;
        if( fields.length > 3 )
            name = fields[3];

        int strand = StrandType.STRAND_NOT_KNOWN;
        if( fields.length > 5 )
            if( fields[5].equals("+") )
                strand = StrandType.STRAND_PLUS;
            else if( fields[5].equals("-") )
                strand = StrandType.STRAND_MINUS;
            else if( fields[5].equals( "." ) )
                strand = StrandType.STRAND_NOT_KNOWN;
            else
                return null;
        
        if(strand == StrandType.STRAND_MINUS)
            start = start + length - 1;
        
        if( start < 1 )
            start = 1;
        if( length < 0 )
            length = 0;


        DynamicPropertySet properties = new DynamicPropertySetAsMap();

        if( name != null )
            properties.add(new DynamicProperty(getDescriptor("name"), String.class, name));
        try
        {
            if( fields.length > 4 )
                properties.add(new DynamicProperty(Site.SCORE_PD, Float.class, Float.parseFloat(fields[4])));
        }
        catch( Exception e )
        {
        }
        try
        {
            if( fields.length > 6 )
                properties.add(new DynamicProperty(getDescriptor("thickStart"), Integer.class, Integer.parseInt(fields[6])));
        }
        catch( Exception e )
        {
        }
        try
        {
            if( fields.length > 7 )
                properties.add(new DynamicProperty(getDescriptor("thickEnd"), Integer.class, Integer.parseInt(fields[7])));
        }
        catch( Exception e )
        {
        }
        if( fields.length > 8 )
            properties.add(new DynamicProperty(getDescriptor("itemRGB"), String.class, fields[8]));
        try
        {
            if( fields.length > 9 )
                properties.add(new DynamicProperty(getDescriptor("blockCount"), Integer.class, Integer.parseInt(fields[9])));
        }
        catch( Exception e )
        {
        }
        if( fields.length > 10 )
            properties.add(new DynamicProperty(getDescriptor("blockSizes"), String.class, fields[10]));
        if( fields.length > 11 )
            properties.add(new DynamicProperty(getDescriptor("blockStarts"), String.class, fields[11]));

        return new SiteImpl(null, chrom, SiteType.TYPE_UNSURE, Basis.BASIS_USER, start, length, Precision.PRECISION_EXACTLY, strand, null,
                properties);
    }
    
    @Override
    protected boolean isComment(String line)
    {
        return line.startsWith( "track " ) || line.startsWith( "browser " ) || super.isComment(line);
    }

    @Override
    public boolean init(Properties properties)
    {
        super.init(properties);
        format = "bed";
        return true;
    }

}
