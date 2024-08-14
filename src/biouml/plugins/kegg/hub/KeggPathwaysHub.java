package biouml.plugins.kegg.hub;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionPool;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.Node;
import biouml.plugins.enrichment.FunctionalHubConstants;
import biouml.plugins.enrichment.SqlCachedFunctionalHubSupport;
import biouml.standard.type.Protein;

/**
 * @author lan
 *
 */
public class KeggPathwaysHub extends SqlCachedFunctionalHubSupport
{
    public KeggPathwaysHub(Properties properties)
    {
        super(properties);
    }

    @Override
    protected Iterable<Group> getGroups() throws Exception
    {
        List<Group> groups = new ArrayList<>();
        for(Diagram diagram : getModulePath().getChildPath(Module.DIAGRAM).getDataCollection(Diagram.class))
        {
            Group group = new Group(diagram.getName(), diagram.getTitle());
            diagram.recursiveStream().select( Node.class )
                .map( Node::getKernel ).select( Protein.class )
                .map( Protein::getName ).filter( name -> name.startsWith( "EC " ) )
                .map( name -> name.substring( "EC ".length() ) ).forEach( group::addElement );
            groups.add(group);
        }
        return groups;
    }

    @Override
    protected ReferenceType getInputReferenceType()
    {
        return ReferenceTypeRegistry.getReferenceType(KeggEnzymeType.class);
    }

    @Override
    protected String getTableName()
    {
        return "KeggPathways";
    }

    private static final Properties SUPPORTED_MATCHING = new Properties()
    {{
        setProperty(TYPE_PROPERTY, ReferenceTypeRegistry.getReferenceType(KeggPathwayType.class).toString());
    }};
    @Override
    public Properties[] getSupportedMatching(Properties input)
    {
        if(input.containsKey(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD))
            return new Properties[] {SUPPORTED_MATCHING};
        return null;
    }

    @Override
    protected Connection doGetConnection() throws BiosoftSQLException
    {
        return SqlConnectionPool.getPersistentConnection(properties);
    }
}
