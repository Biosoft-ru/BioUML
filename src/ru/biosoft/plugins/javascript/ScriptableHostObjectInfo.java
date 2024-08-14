package ru.biosoft.plugins.javascript;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.mozilla.javascript.ScriptableObject;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import com.developmentontheedge.beans.annot.PropertyDescription;

/**
 * @author lan
 *
 */
public class ScriptableHostObjectInfo extends AbstractDataCollection<FunctionInfo>
{
    private ScriptableObject host;
    
    public ScriptableHostObjectInfo(String name, DataCollection<?> origin, Class<? extends ScriptableObject> hostClass) throws Exception
    {
        super(name, origin, null);
        host = hostClass.newInstance();
        try
        {
            getInfo().setDescription(hostClass.getAnnotation(PropertyDescription.class).value());
        }
        catch( Exception e )
        {
        }
    }

    @Override
    public boolean contains(String name)
    {
        return host.has(name, null);
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        List<String> result = new ArrayList<>();
        for(Object id: host.getAllIds()) result.add(id.toString());
        return result;
    }

    @Override
    protected FunctionInfo doGet(String name) throws Exception
    {
        Object object = host.get(name, null);
        if(object == null) return null;
        FunctionInfo functionInfo = new FunctionInfo(name, this);
        functionInfo.setDescription(object.toString());
        return functionInfo;
    }

    public Class<?> getObjectClass()
    {
        return host.getClass();
    }
}
