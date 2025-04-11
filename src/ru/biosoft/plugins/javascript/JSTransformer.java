package ru.biosoft.plugins.javascript;

import java.io.File;
import java.util.regex.Pattern;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.file.FileTextTransformer;

public class JSTransformer extends FileTextTransformer
{
    private static final Pattern EXTENSION_REGEXP = Pattern.compile( "\\.js$", Pattern.CASE_INSENSITIVE );
    
    public JSTransformer()
    {
        super(EXTENSION_REGEXP);
    }

    @Override
    public Class getOutputType()
    {
        return JSElement.class;
    }

    @Override
    public TextDataElement load(File input, String name, DataCollection<TextDataElement> origin) throws Exception
    {
        return new JSElement(origin, name, ApplicationUtils.readAsString(input));
    }
}
