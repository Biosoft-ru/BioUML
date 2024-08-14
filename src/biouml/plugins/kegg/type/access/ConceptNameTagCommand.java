package biouml.plugins.kegg.type.access;

import java.util.StringTokenizer;

import ru.biosoft.access.support.DividedLineTagCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.standard.type.Concept;

public class ConceptNameTagCommand extends DividedLineTagCommand<Concept>
{
    private static final char SYNONYM_DELIM = ';';
    private static final String LN = System.getProperty("line.separator");

    private String completeName;
    private String title;
    private StringBuffer synonyms;
    private int lineNumber;

    public ConceptNameTagCommand(String tag, TagEntryTransformer<? extends Concept> transformer)
    {
        super(tag, transformer);
    }

    @Override
    public void startTag(String tag)
    {
        lineNumber = 0;
        title = null;
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
        else if( lineNumber == 1 )
        {
            if( value.charAt(value.length() - 1) == ';' )
                value = value.substring(0, value.length() - 1);
            title = value;
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
        if( title == null )
            title = completeName;
        title = title.replaceAll("alpha-", "<alpha/>-");
        title = title.replaceAll("beta-", "<beta/>-");
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
