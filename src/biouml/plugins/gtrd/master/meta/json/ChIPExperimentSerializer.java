package biouml.plugins.gtrd.master.meta.json;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import biouml.plugins.gtrd.ChIPExperiment;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.Experiment;
import biouml.plugins.gtrd.ExperimentType;
import biouml.plugins.gtrd.ExternalReference;
import biouml.plugins.gtrd.master.sites.json.ListSerializer;

public abstract class ChIPExperimentSerializer<T extends ChIPExperiment> extends ExperimentSerializer<T>
{
    private static final String FIELD_EXP_TYPE = "expType";
    private static final String FIELD_CONTROL_ID = "controlId";
    private static final String FIELD_ANTIBODY = "antibody";
    
    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        super.readField( parser );
        String name = parser.getCurrentName();
        switch(name)
        {
            case FIELD_EXP_TYPE:
                result.setExpType( ExperimentType.valueOf( parser.getText() ) );
                break;
            case FIELD_CONTROL_ID:
                result.setControlId( parser.getText() );
                break;
            case FIELD_ANTIBODY:
                result.setAntibody( parser.getText() );
                break;
        }
    }

    @Override
    protected void writeFieldsAfterId(T exp, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeStringField( FIELD_EXP_TYPE, exp.getExpType().toString() );

        if( exp.getControlId() != null )
            jGenerator.writeStringField( FIELD_CONTROL_ID, exp.getControlId() );
    }
    
    @Override
    protected void writeFieldsAfterTreatment(T exp, JsonGenerator jGenerator) throws IOException
    {
        String antibody = exp.getAntibody();
        if(antibody == null)
            antibody = "";
        jGenerator.writeStringField( FIELD_ANTIBODY, antibody );
    }
}
