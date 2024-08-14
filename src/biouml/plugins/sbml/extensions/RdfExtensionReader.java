package biouml.plugins.sbml.extensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.standard.type.DatabaseInfo;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.DiagramInfo.AuthorInfo;
import biouml.standard.type.Referrer;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.XmlUtil;

public class RdfExtensionReader extends SbmlExtensionSupport
{
    private static final String RDF_PARSE_TYPE = "rdf:parseType";
    private static final String V_CARD_N = "vCard:N";
    private static final String V_CARD_ORGNAME = "vCard:Orgname";
    private static final String V_CARD_ORG = "vCard:ORG";
    private static final String V_CARD_EMAIL = "vCard:EMAIL";
    private static final String V_CARD_GIVEN = "vCard:Given";
    private static final String V_CARD_FAMILY = "vCard:Family";

    public static final String RDF_ELEMENT = "rdf:RDF";
    public static final String DESCRIPTION_ELEMENT = "rdf:Description";
    public static final String BAG_ELEMENT = "rdf:Bag";
    public static final String LI_ELEMENT = "rdf:li";
    public static final String RESOURCE_ATTRIBUTE = "rdf:resource";
    public static final String ABOUT_ATTRIBUTE = "rdf:about";
    public static final String URN_MIRIAM = "urn:miriam";

    public static final String DCCREATOR_ELEMENT = "dc:creator";

    public static final String DCTERMS_ELEMENT = "dcterms:W3CDTF";
    public static final String DCTERMSCREATOR_ELEMENT = "dcterms:creator";
    public static final String DCTERMSCREATED_ELEMENT = "dcterms:created";
    public static final String DCTERMSMODIFIED_ELEMENT = "dcterms:modified";

    public static final String BQBIOL = "bqbiol";
    public static final String BQMODEL = "bqmodel";

    public static final String ISDERIVEDFROM_ELEMENT = "isDerivedFrom";
    public static final String IS_ELEMENT = "is";
    public static final String HASINSTANCE_ELEMENT = "hasInstance";
    public static final String ISINSTANCEOF_ELEMENT = "isInstanceOf";
    public static final String HASPART_ELEMENT = "hasPart";
    public static final String HASPROPERTY_ELEMENT = "hasProperty";
    public static final String ISPARTOF_ELEMENT = "isPartOf";
    public static final String ISVERSIONOF_ELEMENT = "isVersionOf";
    public static final String HASVERSION_ELEMENT = "hasVersion";
    public static final String ISHOMOLOGTO_ELEMENT = "isHomologTo";
    public static final String ISDESCRIBEDBY_ELEMENT = "isDescribedBy";
    public static final String ENCODES_ELEMENT = "encodes";
    public static final String ISENCODEDBY_ELEMENT = "isEncodedBy";
    public static final String OCCURSIN_ELEMENT = "occursIn";
    public static final String HASTAXON_ELEMENT = "hasTaxon";
    public static final String ISPROPERTYOF_ELEMENT = "isPropertyOf";

    public static final String XMLNS_RDF = "xmlns:rdf";
    public static final String XMLNS_RDF_VALUE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String XMLNS_DC = "xmlns:dc";
    public static final String XMLNS_DC_VALUE = "http://purl.org/dc/elements/1.1/";
    public static final String XMLNS_DCTERM = "xmlns:dcterms";
    public static final String XMLNS_DCTERM_VALUE = "http://purl.org/dc/terms/";
    public static final String XMLNS_VCARD = "xmlns:vCard";
    public static final String XMLNS_VCARD_VALUE = "http://www.w3.org/2001/vcard-rdf/3.0#";
    public static final String XMLNS_BQBIOL = "xmlns:bqbiol";
    public static final String XMLNS_BQBIOL_VALUE = "http://biomodels.net/biology-qualifiers/";
    public static final String XMLNS_BQMODEL = "xmlns:bqmodel";
    public static final String XMLNS_BQMODEL_VALUE = "http://biomodels.net/model-qualifiers/";

    public static final DataElementPath MIRIAM_RESOURCE = DataElementPath.create( "databases/Utils/MIRIAM" );

