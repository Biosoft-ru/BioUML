package ru.biosoft.access.support;

import java.io.BufferedReader;

import ru.biosoft.access.Entry;

/**
 * Parse the field value from database {@link Entry}.
 */
public class EntryParser
{
    static public String parseStringValue(Entry entry, String fieldTag) throws Exception
    {
        return parseStringValue(entry, fieldTag, System.getProperty("line.separator"));
    }

    static public String parseStringValue(Entry entry, String fieldTag, String lineSeparator) throws Exception
    {
        return parseStringValue(entry, fieldTag, lineSeparator, true);
    }

    /**
     * Parse the string field value from database {@link Entry}.
     *
     * @param entry          database {@link Entry} to be parsed.
     * @param fieldTag       field tag
     * @param lineSeparator  line separator to be inserted between original entry lines.
     * @param trim           indicates whether field prefix and spaces sould be truncated.
     */
    static public String parseStringValue(Entry entry, String fieldTag, String lineSeparator, boolean trim) throws Exception
    {
        if(entry == null)
            return null;

        BufferedReader reader = null;
        StringBuffer result;
        boolean find;

        try
        {
            reader = new BufferedReader(entry.getReader());
            String line;

            int prefixOffset = fieldTag.length();
            result = new StringBuffer();
            find = false;

            while( (line=reader.readLine()) != null)
            {
                if(! line.startsWith(fieldTag) )
                {
                    if(!find)
                        continue;

                    if( !line.startsWith(" ") && !line.startsWith(".."))
                        break;
                }

                if(trim)
                {
                    line = line.substring(prefixOffset);
                    line = line.trim();
                }

                if(find)
                    result.append(lineSeparator);
                result.append(line);

                find = true;
            }
        }
        finally
        {
            if (reader != null)
            {
                reader.close();
            }
        }

        return find ? result.toString() : null;
    }
}
