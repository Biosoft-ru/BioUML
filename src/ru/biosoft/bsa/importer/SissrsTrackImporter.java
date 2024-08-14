package ru.biosoft.bsa.importer;

import java.io.BufferedReader;
import java.io.File;
import java.util.Properties;
import java.util.regex.Pattern;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
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
public class SissrsTrackImporter extends TrackImporter
{
    private static final Pattern DELIMITER_LINE_PATTERN = Pattern.compile("={10,}");
    
    private boolean tMode = false;

    @Override
    public int accept(DataCollection<?> parent, File file)
    {
        if( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable(parent, getResultType()) )
            return ACCEPT_UNSUPPORTED;
        if( file == null )
            return ACCEPT_HIGH_PRIORITY;
        try( BufferedReader input = ApplicationUtils.utfReader( file ) )
        {
            String readLine = input.readLine();
            if( readLine == null || !DELIMITER_LINE_PATTERN.matcher(readLine).matches() )
                return ACCEPT_UNSUPPORTED;
            readLine = input.readLine();
            if( !"SISSRs: A tool to identify binding sites from ChIP-Seq data".equals(readLine) )
                return ACCEPT_UNSUPPORTED;
            readLine = input.readLine();
            if( readLine == null || !DELIMITER_LINE_PATTERN.matcher(readLine).matches() )
                return ACCEPT_UNSUPPORTED;
            return ACCEPT_HIGH_PRIORITY;
        }
        catch( Exception e )
        {
        }
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    protected Site parseLine(String line)
    {
        try
        {
            String[] fields = TextUtil.split(line, '\t');
            if(fields[0].equals("Chr"))
            {
                tMode = fields[1].equals("Bsite");
                return null;
            }
            int mandatoryFields = tMode ? 3 : 4;
            if(fields.length < mandatoryFields) return null;
            String chrom = normalizeChromosome(fields[0]);
            int start = Integer.parseInt(fields[1]);
            int length = tMode ? 1 : Integer.parseInt(fields[2])-start;
            Properties properties = new Properties();
            properties.setProperty("tags", String.valueOf(Integer.parseInt(fields[mandatoryFields-1])));
            if(fields.length > mandatoryFields)
            {
                properties.setProperty("fold", String.valueOf(Double.parseDouble(fields[mandatoryFields])));
            }
            if(fields.length > mandatoryFields + 1)
            {
                properties.setProperty("p-value", String.valueOf(Double.parseDouble(fields[mandatoryFields+1])));
            }
            return new SiteImpl(null, chrom, SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, start, length, Precision.PRECISION_NOT_KNOWN,
                    StrandType.STRAND_NOT_APPLICABLE, null, new PropertiesDPS(properties));
        }
        catch( NumberFormatException e )
        {
            return null;
        }
    }

    @Override
    protected boolean isComment(String line)
    {
        return line.isEmpty() || DELIMITER_LINE_PATTERN.matcher(line).matches();
    }

}
