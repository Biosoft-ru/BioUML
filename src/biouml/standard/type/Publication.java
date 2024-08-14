package biouml.standard.type;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.StringTokenizer;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.MutableDataElementSupport;
import ru.biosoft.access.support.EntryParser;

/**
 * Defines common properties of literature reference.
 *
 * Our format is based on MEDLINE (PubMed) literature reference format.
 * Here we use major fields of MEDLINE and commit some accessory fields.
 *
 * From the other hand we add some utility fields allowing a user to
 * organize and categorize references by user defined key words and importance,
 * as well as to store user comments and related URLs.
 *
 * @pending inherit from {@link GenericEntity}.
 * @pending provide BeanExplorer categories support
 *
 * @pending - reference update mechanism
 * Currently if authors or year property is changed then reference is updated.
 * While reference should be unique, then we check it using "reference" index.
 * If reference index is missing, then updateReference method checks all publications.
 *
 * To skip reference update during work of transformers we use following approach:
 * reference can be updated after 500 ms after Publication creation. It is expected that
 * from one hand this is enough for work of transformers and from other hand will not
 * confuse Property Inspector.
 *
 * @pending BioPAX properties
 */
public class Publication extends MutableDataElementSupport
{
    protected static final Logger log = Logger.getLogger(Publication.class.getName());

    ///////////////////////////////////////////////////////////////////
    // Initialization from MEDLINE
    //

    protected static final String medlineQuery = "http://www.ncbi.nlm.nih.gov/entrez/utils/pmfetch.fcgi?db=PubMed&report=MEDLINE&mode=text&id=";

    /**
     * Imports new publication from MEDLINE.
     *
     * Algorithm checks whether publication is already exists in parent ru.biosoft.access.core.DataCollection
     * and returns it if it was able to find the publication.
     *
     * @param pubMedId - PubMed ID (PMID)
     * @param parent - data collection with publications
     * @param checkParent - checks whether such publication already exists
     * @param jobControl
     */
    public static Publication importFromMedline(String pubMedId, DataCollection parent, boolean checkParent)
    {
        // 1. try to create Publication with unique ID
        // 2. init publication from MEDLINE
        // 3. try to find similar publication in parent DC

        return null;

        /*        if( pubMedId != null && authors == null && title == null )
                {
                    (new Thread()
                    {
                        public void start()
                        {
                            //initFromMedline(pubMedId);
                        }
                    }).start();
                }
        */
    }

    protected void initFromMedline(String pubMedId)
    {
        try
        {
            URL url = new URL(medlineQuery + pubMedId);
            URLConnection conn = url.openConnection();
            conn.setReadTimeout(20000);
            conn.connect();
            Object content = conn.getContent();
            if( content == null )
            {
                log.log(Level.SEVERE, "Can not get entry with uid '" + pubMedId + "' from PubMed. Content is null.");
                return;
            }

            StringBuffer result = new StringBuffer();
            try (InputStream is = (InputStream)content)
            {
                byte[] bytes = new byte[1024];
                int len = 0;
                while( ( len = is.read( bytes ) ) > 0 )
                {
                    result.append( new String( bytes, 0, len ) );
                }
            }
            String entryStr = result.toString();
            Entry entry = new Entry(null, name, entryStr);

            // parsing result
            this.pubMedId = EntryParser.parseStringValue(entry, "PMID-");
            affiliation = EntryParser.parseStringValue(entry, "AD  -");
            title = EntryParser.parseStringValue(entry, "TI  -");
            journalTitle = EntryParser.parseStringValue(entry, "TA  -");
            volume = EntryParser.parseStringValue(entry, "VI  -");
            issue = EntryParser.parseStringValue(entry, "IP  -");
            publicationAbstract = EntryParser.parseStringValue(entry, "AB  -");
            language = EntryParser.parseStringValue(entry, "LA  -");
            publicationType = EntryParser.parseStringValue(entry, "PT  -");

            // parsing result
            String dp = EntryParser.parseStringValue(entry, "DP  -");
            year = dp.substring(0, 4);
            month = dp.length() > 5 ? dp.substring(5) : "";

            String pages = EntryParser.parseStringValue(entry, "PG  -");
            pageFrom = pages;
            pageTo = pages;
            if( pages != null )
            {
                int delim = pages.indexOf('-');
                if( delim == -1 )
                    delim = pages.indexOf(',');
                if( delim != -1 )
                {
                    pageFrom = pages.substring(0, delim).trim();
                    pageTo = pages.substring(delim + 1).trim();
                }
            }

            // read authors
            String prefix = "\nFAU";
            if( entryStr.indexOf(prefix) < 1 )
                prefix = "\nAU";

            int from = 0;
            int to;
            result = new StringBuffer();
            while( ( from = entryStr.indexOf(prefix, from) ) > 0 )
            {
                to = entryStr.indexOf('\n', from + 2);
                if( to < 0 )
                    to = entryStr.length() - 1;

                result.append(entryStr.substring(from + 7, to));
                result.append(";\n");

                from = to;
            }
            authors = result.toString();

            reference = generateReference();

            firePropertyChange("*", null, null);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not get entry with uid '" + name + "' from PubMed.", t);
        }
    }


