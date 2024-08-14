package biouml.plugins.reactome.imports;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.ProtectedModule;
import biouml.plugins.download.FileDownloader;
import biouml.plugins.lucene.LuceneInitListener;
import biouml.plugins.lucene.LuceneQuerySystemImpl;
import biouml.plugins.lucene.LuceneUtils;
import biouml.plugins.reactome.access.DiagramSqlTransformerNew;
import biouml.plugins.reactome.access.ReactomeDiagramRepository;
import biouml.plugins.reactome.access.ReactomePathwayTitleIndexNew;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.security.GlobalDatabaseManager;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.journal.ProjectUtils;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.archive.ArchiveFactory;
import ru.biosoft.util.bean.BeanInfoEx2;

public class InstallReactomeAnalysis extends AnalysisMethodSupport<InstallReactomeAnalysis.InstallReactomeAnalysisParameters>
{
    private static final String DEFAULT_REPOSITORY = "default.repository";
    private static final String DEFAULT_CONFIG = "default.config";
    private static final String DATABASE = "Reactome";
    private final Map<String, Properties> propertySets = ExProperties.createPropertySets( new String[][][] {
            {{DEFAULT_CONFIG},
                    {"bioHub.functional", "biouml.plugins.reactome.biohub.ReactomeFunctionalHub;name=Reactome pathways"},
                    {"bioHub.keynodes", "biouml.plugins.reactome.biohub.ReactomeSqlBioHub;name=Reactome database"},
                    {"bioHub.matching", "biouml.plugins.reactome.biohub.ReactomeUniprotHub;name=ReactomeUniprotHub"},
                    {"graph-search", "true"},
                    {"lucene-directory", "luceneIndex"},
                    {"querySystem", LuceneQuerySystemImpl.class.getName()},
                    {"showStatistics", "true"},
                    {DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.reactome;biouml.plugins.server"},
                    {"database", DATABASE},
                    {DataCollectionConfigConstants.DATA_COLLECTION_LISTENER, LuceneInitListener.class.getName()},
                    {"description", "Reactome.html"},
                    {DataCollectionConfigConstants.CLASS_PROPERTY, "biouml.plugins.server.SqlModule"},
                    {DataCollectionConfigConstants.NEXT_CONFIG, DEFAULT_REPOSITORY}},
            {{DEFAULT_REPOSITORY},
                    {DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName()}},
            {{"Data/default.config"},
                    {DataCollectionConfigConstants.NAME_PROPERTY, "Data"},
                    {DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName()},
                    {LocalRepository.CONFIG_ALT_PATH_PROPERTY, "biouml.plugins.reactome:db/Data"}},
            {{"Diagrams/default.config"},
                    {DataCollectionConfigConstants.NAME_PROPERTY, "Diagrams"},
                    {DataCollectionConfigConstants.CLASS_PROPERTY, ReactomeDiagramRepository.class.getName()}}
    } );

    public InstallReactomeAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new InstallReactomeAnalysisParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();

        try
        {
            new URL( parameters.getUrl() );
        }
        catch( MalformedURLException e )
        {
            throw new IllegalArgumentException( "Malformed URL: " + e.getMessage() );
        }

        Repository repository = (Repository)CollectionFactoryUtils.getDatabases();
        String dbName = DATABASE + parameters.getDbVersion();

        try
        {
            DataCollection<?> module = repository.get( dbName );
            if( module != null )
                throw new IllegalArgumentException(
                        "Module '" + dbName + "' already exists. Please, specify another version or delete repository." );
        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ExceptionRegistry.log( e );
        }
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info( "Downloading..." );
        jobControl.pushProgress( 0, 5 );
        File reactomeDirectory = null;
        File modulePath = null;
        File destinationFile = TempFiles.file( "reactome.sql.tar.gz" );
        Connection adminConnection = GlobalDatabaseManager.getDatabaseConnection();
        String database = null;
        String dbName = null;
        boolean ok = false;
        try
        {
            URL url = new URL( parameters.getUrl() );
            FileDownloader.downloadFile( url, destinationFile, jobControl );
            jobControl.popProgress();
            if( jobControl.isStopped() )
                return null;

            log.info( "Unpacking..." );
            jobControl.pushProgress( 5, 15 );
            reactomeDirectory = TempFiles.dir( "proteome" );
            ArchiveFactory.unpack( destinationFile, reactomeDirectory, jobControl );
            destinationFile.delete();
            File dumpFile = null;
            if( reactomeDirectory.isDirectory() )
            {
                for( File file : reactomeDirectory.listFiles() )
                {
                    if( file.getName().endsWith( ".sql" ) )
                    {
                        dumpFile = file;
                        break;
                    }
                }
            }
            if( dumpFile == null )
            {
                throw new IllegalArgumentException( "Given archive does not contain sql dump file" );
            }
            jobControl.popProgress();
            if( jobControl.isStopped() )
                return null;

            log.info( "Init database..." );
            String version = parameters.getDbVersion();
            database = "reactome_" + version.replace( '.', '_' );
            String dbUser = parameters.getDbUser();
            String dbPassword = parameters.getDbPassword();
            SqlUtil.createDatabase( adminConnection, database, dbUser, dbPassword );

            log.info( "Extracting database (this may take a while)..." );
            jobControl.pushProgress( 15, 45 );
            List<String> params = new ArrayList<>();
            params.add( "mysql" );
            params.add( "-u" + dbUser );
            params.add( "-p" + dbPassword );
            params.add( database );
            File outFile = TempFiles.file( "reactome_temp_out.txt" );
            int status = new ProcessBuilder( params ).redirectInput( dumpFile ).redirectOutput( outFile ).start().waitFor();
            if( status != 0 )
            {
                throw new IllegalArgumentException( "Can not extract reactome database, status=" + status );
            }
            jobControl.popProgress();
            if( jobControl.isStopped() )
                return null;

            jobControl.pushProgress( 45, 60 );
            log.info( "Creating SQL indexes (this may take a while)..." );
            Connection dbConnection = SqlConnectionPool.getPersistentConnection( getDBUrl( database ), dbUser, dbPassword );
            createIdIndex( dbConnection );

            jobControl.popProgress();
            if( jobControl.isStopped() )
                return null;
            jobControl.pushProgress( 60, 70 );
            log.info( "Available species scanning..." );
            createSpeciesTable( dbConnection );

            log.info( "Initialization of pathway to species matching..." );
            createDiagram2SpeciesTable( dbConnection );

            List<RSpecies> speciesList = getSpeciesList( dbConnection );

            jobControl.popProgress();
            if( jobControl.isStopped() )
                return null;
            jobControl.pushProgress( 70, 75 );
            log.info( "Creating repository..." );

            dbName = DATABASE + parameters.getDbVersion();
            modulePath = createRepository( dbName, database, parameters.getDbVersion(), speciesList );

            jobControl.popProgress();
            log.info( "Creating collection..." );
            jobControl.pushProgress( 75, 80 );

            Properties properties = new ExProperties( new File( modulePath, DEFAULT_CONFIG ) );
            Repository repository = (Repository)CollectionFactoryUtils.getDatabases();
            DataCollection<?> rCollection = CollectionFactory.createCollection( repository, properties );
            repository.put( rCollection );

            DataCollection<?> module = repository.get( dbName );
            if( module == null || ! ( module instanceof Module ) )
            {
                throw new IllegalArgumentException( "Reactome module was not created properly" );
            }
            module = ProtectedModule.protect( (Module)module, 1 );

            jobControl.popProgress();
            log.info( "Indexing..." );
            jobControl.pushProgress( 80, 99 );
            log.setLevel( Level.INFO );
            LuceneUtils.buildIndexes( module, jobControl, log );
            jobControl.popProgress();
            ok = true;
            return module;
        }
        finally
        {
            destinationFile.delete();
            if( reactomeDirectory != null )
                ApplicationUtils.removeDir( reactomeDirectory );
            if( !ok )
            {
                if( dbName != null )
                {
                    try
                    {
                        CollectionFactoryUtils.getDatabases().remove( dbName );
                    }
                    catch( Exception e )
                    {
                    }
                }
                if( database != null )
                {
                    try
                    {
                        SqlUtil.execute( adminConnection, "DROP DATABASE " + database );
                    }
                    catch( Exception e )
                    {
                    }
                }
                if( modulePath != null )
                    ApplicationUtils.removeDir( modulePath );
            }
        }
    }

