package biouml.plugins.gxl;

import java.util.logging.Logger;

abstract public class GxlSupport implements GxlConstants
{
    protected Logger log;

    protected GxlSupport()
    {
        log = initLog();
    }

    abstract protected Logger initLog();

    protected void warn(String key, String[] params)
    {
        MessageBundle.warn(log, key, params);
    }

    protected void error(String key, String[] params)
    {
        MessageBundle.error(log, key, params);
    }
}