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
public class BreakDancerTrackImporter extends TrackImporter
{
    @Override
    protected Site[] parseLineToMultipleSites(String line)
    {
        String[] fields = line.split("\t");
        if(fields.length < 11) return null;
        String chr = normalizeChromosome(fields[0]);
        String chr2 = normalizeChromosome(fields[3]);
        String type = fields[6];
        int start;
        int start2 = 0;
        int length;
        try
        {
            start = Integer.parseInt(fields[1]);
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        Properties parameters = new Properties();
        if(type.equals("CTX"))
        {
            length = 0;
            parameters.put("Chromosome2", chr2);
            parameters.put("Pos2", fields[4]);
            try
            {
                start2 = Integer.parseInt(fields[4]);
            }
            catch( NumberFormatException e )
            {
                return null;
            }
        } else
        {
            parameters.put("Size", fields[7]);
            try
            {
                length = Integer.parseInt(fields[4])-start;
            }
            catch( NumberFormatException e )
            {
                return null;
            }
        }
        parameters.put("Orientation1", fields[2]);
        parameters.put("Orientation2", fields[5]);
        parameters.put("Score", fields[8]);
        parameters.put("NumReads", fields[9]);
        parameters.put("Lib", fields[10]);
        if(type.equals("CTX"))
        {
            Properties parameters2 = (Properties)parameters.clone();
            parameters2.put("Chromosome2", chr);
            parameters2.put("Pos2", fields[1]);
            return new Site[] {
                    new SiteImpl(null, chr, type+" (from)", Basis.BASIS_USER, start, length, Precision.PRECISION_EXACTLY,
                            StrandType.STRAND_NOT_APPLICABLE, null, new PropertiesDPS(parameters)),
                    new SiteImpl(null, chr2, type+" (to)", Basis.BASIS_USER, start2, length, Precision.PRECISION_EXACTLY,
                            StrandType.STRAND_NOT_APPLICABLE, null, new PropertiesDPS(parameters2))};
        }
        return new Site[]{new SiteImpl(null, chr, type, Basis.BASIS_USER, start, length, Precision.PRECISION_EXACTLY,
                StrandType.STRAND_NOT_APPLICABLE, null, new PropertiesDPS(parameters))};
    }

    @Override
    public boolean init(Properties properties)
    {
        super.init(properties);
        format = "ctx";
        return true;
    }
}
