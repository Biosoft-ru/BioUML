package biouml.plugins.ensembl.analysis;

public class MessageBundle extends biouml.workbench.resources.MessageBundle
{
    @Override
    protected Object[][] getContents() { return new Object[][] {
        { "CN_CLASS", "Parameters"},
        { "CD_CLASS", "Parameters"},
    
        {"PN_GENESET_SOURCE_TRACK", "Input tracks"},
        {"PD_GENESET_SOURCE_TRACK", "Track(s) to be converted to genes"},
        {"PN_GENESET_OUTPUTNAME", "Output name"},
        {"PD_GENESET_OUTPUTNAME", "Output name"},
        {"PN_GENESET_FROM", "5' region size"},
        {"PD_GENESET_FROM", "Include 5' region (promoter) of given size in bp"},
        {"PN_GENESET_TO", "3' region size"},
        {"PD_GENESET_TO", "Include 3' region of given size in bp"},
        {"PN_GENESET_RESULT_TYPE", "Types of resulting column"},
        {"PD_GENESET_RESULT_TYPE", "Types of resulting column"},
        {"PN_SPECIES", "Species"},
        {"PD_SPECIES", "Taxonomical species"},
    
        {"PN_INPUT_TRACK", "Input track"},
        {"PD_INPUT_TRACK", "Track to annotate"},
        {"PN_OUTPUT_TRACK", "Output track"},
        {"PD_OUTPUT_TRACK", "Where to store an output"},
    }; }
    
    @Override
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
