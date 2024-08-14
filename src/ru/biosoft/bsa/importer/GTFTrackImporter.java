package ru.biosoft.bsa.importer;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * @author lan
 *
 */
public class GTFTrackImporter extends TrackImporter
{
    @Override
    protected Site parseLine(String line)
    {
        String[] fields = line.split("\t");
        if( fields.length < 5 )
            return null;
        
        String sequence = normalizeChromosome(fields[0]);
        if(sequence.startsWith(" ")) return null;
        
        String strand = fields.length < 7 ? "" : fields[6];
        int start, end;
        try
        {
            start = Integer.parseInt(fields[3]);
            end = Integer.parseInt(fields[4]);
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        if( start < 0 )
            start = 0;
        if( end < 0 )
            end = 0;
        if( end - start + 1 < 0 ) return null;
        DynamicPropertySet properties = new DynamicPropertySetAsMap();
        try
        {
            properties.add(new DynamicProperty(Site.SCORE_PD, Float.class, Float.parseFloat(fields[5])));
        }
        catch( Exception e )
        {
        }
        try
        {
            properties.add(new DynamicProperty(getDescriptor("source"), String.class, fields[1]));
        }
        catch( Exception e )
        {
        }
        try
        {
            if(!fields[8].equals("."))
                properties.add(new DynamicProperty(getDescriptor("frame"), String.class, fields[7]));
        }
        catch( Exception e )
        {
        }
        if(fields.length>8)
        {
            String[] attributes = fields[8].split(";\\s+");
            Pattern pattern = Pattern.compile("(\\w+)\\s+\"(.+)\"");
            for(String attribute: attributes)
            {
                Matcher matcher = pattern.matcher(attribute);
                if(matcher.find())
                    properties.add(new DynamicProperty(matcher.group(1), String.class, matcher.group(2)));
            }
        }
        return new SiteImpl(null, sequence, fields[2].equals("") ? null : fields[2], Basis.BASIS_USER, strand.equals("-") ? end : start,
                end - start + 1, Precision.PRECISION_EXACTLY, strand.equals("+") ? StrandType.STRAND_PLUS : strand.equals("-")
                        ? StrandType.STRAND_MINUS : StrandType.STRAND_NOT_KNOWN, null, fields.length > 9 ? fields[9] : null, properties);
    }

    @Override
    public boolean init(Properties properties)
    {
        super.init(properties);
        format = "gtf";
        return true;
    }

}
