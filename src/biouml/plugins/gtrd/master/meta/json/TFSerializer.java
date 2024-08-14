package biouml.plugins.gtrd.master.meta.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.meta.TF;
import biouml.plugins.gtrd.master.sites.json.JacksonObjectSerializer;
import biouml.plugins.gtrd.master.sites.json.ListSerializer;
import biouml.plugins.gtrd.master.sites.json.StringSerializer;
import biouml.standard.type.Species;

public class TFSerializer extends JacksonObjectSerializer<TF>
{
    private static final String FIELD_UNIPROT_ID = "uniprotId";
    private static final String FIELD_UNIPROT_NAME = "uniprotName";
    private static final String FIELD_UNIPROT_STATUS = "uniprotStatus";
    private static final String FIELD_UNIPROT_GENE_NAMES = "uniprotGeneNames";
    private static final String FIELD_UNIPROT_PROTEIN_NAMES = "uniprotProteinNames";
    private static final String FIELD_ORGANISM = "organism";
    private static final String FIELD_TF_CLASS_ID = "tfClassId";

    
    @Override
    public TF read(JsonParser parser) throws IOException
    {
        result = new TF();
        return super.read( parser );
    }
    
    private ListSerializer<String> strListSerializer = new ListSerializer<>( new StringSerializer() );
    
    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        switch(name)
        {
            case FIELD_UNIPROT_ID: result.uniprotId = parser.getText(); break;
            case FIELD_UNIPROT_NAME: result.uniprotName = parser.getText(); break;
            case FIELD_ORGANISM: result.organism = parser.getText(); break;
            case FIELD_UNIPROT_STATUS: result.uniprotStatus = parser.getText(); break;
            case FIELD_TF_CLASS_ID: result.tfClassId = parser.getText(); break;
            case FIELD_UNIPROT_GENE_NAMES:
                strListSerializer.setReadTarget( new ArrayList<>() );
                result.uniprotGeneNames = strListSerializer.read( parser );
                break;
            case FIELD_UNIPROT_PROTEIN_NAMES:
                strListSerializer.setReadTarget( new ArrayList<>() );
                result.uniprotProteinNames = strListSerializer.read( parser );
                break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }

    @Override
    protected void writeFields(TF tf, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeStringField( FIELD_UNIPROT_ID, tf.uniprotId );
        jGenerator.writeStringField( FIELD_UNIPROT_NAME, tf.uniprotName );

        jGenerator.writeFieldName( FIELD_UNIPROT_PROTEIN_NAMES );
        strListSerializer.write( tf.uniprotProteinNames, jGenerator );
        
        jGenerator.writeFieldName( FIELD_UNIPROT_GENE_NAMES );
        strListSerializer.write( tf.uniprotGeneNames, jGenerator );
        
        jGenerator.writeStringField( FIELD_ORGANISM, tf.organism );
        jGenerator.writeStringField( FIELD_UNIPROT_STATUS, tf.uniprotStatus );
        jGenerator.writeStringField( FIELD_TF_CLASS_ID, tf.tfClassId );
    }

}
