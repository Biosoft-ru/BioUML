package biouml.plugins.jupyter;

import java.io.File;
import java.util.regex.Pattern;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.jupyter.access.IPythonElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.file.FileTextTransformer;

public class IPythonFileTransformer extends FileTextTransformer
{
    private static final Pattern EXTENSION_REGEXP = Pattern.compile( "\\.ipynb$", Pattern.CASE_INSENSITIVE );

    public IPythonFileTransformer()
    {
        super( EXTENSION_REGEXP );
    }

    @Override
    public Class<? extends IPythonElement> getOutputType()
    {
        return IPythonElement.class;
    }

    @Override
    public IPythonElement load(File input, String name, DataCollection<TextDataElement> origin) throws Exception
    {
        return new IPythonElement( name, origin, ApplicationUtils.readAsString( input ), input.getAbsolutePath() );
    }
}
