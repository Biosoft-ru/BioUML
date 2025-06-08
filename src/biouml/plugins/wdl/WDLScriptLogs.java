package biouml.plugins.wdl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class WDLScriptLogs
{
    private LogConsumer logConsumer;
    
    public enum LogType {STDOUT, STDERR};
    static class FileState
    {
        String taskName;
        String filePath;
        LogType type;
        int attempt, shardIndex;
        long offset;
    }
    public interface LogConsumer
    {
        void consume(String msg, String taskName, LogType type, int attempt, int shardIndex);
    }
    
    private Map<String,FileState> state = new HashMap<>();
    
    public WDLScriptLogs(LogConsumer logConsumer)
    {
        this.logConsumer = logConsumer;
    }
    
    public void updateLogs(JSONObject logsResponse) throws IOException
    {
        JSONObject calls = logsResponse.optJSONObject( "calls" );
        if(calls == null)
            return;
        for(String taskName : calls.keySet())
        {
            JSONArray arr = calls.getJSONArray( taskName );
            for(int i = 0; i < arr.length(); i++)
            {
                JSONObject obj = arr.getJSONObject( i );
                String stdoutFile = obj.getString( "stdout" );
                String stderrFile = obj.getString( "stderr" );
                int attempt = obj.getInt( "attempt" );
                int shardIndex = obj.getInt( "shardIndex" );
                
                if(!state.containsKey( stdoutFile ))
                    addLogFile(stdoutFile, taskName, LogType.STDOUT, attempt, shardIndex);
                if(!state.containsKey( stderrFile ))
                    addLogFile(stderrFile, taskName, LogType.STDOUT, attempt, shardIndex);
            }
        }
        
        for(FileState fs : state.values())
        {
            File file = new File(fs.filePath);
            long size = file.length();
            if(size > fs.offset)
            {
                FileInputStream fis = new FileInputStream( file );
                fis.skip( fs.offset );
                byte[] buffer = new byte[256];
                int n;
                while((n=fis.read( buffer ))!=-1)
                {
                    report(new String( buffer, 0, n ), fs);
                    fs.offset += n;
                }
            }
        }
    }

    private void report(String msg, FileState fs)
    {
        logConsumer.consume( msg, fs.taskName, fs.type, fs.attempt, fs.shardIndex );
    }

    private void addLogFile(String filePath, String taskName, LogType type, int attempt, int shardIndex)
    {
        FileState fileState = new FileState();
        fileState.filePath = filePath;
        fileState.taskName = taskName;
        fileState.type = type;
        fileState.attempt = attempt;
        fileState.shardIndex = shardIndex;
        state.put( filePath, fileState );
    }
}
