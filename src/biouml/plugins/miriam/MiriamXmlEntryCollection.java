package biouml.plugins.miriam;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import one.util.streamex.MoreCollectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.ReadOnlyVectorCollection;
import ru.biosoft.util.XmlStream;
import ru.biosoft.util.XmlUtil;
import biouml.standard.type.DatabaseInfo;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

public class MiriamXmlEntryCollection extends ReadOnlyVectorCollection<DatabaseInfo>
{
    private final String fileName;

    public MiriamXmlEntryCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super( parent, properties );

        String path = properties.getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY, "." );
        fileName = !path.endsWith( File.separator ) ? path + File.separator + properties.getProperty( DataCollectionConfigConstants.FILE_PROPERTY ) : path
                + properties.getProperty( DataCollectionConfigConstants.FILE_PROPERTY );
    }

    @Override
    protected void doInit()
    {
        try (FileInputStream fis = new FileInputStream( new File( fileName ) ))
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse( fis );

            Element root = document.getDocumentElement();
            XmlStream.elements( root ).filter( child -> getNodeName( child ) != null ).map( this::createCollectionElement )
                    .forEach( di -> doPut( di, true ) );
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE,  "Can not init MIRIAM collection.", e );
        }
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return DatabaseInfo.class;
    }

    private DatabaseInfo createCollectionElement(Element node)
    {
        DatabaseInfo databaseInfo = new DatabaseInfo( getOrigin(), getNodeName( node ) );
        String title = getTextContent( XmlUtil.getChildElement( node, "name" ) );
        if( title == null )
            title = databaseInfo.getName();
        databaseInfo.setTitle( title );
        fillDatabaseInfo( databaseInfo, node );
        return databaseInfo;
    }

    private String getNodeName(Node node)
    {
        Node n = node.getAttributes().getNamedItem( "id" );
        if( n != null )
        {
            String value = n.getNodeValue();
            if( value != null )
                return value;
        }
        return null;
    }

    private void fillDatabaseInfo(DatabaseInfo databaseInfo, Element node)
    {
        String pattern = node.getAttribute( "pattern" );
        if( !pattern.isEmpty() )
            databaseInfo.getAttributes().add( new DynamicProperty( "pattern", String.class, pattern ) );

        for( Element child : XmlUtil.elements( node ) )
        {
            String childName = child.getTagName().toLowerCase( Locale.ENGLISH );

            switch( childName )
            {
                case "definition":
                    databaseInfo.setDescription( getTextContent( child ) );
                    break;
                case "synonyms":
                    String[] synonyms = XmlStream.elements( child, "synonym" ).map( MiriamXmlEntryCollection::getTextContent )
                            .toArray( String[]::new );
                    addAttribute( databaseInfo, "synonyms", synonyms );
                    break;
                case "uris":
                    for( Element uri : XmlStream.elements( child, "uri" ) )
                    {
                        String type = uri.getAttribute( "type" );
                        String deprecated = uri.getAttribute( "deprecated" );
                        String content = getTextContent( uri );
                        if( !"true".equals( deprecated ) )
                        {
                            if( "URN".equals( type ) )
                                databaseInfo.setURN( content );
                            else if( "URL".equals( type ) )
                                databaseInfo.setURL( content );
                        }
                    }
                    Map<String, String[]> uris = XmlStream.elements( child, "uri" )
                            .mapToEntry( doc -> "uri:" + doc.getAttribute( "type" ), MiriamXmlEntryCollection::getTextContent )
                            .grouping( MoreCollectors.toArray( String[]::new ) );
                    uris.forEach( (attr, value) -> addAttribute( databaseInfo, attr, value ) );
                    break;
                case "documentations":
                    Map<String, String[]> docs = XmlStream.elements( child, "documentation" )
                            .mapToEntry( doc -> "documentation:" + doc.getAttribute( "type" ), MiriamXmlEntryCollection::getTextContent )
                            .grouping( MoreCollectors.toArray( String[]::new ) );
                    docs.forEach( (attr, value) -> addAttribute( databaseInfo, attr, value ) );
                    break;
                case "resources":
                    List<DynamicPropertySet> resources = new ArrayList<>();
                    for( Element resource : XmlUtil.elements( child, "resource" ) )
                    {
                        String id = resource.getAttribute( "id" );
                        DynamicPropertySet resourceMap = new DynamicPropertySetAsMap();
                        if( !id.isEmpty() )
                        {
                            resourceMap.add( new DynamicProperty( "id", String.class, id ) );
                        }
                        for( Element field : XmlUtil.elements( resource ) )
                        {
                            String value = getTextContent( field );
                            if( value == null || value.trim().isEmpty() )
                                continue;
                            resourceMap.add( new DynamicProperty( field.getNodeName(), String.class, value ) );
                            if( "dataEntry".equalsIgnoreCase( field.getNodeName() )
                                    && ( null == databaseInfo.getQueryById() || databaseInfo.getQueryById().isEmpty() ) )
                            {
                                if( !value.contains( "$id$" ) )
                                {
                                    value = value.replace( "$id", "$id$" );
                                }
                                databaseInfo.setQueryById( value );
                                value = value.replace( "$id$", "$ac$" );
                                databaseInfo.setQueryByAc( value );
                            }
                            if( "dataResource".equalsIgnoreCase( field.getNodeName() ) && null == databaseInfo.getURL() )
                            {
                                databaseInfo.setURL( value );
                            }
                        }
                        resources.add( resourceMap );
                    }
                    if( !resources.isEmpty() )
                        databaseInfo.getAttributes()
                                .add( new DynamicProperty( "resources", DynamicPropertySet[].class, resources
                                        .toArray( new DynamicPropertySet[0] ) ) );
                    break;
                case "tags":
                    String[] tags = XmlStream.elements( child, "tag" ).map( MiriamXmlEntryCollection::getTextContent )
                            .toArray( String[]::new );
                    addAttribute( databaseInfo, "tags", tags );
                    break;
                default:
                    break;
            }
        }
    }

    private void addAttribute(DatabaseInfo databaseInfo, String attributeName, String[] value)
    {
        databaseInfo.getAttributes().add( new DynamicProperty( attributeName, String[].class, value ) );
    }

    public static String getTextContent(Element root)
    {
        if( root == null )
            return null;
        return XmlStream.nodes( root ).select( Text.class ).findFirst().map( Text::getData ).orElse( null );
    }
}
