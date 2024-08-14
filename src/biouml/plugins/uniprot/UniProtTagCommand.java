package biouml.plugins.uniprot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import one.util.streamex.StreamEx;
import ru.biosoft.access.support.DividedLineTagCommand;
import ru.biosoft.util.TextUtil;
import biouml.standard.type.Base;
import biouml.standard.type.Protein;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

public class UniProtTagCommand extends DividedLineTagCommand<Protein>
{
    static final HashMap<String, Tag> TAGS = new HashMap<>();
    static
    {
        TAGS.put("ID", new Tag("Identification", Tag.ONCE, "Starts the entry"));
        TAGS.put("AC", new Tag("Accession number(s)", Tag.ONCE_OR_MORE));
        TAGS.put("DT", new Tag("Date", Tag.THRICE));
        TAGS.put("DE", new Tag("Description", Tag.ONCE_OR_MORE));
        TAGS.put("GN", new Tag("Gene name(s)", Tag.OPTIONAL));
        TAGS.put("OS", new Tag("Organism species", Tag.ONCE));
        TAGS.put("OG", new Tag("Organelle", Tag.OPTIONAL));
        TAGS.put("OC", new Tag("Organism classification", Tag.ONCE_OR_MORE));
        TAGS.put("OX", new Tag("Taxonomy cross-reference", Tag.ONCE));
        TAGS.put("RN", new Tag("Reference number", Tag.ONCE_OR_MORE));
        TAGS.put("RP", new Tag("Reference position", Tag.ONCE_OR_MORE));
        TAGS.put("RC", new Tag("Reference comment(s)", Tag.OPTIONAL));
        TAGS.put("RX", new Tag("Reference cross-reference(s)", Tag.OPTIONAL));
        TAGS.put("RG", new Tag("Reference group", Tag.ONCE_OR_MORE));
        TAGS.put("RA", new Tag("Reference authors", Tag.ONCE_OR_MORE));
        TAGS.put("RT", new Tag("Reference title", Tag.OPTIONAL));
        TAGS.put("RL", new Tag("Reference location", Tag.ONCE_OR_MORE));
        TAGS.put("CC", new Tag("Comments or notes", Tag.OPTIONAL));
        TAGS.put("DR", new Tag("Database cross-references", Tag.OPTIONAL));
        TAGS.put("KW", new Tag("Keywords", Tag.OPTIONAL));
        TAGS.put("FT", new Tag("Feature table data", Tag.OPTIONAL));
        TAGS.put("SQ", new Tag("Sequence header", Tag.ONCE));
        TAGS.put(Tag.EMPTY, new Tag("Sequence data", Tag.ONCE_OR_MORE));
        TAGS.put("//", new Tag("Termination line", Tag.ONCE, "Ends the entry"));

        for( Entry<String, Tag> entry : TAGS.entrySet() )
        {
            entry.getValue().setId(entry.getKey());
        }
    }

    private StringBuffer mtcLine = null;

    @Override
    public String getTaggedValue()
    {
        return tag;
    }

    @Override
    public String getTaggedValue(String value)
    {
        String ret = getTaggedValue();
        if( !ret.endsWith("\n") )
            ret += "\n";
        return ret;
    }

    public UniProtTagCommand(String tag, UniProtEntryTransformer transformer)
    {
        super(tag, transformer);
    }

    @Override
    public void addLine(String line)
    {
        if( null == mtcLine )
        {
            mtcLine = new StringBuffer();
        }
        else
        {
            mtcLine.append('\n');
        }

        mtcLine.append(line);
    }

