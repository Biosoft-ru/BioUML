package biouml.plugins.gtrd.analysis;

import java.io.IOException;
import java.sql.Connection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.bean.BeanInfoEx2;

public class UpdatePubmed extends AnalysisMethodSupport<UpdatePubmed.Parameters>
{
    public UpdatePubmed(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters());
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElement gtrd = DataElementPath.create( "databases/GTRD" ).getDataElement();
        Connection con = DataCollectionUtils.getSqlConnection( gtrd );
        createTables(con);
        List<String> pmIds = fetchPubmedIds(con);
        updatePubmed( con, pmIds );
        return null;
    }

    private void createTables(Connection con)
    {
        final String ARTICLE_TO_AUTHOR = "article_to_author";
        SqlUtil.dropTable( con, ARTICLE_TO_AUTHOR );
        SqlUtil.execute( con, "CREATE TABLE `article_to_author` ("
                + "`article_id` bigint(20) NOT NULL,"
                + "`author_id` bigint(20) NOT NULL,"
                + "`author_order` smallint unsigned NOT NULL,"
                + "PRIMARY KEY (`article_id`,`author_id`),"
                + "KEY `FK26dqbdv8kvb9soc6fu03u950d` (`author_id`) ) ENGINE=MyISAM"
        );
        
        final String JOURNAL_TABLE = "journal";
        SqlUtil.dropTable( con, JOURNAL_TABLE );
        SqlUtil.execute( con, "CREATE TABLE `journal` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT,"
                + "`full_name` varchar(255) DEFAULT NULL,"
                + "`short_name` varchar(255) DEFAULT NULL,"
                + "PRIMARY KEY (`id`)) CHARACTER SET utf8, ENGINE=MyISAM"
        );
        
        final String ARTICLE_TABLE = "article";
        SqlUtil.dropTable( con, ARTICLE_TABLE );
        SqlUtil.execute( con, "CREATE TABLE " + ARTICLE_TABLE + " ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT,"
                + "`abstract_text` longtext,"
                + "`pages` varchar(255) DEFAULT NULL,"
                + "`pubmed_id` bigint(20) DEFAULT NULL,"
                + "`title` longtext,"
                + "`journal_id` bigint(20) DEFAULT NULL,"
                + "PRIMARY KEY (`id`),"
                + "KEY (`journal_id`)"
                + ")  CHARACTER SET utf8, ENGINE=MyISAM"
         );
        
        final String AUTHOR_TABLE = "author";
        SqlUtil.dropTable( con, AUTHOR_TABLE );
        SqlUtil.execute( con, "CREATE TABLE `author` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT,"
                + "`short_name` varchar(255) DEFAULT NULL,"
                + "PRIMARY KEY (`id`) )  CHARACTER SET utf8, ENGINE=MyISAM"
        );
    }
    
    private List<String> fetchPubmedIds(Connection con)
    {
        return SqlUtil.queryStrings( con, "SELECT DISTINCT external_db_id FROM external_refs WHERE external_db='PUBMED'" );
    }
    

    public static void updatePubmed(Connection con, List<String> articles)
    {
        final int PARTITION_SIZE = 100;
        for(int from = 0;  from < articles.size(); from += PARTITION_SIZE)
        {
            List<String> part = articles.subList(from, Math.min(from + PARTITION_SIZE, articles.size()));
            Document doc = fetchPubmedXML(part);
            updateArticlesFromXML(doc, con);
        }
    }
    
    private static Document fetchPubmedXML(List<String> ids)
    {
        String idParam = ids.stream().map(x->x.toString()).collect(Collectors.joining(","));
        try {
            return Jsoup
                    .connect("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi")
                    .data("db", "pubmed")
                    .data("id", idParam)
                    .parser(Parser.xmlParser())
                    .post();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static void updateArticlesFromXML(Document doc, Connection con)
    {
        for (Element e : doc.select("eSummaryResult > DocSum")) {
            
            String shortJournalName = e.select("> Item[Name=\"Source\"]").text();
            String fullJournalName = e.select("> Item[Name=\"FullJournalName\"]").text();
            int journalId = findOrInsertJournal(con, shortJournalName, fullJournalName);


            Set<Integer> authorIdList = e.select("> Item[Name=\"AuthorList\"] > Item[Name=\"Author\"]")
                    .stream().map(Element::text)
                    .map(name->findOrInsertAuthor(con, name))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            
            long pubmedId = Long.parseLong( e.select("> Id").text() );
            String title = e.select("> Item[Name=\"Title\"]").text();
            String pages = e.select("> Item[Name=\"SO\"]").text();
            
            Query query = new Query( "INSERT INTO article (pubmed_id,title,pages,journal_id) VALUES($pubmed_id$,$title$,$pages$,$journal_id$)" )
                    .num( "pubmed_id", pubmedId )
                    .str( "title", title )
                    .str( "pages", pages )
                    .num( "journal_id", journalId );
            int articleId = SqlUtil.insertGeneratingKey( con, query );
                    
            int authorOrder = 1;
            for(Integer authorId : authorIdList)
            {
                SqlUtil.executeUpdate( con, new Query("INSERT INTO article_to_author(article_id,author_id,author_order) VALUES($article_id$,$author_id$,$author_order$)")
                        .num( "article_id", articleId )
                        .num( "author_id", authorId )
                        .num( "author_order", authorOrder++));
            }
        }
    }
    
    private static int findOrInsertAuthor(Connection con, String name)
    {
        int res = SqlUtil.queryInt( con, new Query("SELECT id FROM author WHERE short_name=$val$").str( name ), -1 );
        if(res != -1)
            return res;
        return SqlUtil.insertGeneratingKey( con, new Query( "INSERT INTO author(short_name) VALUES($val$)" ).str( name ) );       
    }
    
    private static int findOrInsertJournal(Connection con, String shortJournalName, String fullJournalName) {
        Query query = new Query("SELECT id FROM journal WHERE short_name=$short$ and full_name=$full$")
                .str("short", shortJournalName )
                .str( "full", fullJournalName );
        int res = SqlUtil.queryInt( con, query, -1 );
        if(res != -1)
            return res;
        query = new Query( "INSERT INTO journal(short_name, full_name) VALUES($short$,$full$)" )
                .str("short", shortJournalName )
                .str( "full", fullJournalName );;
        return SqlUtil.insertGeneratingKey( con, query );       
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
    }
    
    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
        }
    }
}
