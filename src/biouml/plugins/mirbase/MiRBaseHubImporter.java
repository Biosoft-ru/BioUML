package biouml.plugins.mirbase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import biouml.plugins.download.FileDownloader;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.access.biohub.SQLBasedHub;
import ru.biosoft.util.TextUtil;

public class MiRBaseHubImporter
{
    private static final String INSERT_STATEMENT = "INSERT INTO hub (input,input_type,output,output_type,specie) VALUES(?,?,?,?,?)";

    private static final String MIRBASE_URL_TEMPLATE = "ftp://mirbase.org/pub/mirbase/$version$/";
    private static final String MIRNA_DAT_FILE = "miRNA.dat.gz";
    private static final String ALIASES_FILE = "aliases.txt.gz";
    
    private String baseURL;
    //private String db = "mirbase";
    //private String user = "mirbase";
    //private String password = "mirbase";
    private Set<String> allowedSpecies = new HashSet<>();
    
    public MiRBaseHubImporter(String version)
    {
        baseURL = MIRBASE_URL_TEMPLATE.replace( "$version$", version );
        allowedSpecies.add( "Homo sapiens" );
        allowedSpecies.add( "Mus musculus" );
        allowedSpecies.add( "Rattus norvegicus" );
        allowedSpecies.add( "Danio rerio" );
        allowedSpecies.add( "Arabidopsis thaliana" );
    }
    
    public void importHub() throws Exception
    {
        Map<String, String[]> aliases = fetchAliases();
        try(BufferedReader reader = getReader(new URL(baseURL + MIRNA_DAT_FILE));
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement( INSERT_STATEMENT ))
        {
            SQLBasedHub.createHubTable( con );
            
            String stemLoopName = null;
            Set<String> matureNames = new HashSet<>();
            Set<String> matureAccs = new HashSet<>();
            String stemLoopAcc = null;
            String entrezId = null;
            String species = null;
            String line;
            while((line = reader.readLine()) != null)
            {
                if(line.startsWith( "ID   " ))
                    stemLoopName =  line.split( " +" )[1];
                else if( line.startsWith( "AC  " ) )
                {
                    stemLoopAcc = line.split( " +" )[1];
                    stemLoopAcc = stemLoopAcc.substring( 0, stemLoopAcc.length() - 1 );
                }
                else if(line.startsWith( "DR   ENTREZGENE;" ))
                    entrezId = TextUtil.split( line, ';' )[1].trim();
                else if(line.startsWith( "DE   " ))
                {
                    species = line.substring( "DE   ".length() );
                    species = species.substring( 0, species.lastIndexOf( ' ' ) );
                    species = species.substring( 0, species.lastIndexOf( ' ' ) );
                }
                else if(line.startsWith( "FT   " ) && line.contains( "/product=\"" ))
                {
                    String product = line.substring( line.indexOf( '\"' ) + 1, line.lastIndexOf( '\"' ) );
                    matureNames.add( product );
                }
                else if( line.startsWith( "FT   " ) && line.contains( "/accession=\"" ) )
                {
                    String product = line.substring( line.indexOf( '\"' ) + 1, line.lastIndexOf( '\"' ) );
                    matureAccs.add( product );
                }
                else if(line.equals( "//" ))
                {
                    if(stemLoopName != null && entrezId != null && species != null
                            && allowedSpecies.contains( species ))
                    {
                        Set<String> stemLoopWithAliases = expandAliases(stemLoopName, aliases);
                        Set<String> matureWithAliases = matureNames.stream().flatMap( x->expandAliases( x, aliases ).stream() ).collect( Collectors.toSet() );
                        
                        for(String stemLoop : stemLoopWithAliases)
                            insertRow( ps, stemLoop, MiRBaseStemLoopMiRNA.class.getSimpleName(), entrezId, "EntrezGeneTableType", species );
                        
                        for(String mature : matureWithAliases)
                            for(String stemLoop : stemLoopWithAliases)
                                insertRow( ps, mature, MiRBaseMatureMiRNA.class.getSimpleName(), stemLoop, MiRBaseStemLoopMiRNA.class.getSimpleName(), species );
                        
                        Set<String> allNames = new HashSet<>();
                        allNames.addAll( stemLoopWithAliases );
                        allNames.addAll( matureWithAliases );
                        for(String id : allNames)
                        {
                            insertRow( ps, id, MiRBaseName.class.getSimpleName(), entrezId, "EntrezGeneTableType", species );
                            insertRow( ps, id, MiRBaseMixture.class.getSimpleName(), id, MiRBaseName.class.getSimpleName(), species );
                        }
                        for( String acc : matureAccs )
                        {
                            for( String id : aliases.get( acc ) )
                            {
                                insertRow( ps, acc, MiRBaseAccession.class.getSimpleName(), id, MiRBaseName.class.getSimpleName(),
                                        species );
                                insertRow( ps, acc, MiRBaseMixture.class.getSimpleName(), id, MiRBaseName.class.getSimpleName(), species );
                            }
                            insertRow( ps, stemLoopAcc, MiRBaseAccession.class.getSimpleName(), stemLoopName,
                                    MiRBaseName.class.getSimpleName(), species );
                            insertRow( ps, stemLoopAcc, MiRBaseMixture.class.getSimpleName(), stemLoopName,
                                    MiRBaseName.class.getSimpleName(), species );
                        }
                    }
                    matureNames.clear();
                    matureAccs.clear();
                    stemLoopName = null;
                    stemLoopAcc = null;
                    entrezId = null;
                    species = null;
                }
            }
        }
    }
    
    private void insertRow(PreparedStatement ps, String input, String inputType, String output, String outputType, String species) throws SQLException
    {
        ps.setString( 1, input);
        ps.setString( 2, inputType );
        ps.setString( 3, output );
        ps.setString( 4, outputType );
        ps.setString( 5, species );
        ps.executeUpdate();
    }
    
    private Set<String> expandAliases(String stemLoopName, Map<String, String[]> aliases)
    {
        Set<String> result = new HashSet<>();
        result.add( stemLoopName );
        if( aliases.containsKey( stemLoopName ) )
            for(String alias : aliases.get( stemLoopName ))
                result.add( alias );
        return result;
    }

    public BufferedReader getReader(URL url) throws Exception
    {
        File  file = File.createTempFile( "miRBase", "" );
        file.deleteOnExit();
        FileDownloader.downloadFile( url, file, null );
        InputStream is = new FileInputStream( file );
        is = new GZIPInputStream( is );
        return new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) );
    }
    
    protected Connection getConnection() throws Exception
    {
        //Class.forName( "com.mysql.jdbc.Driver" );
        //return DriverManager.getConnection( "jdbc:mysql://localhost:3306/" + db, user, password );
        return Connectors.getConnection( "mirbase" );
    }
    
    public Map<String, String[]> fetchAliases() throws Exception
    {
        try(BufferedReader reader = getReader( new URL(baseURL + ALIASES_FILE) ))
        {
            Map<String, String[]> aliases = new HashMap<>();
            String line;
            while((line = reader.readLine()) != null)
            {
                String acc = line.split( "\t" )[0];
                String group = line.substring( line.indexOf( '\t' ) + 1, line.length() - 1 );
                String[] elements = TextUtil.split( group, ';' );
                for(String e : elements)
                    aliases.put( e, elements );
                aliases.put( acc, elements );
            }
            return aliases;
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        MiRBaseHubImporter importer = new MiRBaseHubImporter( "21" );
        importer.importHub();
    }
}
