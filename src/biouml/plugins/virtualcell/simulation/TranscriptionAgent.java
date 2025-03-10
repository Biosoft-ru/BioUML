package biouml.plugins.virtualcell.simulation;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.json.JSONObject;

import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import one.util.streamex.StreamEx;

public class TranscriptionAgent extends ProcessAgent
{
    protected static final Logger log = Logger.getLogger(SimulatorSupport.class.getName());
    
    private double[] prediction;
    private Set<String> tfs;
    private Set<String> knockedOut;
    private String line;
    private String model;
    
    public TranscriptionAgent(String name, Span span)
    {
        super( name, span );
    }

    public void setLine(String line)
    {
        this.line = line;
    }

    public void setModel(String model)
    {
        this.model = model;
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
        String[] names = JSONObject.getNames( json );
        String valueName = names[0];
        JSONObject obje = json.getJSONObject( valueName );
        String[] ns = JSONObject.getNames( obje );
        for( String name : ns )
        {
            int index = nameToIndex.get( name );
            prediction[index] = Double.parseDouble( obje.get( name ).toString() );
        }
    }

    private String sendRequest()
    {
        try
        {
            String url = "http://10.25.70.231:8841/api/start";
            //                        String url = "http://localhost:5000/api/start";

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
           
            log.info( "List of knocked out TFs: "+StreamEx.of(tfs).filter( tf->knockedOut.contains( tf )).joining(","));
            
            Set<String> activeTfs = new HashSet<>();
            activeTfs.addAll( tfs );
            activeTfs.removeAll( knockedOut );
            String tfString = "[\"" + StreamEx.of( activeTfs ).joining( "\",\"" ) + "\"]";

            String modelString = "\"" + model + "\"";
            String lineString = "\"" + line + "\"";

            String jsonInputString = "{\"line\": " + lineString + ", \"model\": " + modelString + ", \"tf_list\": " + tfString + "}";

            //            String jsonInputString = "{\"line\":\"value\", \"name\":\"John\"}";

            // Send the JSON data
            try (DataOutputStream os = new DataOutputStream( con.getOutputStream() ))
            {
                byte[] input = jsonInputString.getBytes( "utf-8" );
                os.write( input, 0, input.length );
            }

            // Get the response code
            int responseCode = con.getResponseCode();
            //            System.out.println( "Response Code: " + responseCode );

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
                //                System.out.println( "Response: " + response.toString() );
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
    
    public void setKnocked(Set<String> knockedTFS )
    {
        this.knockedOut = knockedTFS;
    }

    @Override
    public void initPoolVariables(MapPool pool)
    {
        super.initPoolVariables( pool );
        prediction = new double[nameToIndex.size()];
    }
}