package biouml.plugins.nextflow;


import java.io.File;
import java.util.regex.Pattern;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.support.FileTextTransformer;

public class NextflowScriptTransformer extends FileTextTransformer
{
    private static final Pattern EXTENSION_REGEXP = Pattern.compile( "\\.nf$", Pattern.CASE_INSENSITIVE );
    
    public NextflowScriptTransformer()
    {
        super(EXTENSION_REGEXP);
    }

    @Override
    public Class<? extends TextDataElement> getOutputType()
    {
        return NextflowScript.class;
    }

    @Override
    public TextDataElement load(File input, String name, DataCollection<TextDataElement> origin) throws Exception
    {
        return new NextflowScript(origin, name, input);
    }
    
    @Override
    public void save(File output, TextDataElement element) throws Exception
    {
        super.save( output, element );
        ((NextflowScript)element).setFile( output );
        
    }
    
    @Override
    public int getOutputPriority(String name)
    {
        if(name.toLowerCase().endsWith( ".nf" ) || name.toLowerCase().endsWith( ".nextflow" ))
            return 3;
        return 0;
    }
}

