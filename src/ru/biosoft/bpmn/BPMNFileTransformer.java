package ru.biosoft.bpmn;

import java.io.File;
import java.util.regex.Pattern;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.support.FileTextTransformer;

public class BPMNFileTransformer extends FileTextTransformer
{
    private static final Pattern EXTENSION_REGEXP = Pattern.compile( "\\.bpmn$", Pattern.CASE_INSENSITIVE );

    public BPMNFileTransformer()
    {
        super( EXTENSION_REGEXP );
    }

    @Override
    public Class<? extends BPMNDataElement> getOutputType()
    {
        return BPMNDataElement.class;
    }

    @Override
    public BPMNDataElement load(File input, String name, DataCollection<TextDataElement> origin) throws Exception
    {
        return new BPMNDataElement( name, origin, ApplicationUtils.readAsString( input ) );
    }
}
