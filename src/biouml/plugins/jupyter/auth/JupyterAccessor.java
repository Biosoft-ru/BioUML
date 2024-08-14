package biouml.plugins.jupyter.auth;

import java.util.List;

public interface JupyterAccessor
{
    public List<String> getAuthCookies(String user, String password);
}
