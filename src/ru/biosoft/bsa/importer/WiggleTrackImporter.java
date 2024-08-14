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
public class WiggleTrackImporter extends TrackImporter
{
    private static class WiggleState
    {
        private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("(.+)=(.*)");
        public static final int VARIABLE_STEP = 0;
        public static final int FIXED_STEP = 1;
        public static final int INVALID_MODE = 2;
        int stepMode;
        int span;
        int start;
        int cur;
        int step;
        String chrom;
        public WiggleState()
        {
            stepMode = INVALID_MODE;
        }
        public void initState(String line)
        {
            String[] fields = line.split("\\s");
            if( fields.length < 1 )
                return;
            if( fields[0].equalsIgnoreCase("fixedStep") )
                stepMode = FIXED_STEP;
            else if( fields[0].equalsIgnoreCase("variableStep") )
                stepMode = VARIABLE_STEP;
            else
                return;
            span = 1;
            start = -1;
            step = -1;
            chrom = null;
            for( int i = 1; i < fields.length; i++ )
            {
                Matcher m = KEY_VALUE_PATTERN.matcher(fields[i]);
                if( m.matches() )
                {
                    try
                    {
                        if( m.group(1).equals("chrom") && m.group(2).startsWith("chr") )
                            chrom = m.group(2).substring("chr".length());
                        if( m.group(1).equals("start") )
                            start = Integer.parseInt(m.group(2));
                        if( m.group(1).equals("step") )
                            step = Integer.parseInt(m.group(2));
                        if( m.group(1).equals("span") )
                            span = Integer.parseInt(m.group(2));
                    }
                    catch( NumberFormatException e )
                    {
                    }
                }
            }
            if( ( ( start < 0 || step < 0 ) && stepMode == FIXED_STEP ) || chrom == null || chrom.equals("") )
                stepMode = INVALID_MODE;
            cur = start;
        }
    }

    private WiggleState ws;

    @Override
    protected Site parseLine(String line)
    {
        if( ws == null )
            ws = new WiggleState();
        ws.initState(line);
        if( ws.stepMode == WiggleState.INVALID_MODE )
            return null;
        line = line.trim();
        String[] fields = line.split("\\s");
        int start, end;
        float score;
        try
        {
            if( ws.stepMode == WiggleState.FIXED_STEP )
            {
                start = ws.cur;
                ws.cur += ws.step;
                score = Float.parseFloat(fields[0]);
            }
            else
            // VARIABLE_STEP
            {
                start = Integer.parseInt(fields[0]);
                score = Float.parseFloat(fields[1]);
            }
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        end = start + ws.span - 1;
        DynamicPropertySet properties = new DynamicPropertySetAsMap();
        properties.add(new DynamicProperty(Site.SCORE_PD, Float.class, score));
        return new SiteImpl(null, ws.chrom, null, Basis.BASIS_USER, start, end - start + 1, Precision.PRECISION_EXACTLY,
                StrandType.STRAND_NOT_APPLICABLE, null, properties);
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
        format = "wig";
        return true;
    }
}
