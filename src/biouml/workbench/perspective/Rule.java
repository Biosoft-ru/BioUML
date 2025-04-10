package biouml.workbench.perspective;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.IConfigurationElement;

import com.eclipsesource.json.JsonObject;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.util.TextUtil2;

public class Rule
{
    private final boolean allow;
    private final String template;
    private final Pattern regExp;
    
    public Rule(boolean allow, String template)
    {
        this.allow = allow;
        this.template = template;
        this.regExp = Pattern.compile(TextUtil2.wildcardToRegex(template));
    }
    
    protected Rule(IConfigurationElement element)
    {
        allow = checkAllowed( element );
        template = element.getAttribute("id");
        regExp = Pattern.compile(TextUtil2.wildcardToRegex(template));
    }
    
    private boolean checkAllowed(IConfigurationElement element)
    {
        if( element.getName().equals( "allowWithClass" ) )
        {
            try
            {
                ClassLoading.loadClass( element.getAttribute( "class" ) );
                return true;
            }
            catch( LoggedClassNotFoundException e )
            {
                return false;
            }
        }
        return element.getName().equals( "allow" );
    }

    public boolean isAllow()
    {
        return allow;
    }
    
    public boolean isMatched(String id)
    {
        return regExp.matcher(id).matches();
    }

    public JsonObject toJSON()
    {
        return new JsonObject().add("type", allow?"allow":"deny").add("template", template);
    }
}