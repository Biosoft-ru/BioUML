package biouml.plugins.obo;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Module;
import biouml.standard.type.Concept;
import biouml.standard.type.DatabaseReference;
import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.Entry;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.access.core.DataElementReadException;

public class OboTransformer extends AbstractTransformer<Entry, Concept>
{
    public static final String MATH_START = "<math";
    public static final String MATH_END = "</math>";
    public static final String MATH_PROPERTY_NAME = "math";
    public static final String PARENT_PROPERTY = "is_a";

    private List<DatabaseReference> xrefs;

    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    @Override
    synchronized public Concept transformInput(Entry entry) throws Exception
    {
        Concept concept = null;
        if( entry == null || entry.getData() == null )
            return concept;
        xrefs = new ArrayList<>();
        for( String line : StreamEx.split(entry.getData(), '\n').map( String::trim ) )
        {
            if( line.equals("[Term]") || line.equals("[Typedef]") || line.equals("[Instance]") )
            {
                if( line.equals("[Term]") )
                {
                    concept = new Concept( (DataCollection<?>)getTransformedCollection().get( "terms" ), entry.getName() );
                }
                else if( line.equals("[Typedef]") )
                {
                    concept = new Concept( (DataCollection<?>)getTransformedCollection().get( "typedef" ), entry.getName() );
                }
                else if( line.equals("[Instance]") )
                {
                    concept = new Concept( (DataCollection<?>)getTransformedCollection().get( "instances" ), entry.getName() );
                }
                else
                    continue;
                DynamicPropertySet dps = concept.getAttributes();
                dps.add(new DynamicProperty("Type", String.class));
                dps.setValue("Type", line.substring(1, line.length() - 1));
            }
            else if( line.indexOf( ':' ) != -1 && concept != null )
            {
                int index = line.indexOf(':');
                parseTag(line.substring(0, index), line.substring(index + 1, line.length()), concept);
            }
        }
        if( concept != null && xrefs.size() != 0 )
            concept.setDatabaseReferences(xrefs.toArray(new DatabaseReference[xrefs.size()]));
        return concept;
    }

    @Override
    public Entry transformOutput(Concept input) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    protected Class<? extends Concept> outputType;
    @Override
    public Class<? extends Concept> getOutputType()
    {
        if( outputType == null )
        {
            Properties properties = getTransformedCollection().getInfo().getProperties();
            String name = properties.getProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY);
            if( name != null )
            {
                try
                {
                    String pluginNames = properties.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
                    outputType = ClassLoading.loadSubClass( name, pluginNames, Concept.class );
                }
                catch( LoggedClassNotFoundException e )
                {
                    throw new DataElementReadException(e, getTransformedCollection(), DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY);
                }
            }
            else
            {
                throw new DataElementReadException(getTransformedCollection(), DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY);
            }
        }

