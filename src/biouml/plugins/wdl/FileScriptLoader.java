package biouml.plugins.wdl;

import java.io.File;
import java.nio.file.Path;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.wdl.model.ScriptInfo;
import biouml.plugins.wdl.nextflow.NextFlowImporter;

public class FileScriptLoader implements ScriptLoader
{
    public File roodDir;

    public FileScriptLoader(File rootDir)
    {
        this.roodDir = rootDir;
    }

    @Override
    public ScriptInfo loadScript(String path) throws Exception
    {
        //        String rootPath = roodDir.getAbsolutePath();
        Path rootPath = roodDir.toPath();
        Path resultPath = rootPath.resolve( Path.of( path ) );

        File resultFile = resultPath.toFile();

        String result = ApplicationUtils.readAsString( resultFile );
        String name = resultFile.getName();
        name = name.substring( 0, name.indexOf( "." ) );
        ScriptInfo importedScript = new NextFlowImporter().parseNextflow( name, result );
        return importedScript;
        //        File scriptFile = rootDir.
    }

}
