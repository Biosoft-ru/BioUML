package biouml.plugins.wdl;

import biouml.plugins.wdl.model.ScriptInfo;

public interface ScriptLoader
{
    public ScriptInfo loadScript(String path) throws Exception;
}
