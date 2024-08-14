package biouml.plugins.ensembl.homology;

import java.util.ListResourceBundle;


/**
 * @author lan
 *
 */
public class MessageBundle extends ListResourceBundle
{
    public MessageBundle()
    {
        setParent(new ru.biosoft.analysis.gui.MessageBundle());
    }
    
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            {"PN_SPECIES", "Input species"},
            {"PD_SPECIES", "Species of the input set"},
            {"PN_OUTPUT_SPECIES", "Output species"},
            {"PD_OUTPUT_SPECIES", "Species of the output set"},
        };
    }
}