    private static String getDBUrl(String database)
    {
        return GlobalDatabaseManager.getCurrentDBUrl() + "/" + database + "?allowLoadLocalInfile=true";
    }

    private void createDiagram2SpeciesTable(Connection conn)
    {
        String dropIfExist = "DROP TABLE IF EXISTS BioUML_diagrams";
        SqlUtil.execute( conn, dropIfExist );

        String createMatchingTable = "CREATE TABLE BioUML_diagrams"
                + " SELECT p2r.db_id,p2r.representedPathway,do._displayName,e2s.species FROM PathwayDiagram_2_representedPathway p2r"
                + " INNER JOIN DatabaseObject do ON (p2r.representedPathway=do.DB_ID AND representedPathway_rank=0)"
                + " INNER JOIN Event_2_species e2s ON (p2r.representedPathway=e2s.DB_ID AND e2s.species_rank=0)";
        SqlUtil.execute( conn, createMatchingTable );

        String createIndex = "CREATE INDEX biouml_diagram_species_index1 ON BioUML_diagrams(species)";
        SqlUtil.execute( conn, createIndex );
        createIndex = "CREATE INDEX biouml_diagram_species_index2 ON BioUML_diagrams(db_id)";
        SqlUtil.execute( conn, createIndex );
        createIndex = "CREATE INDEX biouml_diagram_species_index3 ON BioUML_diagrams(db_id,species)";
        SqlUtil.execute( conn, createIndex );
    }

