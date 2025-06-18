package ru.biosoft.server.servlets.webservices.providers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptJobControl;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.script.ScriptTypeRegistry.ScriptType;
import ru.biosoft.plugins.javascript.JScriptContext;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.Util;

import ru.biosoft.jobcontrol.JobControl;

/**
 * Provides script document functions
 */
public class WebScriptsProvider extends WebJSONProviderSupport
{
    private static void sendEnvironment(final String jobID, JSONResponse response) throws IOException
    {
        WebSession session = WebSession.getCurrentSession();
        Object value = session.getValue("environment_"+jobID);
        if(value instanceof WebJSEnvironment)
        {
            response.sendEnvironment((WebJSEnvironment)value);
        } else
        {
            response.error("Invalid environment");
        }
        session.putValue("environment_"+jobID, "");
    }

    private static void executeScript(final ScriptDataElement de, final String script, final String jobID)
    {
        execute( de, script, jobID, false );
    }

    private static void executeScript(String script, String jobID, String scriptType)
    {
        execute( ScriptTypeRegistry.createScript(scriptType, null, script), script, jobID, true );
    }

    private static void execute(final ScriptDataElement de, final String script, final String jobID, boolean sessionContext)
    {
        final WebJSEnvironment environment = new WebJSEnvironment(jobID);
        WebSession.getCurrentSession().putValue("environment_" + jobID, environment);
        final TaskInfo taskInfo = de.createTask(script, environment, sessionContext);
        final ScriptJobControl jobControl = (ScriptJobControl)taskInfo.getJobControl();
        WebJob.getWebJob(jobID).setJobControl(jobControl);
        TaskManager.getInstance().runTask(taskInfo);
    }

    private static void createScript(DataElementPath path, String type) throws WebException
    {
        DataCollection<? extends DataElement> dc = getDataElement(path.getParentPath(), DataCollection.class);
        if(!dc.isMutable())
        {
            throw new WebException("EX_ACCESS_READ_ONLY", path.getParentPath());
        }
        ScriptDataElement scriptElement = ScriptTypeRegistry.createScript(type, path, "");
        try
        {
            path.save(scriptElement);
        }
        catch( Exception e )
        {
            throw new WebException("EX_INTERNAL_CREATE_SCRIPT", path);
        }
    }

    private static final Set<String> excludedNativeIds = new HashSet<>(Arrays.asList("wait", "notify", "notifyAll", "equals", "hashCode", "getClass", "class"));
    /**
     * Sends root variables and their keys for autocomplete function on the client
     * @param response
     * @throws IOException
     */
    private void sendContext(JSONResponse response) throws IOException
    {
        JScriptContext.getContext();
        ScriptableObject scope = JScriptContext.getScope();
        try
        {
            JSONObject result = new JSONObject();
            for(Object id: scope.getAllIds())
            {
                JSONObject obj = new JSONObject();
                Object secondLevelObj = scope.get(id.toString(), scope);
                if(secondLevelObj instanceof Scriptable)
                {
                    Scriptable secondLevel = (Scriptable)secondLevelObj;
                    for(Object secondLevelId: secondLevel.getIds())
                    {
                        if(secondLevel instanceof NativeJavaObject && excludedNativeIds.contains(secondLevelId))
                            continue;
                        obj.put(secondLevelId.toString(), new JSONObject());
                    }
                }
                result.put(id.toString(), obj);
            }
            response.sendJSON(result);
        }
        catch( JSONException e )
        {
            response.sendJSON(new JSONArray());
        }
    }

    /**
     * JavaScript environment for BioUML web.
     */
    public static class WebJSEnvironment implements ScriptEnvironment
    {
        protected StringBuffer buffer = new StringBuffer();
        protected List<String> tables = new ArrayList<>();
        protected List<String> images = new ArrayList<>();
        protected List<String> html = new ArrayList<>();
        protected WebJob webJob;
        protected boolean hasData = false;
        private boolean overflow = false;

