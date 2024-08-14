package biouml.plugins.gtrd.master.meta.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.json.JacksonObjectSerializer;
import biouml.plugins.gtrd.master.sites.json.ListSerializer;
import biouml.plugins.gtrd.master.sites.json.StringSerializer;
import biouml.standard.type.Species;

public class CellSerializer extends JacksonObjectSerializer<CellLine>
{
    private static final String FIELD_UBERON_ID = "uberonId";
    private static final String FIELD_EXP_FACTOR_ONTOLOGY_ID = "expFactorOntologyId";
    private static final String FIELD_CELLOSAURUS_ID = "cellosaurusId";
    private static final String FIELD_CELL_ONTOLGY_ID = "cellOntolgyId";
    private static final String FIELD_BRENDA_ID = "brendaId";
    private static final String FIELD_CELL_TYPE_IDS = "cellTypeIds";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_SOURCE_ID = "sourceId";
    private static final String FIELD_ORGANISM = "organism";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_ID = "id";

    
    @Override
    public CellLine read(JsonParser parser) throws IOException
    {
        result = null;
        return super.read( parser );
    }
    
    private String id, title;
    private Species organism;
    
    private ListSerializer<String> strListSerializer = new ListSerializer<>( new StringSerializer() );
    
    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        switch(name)
        {
            case FIELD_ID:
                id = parser.getText();
                break;
            case FIELD_TITLE:
                title = parser.getText();
                break;
            case FIELD_ORGANISM:
            {
                String latinName = parser.getText();
                organism = Species.getSpecies( latinName );
                if(organism == null)//when no repository
                    organism = new Species( null, latinName );
                result = new CellLine( id, title, organism, null );
            }
                break;
            case FIELD_SOURCE_ID:
                result.setSourceId( parser.getText() );
                break;
            case FIELD_SOURCE:
                result.setSource( parser.getText() );
                break;
            case FIELD_CELL_TYPE_IDS:
                strListSerializer.setReadTarget( new ArrayList<>() );
                List<String> idList = strListSerializer.read( parser );
                result.setCellTypeId( idList.toArray( new String[0] ) );
                break;
            case FIELD_BRENDA_ID:
                result.setBrendaId( parser.getText() );
                break;
            case FIELD_CELL_ONTOLGY_ID:
                result.setCellOntologyId( parser.getText() );
                break;
            case FIELD_CELLOSAURUS_ID:
                result.setCellosaurusId( parser.getText() );
                break;
            case FIELD_EXP_FACTOR_ONTOLOGY_ID:
                result.setExpFactorOntologyId( parser.getText() );
                break;
            case FIELD_UBERON_ID:
                result.setUberonId( parser.getText() );
                break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }

    @Override
    protected void writeFields(CellLine cell, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeStringField( FIELD_ID, cell.getName() );
        jGenerator.writeStringField( FIELD_TITLE, cell.getTitle() );
        jGenerator.writeStringField( FIELD_ORGANISM, cell.getSpecies().getName());
        
        if(cell.getSourceId() != null)
            jGenerator.writeStringField( FIELD_SOURCE_ID, cell.getSourceId() );
        if(cell.getSource() != null)
            jGenerator.writeStringField( FIELD_SOURCE, cell.getSource() );
        
        if(cell.getCellTypeId() != null)
        {
            jGenerator.writeFieldName( FIELD_CELL_TYPE_IDS );
            strListSerializer.write( Arrays.asList( cell.getCellTypeId() ), jGenerator );
        }
        
        if(cell.getBrendaId() != null)
            jGenerator.writeStringField( FIELD_BRENDA_ID, cell.getBrendaId() );
        if(cell.getCellOntologyId() != null)
            jGenerator.writeStringField( FIELD_CELL_ONTOLGY_ID, cell.getCellOntologyId() );
        if(cell.getCellosaurusId() != null)
            jGenerator.writeStringField( FIELD_CELLOSAURUS_ID, cell.getCellosaurusId() );
        if(cell.getExpFactorOntologyId() != null)
            jGenerator.writeStringField( FIELD_EXP_FACTOR_ONTOLOGY_ID, cell.getExpFactorOntologyId() );
        if(cell.getUberonId() != null)
            jGenerator.writeStringField( FIELD_UBERON_ID, cell.getUberonId() );
    }

}
