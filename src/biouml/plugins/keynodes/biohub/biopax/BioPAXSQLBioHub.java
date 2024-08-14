package biouml.plugins.keynodes.biohub.biopax;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Level;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.RelationType;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.exception.BioHubFetchException;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.Cache;
import biouml.model.Diagram;
import biouml.model.xml.XmlDiagramType;
import biouml.plugins.biopax.access.SBGNConverter;
import biouml.plugins.biopax.biohub.BioPAXSQLHubBuilder;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.plugins.keynodes.biohub.KeyNodesSqlHub;
import biouml.plugins.keynodes.graph.ElementConverter;
import biouml.plugins.keynodes.graph.HubEdge;
import biouml.plugins.keynodes.graph.HubGraph;
import biouml.plugins.keynodes.graph.MemoryHubCache;
import biouml.plugins.keynodes.graph.MemoryHubGraph;
import biouml.plugins.keynodes.graph.MemoryHubGraph.HubRelation;
import biouml.standard.type.Base;

/**
 * BioHub working with BioPAX SQL collections. A separate instance of BioPAXSQLBioHub should be created for collection.
 * In {collectionName}_biohub_id table auto-incremented id, accession and complete name in repository are stored.
 * In {collectionName}_biohub links in format up_id|down_id|parent are stored, parent is a reaction id
 */

public class BioPAXSQLBioHub extends KeyNodesSqlHub<String>
{
    private DataCollection<?> dc = null;
    private String tableName = null;
    private ReferenceType[] types = null;

    private final ElementConverter<String> converter;

    private final MemoryHubCache<String> hubCache;

    private static final Query SQL_ACC_TO_ID = new Query("SELECT id,accession FROM $table$_id");
    private static final Query SQL_GET_COMPLETE_NAME = new Query("SELECT complete_name FROM $table$_id WHERE accession=$acc$");

    private final Function<String, DataElementPath> acc2name = Cache.soft( acc -> DataElementPath.create( SqlUtil.queryString( getConnection(),
            SQL_GET_COMPLETE_NAME.raw( "table", tableName ).str( "acc", acc ) ) ) );


    public static class BioPaxRelation implements HubEdge
    {
        private final String title;

        public BioPaxRelation(String title)
        {
            this.title = title;
        }

        @Override
        public Element createElement(KeyNodesHub<?> hub)
        {
            return new Element( ( (BioPAXSQLBioHub)hub ).getCompleteElementPath( title ) );
        }

        @Override
        public String getRelationType(boolean upstream)
        {
            return upstream ? RelationType.REACTANT : RelationType.PRODUCT;
        }

        @Override
        public String toString()
        {
            return title;
        }
    }

    public BioPAXSQLBioHub(Properties properties)
    {
        super(properties);
        init();
        this.converter = ElementConverter.of(
                el -> el.getAccession(),
                n -> new Element(getCompleteElementPath(n)));
        this.hubCache = new MemoryHubCache<>( spec -> {
            try
            {
                // species is ignored
                TIntObjectMap<String> idMap = new TIntObjectHashMap<>();
                SqlUtil.iterate( getConnection(), SQL_ACC_TO_ID.raw( "table", tableName ),
                        rs -> idMap.put( rs.getInt( 1 ), rs.getString( 2 ) ) );
                return SqlUtil.stream(getConnection(), Query.all( tableName ),
                        rs -> new HubRelation<>( idMap.get( rs.getInt( 1 ) ), idMap.get( rs.getInt( 2 ) ), new BioPaxRelation( idMap
                                .get( rs.getInt( 3 ) ) ), 1.0f ) ).collect( MemoryHubGraph.toMemoryHub() );
            }
            catch( Exception e )
            {
                throw new BioHubFetchException( e, this );
            }
        }, converter );
    }

    private void init()
    {
        if( getModulePath() != null )
            dc = getModulePath().optDataCollection();
        if( dc != null )
        {
            ReferenceType type = ReferenceTypeRegistry.getReferenceType(dc);
            if( type != null )
                types = new ReferenceType[] {type};
        }
        if( types == null )
            types = new ReferenceType[0];

        tableName = properties.getProperty(BioPAXSQLHubBuilder.HUB_TABLE_PROPERTY);
        if( tableName == null )
            log.log(Level.SEVERE, "No BioPAX hub table name specified");

        sqlTest = "SELECT * FROM " + tableName + " limit 1";
    }

    @Override
    public ReferenceType[] getSupportedInputTypes()
    {
        return types.clone();
    }

    @Override
    public String getElementTitle(Element element)
    {
        DataElement de = getCompleteElementPath(element.getAccession()).optDataElement();
        if( de instanceof Base )
            return ( (Base)de ).getTitle();
        return element.getAccession();
    }

    @Override
    public DataElementPath getCompleteElementPath(String acc)
    {
        return acc2name.apply( acc );
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
    protected HubGraph<String> getHub(TargetOptions dbOptions, String[] relationTypes)
    {
        return hubCache.get( "hub", dbOptions );
    }

    @Override
    protected ElementConverter<String> getElementConverter()
    {
        return converter;
    }
}