        public WebJSEnvironment(String jobID)
        {
            webJob = WebJob.getWebJob(jobID);
        }

        public boolean hasData()
        {
            return hasData;
        }

        public String getBuffer()
        {
            String result = buffer.toString();
            buffer = new StringBuffer();
            return result;
        }

        public String[] getTables()
        {
            return tables.toArray(new String[tables.size()]);
        }

        public String[] getImages()
        {
            return images.toArray(new String[images.size()]);
        }

        public String[] getHTML()
        {
            return html.toArray(new String[html.size()]);
        }

        protected synchronized void println(String message)
        {
            if( message != null && message.trim().length() > 0 && !overflow)
            {
                hasData = true;
                if(buffer.length() > 0x8000)
                {
                    overflow = true;
                    buffer.append("...");
                } else
                {
                    if(message.length() > 0x8000)
                    {
                        message = message.substring(0, 0x8000)+"...";
                        overflow = true;
                    }
                    buffer.append(message);
                    buffer.append("\n");
                }
            }
            webJob.addJobMessage(message+"\n");
        }

        @Override
        public void error(String msg)
        {
            println(msg);
        }

        @Override
        public void print(String msg)
        {
            println(msg);
        }

        @Override
        public void info(String msg)
        {
            print( msg );
        }

        @Override
        public void showGraphics(BufferedImage image)
        {
            showGraphics(new ImageDataElement("", null, image));
        }

        @Override
        public void showGraphics(ImageElement element)
        {
            hasData = true;
            String imageName = generateName();
            WebSession.getCurrentSession().getImagesMap().put(imageName, element);
            images.add(imageName);
        }

        @Override
        public void showHtml(String html)
        {
            hasData = true;
            this.html.add(html);
        }

        @Override
        public void showTable(TableDataCollection dataCollection)
        {
            hasData = true;
            String tableName = generateName();
            WebServicesServlet.getSessionCache().addObject(tableName, dataCollection, true);
            tables.add(tableName);
        }

        @Override
        public void warn(String msg)
        {
            println(msg);
        }

        /**
         * Generate unique name
         */
        protected String generateName()
        {
            return String.valueOf(System.currentTimeMillis())+Util.getUniqueId();
        }

        @Override
        public boolean isStopped()
        {
            return webJob.getJobControl().getStatus() == JobControl.TERMINATED_BY_REQUEST;
        }

        @Override
        public String addImage(BufferedImage image)
        {
            String imageName = generateName();
            ImageDataElement imageDataElement = new ImageDataElement("", null, image);
            WebSession.getCurrentSession().getImagesMap().put(imageName, imageDataElement);                       
            return  "../biouml/web/img?de=" + imageName;
        }
    }

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws WebException, IOException
    {
        String action = arguments.getAction();
        if(action.equals("context"))
        {
            sendContext(response);
            return;
        }
        if(action.equals("types"))
        {
            JSONObject jo = new JSONObject();
            Map<String, ScriptType> scriptTypesMap = ScriptTypeRegistry.getScriptTypes();
            for( Entry<String, ScriptType> e : scriptTypesMap.entrySet() )
            {
                jo.put( e.getKey(), e.getValue().toString() );
            }
            response.sendJSON( jo );
            return;
        }
        if( action.equals("create") )
        {
            createScript(arguments.getDataElementPath(), arguments.getString("type"));
            response.sendString("ok");
            return;
        }
        if( action.equals("environment") )
        {
            sendEnvironment(arguments.getString("jobID"), response);
            return;
        }
        if( action.equals("runInline") )
        {
            executeScript(TextUtil2.stripUnicodeMagic(arguments.getString("script")), arguments.get("jobID"), arguments.getString("type"));
            response.sendString("ok");
            return;
        }

        ScriptDataElement script = arguments.getDataElement(ScriptDataElement.class);
        if( action.equals("run") )
        {
            executeScript(script, TextUtil2.stripUnicodeMagic(arguments.getString("script")), arguments.get("jobID"));
            response.sendString("ok");
        }
        else throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION);
    }
}
