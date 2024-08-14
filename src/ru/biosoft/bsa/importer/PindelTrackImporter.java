package ru.biosoft.bsa.importer;

import java.util.Properties;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import com.developmentontheedge.beans.PropertiesDPS;

/**
 * @author lan
 *
 */
public class PindelTrackImporter extends TrackImporter
{
    @Override
    protected Site parseLine(String line)
    {
        if(line.startsWith("#")) return null;
        String[] fields = line.split("\\s+");
        if(fields.length < 27) return null;
        String chr = normalizeChromosome(fields[7]);
        int start, length;
        try
        {
            start = Integer.parseInt(fields[9]);
            length = Integer.parseInt(fields[10])-start;
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        Properties parameters = new Properties();
        parameters.put("name", fields[0]);
        parameters.put("NT_length", fields[4]);
        parameters.put("NT_sequence", fields[5].replace("\"", ""));
        try
        {
            parameters.put("BP_range_from", Integer.parseInt(fields[12]));
            parameters.put("BP_range_to", Integer.parseInt(fields[13]));
            parameters.put("Reads", Integer.parseInt(fields[15]));
            parameters.put("Reads_uniq", Integer.parseInt(fields[16]));
            parameters.put("ReadsPlus", Integer.parseInt(fields[18]));
            parameters.put("ReadsPlus_uniq", Integer.parseInt(fields[19]));
            parameters.put("ReadsMinus", Integer.parseInt(fields[21]));
            parameters.put("ReadsMinus_uniq", Integer.parseInt(fields[22]));
            parameters.put("S1", Integer.parseInt(fields[24]));
            parameters.put("SUM_MS", Integer.parseInt(fields[26]));
        }
        catch( NumberFormatException e )
        {
        }
        
        String type = fields[1];
        return new SiteImpl(null, chr, type, Basis.BASIS_USER, start, length, Precision.PRECISION_EXACTLY,
                StrandType.STRAND_NOT_APPLICABLE, null, new PropertiesDPS(parameters));
    }

    @Override
    protected boolean isComment(String line)
    {
        return line.trim().isEmpty() || line.startsWith("#######") || line.matches("\\s*[ATGCacgt].+");
    }

    @Override
    public boolean init(Properties properties)
    {
        super.init(properties);
        format = "pindel";
        return true;
    }
}
