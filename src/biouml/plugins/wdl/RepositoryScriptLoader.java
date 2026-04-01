package biouml.plugins.wdl;

import biouml.model.Diagram;
import biouml.plugins.wdl.model.ScriptInfo;
import biouml.plugins.wdl.nextflow.NextFlowGenerator;
import biouml.plugins.wdl.nextflow.NextFlowImporter;
import ru.biosoft.access.core.DataElementPath;

public class RepositoryScriptLoader implements ScriptLoader
{
    DataElementPath rootPath;
    
    public RepositoryScriptLoader(DataElementPath rootPath)
    {
        this.rootPath = rootPath;
    }

    @Override
    public ScriptInfo loadScript(String path) throws Exception
    {
        DataElementPath scriptPath = DataElementPath.create( rootPath.toString(), path );
        Diagram scriptDiagram = scriptPath.getDataElement( Diagram.class );     
        String nextflow = new NextFlowGenerator(  ).generate( scriptDiagram );
        ScriptInfo importedScript = new NextFlowImporter().parseNextflow( scriptDiagram.getName(), nextflow );
        return importedScript;
    }
}
