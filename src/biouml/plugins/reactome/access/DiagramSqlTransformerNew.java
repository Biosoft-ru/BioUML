package biouml.plugins.reactome.access;

import java.awt.Dimension;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.util.AddElementsUtils;
import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.model.xml.XmlDiagramViewOptions;
import biouml.plugins.reactome.imports.RSpecies;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import biouml.standard.type.SpecieReference;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.graphics.font.ColorFont;

public class DiagramSqlTransformerNew extends SqlTransformerSupport<Diagram>
{
    public static final String SBGN_NOTATION_NAME = "sbgn_simulation.xml";

    protected static final Logger log = Logger.getLogger( DiagramSqlTransformerNew.class.getName() );

    private RSpecies species;

    public DiagramSqlTransformerNew()
    {
        super();
        idField = "DB_ID";
        table = "PathwayDiagram";
    }

    @Override
    public boolean init(SqlDataCollection<Diagram> owner)
    {
        boolean inited = super.init( owner );
        if( owner == null )
            return inited;
        String speciesStr = owner.getInfo().getProperty( RSpecies.REACTOME_SPECIES_PROPERTY );
        if( speciesStr != null && !speciesStr.isEmpty() )
        {
            species = RSpecies.fromString( speciesStr );
            if( species == null )
                log.warning( "Cannot match species to reactome species list '" + speciesStr + "'" );
        }
        return inited;
    }

    @Override
    public void addInsertCommands(Statement statement, Diagram de) throws Exception
    {
        // Reactome is not mutable
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        // Reactome is not mutable
    }

    @Override
    public Diagram create(ResultSet resultSet, Connection connection) throws Exception
    {
        String name = resultSet.getString( 1 );
        long pathwayID = resultSet.getLong( 2 );
        String title = resultSet.getString( 3 );

        XmlDiagramType diagramType = XmlDiagramType.getTypeObject( SBGN_NOTATION_NAME );

        Diagram diagram = diagramType.createDiagram( owner, name, null );
        diagram.setTitle( title );

        diagram.setNotificationEnabled( false );
        DynamicPropertySet options = ( (XmlDiagramViewOptions)diagram.getViewOptions() ).getOptions();
        options.setValue( "customTitleFont", new ColorFont( "Arial", 0, 12 ) );
        options.setValue( "nodeTitleFont", new ColorFont( "Arial", 0, 12 ) );
        options.setValue( "nodeTitleLimit", 100 );

        fillCompartment( pathwayID, diagram, connection );
        try
        {
            diagram = new DiagramLayouter( diagram, connection, table ).applyLayout();
            AddElementsUtils.fixCurrentNodes( diagram, false );
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE,  "Unable to read layout for " + diagram.getCompletePath() + ": " + ExceptionRegistry.log( e ) );
            HierarchicLayouter layouter = new HierarchicLayouter();
            Graph graph = DiagramToGraphTransformer.generateGraph( diagram, null );
            PathwayLayouter pathwayLayouter = new PathwayLayouter( layouter );
            pathwayLayouter.doLayout( graph, null );
            DiagramToGraphTransformer.applyLayout( graph, diagram );
        }
        diagram.setView( null );

