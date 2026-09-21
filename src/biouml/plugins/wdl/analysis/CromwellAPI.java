package biouml.plugins.wdl.analysis;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONObject;

import com.developmentontheedge.application.ApplicationUtils;

public class CromwellAPI
{
	private static final Logger log = Logger.getLogger( CromwellAPI.class.getName() );
	public static CromwellAPI INSTANCE;

	static {
		File configFile = new File(System.getProperty( "biouml.server.path" ) + "/appconfig/wdl/config.json");

		String url = "http://localhost:8000";
		if(configFile.exists())
		{
			String configStr = null;
			try
			{
				configStr = ApplicationUtils.readAsString( configFile );
				JSONObject configJson = new JSONObject( configStr );
				String x = configJson.optString( "cromwellURL" );
				if(x != null)
					url = x;
			}
			catch( IOException e )
			{
				log.log( Level.SEVERE, "", e );
			}
		}
		INSTANCE = new CromwellAPI(url);
	}

	private String url ;

	public CromwellAPI(String url)
	{
		this.url = url;
	}

	public JSONObject describeWorkflow(String text, String inputs, byte[] dependenciesZip) throws IOException
	{
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		String boundary = "--FbSzivpsqri8iZc6EtghrX2JOpeYpzoCEFOE2I";
		builder.setBoundary( boundary );
		builder = builder.addBinaryBody( "workflowSource", text.getBytes(), ContentType.APPLICATION_OCTET_STREAM, "script.wdl" );
		if(inputs != null)
			builder = builder.addBinaryBody( "workflowInputs", inputs.getBytes(), ContentType.APPLICATION_OCTET_STREAM, "inputs.json" );
		if(dependenciesZip != null && dependenciesZip.length != 0)
		{
			builder.addBinaryBody( "workflowDependencies", dependenciesZip, ContentType.APPLICATION_OCTET_STREAM, "dependencies.zip" );
		}
		HttpEntity entity = builder.build();

		String responseStr = Request
				.Post( url + "/api/womtool/v1/describe" )
				.setHeader( "Content-Type", ContentType.MULTIPART_FORM_DATA.getMimeType() + "; boundary=" + boundary)
				.setHeader( "accept", ContentType.APPLICATION_JSON.getMimeType() )
				.body( entity )
				.execute()
				.returnContent().asString();

		return new JSONObject( responseStr );
	}

	public JSONObject submitWorkflow(String text, String inputs, String options, byte[] dependenciesZip) throws IOException
	{

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		String boundary = "--FbSzivpsqri8iZc6EtghrX2JOpeYpzoCEFOE2I";
		builder.setBoundary( boundary );
		builder = builder.addBinaryBody( "workflowSource", text.getBytes(), ContentType.APPLICATION_OCTET_STREAM, "script.wdl" );
		if(inputs != null)
			builder = builder.addBinaryBody( "workflowInputs", inputs.getBytes(), ContentType.APPLICATION_OCTET_STREAM, "inputs.json" );
		if(options != null)
			builder.addBinaryBody( "workflowOptions", options.getBytes(), ContentType.APPLICATION_OCTET_STREAM, "options.json" );
		if(dependenciesZip != null && dependenciesZip.length != 0)
		{
			builder.addBinaryBody( "workflowDependencies", dependenciesZip, ContentType.APPLICATION_OCTET_STREAM, "dependencies.zip" );
		}
		HttpEntity entity = builder.build();

		String responseStr = Request
				.Post( url + "/api/workflows/v1" )
				.setHeader( "Content-Type", ContentType.MULTIPART_FORM_DATA.getMimeType() + "; boundary=" + boundary)
				.setHeader( "accept", ContentType.APPLICATION_JSON.getMimeType() )
				.body( entity )
				.execute()
				.returnContent().asString();

		return new JSONObject( responseStr );
	}

	public String getTaskStatus(String id) throws IOException
	{
		Response response = Request
				.Get( url + "/api/workflows/v1/" + id + "/status" )
				.setHeader( "accept", ContentType.APPLICATION_JSON.getMimeType() )
				.execute();
		HttpResponse httpResp = response.returnResponse();
		int statusCode = httpResp.getStatusLine().getStatusCode();
        if( statusCode == HttpStatus.SC_NOT_FOUND )
			return "Not found";
		String responseStr = ApplicationUtils.readAsString( httpResp.getEntity().getContent() );
		return new JSONObject( responseStr ).getString( "status" );
	}

	public JSONObject getOutputs(String id) throws IOException
	{
		String responseStr = Request
				.Get( url + "/api/workflows/v1/" + id + "/outputs" )
				.setHeader( "accept", ContentType.APPLICATION_JSON.getMimeType() )
				.execute()
				.returnContent().asString();
		return new JSONObject( responseStr ).getJSONObject( "outputs" );
	}

	public JSONObject getLogs(String taskId) throws IOException, InterruptedException
	{
		String responseStr = null;
		int i = 1;
		while(true && i <= 10) {
		try {
			responseStr = Request
					.Get( url + "/api/workflows/v1/" + taskId + "/logs" )
					.setHeader( "accept", ContentType.APPLICATION_JSON.getMimeType() )
					.execute()
					.returnContent().asString();
			break;
		} 
		catch(Exception e) {
			log.info("Can not connect to the Cromwell instance");
			log.info("Trying to reconnect...");
			log.info("Reconnects remain: " + (10 - i));
			i++;
			Thread.sleep( 1000 );
		}
		}
		return new JSONObject( responseStr );
	}


}
