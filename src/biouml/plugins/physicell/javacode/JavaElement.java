package biouml.plugins.physicell.javacode;

import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptJobControl;
import ru.biosoft.access.core.ClassIcon;
import com.developmentontheedge.beans.annot.PropertyName;

@ClassIcon ( "resources/java.png" )
@PropertyName ( "script" )
public class JavaElement extends ScriptDataElement
{

    public JavaElement(DataCollection<?> parent, String name, String data)
    {
        super( name, parent, data );
    }

    @Override
    public String getContentType()
    {
        return "text/java";
    }

    @Override
    protected ScriptJobControl createJobControl(String content, ScriptEnvironment env, Map<String, Object> scope,
            Map<String, Object> outVars, boolean sessionContext)
    {
        return null;
    }

    @Override
    protected void handleException(ScriptEnvironment env, Throwable ex)
    {

        super.handleException( env, ex );

    }
}