        try
        {
            String summation = getSummation( pathwayID, connection ).replace( "href='/", "href='https://reactome.org/" );
            //If we call diagram#setDecription it will try to save diagram and we will get in the infinite loop
            ( (Referrer)diagram.getKernel() ).setDescription( summation );
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE,  "Error during description read", e );
        }


        diagram.setNotificationEnabled( true );
        return diagram;
    }

    private @Nonnull String getSummation(long pathwayID, Connection connection)
    {
        String result = null;
        List<String> comments = SqlUtil.queryStrings( connection, "SELECT DISTINCT text FROM Summation s "
                + "INNER JOIN Event_2_summation sumdb ON (sumdb.summation = s.DB_ID) WHERE sumdb.DB_ID='" + pathwayID + "'" );
        if( comments.size() > 0 )
            result = String.join( "; ", comments );
        return result == null ? "" : result;
    }

    @Override
    public String getElementQuery(String name)
    {
        int id;
        try
        {
            id = Integer.parseInt( name );
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        return "SELECT db_id,representedPathway,_displayName FROM BioUML_diagrams WHERE db_id=" + id;
    }

    @Override
    public String getCountQuery()
    {
        String query = "SELECT COUNT(1) FROM BioUML_diagrams";
        if( species != null )
            query += " WHERE species=" + species.getInnerId();
        return query;
    }

    @Override
    public String getNameListQuery()
    {
        String query = "SELECT db_id FROM BioUML_diagrams";
        if( species != null )
            query += " WHERE species=" + species.getInnerId();
        return query;
    }

    @Override
    public String getElementExistsQuery(String name)
    {
        int id;
        try
        {
            id = Integer.parseInt( name );
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        return "SELECT db_id FROM BioUML_diagrams WHERE db_id=" + id;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT db_id,representedPathway,_displayName FROM BioUML_diagrams";
    }

    @Override
    public Class<Diagram> getTemplateClass()
    {
        return Diagram.class;
    }

    protected static final String PATHWAY_CLASS = "Pathway";
    protected static final String REACTION_CLASS = "Reaction";
    protected static final String BLACKBOXEVENT_CLASS = "BlackBoxEvent";

    protected void fillCompartment(long pathwayID, Compartment compartment, Connection connection) throws Exception
    {
        try( Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery( "SELECT e.hasEvent,e.hasEvent_class,do._displayName FROM Pathway_2_hasEvent e"
                        + " LEFT JOIN DatabaseObject do ON e.hasEvent=do.DB_ID WHERE e.DB_ID=" + pathwayID ) )
        {
            while( rs.next() )
            {
                String type = rs.getString( 2 );
                if( type.equals( PATHWAY_CLASS ) )
                {
                    long childID = rs.getLong( 1 );
                    String title = rs.getString( 3 );
                    fillCompartment( childID, compartment, connection );
                }
                else if( type.equals( REACTION_CLASS ) )
                {
                    String reactionID = rs.getString( 1 );
                    Reaction reaction = getReaction( reactionID, connection, "Reaction" );
                    if( reaction != null )
                        fillReaction( reaction, compartment );
                }
                else if( type.equals( BLACKBOXEVENT_CLASS ) )
                {
                    String reactionID = rs.getString( 1 );
                    Reaction reaction = getReaction( reactionID, connection, "BlackBoxEvent" );
                    if( reaction != null )
                        fillReaction( reaction, compartment );
                }
            }
        }
    }

    protected Reaction getReaction(String reactionID, Connection connection, String collectionName)
    {
        try
        {
            String reactionName = SqlUtil.queryString( connection,
                    "SELECT identifier FROM StableIdentifier si JOIN DatabaseObject dbo ON (si.DB_ID=dbo.stableIdentifier) WHERE dbo.DB_ID="
                            + reactionID );
            if( reactionName != null )
            {
                return Module.getModulePath( owner ).getChildPath( Module.DATA, collectionName, reactionName )
                        .getDataElement( Reaction.class );
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE,  "Reactome: cannot create reaction " + reactionID + ": " + ExceptionRegistry.log( e ) );
        }
        return null;
    }

    protected void fillReaction(Reaction reaction, Compartment parent) throws Exception
    {
        Node rNode = new Node( parent, reaction );
        setXmlType( rNode, "process" );
        rNode.setLocation( parent.getLocation() );
        parent.put( rNode );

        for( SpecieReference sr : reaction.getSpecieReferences() )
        {
            String specieName = sr.getSpecie();
            DataElement specie = CollectionFactory.getDataElement( specieName, Module.getModule( reaction ) );
            if( specie instanceof Base )
            {
                Node specieNode = null;
                if( parent.contains( specie.getName() ) )
                {
                    specieNode = (Node)parent.get( specie.getName() );
                }
                else
                {
                    specieNode = new Node( parent, (Base)specie );
                    if( "SimpleEntity".equals( ( (Base)specie ).getAttributes().getValue( "Class" ) ) )
                    {
                        setXmlType( specieNode, "entity" );
                        specieNode.getAttributes()
                                .add( new DynamicProperty( SBGNPropertyConstants.SBGN_ENTITY_TYPE_PD, String.class, "unspecified" ) );
                    }
                    else if( "Complex".equals( ( (Base)specie ).getAttributes().getValue( "Class" ) ) )
                    {
                        setXmlType( specieNode, "complex" );
                        specieNode.getAttributes()
                                .add( new DynamicProperty( SBGNPropertyConstants.SBGN_ENTITY_TYPE_PD, String.class, "macromolecule" ) );
                    }
                    else
                    {
                        setXmlType( specieNode, "entity" );
                        specieNode.getAttributes()
                                .add( new DynamicProperty( SBGNPropertyConstants.SBGN_ENTITY_TYPE_PD, String.class, "macromolecule" ) );
                    }
                    specieNode.setShapeSize( new Dimension( 80, 50 ) );
                    specieNode.setLocation( parent.getLocation() );
                    parent.put( specieNode );
                }
                Edge edge = null;
                if( sr.getRole().equals( SpecieReference.REACTANT ) )
                {
                    edge = new Edge( parent, sr, specieNode, rNode );
                    setXmlType( edge, "consumption" );
                }
                else if( sr.getRole().equals( SpecieReference.PRODUCT ) )
                {
                    edge = new Edge( parent, sr, rNode, specieNode );
                    setXmlType( edge, "production" );
                }
                else
                {
                    edge = new Edge( parent, sr, specieNode, rNode );
                    setXmlType( edge, "regulation" );
                }
                parent.put( edge );
            }
        }
    }

    protected void setXmlType(DiagramElement de, String type) throws Exception
    {
        de.getAttributes().add( new DynamicProperty( XmlDiagramTypeConstants.XML_TYPE, String.class, type ) );
    }
}
