package ru.biosoft.bsastats;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class EncodingSelector extends GenericComboBoxEditor
{
    @SuppressWarnings ( "serial" )
    public static final Map<String, Byte> ENCODING_TO_OFFSET = Collections.unmodifiableMap(new LinkedHashMap<String, Byte>()
    {{
        put("Sanger / Illumina 1.9", (byte)33);
        put("Illumina <1.3", (byte)59);
        put("Illumina 1.3", (byte)64);
        put("Illumina 1.5", (byte)64);
    }});

    @Override
    protected Object[] getAvailableValues()
    {
        return EncodingSelector.ENCODING_TO_OFFSET.keySet().toArray(new String[EncodingSelector.ENCODING_TO_OFFSET.size()]);
    }

    public static String detectEncoding(DataElementPath path) throws IOException
    {
        File file = path.getDataElement(FileDataElement.class).getFile();
        int nSites = 0;
        byte lowestByte = Byte.MAX_VALUE;
        try(BufferedReader br = ApplicationUtils.asciiReader( file ))
        {
            while(br.ready())
            {
                br.readLine();br.readLine();br.readLine();
                byte[] quality = br.readLine().trim().getBytes(StandardCharsets.ISO_8859_1);
                for(byte qualityByte: quality)
                {
                    if(qualityByte < lowestByte) lowestByte = qualityByte;
                }
                if(++nSites > 100 || lowestByte < 59) break;
            }
        }
        if(lowestByte < 33) throw new IllegalArgumentException("Invalid encoding");
        if(lowestByte < 59) return "Sanger / Illumina 1.9";
        if(lowestByte < 64) return "Illumina <1.3";
        if(lowestByte == 65) return "Illumina 1.3";
        if(lowestByte <= 126) return "Illumina 1.5";
        throw new IllegalArgumentException("Invalid encoding");
    }
}