    @Override
    public void endTag(String tag)
    {
        if( null == mtcLine )
            return;

        String lineString = mtcLine.toString();

        if( 0 == lineString.length() )
            return;

        Protein kernel = transformer.getProcessedObject();

        if( "ID".equals(tag) )
        {
            String strings[] = lineString.split("\\s");
            kernel.setTitle(strings[0]);
        }
        else if( "AC".equals(tag) )
        {
            setValue(kernel, "AC", splitTrim( lineString, ';' ), String[].class);
        }
        else if( "DT".equals(tag) )
        {
            String strings[] = lineString.split("\n");
            setValue(kernel, "DT", strings, String[].class);
        }
        else if( "DE".equals(tag) )
        {
            kernel.setDescription(lineString);
        }
        else if( "GN".equals(tag) )
        {
            String genes[] = lineString.split("and");
            for( String gene : genes )
            {
                String strings[] = TextUtil.split( gene, ';' );
                for( String string : strings )
                {
                    String keyAndValue[] = TextUtil.split( string, '=' );
                    if( "name".equalsIgnoreCase(keyAndValue[0]) )
                    {
                        kernel.setGene(keyAndValue[1]);
                        continue;
                    }
                    setValue(kernel, keyAndValue[0], splitTrim( keyAndValue[1], ',' ), String[].class);
                }
                break;
            }
        }
        else if( "OS".equals(tag) )
        {
            kernel.setSpecies(lineString);
        }
        else if( "OG".equals(tag) )
        {
            setValue(kernel, "OG", lineString, String.class);
        }
        else if( "OC".equals(tag) )
        {
            setValue(kernel, "OC", splitTrim(lineString, ';'), String[].class);
        }
        else if( "OX".equals(tag) )
        {
            String strings[] = TextUtil.split(lineString, ';');
            for( int i = 0; i < strings.length; i++ )
            {
                String keyAndValue[] = TextUtil.split( strings[i], '=' );
                strings[i] = keyAndValue[1];
            }
            setValue(kernel, "OX", strings, String[].class);
        }
        else if( "OH".equals(tag) )
        {
            DynamicPropertySetAsMap list = new DynamicPropertySetAsMap();
            String strings[] = TextUtil.split(lineString, '.');
            for( String string : strings )
            {
                String valueAndDescription[] = TextUtil.split( string, ';' );
                String keyAndValue[] = TextUtil.split( valueAndDescription[0], '=' );
                setValue(list, keyAndValue[1], valueAndDescription[1], String.class);
            }
            setValue(kernel, "OH", list, DynamicPropertySet.class);
        }
        else if( "RN".equals(tag) )
        {
            DynamicPropertySet dps = kernel.getAttributes();
            Object value = dps.getValue("References");
            if( null == value )
            {
                try
                {
                    dps.add(new DynamicProperty("References", DynamicPropertySet[].class));
                }
                catch( Exception e )
                {
                }
                value = new DynamicPropertySet[0];
                dps.setValue("References", value);
            }
            Object[] values = (Object[])value;
            DynamicPropertySetAsMap innerDps = new DynamicPropertySetAsMap();
            setValue(innerDps, "RN", lineString.substring(1, lineString.length() - 1), String.class);
            values = StreamEx.of(values).append( innerDps ).toArray();
            dps.setValue("References", values);
        }
        else if( "RP".equals(tag) )
        {
            setReferenceValue(kernel, "RP", lineString, String.class);
        }
        else if( "RC".equals(tag) )
        {
            DynamicPropertySetAsMap list = new DynamicPropertySetAsMap();
            String strings[] = TextUtil.split( lineString, ';' );
            for( String string : strings )
            {
                String keyAndValue[] = TextUtil.split( string, '=' );
                setValue(list, keyAndValue[0], keyAndValue[1], String.class);
            }
            setReferenceValue(kernel, "RC", list, DynamicPropertySetAsMap.class);
        }
        else if( "RX".equals(tag) )
        {
            DynamicPropertySetAsMap list = new DynamicPropertySetAsMap();
            String strings[] = TextUtil.split( lineString, ';' );
            for( String string : strings )
            {
                String keyAndValue[] = TextUtil.split( string, '=' );
                if( keyAndValue.length >= 2 )
                {
                    setValue(list, keyAndValue[0], keyAndValue[1], String.class);
                }
            }
            setReferenceValue(kernel, "RX", list, DynamicPropertySetAsMap.class);
        }
        else if( "RG".equals(tag) )
        {
            setReferenceValue(kernel, "RG", lineString, String.class);
        }
        else if( "RA".equals(tag) )
        {
            setReferenceValue(kernel, "RA", splitTrim( lineString, ',' ), String[].class);
        }
        else if( "RT".equals(tag) )
        {
            setReferenceValue(kernel, "RT", lineString.substring(1, lineString.length() - 1), String.class);
        }
        else if( "RL".equals(tag) )
        {
            setReferenceValue(kernel, "RL", lineString, String.class);
        }
        else if( "CC".equals(tag) )
        {
            lineString = lineString.substring(lineString.indexOf("-!-") + 3);
            String strings[] = lineString.split("-!-");
            for( int i = 0; i < strings.length; i++ )
            {
                strings[i] = strings[i].trim();
            }
            setValue(kernel, "CC", strings, String[].class);
        }
        else if( "DR".equals(tag) )
        {
            DynamicPropertySet dps = new DynamicPropertySetAsMap();
            String values[] = TextUtil.split( lineString, ';' );
            setValue(dps, "DATABASE_IDENTIFIER", values[0], String.class);
            setValue(dps, "PRIMARY_IDENTIFIER", values[1], String.class);
            setValue(dps, "SECONDARY_IDENTIFIER", values[2], String.class);
            try
            {
                setValue(dps, "TERTIARY_IDENTIFIER", values[3], String.class);
                setValue(dps, "QUATERNARY_IDENTIFIER", values[4], String.class);
            }
            catch( Exception e )
            {
            }
            setValue(kernel, "DR", dps, DynamicPropertySet.class);
        }
        else if( "PE".equals(tag) )
        {
            setValue(kernel, "PE", lineString, String.class);
        }
        else if( "KW".equals(tag) )
        {
            setValue(kernel, "KW", splitTrim( lineString, ';' ), String[].class);
        }
        else if( "FT".equals(tag) )
        {
            String strings[] = lineString.split("\\n");
            List<DynamicPropertySet> dpss = new ArrayList<>();
            for( String string2 : strings )
            {
                try
                {
                    DynamicPropertySet dps = new DynamicPropertySetAsMap();
                    setValue(dps, "Name", string2.substring(4, 11), String.class);
                    String from = string2.substring(13, 18);
                    Integer.parseInt(from.trim());
                    setValue(dps, "From", from, String.class);
                    String to = string2.substring(20, 25);
                    Integer.parseInt(to.trim());
                    setValue(dps, "To", to, String.class);
                    String description = null;
                    try
                    {
                        description = string2.substring(33);
                    }
                    catch( Exception e )
                    {
                        description = "";
                    }
                    setValue(dps, "Description", description, String.class);
                    dpss.add(dps);
                }
                catch( Exception e )
                {
                    DynamicPropertySet dps = dpss.get(dpss.size() - 1);
                    String string = String.valueOf(dps.getValue("Description"));
                    dps.setValue("Description", string + "\n" + string2);
                }
            }
            setValue(kernel, "FT", dpss.toArray(new DynamicPropertySet[dpss.size()]), DynamicPropertySet[].class);
        }
        else if( "SQ".equals(tag) )
        {
            DynamicPropertySet dps = new DynamicPropertySetAsMap();
            String values[] = TextUtil.split( lineString, ';' );
            String candidates[] = values[0].trim().split("\\s");
            for( int i = 1; i < candidates.length - 1; i++ )
            {
                String candidate = candidates[i].trim();
                if( 0 < candidate.length() )
                {
                    setValue(dps, "Length", candidate, String.class);
                    break;
                }
            }
            candidates = values[1].trim().split("\\s");
            for( int i = 0; i < candidates.length - 1; i++ )
            {
                String candidate = candidates[i].trim();
                if( 0 < candidate.length() )
                {
                    setValue(dps, "Weight", candidate, String.class);
                    break;
                }
            }
            candidates = values[2].trim().split("\\s");
            for( int i = 0; i < candidates.length - 1; i++ )
            {
                String candidate = candidates[i].trim();
                if( 0 < candidate.length() )
                {
                    setValue(dps, "CRC", candidate, String.class);
                    break;
                }
            }
            setValue(dps, "Sequence", values[3].replace("\n", " ").trim(), String.class);
            setValue(kernel, "SQ", dps, DynamicPropertySet.class);
        }
    }

    private static String[] splitTrim(String lineString, char sep)
    {
        return StreamEx.split( lineString, sep ).map( String::trim ).toArray( String[]::new );
    }

    private void setReferenceValue(Protein kernel, String string, Object lineString, Class<?> name)
    {
        DynamicPropertySet dps = kernel.getAttributes();
        Object value = dps.getValue("References");
        Object values[] = (Object[])value;
        DynamicPropertySetAsMap innerDps = (DynamicPropertySetAsMap)values[values.length - 1];
        setValue(innerDps, string, lineString, name);
    }

    private void setValue(Base kernel, String string, Object value, Class<?> klazz)
    {
        setValue(kernel.getAttributes(), string, value, klazz);
    }

    private void setValue(DynamicPropertySet dps, String string, Object value, Class<?> klazz)
    {
        try
        {
            dps.remove(string);
        }
        catch( Exception e )
        {
        }
        try
        {
            if( null == klazz )
                klazz = Object.class;
            dps.add(new DynamicProperty(string, klazz));
        }
        catch( Exception e )
        {
        }
        dps.setValue(string, value);
    }

    @Override
    public void startTag(String tag)
    {
        mtcLine = null;
    }
}
