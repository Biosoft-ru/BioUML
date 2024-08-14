package ru.biosoft.tasks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;

public class NBGINextflowService implements NextflowService
{
    private String apiURL;
    private String execType;
    private String user;

    public NBGINextflowService(String apiURL, String execType, String user)
    {
        this.apiURL = apiURL;
        this.execType = execType;
        this.user = user;
    }
    
    
    public JSONObject submit(String nextflowScriptPath, String[] params) throws IOException
    {
        JSONObject requestJson = new JSONObject();
        requestJson.put( "execType", execType );
        requestJson.put( "script", nextflowScriptPath );
        requestJson.put( "type", "STARTED" );
        
        JSONArray paramsArray = new JSONArray();
        for(String p : params)
            paramsArray.put( p );
        requestJson.put( "scriptparams",  paramsArray);
        requestJson.put( "user", user );

        String responseStr;
            responseStr = Request
                    .Post( apiURL + "/nfcontrol/api/start" )
                    .bodyString( requestJson.toString(), ContentType.APPLICATION_JSON )
                    .execute()
                    .returnContent().asString();

        return new JSONObject( responseStr );
    }


   
    @Override
    public JSONObject status(String runname) throws IOException
    {
        String responseStr = Request
                .Get( apiURL + "/nfcontrol/api/process/" + runname )
                .execute()
                .returnContent().asString();

        return new JSONObject( responseStr );
    }
}
