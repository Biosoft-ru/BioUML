package biouml.plugins.nextflow;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptJobControl;

@ClassIcon("resources/wdl-script.gif")
@PropertyName("Nextflow-script")
@PropertyDescription("Nextflow script")
public class NextflowScript extends ScriptDataElement
{
    private File file;

    public NextflowScript(DataCollection<?> origin, String name,  String content)
    {
        super( name, origin, content );
    }
    
    public NextflowScript(DataCollection<?> origin, String name,  File file) throws IOException
    {
        this( origin, name, ApplicationUtils.readAsString( file ) );
        setFile(file);
    }
    
    
    public File getFile()
    {
        return file;
    }
    public void setFile(File file)
    {
        this.file = file;
    }

    @Override
    protected ScriptJobControl createJobControl(String content, ScriptEnvironment env, Map<String, Object> scope,
            Map<String, Object> outVars, boolean sessionContext)
    {
        return new NextflowJobControl(file, env) ;
    }

}