    ///////////////////////////////////////////////////////////////////
    // Constructors
    //

    protected long creationTime;

    public Publication(DataCollection parent, String name)
    {
        super(parent, name);
        creationTime = System.currentTimeMillis();
    }

    /**
     * Generates unique human readable name for the literature reference.
     * General format is:
     * <ul>
     *  <li> for one author - <code>AUTHOR_LAST_NAME, YEAR[L]</code></li>
     *  <li> for two authors - <code>AUTHOR_LAST_NAME_1 and AUTHOR_LAST_NAME_2, YEAR[L]</code></li>
     *  <li> for third and more authors - <code>AUTHOR_LAST_NAME et al., YEAR[L]</code></li>
     *  <li> no authors - <code>TITLE[L]</code> or defined by user</li>
     * </ul>
     *
     */
    protected String generateReference()
    {
        StringTokenizer tokens = new StringTokenizer(getAuthors(), ",\n");
        int num = tokens.countTokens();

        String id;
        if( num == 0 )
        {
            id = getTitle();
        }
        else if( num == 1 )
        {
            id = getAuthorLastName(tokens.nextToken()) + ", " + year;
        }
        else if( num == 2 )
        {
            id = getAuthorLastName(tokens.nextToken()) + " and " + getAuthorLastName(tokens.nextToken()) + ", " + year;
        }
        else
        {
            id = getAuthorLastName(tokens.nextToken()) + " et al., " + year;
        }

        // check that reference is unique
        if( getOrigin() != null && getOrigin().contains(id) )
        {
            char letter = 'a';
            while( getOrigin().contains(id + letter) )
            {
                letter++;
            }

            id = id + letter;
        }

        return id;
    }

    protected String getAuthorLastName(String author)
    {
        int offset = author.lastIndexOf(' ');
        if( offset > 0 )
            author = author.substring(0, offset);

        return author;
    }

    /**
     * Authors (MEDLINE AU field).
     *
     * @pending use full author names (MEDLINE FAU field).
     * @pending use array of string: one string for each author.
     */
    private String reference;
    public String getReference()
    {
        if( reference == null )
            reference = generateReference();

        return reference;
    }

    /**
     * This method should not be used from PropertyInspector.
     * 
     * It is essential to provide uniquenss of reference.
     */
    public void setReference(String reference)
    {
        String oldValue = reference;
        this.reference = reference;
        firePropertyChange("reference", oldValue, reference);
    }

