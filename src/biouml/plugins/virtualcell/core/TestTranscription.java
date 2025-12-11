package biouml.plugins.virtualcell.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.TreeMap;

import org.json.JSONObject;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;

public class TestTranscription
{
    public static void main(String[] args)
    {

        try
        {
            String url = "http://localhost:5000/api/start";
            URL obj = new URL( url );
            //
            // Create an HttpURLConnection object and open a connection
            HttpURLConnection con = (HttpURLConnection)obj.openConnection();

            // Set the request method to GET or POST (depending on your needs)
            con.setRequestMethod( "POST" ); // Use "POST" for POST requests

            // Set headers if necessary (e.g., authorization)
            con.setRequestProperty( "Content-Type", "application/json" );
            con.setRequestProperty( "Accept-Charset", "UTF-8" );
            con.setDoInput( true );
            con.setDoOutput( true );
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

                parseResponse( response.toString() );
                // Print the response

            }
            else
            {
                System.out.println( "GET request failed." );
            }

        }
        catch( Exception e )
        {
            e.printStackTrace();
        }

    }

    private static void parseResponse(String response)
    {
        JSONObject json = new JSONObject( response );
        String[] names = JSONObject.getNames( json );
        String valueName = names[0];
        JSONObject obje = json.getJSONObject( valueName );
        String[] ns = JSONObject.getNames( obje );
        int i = 0;
        for( String name : ns )
        {
            if( i > 10 )
                break;
            i++;
            System.out.println( name + "\t" + Double.parseDouble( obje.get( name ).toString() ) );
        }
    }


    private void requestServer()
    {
        try
        {
            //            String url = "http://10.25.70.231:8841/api/start";
            String url = "http://localhost:5000/api/start";

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
            //            con.setIn
            String jsonInputString = "{\"line\": \"K562\", \"model\": \"FC\", \"tf_list\": {['CSN2']}}";

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
            }
            else
            {
                System.out.println( "GET request failed." );
            }

        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
}