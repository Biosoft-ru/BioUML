package biouml.plugins.ensembl.biohub;

import java.util.Properties;
import java.util.Set;
import java.util.Collections;
import java.util.function.Function;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.util.Cache;

/**
 * @author lan
 *
 */
public abstract class AbstractEnsemblExternalHub extends AbstractEnsemblHub
{

    public AbstractEnsemblExternalHub(Properties properties)
    {
        super(properties);
    }

    private final Function<ru.biosoft.access.core.DataElementPath, Set<TypeRecord>> enabledTypes = Cache.hard( ensemblPath -> {
        int version = getEnsemblVersion( ensemblPath );
        try
        {
            return StreamEx.of(getSupportedTypeRecords()).filter(type -> type.isVersionSupported(version)).toSet();
        }
        catch( Exception e )
        {
            return Collections.emptySet();
        }
    });

    private static int getEnsemblVersion(DataElementPath ensemblPath)
    {
        try
        {
            return Integer.parseInt( ensemblPath.getDataCollection().getInfo().getProperty( "version" ).split( "[_.]" )[0] );
        }
        catch( Exception e )
        {
            new DataElementReadException(e, ensemblPath, "version").log();
            return 0;
        }
    }
    
    protected synchronized Set<TypeRecord> getEnabledTypes(DataElementPath ensemblPath)
    {
        return this.enabledTypes.apply( ensemblPath );
    }

    protected abstract TypeRecord[] getSupportedTypeRecords();

    protected abstract String getObjectType();
}
