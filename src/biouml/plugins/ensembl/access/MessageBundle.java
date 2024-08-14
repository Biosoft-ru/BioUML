package biouml.plugins.ensembl.access;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            {"CN_ENSEMBL_GENE_DC", "Ensembl Gene Data Collection"},
            {"CD_ENSEMBL_GENE_DC", "Ensembl Gene Data Collection"},
        };
    }

    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch (Throwable th)
        {

        }
        return key;
    }
}


