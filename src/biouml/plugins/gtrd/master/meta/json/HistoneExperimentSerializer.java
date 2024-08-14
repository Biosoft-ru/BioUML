package biouml.plugins.gtrd.master.meta.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.HistonesExperiment;

public class HistoneExperimentSerializer extends ChIPExperimentSerializer<HistonesExperiment>
{
    public static final String FIELD_TARGET = "target";

    @Override
    protected HistonesExperiment createExperiment(String id)
    {
        return new HistonesExperiment(null, id);
    }
    
    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        super.readField( parser );
        String name = parser.getCurrentName();
        if(name.equals(FIELD_TARGET))
            result.setTarget( parser.getText() );
    }
    
    @Override
    protected void writeFieldsAfterTreatment(HistonesExperiment exp, JsonGenerator jGenerator) throws IOException
    {
        super.writeFieldsAfterTreatment( exp, jGenerator );
        if(exp.getTarget() != null)
            jGenerator.writeStringField( FIELD_TARGET, exp.getTarget() );
    }
}
