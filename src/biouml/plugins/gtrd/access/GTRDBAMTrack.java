package biouml.plugins.gtrd.access;

import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.BAMTrack;

public class GTRDBAMTrack extends BAMTrack
{
    private Map<String, String> chrToEnsembl, chrFromEnsembl;
    public GTRDBAMTrack(DataCollection<?> parent, Properties properties, Map<String, String> chrFromEnsembl, Map<String, String> chrToEnsembl)
    {
        super( parent, properties );
        this.chrFromEnsembl = chrFromEnsembl;
        this.chrToEnsembl = chrToEnsembl;
    }
    
    @Override
    public String fromEnsembl(String chr)
    {
        return chrFromEnsembl.getOrDefault( chr, chr );
    }
    
    @Override
    protected String toEnsembl(String chr)
    {
        return chrToEnsembl.getOrDefault( chr, chr );
    }

}
