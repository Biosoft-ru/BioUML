package biouml.plugins.wdl;

import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.model.ScriptInfo;
import biouml.plugins.wdl.nextflow.NextFlowImporter;

/**
 * Class which loads ScriptInfo object on the base of text. 
 * 
 * TODO: automatic script type detection
 * 
 */
public abstract class ScriptLoader
{
    public final static String WDL_TYPE = "WDL";
    public final static String NEXTFLOW_TYPE = "Nextflow";
    
    private String type;
    
    public ScriptLoader(String type)
    {
        this.type = type;
    }
    
    /**
     * @param path - some kind of path for script (i.e. path in file system or path in repository)
     */
    public abstract ScriptInfo loadScript(String path) throws Exception;
    
    protected ScriptInfo readScript(String name, String script) throws Exception
    {
        switch (type)
        {
            case WDL_TYPE:
            {
                WDLImporter importer = new WDLImporter();
                importer.setScriptLoader( this );
                return importer.readScript( name, script );
            }
            case NEXTFLOW_TYPE:
            {
                NextFlowImporter importer = new NextFlowImporter();
                importer.setScriptLoader( this );
                return importer.parseNextflow( name, script );
            }
        }
        return null;
    }
    
}
