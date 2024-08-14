package ru.biosoft.plugins.jri;

import java.io.File;
import java.util.regex.Pattern;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.support.FileTextTransformer;

import com.developmentontheedge.application.ApplicationUtils;

public class RElementTransformer extends FileTextTransformer
{
    private static final Pattern EXTENSION_REGEXP = Pattern.compile( "\\.R$", Pattern.CASE_INSENSITIVE );
    
    public RElementTransformer()
    {
        super(EXTENSION_REGEXP);
    }

    @Override
    public Class getOutputType()
    {
        return RElement.class;
    }

    @Override
    public TextDataElement load(File input, String name, DataCollection<TextDataElement> origin) throws Exception
    {
        return new RElement(origin, name, ApplicationUtils.readAsString(input));
    }
}