    protected Logger log = Logger.getLogger( RdfExtensionReader.class.getName() );

    private Map<String, DatabaseInfo> miriamUrnToInfo;

    private static final List<String> qualifiers = Collections
            .unmodifiableList( Arrays.asList( IS_ELEMENT, HASINSTANCE_ELEMENT, ISINSTANCEOF_ELEMENT, ISDERIVEDFROM_ELEMENT,
                    ISDESCRIBEDBY_ELEMENT, HASPART_ELEMENT, HASPROPERTY_ELEMENT, ISPARTOF_ELEMENT, ISVERSIONOF_ELEMENT, HASVERSION_ELEMENT,
                    ISHOMOLOGTO_ELEMENT, ENCODES_ELEMENT, ISENCODEDBY_ELEMENT, OCCURSIN_ELEMENT, HASTAXON_ELEMENT, ISPROPERTYOF_ELEMENT ) );

    public RdfExtensionReader()
    {
        try
        {
            miriamUrnToInfo = StreamEx.of( MIRIAM_RESOURCE.getDataCollection( DatabaseInfo.class ).stream() )
                    .<String> cross(
                            di -> StreamEx.ofNullable( (String[])di.getAttributes().getValue( "uri:URN" ) ).flatMap( Arrays::stream ) )
                    .invert().toMap( (a, b) -> a ); // select first merge strategy used
        }
        catch( RepositoryException e )
        {
            miriamUrnToInfo = Collections.emptyMap();
        }
    }

    public static List<String> getQualifiers()
    {
        return qualifiers;
    }

    public DatabaseReference[] readElement(Element element)
    {
        if( !element.getNodeName().equals( RDF_ELEMENT ) )
            return null;

        Element description = getElement( element, DESCRIPTION_ELEMENT );
        List<DatabaseReference> databaseReferences = new ArrayList<>();
        List<String> literatureReferences = new ArrayList<>();

        for( Element node : XmlUtil.elements( description ) )
        {
            String referenceType = node.getNodeName();
            if( referenceType.startsWith( BQBIOL ) )
                referenceType = referenceType.substring( BQBIOL.length() + 1, referenceType.length() );
            if( referenceType.startsWith( BQMODEL ) )
                referenceType = referenceType.substring( BQMODEL.length() + 1, referenceType.length() );
            if( checkRelationshipType( referenceType ) )
            {
                Element bag = getElement( node, BAG_ELEMENT );
                if( bag != null )
                    readBag( bag, databaseReferences, literatureReferences, referenceType );
            }
            else if( !referenceType.equals( DCCREATOR_ELEMENT ) && !referenceType.equals( DCTERMSCREATED_ELEMENT )
                    && !referenceType.equals( DCTERMSMODIFIED_ELEMENT ) && !referenceType.equals( DCTERMSCREATOR_ELEMENT ) )
            {
                log.log( Level.SEVERE, "Can not read reference. Reason: unknown relationship type " + referenceType );
            }
        }

        if( !databaseReferences.isEmpty() )
            return databaseReferences.toArray( new DatabaseReference[0] );
        return new DatabaseReference[0];
    }

