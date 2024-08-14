package ru.biosoft.galaxy.javascript;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.galaxy.GalaxyDataCollection;
import ru.biosoft.plugins.javascript.Global;
import com.developmentontheedge.beans.annot.PropertyDescription;

/**
 * Host object to launch JavaScript analyses
 * @author lan
 */
@PropertyDescription("Galaxy analyses")
public class JavaScriptGalaxy extends ScriptableObject
{
    public static final DataElementPath GALAXY_COLLECTION = DataElementPath.create("analyses/Galaxy");

    private final Map<String, GalaxyCategory> categories = new LinkedHashMap<>();
    private boolean init = false;
    private String help;
    
    private static class ReloadFunction extends BaseFunction
    {
        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
        {
            GalaxyDataCollection gdc = GALAXY_COLLECTION.getDataElement(GalaxyDataCollection.class);
            gdc.reinit();
            Collection<String> initMessages = gdc.getInitMessages();
            if(initMessages == null || initMessages.isEmpty()) return "ok";
            return "The following errors occured during reload:\n\t" + String.join("\n\t", initMessages);
        }
    }
    
    private final Function reload = new ReloadFunction();
    
    /**
     * Converts category or analysis name to JavaScript name
     */
    public static String convertName(String name)
    {
        return name.replaceAll("[\\- ]", "_").toLowerCase();
    }
    
    private synchronized void init()
    {
        if(init) return;
        init = true;
        DataCollection<?> dc = GALAXY_COLLECTION.optDataCollection();
        if(dc == null) return;
        Index titleIndex = dc.getInfo().getQuerySystem().getIndex("title");
        for(String category: dc.getNameList())
        {
            try
            {
                categories.put(convertName(category), new GalaxyCategory(category, titleIndex.get(category).toString()));
            }
            catch( Exception e )
            {
            }
        }
    }

    @Override
    public String getClassName()
    {
        return "JavaScriptGalaxy";
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
        if(name.equals("reload") && SecurityManager.isAdmin())
        {
            return reload;
        }
        return categories.get(name);
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
        return categories.containsKey(name);
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
        return categories.keySet().toArray();
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
            StringBuilder sb = new StringBuilder("<center><h3>Galaxy host object</h3></center><br>Access to the Galaxy analyses");
            sb.append("<ul>");
            for(Entry<String,GalaxyCategory> entry: categories.entrySet())
            {
                sb.append("<li>"+entry.getKey()+" &ndash; "+entry.getValue().getTitle()+"</li>");
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