    private void createSpeciesTable(Connection conn)
    {
        String dropIfExist = "DROP TABLE IF EXISTS BioUML_species_list";
        SqlUtil.execute( conn, dropIfExist );

        String createSpeciesListTable = "CREATE TABLE BioUML_species_list"
                + " SELECT t2n.DB_ID, t2n.name FROM Taxon_2_name t2n"
                + " INNER JOIN Species s ON (s.DB_ID=t2n.DB_ID) WHERE name_rank=0";
        SqlUtil.execute( conn, createSpeciesListTable );
    }

    private void createIdIndex(Connection conn) throws SQLException
    {
        String queryCheck = "SHOW INDEXES FROM StableIdentifier WHERE Key_name LIKE 'biouml_%'";
        String indexQuery = "CREATE INDEX biouml_identifier_index ON StableIdentifier(identifier(15))";
        try( Statement st = conn.createStatement(); ResultSet rs = st.executeQuery( queryCheck ) )
        {
            if( rs.next() )
                log.info( "Index already exists" );
            else
                SqlUtil.execute( conn, indexQuery );
        }
    }

    private List<RSpecies> getSpeciesList(Connection conn) throws SQLException
    {
        List<RSpecies> result = new ArrayList<>();
        String speciesListQuery = "SELECT DISTINCT sl.name,sl.db_id FROM BioUML_species_list sl"
                + " INNER JOIN BioUML_diagrams d ON (d.species=sl.db_id)";
        try( Statement st = conn.createStatement(); ResultSet rs = st.executeQuery( speciesListQuery ) )
        {
            while( rs.next() )
                result.add( new RSpecies( rs.getString( 1 ), rs.getLong( 2 ) ) );
        }
        return result;
    }

    @Override
    protected void writeProperties(DataElement de) throws Exception
    {
        if( de instanceof DataCollection )
        {
            //do not write analysis properties, since there is SQL info
            CollectionFactoryUtils.save( de );
        }
    }

    /**
     * @param dbName - name of database
     * @param version - version of database
     * @return
     */
    private File createRepository(String dbName, String sqlDBName, String version, List<RSpecies> speciesList) throws Exception
    {
        File modulePath = CollectionFactoryUtils.createDatabaseDirectory( dbName );

        propertySets.get( DEFAULT_CONFIG ).setProperty( DataCollectionConfigConstants.NAME_PROPERTY, dbName );
        propertySets.get( DEFAULT_CONFIG ).setProperty( SqlDataCollection.JDBC_DRIVER_PROPERTY, SqlDataCollection.JDBC_DEFAULT_DRIVER );
        propertySets.get( DEFAULT_CONFIG ).setProperty( SqlDataCollection.JDBC_URL_PROPERTY, getDBUrl( sqlDBName ) );
        propertySets.get( DEFAULT_CONFIG ).setProperty( SqlDataCollection.JDBC_USER_PROPERTY, parameters.getDbUser() );
        propertySets.get( DEFAULT_CONFIG ).setProperty( SqlDataCollection.JDBC_PASSWORD_PROPERTY, parameters.getDbPassword() );
        propertySets.get( DEFAULT_CONFIG ).setProperty( "version", version );

        propertySets.get( DEFAULT_REPOSITORY ).setProperty( DataCollectionConfigConstants.NAME_PROPERTY, dbName );

        for( RSpecies species : speciesList )
            propertySets.putAll( createSpeciesSpecificDiagramProperties( species ) );

        ExProperties.createConfigs( propertySets, modulePath );
        createDescription( version, modulePath );
        return modulePath;
    }

