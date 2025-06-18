package biouml.plugins.expasy;

import java.io.BufferedReader;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.BeanInfoEx;

import biouml.model.Module;
import biouml.model.ProtectedModule;
import biouml.plugins.download.FileDownloader;
import biouml.plugins.lucene.LuceneUtils;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.Repository;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.security.GlobalDatabaseManager;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.classification.ClassificationUnitAsSQL;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil2;

public class InstallExPASy extends AnalysisMethodSupport<InstallExPASy.Parameters>
{
    private static final String DB_NAME = "ExPASy";
    
    public InstallExPASy(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        jobControl.pushProgress( 0, 20 );
        URL classURL = new URL( parameters.getUrl() + "/enzclass.txt");
        File classFile = TempFiles.file( "enzclass.txt" );
        FileDownloader.downloadFile( classURL, classFile, jobControl );
        jobControl.popProgress();
        
        jobControl.pushProgress( 20, 40 );
        URL datURL = new URL( parameters.getUrl() + "/enzyme.dat");
        File datFile = TempFiles.file( "enzyme.dat" );
        FileDownloader.downloadFile( datURL, datFile, jobControl );
        jobControl.popProgress();

        importEnzymes(classFile, datFile);
        
        DataCollection<?> module = CollectionFactoryUtils.getDatabases().get(DB_NAME);
        if(!(module instanceof Module))
        {
            throw new IllegalArgumentException("ExPASy module was not created properly");
        }
        module = ProtectedModule.protect((Module)module, 1);
        
        log.info("Indexing...");
        jobControl.pushProgress(80, 100);
        log.setLevel(Level.INFO);
        LuceneUtils.buildIndexes((Module)module, jobControl, log);
        jobControl.popProgress();
        
        return module;
    }
    
    private String getVersion() throws Exception
    {
        String version = new SimpleDateFormat( "MMM yyyy" ).format( new Date() );
        try(TempFile dbInfoFile = TempFiles.file( "expasy_enzyme" ))
        {
            FileDownloader.downloadFile( new URL( parameters.getUrl() + "/enzyme.get" ), dbInfoFile, null );
            try(BufferedReader reader = ApplicationUtils.asciiReader( dbInfoFile ))
            {
                String line;
                while((line = reader.readLine()) != null)
                {
                    String versionPrefix = "Last revised:";
                    if(line.startsWith( versionPrefix ))
                        version = line.substring( versionPrefix.length() ).trim();
                }
            }
        }
        return version;
    }
    
