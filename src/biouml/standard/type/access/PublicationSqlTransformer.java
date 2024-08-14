package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import biouml.standard.type.Publication;

public class PublicationSqlTransformer extends SqlTransformerSupport<Publication>
{
    @Override
    public Class<Publication> getTemplateClass()
    {
        return Publication.class;
    }

    @Override
    public boolean init(SqlDataCollection<Publication> owner)
    {
        table = "publications";
        this.owner = owner;
        return true;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT ID, ref, PMID, authors, affiliation, title, journalTitle, " +
               "year, month, volume, issue, pageFrom, pageTo, language, publicationType, abstract, " +
               "url, importance, keyWords, comment, status " +
               "FROM " + table;
    }

    @Override
    public Publication create(ResultSet resultSet, Connection connection) throws Exception
    {
        Publication p = new Publication(owner, resultSet.getString(1));

        p.setReference          (resultSet.getString("ref"));
        p.setPubMedId           (resultSet.getString("PMID"));
        p.setAuthors            (resultSet.getString("authors"));
        p.setAffiliation        (resultSet.getString("affiliation"));
        p.setTitle              (resultSet.getString("title"));
        p.setJournalTitle       (resultSet.getString("journalTitle"));
        p.setYear               (resultSet.getString("year"));
        p.setMonth              (resultSet.getString("month"));
        p.setVolume             (resultSet.getString("volume"));
        p.setIssue              (resultSet.getString("issue"));
        p.setPageFrom           (resultSet.getString("pageFrom"));
        p.setPageTo             (resultSet.getString("pageTo"));
        p.setLanguage           (resultSet.getString("language"));
        p.setPublicationType    (resultSet.getString("publicationType"));
        p.setPublicationAbstract(resultSet.getString("abstract"));
        p.setFullTextURL        (resultSet.getString("url"));
        p.setImportance         (resultSet.getInt("importance"));
        p.setKeywords           (resultSet.getString("keywords"));
        p.setComment            (resultSet.getString("comment"));
        p.setStatus             (resultSet.getString("status"));

        return p;
    }

    @Override
    public void addInsertCommands(Statement statement, Publication p) throws Exception
    {
        String result = "INSERT INTO " + table +
            "(ID, ref, source, PMID, authors, affiliation, title, journalTitle, " +
            "year, month, volume, issue, pageFrom, pageTo, language, publicationType, abstract, " +
            "url, importance, keyWords, comment, status)"
            + " VALUES" +
            "("  + validateValue(p.getName()) +
            ", " + validateValue(p.getReference()) +
            ", " + validateValue(p.getSource()) +
            ", " + validateValue(p.getPubMedId()) +
            ", " + validateValue(p.getAuthors()) +
            ", " + validateValue(p.getAffiliation()) +
            ", " + validateValue(p.getTitle()) +
            ", " + validateValue(p.getJournalTitle()) +
            ", " + validateValue(p.getYear()) +
            ", " + validateValue(p.getMonth()) +
            ", " + validateValue(p.getVolume()) +
            ", " + validateValue(p.getIssue()) +
            ", " + validateValue(p.getPageFrom()) +
            ", " + validateValue(p.getPageTo()) +
            ", " + validateValue(p.getLanguage()) +
            ", " + validateValue(p.getPublicationType()) +
            ", " + validateValue(p.getPublicationAbstract()) +
            ", " + validateValue(p.getFullTextURL()) +
            ", " + p.getImportance() +
            ", " + validateValue(p.getKeywords()) +
            ", " + validateValue(p.getComment()) +
            ", " + validateValue(p.getStatus()) +
            ")";

        statement.addBatch(result);
    }
    
    @Override
    public String[] getUsedTables()
    {
        return new String[] {table};
    }
    
    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `publications` (" +
            "  `ID` varchar(100) NOT NULL," +
            "  `ref` text," +
            "  `PMID` bigint(20) unsigned default NULL," +
            "  `authors` text," +
            "  `title` text," +
            "  `source` varchar(100) default NULL," +
            "  `journalTitle` varchar(100) default NULL," +
            "  `year` int(10) unsigned default NULL," +
            "  `month` varchar(10) default NULL," +
            "  `volume` int(10) unsigned default NULL," +
            "  `issue` int(10) unsigned default NULL," +
            "  `pageFrom` varchar(10) default NULL," +
            "  `pageTo` varchar(10) default NULL," +
            "  `language` char(3) default NULL," +
            "  `publicationType` varchar(50) default NULL," +
            "  `abstract` text," +
            "  `url` varchar(512) default NULL," +
            "  `importance` int(11) default '3'," +
            "  `keyWords` text," +
            "  `comment` text," +
            "  `affiliation` text," +
            "  `status` varchar(30) default NULL," +
            "  PRIMARY KEY  (`ID`)," +
            "  UNIQUE KEY `IDX_UNIQUE_PUBLICATIONS_ID` (`ID`)" +
            ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