    private int dConfig = 1;
    private Map<String, Properties> createSpeciesSpecificDiagramProperties(RSpecies species)
    {
        Map<String, Properties> propsMap = new LinkedHashMap<>();

        String configPrefix = "Diagrams" + dConfig;
        String mainConfig = configPrefix + ".node.config";
        String primaryConfig = configPrefix + ".primary.node.config";

        Properties pMain = new Properties();
        //        pMain.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "Diagrams (" + species.getName() + ")" );
        pMain.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, species.getName() );
        pMain.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, "ru.biosoft.access.security.NetworkDataCollection" );
        pMain.setProperty( DataCollectionConfigConstants.NEXT_CONFIG, primaryConfig );
        propsMap.put( "Diagrams/" + mainConfig, pMain );

        Properties pPrimary = new Properties();
        //        pPrimary.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "Diagrams (" + species.getName() + ")" );
        pPrimary.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, species.getName() );
        pPrimary.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, "ru.biosoft.access.SqlDataCollection" );
        pPrimary.setProperty( DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, Diagram.class.getName() );
        pPrimary.setProperty( "transformerClass", DiagramSqlTransformerNew.class.getName() );
        pPrimary.setProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.lucene;biouml.plugins.reactome" );
        pPrimary.setProperty( "lucene-indexes", "name;title;components;__childNames" );
        pPrimary.setProperty( "querySystem", "ru.biosoft.access.DefaultQuerySystem" );
        pPrimary.setProperty( "querySystem.indexes", "title" );
        pPrimary.setProperty( "index.title", ReactomePathwayTitleIndexNew.class.getName() );
        pPrimary.setProperty( "lucene-helper", "biouml.plugins.lucene.DiagramLuceneHelper" );
        pPrimary.setProperty( RSpecies.REACTOME_SPECIES_PROPERTY, species.toString() );
        propsMap.put( "Diagrams/" + primaryConfig, pPrimary );

        dConfig++;
        return propsMap;
    }

    private void createDescription(String version, File root) throws Exception
    {
        String fileName = "Reactome.html";
        File file = new File( root, fileName );
        file.getParentFile().mkdirs();
        try( FileOutputStream out = new FileOutputStream( file );
                Writer writer = new OutputStreamWriter( out, StandardCharsets.UTF_8 );
                BufferedWriter bw = new BufferedWriter( writer ) )
        {
            InputStream is = InstallReactomeAnalysis.class.getResourceAsStream( "resources/Reactome.html" );
            String description = ApplicationUtils.readAsString( is );
            description = description.replace( "$version$", version );
            bw.write( description );
        }
    }

    @SuppressWarnings ( "serial" )
    public static class InstallReactomeAnalysisParameters extends AbstractAnalysisParameters
    {
        String url = "";
        String dbVersion;
        String dbUser = "reactome";
        String dbPassword = "reactome";

        public InstallReactomeAnalysisParameters()
        {
            try
            {
                dbVersion = CollectionFactoryUtils.getDatabases().stream()
                        .filter( dc -> DATABASE.equals( ProjectUtils.getDatabaseName( dc ) ) )
                        .map( ProjectUtils::getVersion )
                        .max( Comparator.comparing( String::valueOf ) )
                        .orElse( "57" );
            }
            catch( Exception e )
            {
                ExceptionRegistry.log( e );
                dbVersion = "57";
            }
        }

        @PropertyName ( "Reactome URL" )
        @PropertyDescription ( "URL to Reactome .tar.gz archive" )
        public String getUrl()
        {
            return url;
        }
        public void setUrl(String url)
        {
            String oldValue = this.url;
            this.url = url;
            firePropertyChange( "url", oldValue, url );
        }

        @PropertyName ( "Version" )
        @PropertyDescription ( "Version of installing Reactome database" )
        public String getDbVersion()
        {
            return dbVersion;
        }
        public void setDbVersion(String dbVersion)
        {
            this.dbVersion = dbVersion;
        }

        @PropertyName ( "SQL DB user" )
        public String getDbUser()
        {
            return dbUser;
        }
        public void setDbUser(String dbUser)
        {
            this.dbUser = dbUser;
        }

        @PropertyName ( "SQL DB password" )
        public String getDbPassword()
        {
            return dbPassword;
        }
        public void setDbPassword(String dbPassword)
        {
            this.dbPassword = dbPassword;
        }
    }

    public static class InstallReactomeAnalysisParametersBeanInfo extends BeanInfoEx2<InstallReactomeAnalysisParameters>
    {
        public InstallReactomeAnalysisParametersBeanInfo()
        {
            super( InstallReactomeAnalysisParameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "url" );
            addExpert( "dbUser" );
            addExpert( "dbPassword" );
            add( "dbVersion" );
        }
    }
}
