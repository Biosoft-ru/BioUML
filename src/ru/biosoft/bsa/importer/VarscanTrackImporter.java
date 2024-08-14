package ru.biosoft.bsa.importer;

import java.util.Properties;

import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import com.developmentontheedge.beans.PropertiesDPS;

public class VarscanTrackImporter extends TrackImporter
{
    private String[] columnNames;
    private boolean valid;
    
    @Override
    protected Site parseLine(String line)
    {
        if(columnNames == null) {
           valid = parseHeader(line);
           return null;
        }
        if(!valid)
            return null;
        String[] fields = line.split("\t", 0);
        if(fields.length != columnNames.length)
            return null;
        
        String chr = fields[0];
        int start;
        try
        {
            start = Integer.parseInt(fields[1]);
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        
        String ref = fields[2];
        String alt = fields[3];
        if(alt.startsWith( "+" ))
        {
            alt = ref + alt.substring( 1 );
        }
        else if(alt.startsWith( "-" ))
        {
            String oldAlt = alt;
            alt = ref;
            ref = ref + oldAlt.substring( 1 );
        }
        
        int length = ref.length();
        Properties parameters = new Properties();
        
        parameters.put("RefAllele", ref);
        parameters.put("AltAllele", alt);
        
        for(int i = 4; i < columnNames.length; i++)
            parameters.put(columnNames[i], fields[i]);
        
        return new SiteImpl(null, chr, SiteType.TYPE_VARIATION, Basis.BASIS_USER, start, length, Precision.PRECISION_EXACTLY,
                StrandType.STRAND_NOT_APPLICABLE, null, new PropertiesDPS(parameters));
    }
    
    private boolean parseHeader(String line) throws IllegalArgumentException
    {
        columnNames = line.split("\t", 0);
        return columnNames[0].equals("chrom") && columnNames[1].equals("position") && columnNames[2].equals("ref") && columnNames[3].equals("var");
    }
    
    @Override
    protected void beforeParse()
    {
        columnNames = null;
    }
    
    @Override
    public boolean init(Properties properties)
    {
        super.init(properties);
        format = "varscan";
        return true;
    }
}