    protected void updateReference()
    {
        if( ( System.currentTimeMillis() - creationTime ) > 500 )
        {
            String ref = generateReference();
            if( ref != null && !ref.equals(reference) )
            {
                String oldValue = reference;
                reference = ref;
                firePropertyChange("reference", oldValue, reference);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties corresponding to major MEDLINE fields.
    //

    /** Unique number assigned to each PubMed citation. (MEDLINE PMID field). */
    private String pubMedId;
    public String getPubMedId()
    {
        return pubMedId;
    }
    public void setPubMedId(String pubMedId)
    {
        String oldValue = pubMedId;
        this.pubMedId = pubMedId;
        firePropertyChange("pubMedId", oldValue, pubMedId);
    }

    /**
     * Authors (MEDLINE AU field).
     *
     * @pending use full author names (MEDLINE FAU field).
     * @pending use array of string: one string for each author.
     */
    private String authors;
    public String getAuthors()
    {
        if( authors == null )
            return "";
        return authors;
    }
    public void setAuthors(String authors)
    {
        String oldValue = authors;
        this.authors = authors;
        firePropertyChange("authors", oldValue, authors);
        updateReference();
    }

    /**
     * Institutional affiliation and address of the first author, and grant numbers.
     * (MEDLINE AD field).
     */
    private String affiliation;
    public String getAffiliation()
    {
        return affiliation;
    }
    public void setAffiliation(String affiliation)
    {
        String oldValue = affiliation;
        this.affiliation = affiliation;
        firePropertyChange("affiliation", oldValue, affiliation);
    }

    /** The title of the article (MEDLINE TI field). */
    private String title;
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        String oldValue = title;
        this.title = title;
        firePropertyChange("title", oldValue, title);
    }

    /**
     * Returns composite field containing bibliographic information.
     * (MEDLINE SO field).
     */
    public String getSource()
    {
        if( journalTitle == null )
            return "";

        StringBuffer buf = new StringBuffer(journalTitle);
        buf.append(". ");

        buf.append(getYear());
        if( getMonth() != null && !getMonth().equals("") )
            buf.append(" " + getMonth());
        buf.append(';');

        if( getVolume() != null && !getVolume().equals("") )
            buf.append(getVolume());

        if( getIssue() != null && !getIssue().equals("") )
            buf.append("(" + getIssue() + ")");

        if( getPageFrom() != null && !getPageFrom().equals("") )
            buf.append(":" + getPageFrom());

        if( getPageTo() != null && !getPageTo().equals("") )
            buf.append("-" + pageTo);

        return buf.toString();
    }

    /** Standard journal title abbreviation (MEDLINE TA field). */
    private String journalTitle;
    public String getJournalTitle()
    {
        return journalTitle;
    }
    public void setJournalTitle(String journalTitle)
    {
        String oldValue = journalTitle;
        this.journalTitle = journalTitle;
        firePropertyChange("journalTitle", oldValue, journalTitle);
        firePropertyChange("source", null, null);
    }

    /** Journal volume (MEDLINE VI field). */
    private String volume;
    public String getVolume()
    {
        return volume;
    }
    public void setVolume(String volume)
    {
        String oldValue = volume;
        this.volume = volume;
        firePropertyChange("volume", oldValue, volume);
        firePropertyChange("source", null, null);
    }

    /**
     * The number of the issue, part, or supplement of the journal in which
     * the article was published (MEDLINE IP field).
     */
    private String issue;
    public String getIssue()
    {
        return issue;
    }
    public void setIssue(String issue)
    {
        String oldValue = issue;
        this.issue = issue;
        firePropertyChange("issue", oldValue, issue);
        firePropertyChange("source", null, null);
    }

    /**
     * The pagination of the article (first part of MEDLINE PG field).
     * Sometimes page number can be a string value, for example 653s.
     */
    private String pageFrom;
    public String getPageFrom()
    {
        return pageFrom;
    }
    public void setPageFrom(String pagination)
    {
        String oldValue = pageFrom;
        this.pageFrom = pagination;
        firePropertyChange("pageFrom", oldValue, pagination);
        firePropertyChange("source", null, null);
    }

    /**
     * The pagination of the article (last part of MEDLINE PG field).
     * Sometimes page number can be a string value, for example 653s.
     */
    private String pageTo;
    public String getPageTo()
    {
        return pageTo;
    }
    public void setPageTo(String pagination)
    {
        String oldValue = pageTo;
        this.pageTo = pagination;
        firePropertyChange("pageTo", oldValue, pagination);
        firePropertyChange("source", null, null);
    }


    /** The date (year) the article was published (MEDLINE DP field). */
    private String year;
    public String getYear()
    {
        return year;
    }
    public void setYear(String year)
    {
        String oldValue = year;
        this.year = year;
        firePropertyChange("year", oldValue, year);
        firePropertyChange("source", null, null);
        updateReference();
    }

    /** The date (month) the article was published (MEDLINE DP field). */
    private String month;
    public String getMonth()
    {
        return month;
    }
    public void setMonth(String month)
    {
        String oldValue = month;
        this.month = month;
        firePropertyChange("month", oldValue, month);
        firePropertyChange("source", null, null);
    }

    /** The publication abstract. (MEDLINE AB field). */
    private String publicationAbstract;
    public String getPublicationAbstract()
    {
        return publicationAbstract;
    }
    public void setPublicationAbstract(String publicationAbstract)
    {
        String oldValue = publicationAbstract;
        this.publicationAbstract = publicationAbstract;
        firePropertyChange("publicationAbstract", oldValue, publicationAbstract);
    }

    /** Link to the full-text of article at provider's website (MEDLINE URLF field).  */
    private String fullTextURL;
    public String getFullTextURL()
    {
        return fullTextURL;
    }
    public void setFullTextURL(String fullTextURL)
    {
        String oldValue = fullTextURL;
        this.fullTextURL = fullTextURL;
        firePropertyChange("fullTextURL", oldValue, fullTextURL);
    }

    /** The language in which the article was published (MEDLINE LA field). */
    private String language;
    public String getLanguage()
    {
        return language;
    }
    public void setLanguage(String language)
    {
        String oldValue = language;
        this.language = language;
        firePropertyChange("language", oldValue, language);
    }

    /** The type of material the article represents (MEDLINE PT field). */
    private String publicationType;
    public String getPublicationType()
    {
        return publicationType;
    }
    public void setPublicationType(String publicationType)
    {
        String oldValue = publicationType;
        this.publicationType = publicationType;
        firePropertyChange("publicationType", oldValue, publicationType);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utility fields to organize, categorize and  store user defined information.
    //

    /** Using this property user can assign some status to article, for example: read, wanted, to read. */
    private String status;
    public String getStatus()
    {
        return status;
    }
    public void setStatus(String status)
    {
        String oldValue = status;
        this.status = status;
        firePropertyChange("status", oldValue, status);
    }

    /** A set of user defined key words assotioated with a literature reference. */
    private String keywords;
    public String getKeywords()
    {
        return keywords;
    }
    public void setKeywords(String keywords)
    {
        String oldValue = keywords;
        this.keywords = keywords;
        firePropertyChange("keywords", oldValue, keywords);
    }

    /**
     * The reference importance.
     * Using this property user can organise references by their importance.
     */
    public int importance;
    public int getImportance()
    {
        return importance;
    }
    public void setImportance(int importance)
    {
        int oldValue = importance;
        this.importance = importance;
        firePropertyChange("importance", oldValue, importance);
    }

    /** Arbitrary user comment assitiated with this refeence. */
    private String comment;
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        String oldValue = comment;
        this.comment = comment;
        firePropertyChange("comment", oldValue, comment);
    }

    ///////////////////////////////////////////////////////////////////
    // BioPAX properties
    //

    /** DB name.*/
    private String db;
    public String getDb()
    {
        return db;
    }
    public void setDb(String db)
    {
        this.db = db;
    }

    /** DB version field.*/
    private String dbVersion;
    public String getDbVersion()
    {
        return dbVersion;
    }
    public void setDbVersion(String dbVersion)
    {
        this.dbVersion = dbVersion;
    }

    /** ID field.*/
    private String idName;
    public String getIdName()
    {
        return idName;
    }
    public void setIdName(String idName)
    {
        this.idName = idName;
    }

    /** ID version field.*/
    private String idVersion;
    public String getIdVersion()
    {
        return idVersion;
    }
    public void setIdVersion(String idVersion)
    {
        this.idVersion = idVersion;
    }

    /** Source for BioPAX support.*/
    private String simpleSource;
    public String getSimpleSource()
    {
        return simpleSource;
    }
    public void setSimpleSource(String simpleSource)
    {
        this.simpleSource = simpleSource;
    }
}
