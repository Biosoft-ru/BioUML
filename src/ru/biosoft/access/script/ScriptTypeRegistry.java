package ru.biosoft.access.script;

import java.lang.reflect.Constructor;
import java.util.Map;

import javax.swing.text.StyledDocument;

import org.eclipse.core.runtime.IConfigurationElement;

import com.Ostermiller.Syntax.HighlightedDocument;
import com.Ostermiller.Syntax.Lexer.Lexer;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementCreateException;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.exception.ProductNotAvailableException;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.util.ExtensionRegistrySupport;

/**
 * @author lan
 *
 */
public class ScriptTypeRegistry extends ExtensionRegistrySupport<ScriptTypeRegistry.ScriptType>
{
    private static ScriptTypeRegistry instance = new ScriptTypeRegistry();
    
    public static class ScriptType implements Comparable<ScriptType>
    {
        private final String type;
        private final String title;
        private final String product;
        private final Class<? extends ScriptDataElement> clazz;
        private final Class<? extends Lexer> lexer;

        public ScriptType(String type, String title, String product, Class<? extends ScriptDataElement> clazz, Class<? extends Lexer> lexer)
        {
            this.type = type;
            this.title = title;
            this.product = product;
            this.clazz = clazz;
            this.lexer = lexer;
        }

        public String getType()
        {
            return type;
        }

        public String getTitle()
        {
            return title;
        }

        public Class<? extends ScriptDataElement> getScriptClass()
        {
            return clazz;
        }

        @Override
        public String toString()
        {
            return title;
        }

        public ScriptDataElement createScript(DataElementPath path, String content)
        {
            try
            {
                SecurityManager.checkProductAvailable(product);
                Constructor<? extends ScriptDataElement> constructor = clazz.getConstructor(DataCollection.class, String.class, String.class);
                if(path == null)
                    return constructor.newInstance(null, "", content);
                else
                    return constructor.newInstance(path.getParentPath().getDataElement(DataCollection.class), path.getName(), content);
            }
            catch( Throwable t )
            {
                throw new DataElementCreateException(t, path, clazz);
            }
        }
        
        public String execute(String script, ScriptEnvironment env)
        {
            return createScript(null, script).execute(script, env, true);
        }

        @Override
        public int compareTo(ScriptType o)
        {
            return title.compareTo(o.title);
        }

        public StyledDocument getHighlightedDocument()
        {
            HighlightedDocument document = new HighlightedDocument();
            document.setHighlightStyle(lexer);
            return document;
        }
        
    }
    
    private ScriptTypeRegistry()
    {
        super("ru.biosoft.access.scriptType", "type");
    }
    
    @Override
    protected ScriptType loadElement(IConfigurationElement element, String type) throws Exception
    {
        String title = getStringAttribute(element, "title");
        Class<? extends ScriptDataElement> clazz = getClassAttribute(element, "class", ScriptDataElement.class);
        String product = element.getAttribute("product");
        String className = getStringAttribute(element, "lexer");
        Class<? extends Lexer> lexer;
        try
        {
            lexer = ClassLoading.loadSubClass( className, ClassLoading.getPluginForClass( ScriptTypeRegistry.class ) + ";" + element.getNamespaceIdentifier(), Lexer.class );
        }
        catch( Exception e )
        {
            throw new ParameterNotAcceptableException(e, "lexer", className);
        }
        return new ScriptType(type, title, product, clazz, lexer);
    }

    public static Map<String, ScriptType> getScriptTypes()
    {
        return instance.stream().filter( type -> SecurityManager.isProductAvailable( type.product ) )
                .toMap( type -> type.type, type -> type );
    }
    
    public static ScriptType getScriptType(ScriptDataElement script)
    {
        return instance.stream().findAny( type -> type.clazz.equals(script.getClass()) ).orElse( null );
    }
    
    public static String execute(String scriptType, String content, ScriptEnvironment env, boolean sessionContext)
    {
        return createScript(scriptType, null, content).execute(content, env, sessionContext);
    }
    
    public static ScriptDataElement createScript(String scriptType, DataElementPath path, String content)
    {
        ScriptType type = instance.getExtension(scriptType);
        if(type != null)
            return type.createScript(path, content);
        throw new DataElementCreateException(new ParameterNotAcceptableException("Script type", scriptType), path, ScriptDataElement.class);
    }

    protected static void checkProduct(ScriptDataElement script) throws ProductNotAvailableException
    {
        for(ScriptType type: instance)
        {
            if(type.clazz.equals(script.getClass()))
                SecurityManager.checkProductAvailable(type.product);
        }
    }
}
