package ru.biosoft.access.support;

import java.io.File;
import java.util.regex.Pattern;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.generic.PriorityTransformer;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * @author anna
 * Transformer for text files
 */
public class FileTextTransformer extends AbstractFileTransformer<TextDataElement> implements PriorityTransformer
{
    private static final Pattern EXTENSION_REGEXP = Pattern.compile( "\\.txt$", Pattern.CASE_INSENSITIVE );
    private final Pattern extensionRegexp;

    public FileTextTransformer()
    {
        this(EXTENSION_REGEXP);
    }

    public FileTextTransformer(Pattern extensionRegexp)
    {
        this.extensionRegexp = extensionRegexp;
    }

    @Override
    public Class<? extends TextDataElement> getOutputType()
    {
        return TextDataElement.class;
    }

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement de)
    {
        return de.getClass() == getOutputType() ? 1 : 0;
    }

    @Override
    public TextDataElement load(File input, String name, DataCollection<TextDataElement> origin) throws Exception
    {
        return new TextDataElement(name, origin, ApplicationUtils.readAsString(input));
    }

    @Override
    public void save(File output, TextDataElement element) throws Exception
    {
        ApplicationUtils.writeString(output, element.getContent());
    }

    @Override
    public int getOutputPriority(String name)
    {
        if(extensionRegexp.matcher( name ).find())
            return 3;
        return 0;
    }
}
