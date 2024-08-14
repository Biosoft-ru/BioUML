package biouml.plugins.ensembl.tabletype;

import java.util.regex.Pattern;

import ru.biosoft.analysis.type.ProteinTableType;

/**
 * @author lan
 *
 */
public class NCBIProteinType extends ProteinTableType
{
    private static final Pattern PATTERN = Pattern.compile("gi:\\d+", Pattern.CASE_INSENSITIVE);

    @Override
    public int getIdScore(String id)
    {
        if(PATTERN.matcher(id).matches())
            return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSource()
    {
        return "NCBI";
    }

    @Override
    public String getSampleID()
    {
        return "GI:148491078";
    }
}
