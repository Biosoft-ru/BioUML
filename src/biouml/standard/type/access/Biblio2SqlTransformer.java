package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.standard.type.Publication;
import ru.biosoft.access.SqlDataCollection;

public class Biblio2SqlTransformer extends PublicationSqlTransformer
{
    Logger log = Logger.getLogger( Biblio2SqlTransformer.class.getName() );

    // first one is ID of our category
    // next ones are its parents
    private final List<Integer> categoryIds = new ArrayList<>();

    @Override
    public boolean init(SqlDataCollection<Publication> owner)
    {
        if(!super.init(owner)) return false;
        String category = owner.getInfo().getProperty("category");
        if(category == null) return false;
        try
        {
            Connection conn = owner.getConnection();
            try( PreparedStatement ps = conn.prepareStatement( "SELECT ID,parentID from categories WHERE name=?" ) )
            {
                ps.setString( 1, category );
                try( ResultSet rs = ps.executeQuery() )
                {
                    if( !rs.next() )
                        return false;
                    categoryIds.add( rs.getInt( 1 ) );
                    int parentId = rs.getInt( 2 );
                    try( PreparedStatement ps2 = conn.prepareStatement( "SELECT parentID from categories WHERE ID=?" ) )
                    {
                        while( parentId != 1 )
                        {
                            categoryIds.add( parentId );
                            ps2.setInt( 1, parentId );
                            try( ResultSet rs2 = ps2.executeQuery() )
                            {
                                if( !rs2.next() )
                                    break;
                                parentId = rs2.getInt( 1 );
                            }
                        }
                    }
                }
            }
        }
        catch(SQLException e)
        {
            log.log( Level.SEVERE, e.getMessage(), e );
            categoryIds.add(-1);
            return false;
        }
        return true;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT t.ID, ref, PMID, authors, affiliation, title, journalTitle, "
                + "year, month, volume, issue, pageFrom, pageTo, language, publicationType, abstract, "
                + "url, importance, keyWords, comment, status " + "FROM " + table
                + " t, classifications cl WHERE CONCAT('biblio2.publications.',t.ID)=cl.recordID and cl.categoryID=" + categoryIds.get(0);
    }

    @Override
    public String getElementQuery(String name)
    {
        return getSelectQuery()+" AND t.ID=" + validateValue(name);
    }

    @Override
    public void addInsertCommands(Statement statement, Publication de) throws Exception
    {
        super.addInsertCommands(statement, de);
        String recordID = validateValue("biblio2.publications."+de.getName());
        for(Integer id: categoryIds)
        {
            statement.addBatch("INSERT INTO classifications(recordID,categoryID) VALUES("+recordID+","+id+")");
        }
    }

    @Override
    public String getCountQuery()
    {
        return "SELECT COUNT(*) FROM " + table + " t, classifications cl"
                + " WHERE CONCAT('biblio2.publications.',t.ID)=cl.recordID and cl.categoryID=" + categoryIds.get(0);
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT t.ID FROM " + table
                + " t, classifications cl WHERE CONCAT('biblio2.publications.',t.ID)=cl.recordID and cl.categoryID=" + categoryIds.get(0)
                + " ORDER BY t.ID";
    }

    @Override
    public String getElementExistsQuery(String name)
    {
        return "SELECT t.ID FROM " + table
                + " t, classifications cl WHERE CONCAT('biblio2.publications.',t.ID)=cl.recordID and cl.categoryID=" + categoryIds.get(0)
                + " AND t.ID=" + validateValue(name);
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        super.addDeleteCommands(statement, name);
        String recordID = validateValue("biblio2.publications."+name);
        statement.addBatch("DELETE FROM classifications WHERE recordID="+recordID);
    }

    @Override
    public void addUpdateCommands(Statement statement, Publication de) throws Exception
    {
        // Not necessary to update classifications table also, thus calling superclass implementations
        super.addDeleteCommands(statement, de.getName());
        super.addInsertCommands(statement, de);
    }
}
