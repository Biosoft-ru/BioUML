package biouml.plugins.chebi;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.ResultSetMapper;
import ru.biosoft.access.sql.SqlUtil;
import biouml.standard.type.DatabaseReference;
import com.developmentontheedge.beans.DynamicProperty;

public class MoleculeSqlTransformer extends SqlTransformerSupport<ChebiMolecule>
{
    public static final String PROPERTY_SOURCE = "source";
    public static final String PROPERTY_PARENT = "parent";

    private static final Query REF_QUERY = new Query( "SELECT reference_id, reference_db_name FROM reference WHERE compound_id=$id$ ORDER BY id" );
    private static final Query ADD_QUERY = new Query( "SELECT accession_number, type FROM database_accession WHERE compound_id=$id$ ORDER BY id" );
    private static final Query NAMES_QUERY = new Query( "SELECT name FROM names WHERE compound_id=$id$ ORDER BY id" );
    private static final Query COMMENTS_QUERY = new Query( "SELECT text FROM comments WHERE compound_id=$id$ ORDER BY id" );
    //private static final Query STRUCTURES_QUERY = new Query( "SELECT id FROM structures WHERE compound_id=$id$ ORDER BY id" );
    //TODO: support image creating for other types of structures
    private static final Query STRUCTURES_QUERY = new Query( "SELECT id FROM structures WHERE compound_id=$id$ AND type='mol' ORDER BY id" );

    @Override
    public ChebiMolecule create(ResultSet resultSet, Connection connection) throws Exception
    {
        ChebiMolecule chebiMol = createElement( resultSet );

        String id = chebiMol.getName().substring( "CHEBI:".length() );

        // get database references
        ResultSetMapper<DatabaseReference> transformer = rs -> new DatabaseReference( processDBName( rs.getString( 2 ) ), rs.getString( 1 ) );
        DatabaseReference[] dbRefs = processDBRefs( SqlUtil.stream( connection, REF_QUERY.str( id ), transformer )
                .append( SqlUtil.stream( connection, ADD_QUERY.str( id ), transformer ) ).toList() );
        if( dbRefs.length > 0 )
            chebiMol.setDatabaseReferences( dbRefs );

        // get synonyms
        String synonyms = SqlUtil.stringStream( connection, NAMES_QUERY.str( id ) ).joining( ", " );
        chebiMol.setSynonyms( synonyms );
        if( synonyms != null && !synonyms.isEmpty()
                && ( chebiMol.getTitle() == null || chebiMol.getTitle().isEmpty() || chebiMol.getTitle().equals( chebiMol.getName() ) ) )
            chebiMol.setTitle( synonyms.split( ", " )[0] );

        // get comments
        chebiMol.setComment( SqlUtil.stringStream( connection, COMMENTS_QUERY.str( id ) ).joining( "; " ) );

        // get structures
        String[] structures = SqlUtil.stringStream( connection, STRUCTURES_QUERY.str( id ) ).map( struct -> "structure/" + struct )
                .toArray( String[]::new );
        if( structures.length > 0 )
        {
            chebiMol.setStructureReferences(structures);
        }

        //process attributes
        String source = resultSet.getString(3);
        if( source != null )
        {
            DynamicProperty sourceDP = new DynamicProperty(PROPERTY_SOURCE, String.class, source);
            chebiMol.getAttributes().add(sourceDP);
        }
        String parentID = resultSet.getString(4);
        if( parentID != null )
        {
            DynamicProperty parentDP = new DynamicProperty(PROPERTY_PARENT, String.class, parentID);
            chebiMol.getAttributes().add(parentDP);
        }

        return chebiMol;
    }
    protected ChebiMolecule createElement(ResultSet resultSet) throws SQLException
    {
        ChebiMolecule chebiMol = new ChebiMolecule( owner, resultSet.getString( 1 ) );
        chebiMol.setTitle(resultSet.getString(2));
        chebiMol.setCompleteName(resultSet.getString(1));
        chebiMol.setDescription(resultSet.getString(5));

        return chebiMol;
    }

    /**
     * Process DB references, e.g.:
     * 1) we need to remove "EC " from the start of the ID for 'BRENDA' DB
     * 2) some IDs with type 'CAS Registry Number' are written as "ID1 ID2" in the database
     * and should be separated
     * @param source
     * @return
     */
    private DatabaseReference[] processDBRefs(List<DatabaseReference> source)
    {
        List<DatabaseReference> result = new ArrayList<>();
        for( DatabaseReference dbRef : source )
        {
            String dbName = dbRef.getDatabaseName();
            String[] parts = dbRef.getId().split( " " );
            if( "BRENDA".equals( dbName ) && parts.length > 1 )
                result.add( new DatabaseReference( dbName, parts[1] ) );
            else if( "CAS Registry Number".equals( dbName ) && parts.length > 1 )
            {
                result.add( new DatabaseReference( dbName, parts[0] ) );
                result.add( new DatabaseReference( dbName, parts[1] ) );
            }
            //TODO: process uniprot refs (now there are too many redundant uniprot refs)
            else if( "UniProt".equals( dbName ) )
                continue;
            else
                result.add( dbRef );
        }
        return result.toArray( new DatabaseReference[0] );
    }

    private String processDBName(String name)
    {
        int length = name.length();
        if( name.endsWith( " accession" ) )
            return name.substring( 0, length - 10 );
        else if( name.endsWith( " citation" ) )
            return name.substring( 0, length - 9 );
        return name;
    }

    @Override
    public boolean init(SqlDataCollection<ChebiMolecule> owner)
    {
        table = "compounds";
        idField = "chebi_accession";
        this.owner = owner;
        return true;
    }

    @Override
    public Class<ChebiMolecule> getTemplateClass()
    {
        return ChebiMolecule.class;
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT chebi_accession FROM " + table + " ORDER BY chebi_accession";
    }

    @Override
    public String getElementQuery(String name)
    {
        if( !name.matches( "^CHEBI:\\d+" ) )
            return null;
        return super.getElementQuery( name );
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT chebi_accession, name, source, parent_id, definition " + "FROM " + table;
    }

    @Override
    public void addInsertCommands(Statement statement, ChebiMolecule de) throws Exception
    {
        throw new Exception("You can't add or remove elements from this module");
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        throw new Exception("You can't add or remove elements from this module");
    }
}
