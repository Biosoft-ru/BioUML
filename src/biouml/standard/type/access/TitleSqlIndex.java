package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.ListModel;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformer;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.sql.SqlConnectionHolder;
import biouml.standard.type.Base;

/**
 * TitleSqlIndex is map where key is {@link Base#title} and value
 * {@link Base#name}. The index is loaded during DataCollection initialization
 * and updates itself using DataCollectionEvents.
 * 
 * Additionally titles are stores in sorted list (titleList). This list is used
 * by {@link ListModel} and {@link ComboBoxModel}, thus TitleSqlIndex can be
 * easily used by {@link javax.swing.JList} and {@link javax.swing.JComboBox}
 * controls.
 * 
 * @pending addElement - check for duplicates, trim
 * @pending update index when element name is changed.
 */
@SuppressWarnings ( "serial" )
public class TitleSqlIndex extends TitleIndex
{
    public static final String INDEX_TITLE_TABLE = "index.title.table";
    public static final String INDEX_TITLE_QUERY = "index.title.query";

    public TitleSqlIndex(DataCollection<?> dc, String indexName) throws Exception
    {
        super(dc, indexName);
    }

    /**
     * Query for obtaining all titles - return
     * identifiers as <B>"name"</B> and title as <B>"title"</B>
     * @throws Exception
     */
    protected String makeQuery(DataCollection<?> dc) throws Exception
    {
        // get table name from properties
        String table = dc.getInfo().getProperty(INDEX_TITLE_TABLE);
        String query = dc.getInfo().getProperty(INDEX_TITLE_QUERY);

        // try to define table automatically by transformer
        if( table == null && query == null && dc instanceof SqlDataCollection )
        {
            SqlTransformer<?> transformer = ((SqlDataCollection<?>)dc).getTransformer();
            if(transformer instanceof SqlTransformerSupport)
            {
                table = ((SqlTransformerSupport<?>)transformer).getTable();
            }
        }

        if( table == null && query == null )
            throw new Exception ( "Table name is missing (not specified in config file)." );

        if( query == null )
            query = "SELECT id, title FROM " + table;// + " ORDER BY title";

        return query;
    }

    /**
     * Sanitize title after querying from DB. Does nothing by default, but subclasses may reimplement it.
     * @param title raw title
     * @return sanitized title
     */
    protected String sanitizeTitle(String title)
    {
        return title;
    }

    @Override
    protected void doInit() throws Exception
    {
        if( !(dc instanceof SqlConnectionHolder) )
        {
            super.doInit();
        }
        else
        {
            SqlConnectionHolder sqlDC = (SqlConnectionHolder)dc;

            Connection connection = sqlDC.getConnection();
            List<String> strings = new ArrayList<>();
            Set<String> titles = new HashSet<>();
            try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery( makeQuery( dc ) ))
            {
                while( rs.next() )
                {
                    String id = rs.getString( 1 );
                    strings.add( id );
                    String title = sanitizeTitle( rs.getString( 2 ) );
                    if( titles.contains( title ) )
                        title = getCompositeName( title, id );
                    strings.add( title );
                    titles.add( title );
                }
                rs.close();
            }
            for(List<String> slide : StreamEx.ofSubLists(strings, 2))
            {
                id2title.put(slide.get(0), slide.get(1));
                title2id.put(slide.get(1), slide.get(0));
            }
        }
    }
}