    private void importEnzymes(File classFile, File datFile) throws Exception
    {
        File root = CollectionFactoryUtils.createDatabaseDirectory(DB_NAME);
        
        Properties defaultConfig = new Properties();
        defaultConfig.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, DB_NAME );
        defaultConfig.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, "biouml.plugins.server.SqlModule" );
        defaultConfig.setProperty( DataCollectionConfigConstants.NEXT_CONFIG, "default.repository" );
        defaultConfig.setProperty( "module-type", "biouml.standard.StandardModuleType" );
        defaultConfig.setProperty( "querySystem", "biouml.plugins.lucene.LuceneQuerySystemImpl" );
        defaultConfig.setProperty( "data-collection-listener", "biouml.plugins.lucene.LuceneInitListener" );
        defaultConfig.setProperty( "lucene-directory", "luceneIndex" );
        defaultConfig.setProperty( "database", "ExPASy" );
        defaultConfig.setProperty( "version", getVersion() );
        defaultConfig.setProperty( "plugins", "biouml.plugins.expasy" );
        defaultConfig.setProperty( "bioHub.matching", "biouml.plugins.expasy.ExPASyHub;name=ExPASy hub" );
        
        String dbUser = "expasy";
        String dbPassword = RandomStringUtils.randomAlphanumeric( 8 );
        Connection rootConnection = GlobalDatabaseManager.getDatabaseConnection();
        String baseUser = dbUser;
        for( int i = 0; SqlUtil.hasUser(rootConnection, dbUser); dbUser = baseUser + "_" + ( ++i ) );
        
        
        defaultConfig.setProperty(SqlDataCollection.JDBC_DRIVER_PROPERTY, SqlDataCollection.JDBC_DEFAULT_DRIVER);
        String dbURL = GlobalDatabaseManager.getCurrentDBUrl() + "/" + DB_NAME;
        defaultConfig.setProperty(SqlDataCollection.JDBC_URL_PROPERTY, dbURL);
        defaultConfig.setProperty(SqlDataCollection.JDBC_USER_PROPERTY, dbUser);
        defaultConfig.setProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY, dbPassword);
        defaultConfig.setProperty(GenericDataCollection.PREFERED_TABLE_IMPLEMENTATION_PROPERTY, "SQL");
        
        Properties defaultRepository = new Properties();
        defaultRepository.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, DB_NAME );
        defaultRepository.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, "ru.biosoft.access.LocalRepository" );
        
        Properties enzymesConfig = new Properties();
        enzymesConfig.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "enzymes" );
        enzymesConfig.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, "ru.biosoft.bsa.classification.ClassificationUnitAsSQL" );
        enzymesConfig.setProperty( ClassificationUnitAsSQL.CLASSIFICATION_TABLE_NAME, "classification" );
        enzymesConfig.setProperty( ClassificationUnitAsSQL.TF_ID, "" );
        enzymesConfig.setProperty("querySystem", "ru.biosoft.access.DefaultQuerySystem");
        enzymesConfig.setProperty( "querySystem.indexes", "title" );
        enzymesConfig.setProperty( "index.title", "biouml.standard.type.access.TitleIndex");
        enzymesConfig.setProperty( "index.title.property", "displayName");
        
        Map<String, Properties> propertySet = new HashMap<>();
        propertySet.put( "default.config", defaultConfig );
        propertySet.put( "default.repository", defaultRepository );
        propertySet.put( "enzymes.node.config", enzymesConfig );
        ExProperties.createConfigs( propertySet, root );
        
        SqlUtil.createDatabase(rootConnection, DB_NAME, dbUser, dbPassword);
        Connection con = SqlConnectionPool.getPersistentConnection( dbURL, dbUser, dbPassword );
        SqlUtil.execute( con, "CREATE TABLE `classification` ( `name` varchar(20) DEFAULT NULL,"
                + "`parent` varchar(20) DEFAULT NULL,"
                + "`title` text DEFAULT NULL,"
                + "`description` text,"
                + "`level` int(1) DEFAULT NULL,"
                + "UNIQUE KEY `name` (`name`))" );
        SqlUtil.execute( con, "CREATE TABLE `hub` ( `input` varchar(20) default NULL,"
                +"`input_type` varchar(30) default NULL,"
                +"`output` varchar(20) default NULL,"
                +"`output_type` varchar(30) default NULL,"
                +"`specie` varchar(50) default NULL,"
                +"KEY `input` (`input`,`specie`,`output_type`),"
                +"KEY `output` (`output`,`specie`,`input_type`))");
        
        try(BufferedReader reader = ApplicationUtils.asciiReader( classFile ))
        {
            String line;
            while( ( line = reader.readLine() ) != null )
                if( !line.isEmpty() && Character.isDigit( line.charAt( 0 ) ) )
                {
                    String[] fields = line.split( "  ", 2 );
                    String title = fields[1].trim();
                    String name = fields[0].replaceAll( " ", "" ).replaceAll( "-", "" ).replaceAll( "[.]*$", "" );
                    int idx = name.lastIndexOf( '.' );
                    String parent = idx > 0 ? name.substring( 0, idx ) : "";
                    String description = "";
                    int level = StringUtils.countMatches( name, "." ) + 1;
                    SqlUtil.execute(
                            con,
                            "INSERT INTO classification VALUES ("
                                    + StringUtils.join(
                                            new Object[] {SqlUtil.quoteString( name ), SqlUtil.quoteString( parent ),
                                                    SqlUtil.quoteString( title ), SqlUtil.quoteString( description ), level}, "," ) + ")" );
                }
        }
        
        try(BufferedReader reader = ApplicationUtils.asciiReader( datFile ))
        {
            String line;
            String id = null;
            StringBuilder description = new StringBuilder();
            StringBuilder anternativeName = new StringBuilder();
            StringBuilder cataliticActivity = new StringBuilder();
            StringBuilder cofactors = new StringBuilder();
            StringBuilder comments = new StringBuilder();
            StringBuilder prosite = new StringBuilder();
            StringBuilder swissProt = new StringBuilder();
            while((line = reader.readLine()) != null)
            {
                if(line.equals( "//" ))
                {
                    if(id != null)
                    {
                        SqlUtil.execute(con, "INSERT INTO classification VALUES (" + StringUtils.join(
                            new Object[] {SqlUtil.quoteString( id ), SqlUtil.quoteString( id.substring( 0, id.lastIndexOf( '.' ) ) ), SqlUtil.quoteString( description.toString() ),
                                                    SqlUtil.quoteString( description.toString() ), 5}, "," ) + ")" );
                        for(String spEntry : TextUtil2.split( swissProt.toString(), ';' ))
                        {
                            spEntry = spEntry.trim();
                            if(spEntry.isEmpty()) continue;
                            String[] fields = spEntry.split( ", ", 2 );
                            String spID = fields[0];
                            String specie = fields[1].split( "_", 2 )[1];
                            
                            if(specie.equals( "HUMAN" ))
                                specie = "Homo sapiens";
                            if(specie.equals( "MOUSE" ))
                                specie = "Mus musculus";
                            if(specie.equals( "RAT" ))
                                specie = "Rattus norvegicus";
                            SqlUtil.execute( con, "INSERT INTO hub VALUES(" + SqlUtil.quoteString( id ) + ", " + SqlUtil.quoteString( "EnzymeExpasyType" ) + ", " + SqlUtil.quoteString( spID ) + ", " + SqlUtil.quoteString( "UniprotProteinTableType" ) + ", " + SqlUtil.quoteString( specie ) + ")" );
                        }
                    }
                    id = null;
                    description = new StringBuilder();
                    anternativeName = new StringBuilder();
                    cataliticActivity = new StringBuilder();
                    cofactors = new StringBuilder();
                    comments = new StringBuilder();
                    prosite = new StringBuilder();
                    swissProt = new StringBuilder();
                    continue;
                }
                
                
                String value = line.length() > 5 ? line.substring( 5 ) : "";
                if(line.startsWith( "ID   " )) id = value;
                else if(line.startsWith( "DE   " )) description.append( value );
                else if(line.startsWith( "AN   " )) anternativeName.append( value );
                else if(line.startsWith( "CA   " )) cataliticActivity.append( value );
                else if(line.startsWith( "CF   " )) cofactors.append( value );
                else if(line.startsWith( "CC   " )) comments.append( value );
                else if(line.startsWith( "PR   " )) prosite.append( value );
                else if(line.startsWith( "DR   " )) swissProt.append( value );
            }
        }
        
        log.info("Creating collection...");
        defaultConfig.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, root.getAbsolutePath());
        Repository repository = (Repository)CollectionFactoryUtils.getDatabases();
        DataCollection<?> dbCollection = CollectionFactory.createCollection(repository, defaultConfig);
        repository.put(dbCollection);
    }
    
    
    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private String url = "ftp://ftp.expasy.org/databases/enzyme/";

        public String getUrl()
        {
            return url;
        }

        public void setUrl(String url)
        {
            Object oldValue = this.url;
            this.url = url;
            firePropertyChange( "url", oldValue, url );
        }
        
    }
    
    public static class ParametersBeanInfo extends BeanInfoEx
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class, true );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add( "url" );
        }
    }
}

