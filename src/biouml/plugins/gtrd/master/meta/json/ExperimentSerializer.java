package biouml.plugins.gtrd.master.meta.json;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.Experiment;
import biouml.plugins.gtrd.ExternalReference;
import biouml.plugins.gtrd.master.sites.json.JacksonObjectSerializer;
import biouml.plugins.gtrd.master.sites.json.ListSerializer;

public abstract class ExperimentSerializer<T extends Experiment> extends JacksonObjectSerializer<T>
{
    private static final String FIELD_ID = "id";
    
    private static final String FIELD_CELL_ID = "cellId";
    private static final String FIELD_TREATMENT = "treatment";
    
    private static final String FIELD_PEAKS = "peaks";
    private static final String FIELD_ALIGNS = "aligns";
    private static final String FIELD_READS = "reads";
    
    private static final String FIELD_EXTERNAL_REFS = "externalRefs";
    

    private ListSerializer<ExternalReference> externalRefsSerializer = new ListSerializer<>( new ExternalRefSerializer() );

    
    protected abstract T createExperiment(String id);
    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        switch(name)
        {
            case FIELD_ID:
                result = createExperiment( parser.getText() );
                break;
            case FIELD_CELL_ID:
                result.setCell( new CellLine( parser.getText(), null, null, null ) );//stub
                break;
            case FIELD_TREATMENT:
                result.setTreatment( parser.getText() );
                break;
            case FIELD_PEAKS:
                result.setPeakId( readElementWithProps(parser) );
                break;
            case FIELD_ALIGNS:
                result.setAlignmentId( readElementWithProps( parser ) );
                break;
            case FIELD_READS:
                parseReads(parser);
                break;
            case FIELD_EXTERNAL_REFS:
                externalRefsSerializer.setReadTarget( result.getExternalRefs() );
                externalRefsSerializer.read( parser );
                break;
        }
    }

    private void parseReads(JsonParser parser) throws IOException
    {
        if(parser.getCurrentToken() != JsonToken.START_ARRAY)
            throw new JsonParseException( parser, "Expecting [" );
        
        while(true)
        {
            JsonToken t = parser.nextToken();
            if(t == JsonToken.END_ARRAY)
                break;
            if(t == null)
                throw new JsonParseException(parser, "Expecting ]");
            String readsId = readElementWithProps( parser );
            result.getReadsIds().add( readsId );
            
        }
    }

    private String readElementWithProps(JsonParser parser) throws IOException
    {
        if(parser.getCurrentToken() != JsonToken.START_OBJECT)
            throw new JsonParseException( parser, "Expecting {" );
        JsonToken t;
        String id = null;
        while((t = parser.nextToken()) == JsonToken.FIELD_NAME)
        {
            parser.nextToken();
            String propName = parser.getCurrentName();
            if(propName.equals( "id" ))
            {
                id = parser.getText();
                continue;
            }
            if(id == null)
                throw new JsonParseException(parser, "id should be the first field");
            result.setElementProperty( id, propName, parser.getText() );
        }
        
        if(id == null)
            throw new JsonParseException(parser, "Missing id");
        t = parser.currentToken();
        if(t != JsonToken.END_OBJECT)
            throw new JsonParseException(parser, "expecting }");
        return id;
    }

    @Override
    protected void writeFields(T exp, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeStringField( FIELD_ID, exp.getName() );
        
        writeFieldsAfterId(exp, jGenerator);
        
        jGenerator.writeStringField( FIELD_CELL_ID, exp.getCell().getName() );
        String treatment = exp.getTreatment();
        if(treatment == null)
            treatment = "";
        jGenerator.writeStringField( FIELD_TREATMENT, treatment );
        
        writeFieldsAfterTreatment(exp, jGenerator);
        
        if(exp.getPeakId() != null)
        {
            jGenerator.writeFieldName( FIELD_PEAKS );
            writeElementWithProps( exp.getPeakId(), exp, jGenerator );
        }
        
        jGenerator.writeFieldName( FIELD_ALIGNS );
        writeElementWithProps( exp.getAlignmentId(), exp, jGenerator );
        
        jGenerator.writeArrayFieldStart( FIELD_READS );
        for(String readsId : exp.getReadsIds())
        {
            writeElementWithProps( readsId, exp, jGenerator );
        }
        jGenerator.writeEndArray();
        
        jGenerator.writeFieldName( FIELD_EXTERNAL_REFS );
        externalRefsSerializer.write( exp.getExternalRefs(), jGenerator );
    }
    
    protected void writeFieldsAfterTreatment(T exp, JsonGenerator jGenerator) throws IOException
    {
    }
    protected void writeFieldsAfterId(T exp, JsonGenerator jGenerator) throws IOException
    {
    }
    
    protected void writeElementWithProps(String elementId, Experiment exp, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeStartObject();
        jGenerator.writeStringField( FIELD_ID, elementId );
        for(Map.Entry<String, String> e : exp.getElementProperties( elementId ).entrySet())
        {
            jGenerator.writeStringField( e.getKey(), e.getValue() );
        }
        jGenerator.writeEndObject();
    }
}
