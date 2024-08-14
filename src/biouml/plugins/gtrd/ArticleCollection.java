package biouml.plugins.gtrd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


public class ArticleCollection
{
    private Map<Long, Article> articles = new HashMap<>();
    private Map<Long, Journal> journals = new HashMap<>();
    private Map<Long, Author> authors = new HashMap<>();
    
    private Map<Long, Article> articlesByPubmedId = new HashMap<>();
    
    private static volatile ArticleCollection instance;
    public static synchronized ArticleCollection getInstance(Connection con) throws SQLException
    {
        if(instance == null)
            instance = new ArticleCollection( con );
        return instance;
    }
    
    private ArticleCollection(Connection con) throws SQLException
    {
        try(Statement st = con.createStatement();
                ResultSet rs = st.executeQuery( "SELECT id, short_name FROM author" ))
        {
            while(rs.next())
            {
                long id = rs.getLong( 1 );
                String name = rs.getString( 2 );
                Author author = new Author();
                author.setId( id );
                author.setShortName( name );
                authors.put( id, author );
            }
        }
        
        try(Statement st = con.createStatement();
                ResultSet rs = st.executeQuery( "SELECT id, full_name, short_name FROM journal" ))
        {
            while(rs.next())
            {
                long id = rs.getLong( 1 );
                String fullName = rs.getString( 2 );
                String shortName = rs.getString( 3 );
                Journal journal = new Journal();
                journal.setId( id );
                journal.setFullName( fullName );
                journal.setShortName( shortName );
                journals.put( id, journal );
            }
        }        
        
        
        try(Statement st = con.createStatement();
                ResultSet rs = st.executeQuery( "SELECT id,pages,pubmed_id,title,journal_id FROM article" ))
        {
            while(rs.next())
            {
                long id = rs.getLong( 1 );
                String pages = rs.getString( 2 );
                long pubmedId = rs.getLong( 3 );
                String title = rs.getString( 4 );
                long journalId = rs.getLong( 5 );
                Article article = new Article();
                article.setId( id );
                article.setPages( pages );
                article.setPubmedId( pubmedId );
                article.setTitle( title );
                
                Journal journal = journals.get( journalId );
                article.setJournal( journal );
                
                Set<Author> authorList = queryAuthors( con, id );
                article.setAuthors( authorList );
                
                articles.put( id, article );
            }
        }       
        
        for(Article a : articles.values())
            articlesByPubmedId.put( a.getPubmedId(), a );
        
    }
    
    private Set<Author> queryAuthors(Connection con, long articleId) throws SQLException
    {
        Set<Author> result = new LinkedHashSet<>();
        try(Statement st = con.createStatement( );
                ResultSet rs = st.executeQuery( "SELECT author_id FROM article_to_author WHERE article_id=" + articleId ))
        {
            while(rs.next())
            {
                long authorId = rs.getLong( 1 );
                Author author = authors.get( authorId );
                result.add( author );
            }
        }
        return result;
    }
    
    public Article getArticleByPubmedId(String pubmedId)
    {
        long longId = Long.parseLong( pubmedId );
        return articlesByPubmedId.get(longId);
    }
}
