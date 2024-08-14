package biouml.plugins.keynodes.customhub;

import java.util.Properties;

import biouml.model.Module;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.UserHubCollection;

public class UserHubModule extends Module implements UserHubCollection
{

    public UserHubModule(DataCollection<?> origin, Properties properties) throws Exception
    {
        super( origin, properties );
    }

}