    @Override
    public void readElement(Element element, DiagramElement specie, @Nonnull Diagram diagram)
    {
        if( !element.getNodeName().equals( RDF_ELEMENT ) )
            return;

        if( specie.getKernel() instanceof DiagramInfo )
        {
            readAuthors( element, (DiagramInfo)diagram.getKernel() );
            readTerms( element, (DiagramInfo)diagram.getKernel() );
        }

        Element description = getElement( element, DESCRIPTION_ELEMENT );
        if( description == null || ! ( specie.getKernel() instanceof Referrer ) )
            return;
        Referrer referrer = (Referrer)specie.getKernel();
        List<DatabaseReference> databaseReferences = new ArrayList<>();
        List<String> literatureReferences = new ArrayList<>();

        for( Element node : XmlUtil.elements( description ) )
        {
            String referenceType = node.getNodeName();
            if( referenceType.startsWith( BQBIOL ) )
                referenceType = referenceType.substring( BQBIOL.length() + 1, referenceType.length() );
            if( referenceType.startsWith( BQMODEL ) )
                referenceType = referenceType.substring( BQMODEL.length() + 1, referenceType.length() );
            if( checkRelationshipType( referenceType ) )
            {
                Element bag = getElement( node, BAG_ELEMENT );
                if( bag != null )
                {
                    readBag( bag, databaseReferences, literatureReferences, referenceType );
                }
            }
            else if( !referenceType.equals( DCCREATOR_ELEMENT ) && !referenceType.equals( DCTERMSCREATED_ELEMENT )
                    && !referenceType.equals( DCTERMSMODIFIED_ELEMENT ) && !referenceType.equals( DCTERMSCREATOR_ELEMENT ) )
            {
                log.log( Level.SEVERE, "Can not read reference. Reason: unknown relationship type " + referenceType );
            }
        }

        if( !databaseReferences.isEmpty() )
        {
            referrer.setDatabaseReferences( databaseReferences.toArray( new DatabaseReference[0] ) );
        }
        if( !literatureReferences.isEmpty() )
        {
            referrer.setLiteratureReferences( literatureReferences.toArray( new String[0] ) );
        }
    }

    protected void readBag(Element element, List<DatabaseReference> databaseReferences, List<String> literatureReferences, String refType)
    {
        for( Element li : XmlUtil.elements( element, LI_ELEMENT ) )
        {
            if( li.hasAttribute( RESOURCE_ATTRIBUTE ) )
            {
                try
                {
                    String address = li.getAttribute( RESOURCE_ATTRIBUTE );
                    String databaseName = null;
                    String id = null;

                    if( address.startsWith( URN_MIRIAM ) )
                    {
                        if( address.startsWith( URN_MIRIAM + ":ENS" ) )
                        {
                            databaseName = "urn:miriam:ensembl";

                            id = TextUtil.decodeURL( address.substring( URN_MIRIAM.length() + 1 ) );
                        }
                        else
                        {
                            int pos = address.indexOf( ':', URN_MIRIAM.length() + 1 );
                            if( pos == -1 )
                            {
                                log.warning( "Unparseable database reference: " + address );
                                continue;
                            }
                            databaseName = address.substring( 0, pos );
                            id = TextUtil.decodeURL( address.substring( pos + 1 ) );
                        }

                        DatabaseInfo info = miriamUrnToInfo.get( databaseName );
                        if( info != null )
                        {
                            databaseName = info.getName();
                        }
                    }
                    else if( address.indexOf( '#' ) != -1 )
                    {
                        int pos = address.indexOf( '#' );
                        databaseName = address.substring( 0, pos );
                        id = address.substring( pos + 1, address.length() );
                    }

                    if( databaseName != null && id != null )
                    {
                        if( refType.equals( ISDESCRIBEDBY_ELEMENT ) )
                        {
                            literatureReferences.add( id );
                        }
                        else
                        {
                            // Delete erroneous EC-prefix from ec-code URL (appears in some SBML files in a wild)
                            if( databaseName.equals( "MIR:00000004" ) )
                            {
                                String[] ids = TextUtil.split( id, ';' );
                                for( String singleId : ids )
                                {
                                    if( singleId.startsWith( "EC" ) )
                                        singleId = singleId.substring( 3 );
                                    DatabaseReference dr = new DatabaseReference( databaseName, singleId );
                                    dr.setRelationshipType( refType );
                                    databaseReferences.add( dr );
                                }
                            }
                            else
                            {
                                DatabaseReference dr = new DatabaseReference( databaseName, id );
                                dr.setRelationshipType( refType );
                                databaseReferences.add( dr );
                            }
                        }
                    }

                    if( address.startsWith( "http://identifiers.org/" ) )
                    {
                        address = address.substring( 23 );
                        //                        String[] parts = address.split("/");
                        //                        databaseName = parts[0];
                        //                        id = parts.length > 1 ? parts[1] : "";
                        int index = address.indexOf( "/" );
                        databaseName = address.substring( 0, index );
                        id = address.substring( index + 1, address.length() );
                        DatabaseReference dr = new DatabaseReference( databaseName, id );
                        dr.setRelationshipType( refType );
                        databaseReferences.add( dr );
                    }
                    else if( address.startsWith( "https://identifiers.org/" ) )
                    {
                        address = address.substring( 24 );
                        int index = address.indexOf( "/" );
                        databaseName = address.substring( 0, index );
                        id = address.substring( index + 1, address.length() );
                        //                        String[] parts = address.split( "/" );
                        //                        databaseName = parts[0];
                        //                        id = parts.length > 1 ? parts[1] : "";
                        DatabaseReference dr = new DatabaseReference( databaseName, id );
                        dr.setRelationshipType( refType );
                        databaseReferences.add( dr );
                    }
                }
                catch( Exception e )
                {
                    log.log( Level.SEVERE, "Rd fExtensionReader: can not read reference element: " + ExceptionRegistry.log( e ) );
                }
            }
        }
    }

