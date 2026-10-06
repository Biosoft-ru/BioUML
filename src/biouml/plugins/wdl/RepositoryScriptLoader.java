package biouml.plugins.wdl;

import biouml.model.Diagram;
import biouml.plugins.wdl.model.ScriptInfo;
import ru.biosoft.access.core.DataElementPath;

public class RepositoryScriptLoader extends ScriptLoader
{
    private DataElementPath rootPath;
    
    public RepositoryScriptLoader(String type, DataElementPath rootPath)
    {
        super(type);
        this.rootPath = rootPath;
    }

    @Override
    public ScriptInfo loadScript(String path) throws Exception
    {
        DataElementPath scriptPath = DataElementPath.create( rootPath.toString(), path );
        Diagram scriptDiagram = scriptPath.getDataElement( Diagram.class );     
        String wdl = new WDLGenerator(  ).generate( scriptDiagram );
        ScriptInfo importedScript = this.readScript( scriptDiagram.getName(), wdl );
        return importedScript;
    }
}
