package ru.biosoft.plugins.javascript;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

import ru.biosoft.access.BiosoftSecurityManager;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DerivedDataCollection;
import ru.biosoft.access.FileCollection;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.util.ExProperties;

/**
 * @author lan
 *
 */
public class ScriptCollection extends DerivedDataCollection<ru.biosoft.access.core.DataElement, FileDataElement>
{
    public static final String SCRIPT_PROPERTY = "script";
    Map<String, ScriptInfo> elementInfos = new HashMap<>();
    
    private static class ScriptInfo
    {
        private final String name;
        private final Class<? extends DataElement> targetClass;
        private String iconId;
        private final Script script;
        
        public ScriptInfo(File file) throws Exception
        {
            name = file.getName();
            Properties properties = new ExProperties(file);
            String targetClassName = properties.getProperty( DataCollectionConfigConstants.CLASS_PROPERTY );
            if(targetClassName == null) throw new IllegalArgumentException("No class found in "+file);
            targetClass = ClassLoading.loadSubClass( targetClassName, DataElement.class );
            iconId = properties.getProperty(DataCollectionConfigConstants.NODE_IMAGE);
            if(iconId == null) iconId = IconFactory.getClassIconId(targetClass);
            String scriptString = properties.getProperty(SCRIPT_PROPERTY);
            if(scriptString == null) throw new IllegalArgumentException("No script found in "+file);
            Context context = JScriptContext.getContext();
            script = context.compileString(scriptString, "", 0, null);
        }

        /**
         * @return the name of the element
         */
        public String getName()
        {
            return name;
        }
        /**
         * @return class of the element (script must create element of given class)
         */
        public Class<? extends DataElement> getTargetClass()
        {
            return targetClass;
        }
        /**
         * @return the compiled script
         */
        public Script getScript()
        {
            return script;
        }

        /**
         * @return the iconId
         */
        public String getIconId()
        {
            return iconId;
        }
    }
    
    private synchronized @Nonnull ScriptInfo getScriptInfo(String name)
    {
        ScriptInfo result = elementInfos.get(name);
        if(result == null)
        {
            FileDataElement fde;
            try
            {
                fde = getPrimaryCollection().get(name);
            }
            catch( Exception e )
            {
                throw new IllegalArgumentException(getCompletePath()+": Cannot find file element for "+name);
            }
            try
            {
                result = new ScriptInfo(fde.getFile());
            }
            catch( Exception e )
            {
                throw new IllegalArgumentException(getCompletePath()+": "+e.getMessage(), e);
            }
            elementInfos.put(name, result);
        }
        return result;
    }
    
    public ScriptCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties.getProperty(DataCollectionConfigConstants.NAME_PROPERTY), createPrimaryCollection(properties), properties);
    }

    /**
     * Creates primary collection and returns it
     * @param properties passed to this collection
     * @return
     * @throws IOException if path is not accessible
     */
    private static DataCollection<FileDataElement> createPrimaryCollection(Properties properties) throws IOException
    {
        Properties primaryProperties = new Properties();
        primaryProperties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, properties.getProperty(DataCollectionConfigConstants.NAME_PROPERTY));
        primaryProperties.setProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, properties.getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY));
        primaryProperties.setProperty(FileCollection.FILE_FILTER, "");
        return new FileCollection(null, primaryProperties);
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return ru.biosoft.access.core.DataElement.class;
    }

    @Override
    public DataElement doGet(String name) throws Exception
    {
        ScriptInfo scriptInfo = getScriptInfo(name);
        Script script = scriptInfo.getScript();
        ScriptableObject scope = JScriptContext.getScope();
        scope.put("path", scope, DataElementPath.create(this, name));
        
        AtomicReference<Object> ref = new AtomicReference<>( );
        BiosoftSecurityManager.runInSandbox( () -> {
            Object scriptResult = script.exec(JScriptContext.getContext(), scope);
            ref.set( scriptResult );
        } );
        Object scriptResult = ref.get();
        
        if(scriptResult instanceof NativeJavaObject)
            scriptResult = ((NativeJavaObject)scriptResult).unwrap();
        if(scriptInfo.getTargetClass().isInstance(scriptResult))
            return (DataElement)scriptResult;
        throw new IllegalArgumentException(DataElementPath.create(this, name) + ": script result is "
                + ( scriptResult == null ? null : scriptResult.getClass() ) + " (expected: " + scriptInfo.getTargetClass() + ")");
    }

    @Override
    protected void doPut(DataElement element, boolean isNew) throws Exception
    {
        elementInfos.remove(element.getName());
        super.doPut(element, isNew);
    }

    @Override
    protected void doRemove(String name) throws Exception
    {
        elementInfos.remove(name);
        super.doRemove(name);
    }

    @Override
    public void release(String name)
    {
        elementInfos.remove(name);
        super.release(name);
    }

    @Override
    public @Nonnull DataElementDescriptor getDescriptor(String name)
    {
        ScriptInfo scriptInfo = getScriptInfo(name);
        return new DataElementDescriptor(scriptInfo.getTargetClass(), scriptInfo.getIconId(), true);
    }
}
