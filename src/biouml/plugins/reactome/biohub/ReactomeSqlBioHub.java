package biouml.plugins.reactome.biohub;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.xml.XmlDiagramType;
import biouml.plugins.keynodes.biohub.KeyNodesSqlHub;
import biouml.plugins.keynodes.graph.ElementConverter;
import biouml.plugins.keynodes.graph.HubGraph;
import biouml.plugins.keynodes.graph.MemoryHubCache;
import biouml.plugins.reactome.ReactomeProteinTableType;
import biouml.plugins.reactome.ReactomeSqlUtils;
import biouml.plugins.reactome.sbgn.SBGNConverter;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.exception.BioHubFetchException;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.Cache;
import ru.biosoft.util.ObjectCache;

public class ReactomeSqlBioHub extends KeyNodesSqlHub<ReactomeElement>
{
    public static final String REACTOME_PREFIX = "stub/Reactome//";

    private final ElementConverter<ReactomeElement> converter;

    private final MemoryHubCache<ReactomeElement> hubCache;
    
    private static final String SQL_GET_CLASSES = "SELECT si.identifier, _class "
            + "FROM DatabaseObject dbt INNER JOIN StableIdentifier si ON(dbt.stableIdentifier=si.DB_ID)";
    
    private final Supplier<Map<String, String>> classes = Cache.soft( () -> {
        Map<String, String> map = new HashMap<>();
        ObjectCache<String> classCache = new ObjectCache<>();
        
        SqlUtil.iterate( getConnection(), SQL_GET_CLASSES, rs -> {
            map.put( rs.getString( 1 ), classCache.get( rs.getString( 2 ) ) );
        });
        return map;
    });
    
    //Do not take into account molecules like ATP etc.
    public ReactomeSqlBioHub(Properties properties)
    {
        super(properties);
        sqlTest = "SELECT * FROM DatabaseObject limit 1";
        sqlGetTitle = "SELECT DISTINCT _displayName, _class FROM DatabaseObject dbt INNER JOIN StableIdentifier si"
                + " ON(dbt.stableIdentifier=si.DB_ID) WHERE si.identifier=?";
        
        converter = ElementConverter.of(
                el -> new ReactomeElement( el.getAccession() ),
                n -> new Element(getCompleteElementPath(n.toString())));
        
        hubCache = new MemoryHubCache<>( spec -> {
            try
            {
                // species is ignored
                return new ReactomeHubReader().readHub(getConnection());
            }
            catch( Exception e )
            {
                throw new BioHubFetchException( e, this );
            }
        }, converter );
    }

    //for test issues
    public ReactomeSqlBioHub()
    {
        this(getDefaultProperties());
    }

    private static Properties getDefaultProperties()
    {
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, "Reactome database");
        properties.put(SqlDataCollection.JDBC_URL_PROPERTY, "reactome");
        properties.put(MODULE_NAME_PROPERTY, "databases/Reactome");
        return properties;
    }

    private final ReferenceType[] types = ReferenceTypeRegistry.getReferenceTypes(ReactomeProteinTableType.class);

    @Override
    public ReferenceType[] getSupportedInputTypes()
    {
        return types.clone();
    }

    @Override
    public DataElementPath getCompleteElementPath(String acc)
    {
        String className = classes.get().get( acc );
        if(className == null)
            return DataElementPath.create(REACTOME_PREFIX + acc);
        return getModulePath().getChildPath(Module.DATA, ReactomeSqlUtils.getCollectionNameByClass(className), acc);
    }

    @Override
    public DiagramType getVisualizationDiagramType()
    {
        return new PathwaySimulationDiagramType();
    }

    @Override
    public Diagram convert(Diagram diagram)
    {
        Diagram d = null;
        SBGNConverter converter = new SBGNConverter();
        try
        {
            d = converter.convert(diagram, XmlDiagramType.class);
        }
        catch( Exception e )
        {
            throw new BioHubFetchException( e, this );
        }
        annotateDiagram( d );
        return d;
    }

    @Override
    protected HubGraph<ReactomeElement> getHub(TargetOptions dbOptions, String[] relationTypes)
    {
        return hubCache.get( "hub", dbOptions );
    }

    @Override
    protected ElementConverter<ReactomeElement> getElementConverter()
    {
        return converter;
    }
}
