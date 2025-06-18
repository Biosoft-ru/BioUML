package biouml.plugins.wdl;


import java.io.File;
import java.util.regex.Pattern;

//import ru.biosoft.access.FileDataElement;
//import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.file.FileTextTransformer;
//import ru.biosoft.access.support.FileTextTransformer;

public class WDLScriptTransformer extends FileTextTransformer
{
    private static final Pattern EXTENSION_REGEXP = Pattern.compile( "\\.wdl$", Pattern.CASE_INSENSITIVE );
    
    public WDLScriptTransformer()
    {
        super(EXTENSION_REGEXP);
    }

    @Override
    public Class<? extends TextDataElement> getOutputType()
    {
        return WDLScript.class;
    }

    @Override
    public TextDataElement load(File input, String name, DataCollection<TextDataElement> origin) throws Exception
    {
        return new WDLScript(origin, name, input);
    }
    
    @Override
    public void save(File output, TextDataElement element) throws Exception
    {
        super.save( output, element );
    }
    
    @Override
    public FileDataElement transformOutput(TextDataElement output) throws Exception
    {
        FileDataElement res = super.transformOutput( output );
        ((WDLScript)output).setFile( res.getFile() );
        return res;
    }
}

