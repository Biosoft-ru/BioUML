package biouml.plugins.pharm.nlme;

import one.util.streamex.StreamEx;

import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.standard.simulation.ResultListener;
import cern.jet.random.Uniform;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.model.Property;

import ru.biosoft.gui.GUI;
import ru.biosoft.gui.ViewPartRegistry;
import ru.biosoft.util.bean.JSONBean;
import ru.biosoft.workbench.script.ScriptConsole;

public class NlmeUtils
{
    public static void callConsole(String rCode)
    {
        ScriptConsole viewPart = (ScriptConsole)ViewPartRegistry.getViewPart( "script.console" );
        GUI.getManager().showViewPart( viewPart );
        viewPart.eval( rCode, "R" );
    }

    private static final Class<?>[] classes = {MixedEffectModelRunner.class, Option.class, Property.class, EventLoopSimulator.class,
            Uniform.class, JSONBean.class, StreamEx.class, ResultListener.class};

    public static String[] getPluginPathes()
    {
        return StreamEx.of(classes).map(cl -> getSourceDir(cl)).toArray( String[]::new);
    }

    public static String getSourceDir(Class<?> clazz)
    {
        String result = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        return result.substring(result.indexOf("/"));
    }

}
