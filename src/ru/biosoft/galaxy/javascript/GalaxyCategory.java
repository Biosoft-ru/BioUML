package ru.biosoft.galaxy.javascript;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.galaxy.DataSourceMethodInfo;
import ru.biosoft.galaxy.GalaxyMethodInfo;
import ru.biosoft.plugins.javascript.Global;

/**
 * Category of galaxy analyses accessible via JavaScript
 * @author lan
 */
public class GalaxyCategory extends ScriptableObject
{
    private String name;
    private String title;
    private Map<String, GalaxyAnalysisRecord> analyses = new LinkedHashMap<>();
    private boolean init;
    private String help;
    
    GalaxyCategory(String name, String title)
    {
        this.name = name;
        this.title = title;
    }

    private synchronized void init()
    {
        if(init) return;
        init = true;
        try
        {
            DataCollection<DataElement> dc = JavaScriptGalaxy.GALAXY_COLLECTION.getChildPath(name).getDataCollection();
            for(String analysisName: dc.getNameList())
            {
                try
                {
                    GalaxyMethodInfo analysis = (GalaxyMethodInfo)dc.get(analysisName);
                    if(!(analysis instanceof DataSourceMethodInfo))
                        analyses.put(JavaScriptGalaxy.convertName(analysisName), new GalaxyAnalysisRecord(analysis));
                }
                catch( Exception e )
                {
                }
            }
        }
        catch( Exception e )
        {
        }
    }

    @Override
    public String getClassName()
    {
        return "GalaxyCategory";
    }

    public String getName()
    {
        return name;
    }

    public String getTitle()
    {
        return title;
    }


    @Override
    public Object get(int index, Scriptable start)
    {
        return null;
    }

    @Override
    public Object get(String name, Scriptable start)
    {
        init();
        return analyses.get(name);
    }

    @Override
    public boolean has(int index, Scriptable start)
    {
        return false;
    }

    @Override
    public boolean has(String name, Scriptable start)
    {
        init();
        return analyses.containsKey(name);
    }

    @Override
    public void put(int index, Scriptable start, Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void put(String name, Scriptable start, Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(int index)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] getAllIds()
    {
        init();
        return analyses.keySet().toArray();
    }

    @Override
    public Object[] getIds()
    {
        return getAllIds();
    }

    @Override
    public String toString()
    {
        init();
        if(help == null)
        {
            StringBuilder sb = new StringBuilder("<center><h3>Galaxy category \""+getName()+"\"</h3></center><br>"+getTitle()+"<br><br>");
            sb.append("Each analysis takes single argument, which contains the parameters. The return value contains informational messages.");
            sb.append("<ul>");
            for(Entry<String, GalaxyAnalysisRecord> entry: analyses.entrySet())
            {
                sb.append("<li>String <b>"+entry.getKey()+"</b>([Scriptable parameters])<br> "+entry.getValue().getTitle()+"<br><br></li>");
            }
            sb.append("</ul>");
            help = sb.toString();
        }
        ScriptEnvironment environment = Global.getEnvironment();
        if(environment != null)
            environment.showHtml(help);
        return help;
    }
}