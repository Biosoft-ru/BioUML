package biouml.plugins.physicell.javacode;

import java.io.File;
import java.util.regex.Pattern;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.support.FileTextTransformer;

public class JavaTransformer extends FileTextTransformer
{
    private static final Pattern EXTENSION_REGEXP = Pattern.compile( "\\.java$", Pattern.CASE_INSENSITIVE );
    
    public JavaTransformer()
    {
        super(EXTENSION_REGEXP);
    }

    @Override
    public Class getOutputType()
    {
        return JavaElement.class;
    }

    @Override
    public TextDataElement load(File input, String name, DataCollection<TextDataElement> origin) throws Exception
    {
        return new JavaElement(origin, name, ApplicationUtils.readAsString(input));
    }
}
