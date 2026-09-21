package ru.biosoft.bsa.importer;

import java.util.Properties;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.track.big.BigWigTrackViewBuilder;

public class BedGraphTrackImporter extends TrackImporter
{
    
    @Override
    protected Site parseLine(String line)
    {
        return parseLine(line, null);
    }
    
    public Site parseLine(String line, String name) {
        String[] fields = line.split("\\s");
        String chr = fields[0];
        int start, end;
        float score;
        try
        {
        	start = Integer.parseInt(fields[1]);
        	end = Integer.parseInt(fields[2]);
            score = Float.parseFloat(fields[3]);
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        DynamicPropertySet properties = new DynamicPropertySetAsMap();
        properties.add(new DynamicProperty(Site.SCORE_PD, Float.class, score));
        Sequence seq = getSequence( chr );
        return new SiteImpl(null, name, null, Basis.BASIS_USER, start, end - start + 1, Precision.PRECISION_EXACTLY,
                StrandType.STRAND_NOT_APPLICABLE, seq, properties);
}

    @Override
    protected boolean isComment(String line)
    {
        return super.isComment(line) || line.startsWith("browser") || line.startsWith("track");
    }

    @Override
    public boolean init(Properties properties)
    {
        super.init(properties);
        format = "bedGraph";
        getProperties().getTrackProperties().put(SqlTrack.VIEW_BUILDER, BigWigTrackViewBuilder.class.getName());
        return true;
    }
}
