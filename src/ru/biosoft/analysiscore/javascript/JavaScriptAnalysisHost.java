package ru.biosoft.analysiscore.javascript;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import one.util.streamex.StreamEx;

import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.plugins.javascript.Global;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("analysis")
@PropertyDescription("All analyses available in biouml, use analysis['<Analysis name>']")
public class JavaScriptAnalysisHost extends ScriptableObject
{
    private static Map<String, Map<String, AnalysisFunction>> analyses = new HashMap<>();
    private final Map<String, Method> methods = new HashMap<>();
    private static boolean locked = false;
    protected String hostName;
    protected String description;
    
    public static synchronized void addAnalysis(String hostName, String name, AnalysisMethodInfo methodInfo)
    {
        if(locked) return;
        if(!analyses.containsKey(hostName)) analyses.put(hostName, new HashMap<String, AnalysisFunction>());
        if(methodInfo != null) analyses.get(hostName).put(name, new AnalysisFunction(hostName, name, methodInfo));
    }
    
    public static synchronized void lock()
    {
        locked = true;
    }
    
    public static String getFunctionForAnalysis(String hostName, String analysisName)
    {
        return StreamEx.ofKeys(analyses.get(hostName), val -> val.getAnalysisName().equals(analysisName)).findAny()
                .orElse(null);
    }
    
    public JavaScriptAnalysisHost()
    {
        hostName = getClass().getAnnotation(PropertyName.class).value();
        if(!analyses.containsKey(hostName)) analyses.put(hostName, new HashMap<String, AnalysisFunction>());
        description = getClass().getAnnotation(PropertyDescription.class).value();
        try
        {
            for(Method method: getClass().getDeclaredMethods())
            {
                int modifiers = method.getModifiers();
                if(!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers)) continue;
                try
                {
                    methods.put(method.getName(), method);
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
        return "ANALYSIS_HOST";
    }

    @Override
    public Object get(int index, Scriptable start)
    {
        return null;
    }

    @Override
    public Object get(String name, Scriptable start)
    {
        if( methods.containsKey(name) )
        {
            final Method method = methods.get(name);
            return new NativeJavaMethod(method, name)
            {
                @Override
                public String toString()
                {
                    try
                    {
                        return super.toString()+"<br>"+method.getAnnotation(PropertyDescription.class).value();
                    }
                    catch( Exception e )
                    {
                        return super.toString();
                    }
                }
            };
        }
        AnalysisFunction function = analyses.get(hostName).get(name);
        if( function == null || AnalysisMethodRegistry.getMethodInfo(function.getAnalysisName()) == null )
            return null;
        return function;
    }

    @Override
    public boolean has(int index, Scriptable start)
    {
        return false;
    }

    @Override
    public boolean has(String name, Scriptable start)
    {
        if(methods.containsKey(name)) return true;
        AnalysisFunction function = analyses.get(hostName).get(name);
        return function != null && AnalysisMethodRegistry.getMethodInfo(function.getAnalysisName()) != null;
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
        return StreamEx.ofKeys( analyses.get( hostName ), fn -> AnalysisMethodRegistry.getMethodInfo( fn.getAnalysisName() ) != null )
                .sorted().toArray();
    }

    @Override
    public Object[] getIds()
    {
        return getAllIds();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<center><h2>JavaScript host object:</h2></center>");
        sb.append("ScriptableObject <b>").append(hostName).append("</b><br>");
        if(description != null) sb.append(description).append("<br><br>");
        sb.append("<center><h3>Functions:</h3></center>");
        sb.append("<ul>");
        for(Object funcName: getAllIds())
        {
            Method method = methods.get(funcName);
            if(method != null)
            {
                // TODO: support parameters
                sb.append("<li>").append(method.getReturnType().getSimpleName()).append(" <b>").append(funcName).append("</b>()<br>")
                        .append(method.getAnnotation(PropertyDescription.class).value()).append("</li>");
            } else
            {
                AnalysisFunction analysisFunction = analyses.get(hostName).get(funcName);
                sb.append("<li>String <b>").append(funcName).append("</b>({parameters})<br>Launches analysis '").append(analysisFunction.getAnalysisName()).append("'</li>");
            }
        }
        sb.append("</ul>");
        ScriptEnvironment environment = Global.getEnvironment();
        if(environment != null)
            environment.showHtml(sb.toString());
        return sb.toString();
    }
}
