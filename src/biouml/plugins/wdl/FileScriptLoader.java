package biouml.plugins.wdl;

import java.io.File;
import java.nio.file.Path;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.wdl.model.ScriptInfo;

public class FileScriptLoader extends ScriptLoader
{
    public File roodDir;

    public FileScriptLoader(String type, File rootDir)
    {
        super(type);
        this.roodDir = rootDir;
    }

    @Override
    public ScriptInfo loadScript(String path) throws Exception
    {
        Path rootPath = roodDir.toPath();
        Path resultPath = rootPath.resolve( Path.of( path ) );

        File resultFile = resultPath.toFile();

        String script = ApplicationUtils.readAsString( resultFile );
        String name = resultFile.getName();
        name = name.substring( 0, name.indexOf( "." ) );
        ScriptInfo importedScript = this.readScript( name, script );
        return importedScript;
    }
}