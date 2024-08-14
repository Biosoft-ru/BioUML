package ru.biosoft.bsa.importer;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import com.developmentontheedge.beans.PropertiesDPS;
import ru.biosoft.util.TextUtil;

/**
 * @author lan
 *
 */
public class GenotypeTrackImporter extends TrackImporter
{
    private static final Pattern POS_PATTERN = Pattern.compile("chr(\\w+):(\\d+)\\-(\\d+)");
    
    @Override
    protected Site parseLine(String line)
    {
        String[] fields = TextUtil.split( line, ' ' );
        if(fields.length < 5) return null;
        Matcher matcher = POS_PATTERN.matcher(fields[1]);
        if(!matcher.matches()) return null;
        String chr = matcher.group(1);
        if(chr.equals("M")) chr = "MT";
        int start, length;
        try
        {
            start = Integer.parseInt(matcher.group(2));
            length = Integer.parseInt(matcher.group(3))-start+1;
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        Properties parameters = new Properties();
        parameters.put("CNV", fields[3]);
        parameters.put("CNV_low", fields[4]);
        parameters.put("Source", fields[2]);
        return new SiteImpl(null, chr, SiteType.TYPE_VARIATION, Basis.BASIS_USER, start, length, Precision.PRECISION_EXACTLY,
                StrandType.STRAND_NOT_APPLICABLE, null, new PropertiesDPS(parameters));
    }

    @Override
    public boolean init(Properties properties)
    {
        super.init(properties);
        format = "genotype";
        return true;
    }
}
