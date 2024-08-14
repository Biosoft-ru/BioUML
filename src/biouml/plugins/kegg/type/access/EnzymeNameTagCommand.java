package biouml.plugins.kegg.type.access;

import java.util.StringTokenizer;

import ru.biosoft.access.support.DividedLineTagCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.standard.type.Concept;

public class EnzymeNameTagCommand extends DividedLineTagCommand<Concept>
{
    private static final char SYNONYM_DELIM = ';';
    private static final String LN = System.getProperty("line.separator");

    private String completeName;
    private StringBuffer synonyms;
    private int lineNumber;

    public EnzymeNameTagCommand(String tag, TagEntryTransformer<? extends Concept> transformer)
    {
        super(tag, transformer);
    }

    @Override
    public void startTag(String tag)
    {
        lineNumber = 0;
        completeName = null;
        synonyms = new StringBuffer();
    }

    @Override
    public void addLine(String line)
    {
        if( line == null || line.length() == 0 )
            return;

        putValue(line);
        lineNumber++;
    }

    private void putValue(String value)
    {
        if( lineNumber == 0 )
        {
            if( value.charAt(value.length() - 1) == ';' )
                value = value.substring(0, value.length() - 1);
            completeName = value;
        }
        else
        {
            synonyms.append(value);
        }
    }

    @Override
    public void endTag(String tag)
    {
        //      if (current.length() > 0)
        //          putValue(current.toString());

        Concept concept = transformer.getProcessedObject();
        String title = concept.getName();
        if( title.startsWith("EC ") )
            title = title.substring(3);
        concept.setTitle(title);
        concept.setCompleteName(completeName);
        concept.setSynonyms(synonyms.toString());
    }

    @Override
    public String getTag()
    {
        return tag;
    }

    @Override
    public String getTaggedValue()
    {
        StringBuffer value = new StringBuffer();
        value.append(tag);
        Concept concept = transformer.getProcessedObject();
        value.append('\t').append(concept.getTitle()).append(LN);
        StringTokenizer st = new StringTokenizer(concept.getSynonyms(), String.valueOf(SYNONYM_DELIM));
        while( st.hasMoreTokens() )
        {
            value.append('\t').append(st.nextToken()).append(LN);
        }
        return value.toString();
    }

    @Override
    public String getTaggedValue(String value)
    {
        throw new UnsupportedOperationException();
    }
}
