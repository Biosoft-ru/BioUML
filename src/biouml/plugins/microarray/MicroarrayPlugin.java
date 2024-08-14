package biouml.plugins.microarray;

import java.util.logging.Logger;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.Plugin;

public class MicroarrayPlugin extends Plugin
{
    protected static final Logger log = Logger.getLogger(MicroarrayPlugin.class.getName());

    private static String modulePath;

    private static MicroarrayPlugin instance;
    public static MicroarrayPlugin getInstance()
    {
        return instance;
    }

    public MicroarrayPlugin(IPluginDescriptor descriptor)
    {
        super(descriptor);
        instance = this;
    }

    @Override
    public void startup()
    {
        if( modulePath == null )
            modulePath = "data";
    }
}
