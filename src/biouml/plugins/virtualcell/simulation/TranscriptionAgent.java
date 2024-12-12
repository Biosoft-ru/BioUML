package biouml.plugins.virtualcell.simulation;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.json.JSONObject;

import biouml.plugins.simulation.Span;
import one.util.streamex.StreamEx;

public class TranscriptionAgent extends ProcessAgent
{
    protected static final Logger log = Logger.getLogger( TranscriptionAgent.class.getName() );
    Map<String, Integer> tfToIndex = new HashMap<>();
    private double[] prediction;
    private Set<String> tfs;

    public TranscriptionAgent(String name, Span span)
    {
        super( name, span );
    }



    @Override
    public void doStep()
    {
        String response = sendRequest();
        parseResponse( response );
    }

    @Override
    public void setValue(String variable, String name, double value)
    {
    }

    @Override
    public double getValue(String variable, String name)
    {
        return prediction[nameToIndex.get( name )];
    }

    private void parseResponse(String response)
    {
        JSONObject json = new JSONObject( response );
        String content = json.keys().next();
        JSONObject jsonContent = new JSONObject( content );
        String[] names = JSONObject.getNames( jsonContent );
        Object obje = jsonContent.get( names[0] );
        JSONObject jsonObje = (JSONObject)obje;
        String[] ns = JSONObject.getNames( jsonObje );

        for( String name : ns )
        {
            int index = nameToIndex.get( name );
            prediction[index] = Double.parseDouble( jsonObje.get( name ).toString() );
        }
    }

    private String sendRequest()
    {
        try
        {
            String url = "http://10.25.70.231:8841/api/start";
            //            String url = "http://localhost:5000/api/start";

            // Create a URL object from the string
            URL obj = new URL( url );

            // Create an HttpURLConnection object and open a connection
            HttpURLConnection con = (HttpURLConnection)obj.openConnection();

            // Set the request method to GET or POST (depending on your needs)
            con.setRequestMethod( "POST" ); // Use "POST" for POST requests

            // Set headers if necessary (e.g., authorization)
            con.setRequestProperty( "Content-Type", "application/json" );
            con.setRequestProperty( "Accept-Charset", "UTF-8" );
            con.setDoInput( true );
            con.setDoOutput( true );

            String tfString = "[\"" + StreamEx.of( tfs ).joining( "\",\"" ) + "\"]";


            String jsonInputString = "{\"line\": \"K562\", \"model\": \"FC\", \"tf_list\": " + tfString + "}";

            //            String jsonInputString = "{\"line\":\"value\", \"name\":\"John\"}";

            // Send the JSON data
            try (DataOutputStream os = new DataOutputStream( con.getOutputStream() ))
            {
                byte[] input = jsonInputString.getBytes( "utf-8" );
                os.write( input, 0, input.length );
            }

            // Get the response code
            int responseCode = con.getResponseCode();
            System.out.println( "Response Code: " + responseCode );

            // If the response is successful, read the response
            if( responseCode == HttpURLConnection.HTTP_OK )
            {
                BufferedReader in = new BufferedReader( new InputStreamReader( con.getInputStream() ) );
                String inputLine;
                StringBuffer response = new StringBuffer();

                while( ( inputLine = in.readLine() ) != null )
                {
                    response.append( inputLine );
                }
                in.close();

                // Print the response
                System.out.println( "Response: " + response.toString() );
                return response.toString();
            }
            else
            {
                System.out.println( "GET request failed." );
                return null;
            }

        }
        catch( Exception e )
        {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void initParameters()
    {
        MapPool tfPool = parametersMap.get( "Transcription Factors" );
        this.tfs = tfPool.getNames();
    }

    @Override
    public void read(String variable, MapPool pool)
    {

    }
}