        return outputType;
    }

    private void parseTag(String tag, String value, Concept concept) throws Exception
    {
        DynamicPropertySet dps = concept.getAttributes();
        int end = value.indexOf('!');
        String string;
        if( end != -1 )
        {
            string = value.substring(0, end).trim();
            String comments = dps.getValueAsString(tag + ":comment");
            if( comments == null )
            {
                dps.add(new DynamicProperty(tag + ":comment", String.class));
                dps.setValue(tag + ":comment", value.substring(end + 1).trim());
            }
            else
            {
                dps.setValue(tag + ":comment", comments + "; " + value.substring(end + 1).trim());
            }
        }
        else
        {
            string = value.trim();
        }

        if( string.matches("\".*\" *\\[.*\\]") )
        {
            int start = string.lastIndexOf('[');
            String tmp = string.substring(0, start);
            end = string.lastIndexOf(']');
            if( end > start )
            {
                String refs = string.substring(start + 1, end);
                if( refs != null && !refs.trim().equals("") )
                {
                    String fieldRefs = dps.getValueAsString(tag + ":xref");
                    if( fieldRefs == null )
                    {
                        dps.add(new DynamicProperty(tag + ":xref", String.class));
                        dps.setValue(tag + ":xref", refs);
                    }
                    else
                    {
                        dps.setValue(tag + ":xref", fieldRefs + "; " + refs);
                    }
                }
            }
            string = string.substring(tmp.indexOf('\"') + 1, tmp.lastIndexOf('\"'));
        }

        string = ignoreTrailingModifiers(string);

        if( tag.equals("id") )
        {
            //do nothing. already parsed
        }
        else if( tag.equals("comment") )
        {
            string = replaceEscapeCharacters(string);
            concept.setComment(string);
        }
        else if( tag.equals("def") )
        {
            string = extractMath(string, dps);
            string = string.replaceAll("\\\\", "");
            concept.setDescription(string);
        }
        else if( tag.equals("name") )
        {
            string = string.replaceAll("\\\\", "");
            concept.setCompleteName(string);
            concept.setTitle(string);
        }
        else if( tag.equals("xref") || tag.equals("xref_analog") || tag.equals("xref_unk") )
        {
            xrefs.add(parseXref(string));
        }
        else if( tag.equals("synonym") || tag.equals("exact_synonym") || tag.equals("narrow_synonym") || tag.equals("broad_synonym") )
        {
            string = replaceEscapeCharacters(string);
            String oldSynonyms = concept.getSynonyms();
            if( oldSynonyms == null )
            {
                concept.setSynonyms(string);
            }
            else
            {
                concept.setSynonyms(oldSynonyms + ", " + string);
            }
        }
        else
        {
            string = replaceEscapeCharacters(string);
            if( dps.getProperty(tag) == null )
            {
                dps.add(new DynamicProperty(tag, String.class));
                dps.setValue(tag, string);
            }
            else
            {
                Object obj = dps.getValue(tag);
                if( obj.getClass().isArray() )
                {
                    String[] str = (String[])obj;
                    String[] newString = StreamEx.of(str).append(string).toArray( String[]::new );
                    dps.setValue(tag, newString);
                }
                else
                {
                    String oldValue = (String)obj;
                    String[] newString = new String[2];
                    newString[0] = oldValue;
                    newString[1] = string;
                    dps.remove(tag);
                    dps.add(new DynamicProperty(tag, String[].class));
                    dps.setValue(tag, newString);
                }
            }
        }
    }

    private String ignoreTrailingModifiers(String string)
    {
        if( string.matches(".*\\{.*\\}") )
        {
            String tmp = string.substring(0, string.lastIndexOf("{"));
            return tmp.trim();
        }
        return string;
    }

    private DatabaseReference parseXref(String string)
    {
        DatabaseReference dr = new DatabaseReference();
        String value = string;
        int spacePos = string.indexOf(' ');
        if( spacePos != -1 )
        {
            value = string.substring(0, spacePos);
            String comment = string.substring(spacePos);
            if( comment.contains( "\"" ) )
            {
                int beginIndex = comment.indexOf( '\"' ) + 1;
                int endIndex = comment.lastIndexOf( '\"' );
                if( endIndex >= beginIndex )
                    comment = comment.substring( beginIndex, endIndex );
            }
            dr.setComment(comment);
        }
        int delimiterPos = value.indexOf(':');
        if( delimiterPos != -1 )
        {
            dr.setDatabaseName(value.substring(0, delimiterPos));
            dr.setId(value.substring(delimiterPos + 1, value.length()));
        }
        return dr;
    }

    private String replaceEscapeCharacters(String string)
    {
        String result = string;
        result = result.replaceAll("\\\\,", ",");
        result = result.replaceAll("\\\\:", ":");
        result = result.replaceAll("\\\\W", " ");
        result = result.replaceAll("\\\\\"", "\"");
        result = result.replaceAll("\\\\n", "\n");
        result = result.replaceAll("\\\\t", "\t");
        return result;
    }

    @Override
    public DataCollection getTransformedCollection()
    {
        try
        {
            DataCollection dc = (DataCollection)transformedCollection.get(Module.DATA);
            return dc;
        }
        catch( Exception e )
        {
            return transformedCollection;
        }
    }

    private String extractMath(String str, DynamicPropertySet dps) throws Exception
    {
        String result = str;
        int mathStartPos = str.indexOf(MATH_START);
        int mathEndPos = str.indexOf(MATH_END) + 7;
        if( ( mathStartPos != -1 ) && ( mathEndPos != -1 ) && ( mathStartPos < mathEndPos ) )
        {
            String math = str.substring(mathStartPos, mathEndPos);
            math = replaceEscapeCharacters(math);
            dps.add(new DynamicProperty(MATH_PROPERTY_NAME, String.class, math));
            result = str.substring(0, mathStartPos) + str.substring(mathEndPos);
        }
        return result;
    }
}