    private String[] parseRDFResource(String URI)
    {
        if( URI.startsWith( "http://identifiers.org/" ) )
            URI = URI.substring( 23 );
        return URI.split( "/" );
    }

    protected void readTerms(Element rdf, DiagramInfo info)
    {
        Element created = XmlUtil.findElementByTagName( rdf, "dcterms:created" );
        if( created != null )
        {
            Element date = XmlUtil.findElementByTagName( created, "dcterms:W3CDTF" );
            if( date != null )
                info.setCreated( XmlUtil.getTextContent( date ) );
        }
        List<String> modifiedList = new ArrayList<>();
        for( Element modified : XmlUtil.elements( rdf, "dcterms:modified" ) )
        {
            Element date = XmlUtil.findElementByTagName( modified, "dcterms:W3CDTF" );
            if( date != null )
                modifiedList.add( XmlUtil.getTextContent( date ) );
        }

        info.setModified( modifiedList.toArray( new String[modifiedList.size()] ) );
    }

    protected void readAuthors(Element element, DiagramInfo info)
    {
        List<AuthorInfo> authors = new ArrayList<AuthorInfo>();
        Element creator = XmlUtil.findElementByTagName( element, DCCREATOR_ELEMENT );//old style
        if( creator == null )
            creator = XmlUtil.findElementByTagName( element, DCTERMSCREATOR_ELEMENT );
        if( creator == null )
            return;
        Element bag = XmlUtil.findElementByTagName( creator, BAG_ELEMENT );
        if( bag == null )
            return;
        for( Element li : XmlUtil.elements( bag, LI_ELEMENT ) )
        {
            AuthorInfo author = new AuthorInfo();
            authors.add( author );
            try
            {
                Element vCardN = XmlUtil.findElementByTagName( li, V_CARD_N );
                if( vCardN != null )
                {
                    Element vCardFamily = XmlUtil.findElementByTagName( vCardN, V_CARD_FAMILY );
                    if( vCardFamily != null )
                        author.setFamilyName( XmlUtil.getTextContent( vCardFamily ) );

                    Element vCardGiven = XmlUtil.findElementByTagName( vCardN, V_CARD_GIVEN );
                    if( vCardGiven != null )
                        author.setGivenName( XmlUtil.getTextContent( vCardGiven ) );
                }
                Element vCardORG = XmlUtil.findElementByTagName( li, V_CARD_ORG );
                if( vCardORG != null )
                {
                    Element orgName = XmlUtil.findElementByTagName( vCardORG, V_CARD_ORGNAME );
                    if( orgName != null )
                        author.setOrgName( XmlUtil.getTextContent( vCardORG ) );
                }
                Element vCardEMAIL = XmlUtil.findElementByTagName( li, V_CARD_EMAIL );
                if( vCardEMAIL != null )
                    author.setEmail( XmlUtil.getTextContent( vCardEMAIL ) );

            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "Athour can not be parsed" + ExceptionRegistry.log( e ) );
            }
        }
        info.setAuthors( authors.toArray( new AuthorInfo[authors.size()] ) );
    }

    public static DatabaseReference[] getDatabaseReferences(DatabaseReference[] refs, String relationshipType)
    {
        if( refs == null )
            return new DatabaseReference[0];
        return StreamEx.of( refs ).filter( ref -> ref.getRelationshipType().equals( relationshipType ) )
                .toArray( DatabaseReference[]::new );
    }

    public Element[] writeElement(DatabaseReference[] references, Document document)
    {
        Element rdf = document.createElement( RDF_ELEMENT );
        rdf.setAttribute( XMLNS_RDF, XMLNS_RDF_VALUE );
        rdf.setAttribute( XMLNS_DC, XMLNS_DC_VALUE );
        rdf.setAttribute( XMLNS_DCTERM, XMLNS_DCTERM_VALUE );
        rdf.setAttribute( XMLNS_VCARD, XMLNS_VCARD_VALUE );
        rdf.setAttribute( XMLNS_BQBIOL, XMLNS_BQBIOL_VALUE );
        rdf.setAttribute( XMLNS_BQMODEL, XMLNS_BQMODEL_VALUE );

        Element description = document.createElement( DESCRIPTION_ELEMENT );

        Element relation = null;
        Set<String> types = new TreeSet<>();

        for( DatabaseReference databasereference : references )
        {
            String nextType = databasereference.getRelationshipType();
            if( checkRelationshipType( nextType ) )
            {
                if( !types.contains( nextType ) )
                {
                    DatabaseReference[] filteredReferences = getDatabaseReferences( references, nextType );
                    String rdfType = getRdfType( nextType, false );
                    relation = writeRelationElement( filteredReferences, rdfType, document );
                    types.add( nextType );

                    if( relation != null )
                        description.appendChild( relation );
                }
            }
            else
            {
                log.info( "Unknown relationship type: " + nextType + ". Database references with this type was removed." );
            }
        }

        if( description.getChildNodes() != null && description.getChildNodes().getLength() != 0 )
            rdf.appendChild( description );

        if( rdf.getChildNodes().getLength() != 0 )
            return new Element[] {rdf};
        return null;
    }

    @Override
    public Element[] writeElement(DiagramElement specie, Document document)
    {
        if( ! ( specie.getKernel() instanceof Referrer ) )
            return null;

        DatabaseReference[] databasereferences = ( (Referrer)specie.getKernel() ).getDatabaseReferences();
        String[] literaturereferences = ( (Referrer)specie.getKernel() ).getLiteratureReferences();

        boolean writeAuthors = specie instanceof Diagram && ( (DiagramInfo)specie.getKernel() ).getAuthors().length > 0;
        boolean writeTerms = specie instanceof Diagram;
        boolean writeReferences = ( databasereferences != null && databasereferences.length > 0 )
                || ( literaturereferences != null && literaturereferences.length > 0 );

        if( writeReferences || writeAuthors )
        {
            Element rdf = document.createElement( RDF_ELEMENT );
            rdf.setAttribute( XMLNS_RDF, XMLNS_RDF_VALUE );
            rdf.setAttribute( XMLNS_DC, XMLNS_DC_VALUE );
            rdf.setAttribute( XMLNS_DCTERM, XMLNS_DCTERM_VALUE );
            rdf.setAttribute( XMLNS_VCARD, XMLNS_VCARD_VALUE );
            rdf.setAttribute( XMLNS_BQBIOL, XMLNS_BQBIOL_VALUE );
            rdf.setAttribute( XMLNS_BQMODEL, XMLNS_BQMODEL_VALUE );

            if( writeAuthors )
                writeAuthors( (Diagram)specie, document, rdf );

            if( writeTerms )
                writeTerms( (Diagram)specie, document, rdf );

            Element description = document.createElement( DESCRIPTION_ELEMENT );
            description.setAttribute( ABOUT_ATTRIBUTE, "#" + specie.getName() );

            boolean isModelAnnotated = false;
            if( Diagram.getDiagram( specie ).equals( specie ) )
                isModelAnnotated = true;

            Element relation = null;
            Set<String> types = new TreeSet<>();
            if( databasereferences != null )
            {
                for( DatabaseReference databasereference : databasereferences )
                {
                    String nextType = databasereference.getRelationshipType();
                    if( checkRelationshipType( nextType ) )
                    {
                        if( !types.contains( nextType ) )
                        {
                            DatabaseReference[] filteredReferences = ( (Referrer)specie.getKernel() ).getDatabaseReferences( nextType );
                            String rdfType = getRdfType( nextType, isModelAnnotated );
                            relation = writeRelationElement( filteredReferences, rdfType, document );
                            types.add( nextType );

                            if( relation != null )
                                description.appendChild( relation );
                        }
                    }
                    else
                    {
                        log.info( "Unknown relationship type: " + nextType + ". Database references with this type was removed." );
                    }
                }
            }
            if( literaturereferences != null && literaturereferences.length > 0 )
            {
                String rdfType = getRdfType( ISDESCRIBEDBY_ELEMENT, isModelAnnotated );
                relation = writeRelationElement( literaturereferences, rdfType, document );

                if( relation != null )
                    description.appendChild( relation );
            }

            if( description.getChildNodes() != null && description.getChildNodes().getLength() != 0 )
                rdf.appendChild( description );

            if( rdf.getChildNodes().getLength() != 0 )
                return new Element[] {rdf};
        }
        return null;
    }

    private void writeAuthors(Diagram diagram, Document doc, Element rdf)
    {
        DiagramInfo info = (DiagramInfo)diagram.getKernel();
        AuthorInfo[] authors = info.getAuthors();
        Element creator = doc.createElement( DCTERMSCREATOR_ELEMENT );
        Element bag = doc.createElement( BAG_ELEMENT );
        for( int i = 0; i < authors.length; i++ )
        {
            AuthorInfo author = authors[i];
            Element li = doc.createElement( LI_ELEMENT );
            li.setAttribute( RDF_PARSE_TYPE, RESOURCE_ATTRIBUTE );

            Element vCardN = doc.createElement( V_CARD_N );
            vCardN.setAttribute( RDF_PARSE_TYPE, RESOURCE_ATTRIBUTE );
            if( author.getFamilyName() != null )
            {
                Element vCardFamily = doc.createElement( V_CARD_FAMILY );
                vCardFamily.appendChild( doc.createTextNode( author.getFamilyName() ) );
                vCardN.appendChild( vCardFamily );
            }
            if( author.getGivenName() != null )
            {
                Element vCardGiven = doc.createElement( V_CARD_GIVEN );
                vCardGiven.appendChild( doc.createTextNode( author.getGivenName() ) );
                vCardN.appendChild( vCardGiven );
            }
            if( vCardN.hasChildNodes() )
                li.appendChild( vCardN );

            if( author.getEmail() != null )
            {
                Element vCardEmail = doc.createElement( V_CARD_EMAIL );
                vCardEmail.appendChild( doc.createTextNode( author.getEmail() ) );
                li.appendChild( vCardEmail );
            }

            if( author.getOrgName() != null )
            {
                Element vCardORG = doc.createElement( V_CARD_ORG );
                Element vCardOrgname = doc.createElement( V_CARD_ORGNAME );
                vCardOrgname.appendChild( doc.createTextNode( author.getOrgName() ) );
                vCardORG.appendChild( vCardOrgname );
                li.appendChild( vCardORG );
            }

            if( li.hasChildNodes() )
                bag.appendChild( li );
        }

        if( bag.hasChildNodes() )
        {
            creator.appendChild( bag );
            rdf.appendChild( creator );
        }
    }


    public void writeTerms(Diagram diagram, Document doc, Element rdf)
    {
        DiagramInfo info = (DiagramInfo)diagram.getKernel();
        String date = info.getCreated();
        if( date != null && !date.isEmpty() )
        {
            Element created = doc.createElement( DCTERMSCREATED_ELEMENT );
            Element w3cdtf = doc.createElement( DCTERMS_ELEMENT );
            w3cdtf.appendChild( doc.createTextNode( date ) );
            created.appendChild( w3cdtf );
            rdf.appendChild( created );
        }

        String[] modified = info.getModified();
        Element modfiedElement = doc.createElement( DCTERMSMODIFIED_ELEMENT );
        for( int i = 0; i < modified.length; i++ )
        {
            if( ! ( modified[i].isEmpty() ) )
            {
                Element w3cdtf = doc.createElement( DCTERMS_ELEMENT );
                w3cdtf.appendChild( doc.createTextNode( modified[i] ) );
                modfiedElement.appendChild( w3cdtf );
            }

        }
        if( modfiedElement.hasChildNodes() )
            rdf.appendChild( modfiedElement );
    }


    protected Element writeRelationElement(DatabaseReference[] references, String type, Document document)
    {
        if( type != null )
        {
            Element relation = document.createElement( type );
            Element bag = document.createElement( BAG_ELEMENT );

            try
            {
                for( DatabaseReference dr : references )
                {
                    Element drElement = document.createElement( LI_ELEMENT );

                    DataCollection<DatabaseInfo> miriamCollection;
                    try
                    {
                        miriamCollection = MIRIAM_RESOURCE.getDataCollection( DatabaseInfo.class );
                    }
                    catch( RepositoryException e )
                    {
                        miriamCollection = null;
                    }
                    if( miriamCollection != null )
                    {
                        DatabaseInfo info = miriamCollection.get( dr.getDatabaseName() );
                        if( info != null )
                            drElement.setAttribute( RESOURCE_ATTRIBUTE, info.getURN() + ":" + TextUtil.encodeURL( dr.getId() ) );
                        else
                        {
                            //                            String divider = dr.getDatabaseName().startsWith( URN_MIRIAM ) ? ":" : "/";
                            String prefix = "https://identifiers.org/";
                            drElement.setAttribute( RESOURCE_ATTRIBUTE, prefix + dr.getDatabaseName() + "/" + dr.getId() );//TextUtil.encodeURL( dr.getId() ) );
                        }
                    }
                    else
                    {
                        String divider = dr.getDatabaseName().startsWith( URN_MIRIAM ) ? ":" : "/";
                        String prefix = "https://identifiers.org/";
                        drElement.setAttribute( RESOURCE_ATTRIBUTE, prefix + dr.getDatabaseName() + divider + dr.getId() );//TextUtil.encodeURL( dr.getId() ) );
                    }
                    bag.appendChild( drElement );
                }

                relation.appendChild( bag );
                return relation;
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "RdfExtensionReader: can not write relation elements:" + e );
            }
        }
        return null;
    }

    protected Element writeRelationElement(String[] references, String type, Document document)
    {
        Element relation = document.createElement( type );
        Element bag = document.createElement( BAG_ELEMENT );

        try
        {
            for( String dr : references )
            {
                Element drElement = document.createElement( LI_ELEMENT );

                DataCollection<DatabaseInfo> miriamCollection;
                try
                {
                    miriamCollection = MIRIAM_RESOURCE.getDataCollection( DatabaseInfo.class );
                }
                catch( RepositoryException e )
                {
                    miriamCollection = null;
                }
                if( miriamCollection != null )
                {
                    DatabaseInfo info = miriamCollection.get( "MIR:00000015" ); //PubMed database ID
                    drElement.setAttribute( RESOURCE_ATTRIBUTE, info.getURN() + ":" + dr );
                }
                else
                {
                    drElement.setAttribute( RESOURCE_ATTRIBUTE, "urn:miriam:pubmed:" + dr );
                }
                bag.appendChild( drElement );
            }

            relation.appendChild( bag );
            return relation;
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "RdfExtensionReader: can not write relation elements:" + e );
        }
        return null;
    }

    public static boolean checkRelationshipType(String type)
    {
        if( type != null && getQualifiers().contains( type ) )
        {
            return true;
        }
        return false;
    }

    protected String getRdfType(String type, boolean isModelAnnotation)
    {
        String rdfType = null;
        if( isModelAnnotation && ( type.equals( IS_ELEMENT ) || type.equals( HASINSTANCE_ELEMENT ) || type.equals( ISINSTANCEOF_ELEMENT )
                || type.equals( ISDESCRIBEDBY_ELEMENT ) || type.equals( ISDERIVEDFROM_ELEMENT ) ) )
        {
            rdfType = BQMODEL + ":" + type;
        }
        else if( !type.equals( ISDERIVEDFROM_ELEMENT ) )
        {
            rdfType = BQBIOL + ":" + type;
        }
        return rdfType;
    }
}
