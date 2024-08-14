
package ru.biosoft.access.support;

import java.io.File;
import java.util.regex.Pattern;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.HtmlDataElement;
import ru.biosoft.access.TextDataElement;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * @author anna
 * Transformer for html files
 */
public class FileHtmlTransformer extends FileTextTransformer
{
    private static final Pattern EXTENSION_REGEXP = Pattern.compile( "\\.html?$", Pattern.CASE_INSENSITIVE );
    
    public FileHtmlTransformer()
    {
        super(EXTENSION_REGEXP);
    }
    
    @Override
    public Class getOutputType()
    {
        return HtmlDataElement.class;
    }

    @Override
    public HtmlDataElement load(File input, String name, DataCollection<TextDataElement> origin) throws Exception
    {
        return new HtmlDataElement(name, origin, ApplicationUtils.readAsString(input));
    }
}
