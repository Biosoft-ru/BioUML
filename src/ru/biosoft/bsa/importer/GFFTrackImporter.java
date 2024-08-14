package ru.biosoft.bsa.importer;

import java.util.HashMap;
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
public class GFFTrackImporter extends TrackImporter
{
    @Override
    protected Site parseLine(String line)
    {
        return parseGFFLine( line );
    }
    public static Site parseGFFLine(String line)
    {
        String[] fields = line.split("\t");
        if( fields.length < 5 )
            return null;
        String chrom = normalizeChromosome(fields[0]);
        String strand = fields.length < 7 ? "." : fields[6];
        if( !strand.equals("+") && !strand.equals("-") && !strand.equals(".") )
            return null;
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
        DynamicPropertySet properties = new DynamicPropertySetAsMap();
        try
        {
            properties.add(new DynamicProperty(Site.SCORE_PD, Float.class, Float.parseFloat(fields[5])));
        }
        catch( Exception e )
        {
        }
        properties.add(new DynamicProperty(getDescriptor("source"), String.class, fields[1]));
        if(!fields[7].equals("."))
            properties.add(new DynamicProperty(getDescriptor("frame"), String.class, fields[7]));
        HashMap<String, String> descrProps = getPropsFromDescr(fields[8]);
        for(String dp : descrProps.keySet())
        {
        	properties.add(new DynamicProperty(getDescriptor(dp), String.class, descrProps.get(dp)));
        }
        return new SiteImpl(null, chrom, fields[2].equals("") ? null : fields[2], Basis.BASIS_USER, strand.equals("-") ? end : start,
                end - start + 1, Precision.PRECISION_EXACTLY, strand.equals("+") ? StrandType.STRAND_PLUS : strand.equals("-")
                        ? StrandType.STRAND_MINUS : StrandType.STRAND_NOT_KNOWN, null, properties);
    }

    @Override
    public boolean init(Properties properties)
    {
        super.init(properties);
        format = "gff";
        return true;
    }

    @Override
    protected boolean isComment(String line)
    {
        return line.startsWith( "track " ) || line.startsWith( "browser " ) || super.isComment(line);
    }
    protected static HashMap<String, String> getPropsFromDescr(String gffDescription)
	{
        String[] pairs = gffDescription.split( ";" );
		HashMap<String,String> pm = new HashMap<>();
        //GFF2 format support
        if( pairs.length == 1 && gffDescription.indexOf( "=" ) == -1 )
            pm.put( "group", pairs[0] );
        else
            for( String p : pairs )
            {
                String[] pair = p.split( "=" );
                if(pair.length != 2)
                    continue;
                //TODO: resolve clashing of case-insensitive properties
                if( pair[0].equals( "note" ) )
                {
                    pair[0] = "note_p";
                }
                pm.put( pair[0], pair[1] );
            }
		return pm;
	}    
}
