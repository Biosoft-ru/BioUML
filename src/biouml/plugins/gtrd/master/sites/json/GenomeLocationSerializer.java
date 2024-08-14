package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.master.sites.GenomeLocation;
import biouml.plugins.gtrd.master.utils.StringPool;

public abstract class GenomeLocationSerializer<T extends GenomeLocation> extends JacksonObjectSerializer<T>
{
    public static final String FIELD_ID = "id";
    public static final String FIELD_POS = "pos";
    
    @Override
    protected void writeFields(T o, JsonGenerator jGenerator) throws IOException
    {
        writePos( o, jGenerator );
    }

    //big bed intervals are zero-based half open
    public static void writePos(GenomeLocation o, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeStringField( FIELD_ID, o.getStableId() );//should be the first field
        jGenerator.writeStringField( "pos", o.getChr() + ":" + ( o.getFrom() - 1 ) + "-" + o.getTo() );
    }

    public static void parsePos(GenomeLocation loc, JsonParser parser) throws IOException
    {
        String text = parser.getValueAsString();
        int colonIdx = text.indexOf( ':' );
        if(colonIdx == -1)
            throw new JsonParseException( parser, "wrong genome position: " + text );
        loc.setChr( StringPool.get( text.substring( 0, colonIdx ) ) );
        
        int dashIndex = text.indexOf( '-', colonIdx + 1 );
        if(dashIndex == -1)
            throw new JsonParseException( parser, "wrong genome position: " + text );
        loc.setFrom( 1 + Integer.parseInt( text.substring( colonIdx+1, dashIndex ) ) );
        
        loc.setTo( Integer.parseInt( text.substring( dashIndex + 1 ) ) );
    }
}
