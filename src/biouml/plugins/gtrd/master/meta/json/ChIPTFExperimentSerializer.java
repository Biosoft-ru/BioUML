package biouml.plugins.gtrd.master.meta.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.ChIPTFExperiment;

public abstract class ChIPTFExperimentSerializer<T extends ChIPTFExperiment> extends ChIPExperimentSerializer<T>
{
    private static final String FIELD_TF_UNIPROT_ID = "tfUniprotId";
    private static final String FIELD_TF_TITLE = "tfTitle";
    private static final String FIELD_TF_CLASS_ID = "tfClassId";

    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        super.readField( parser );
        String name = parser.getCurrentName();
        switch(name)
        {
            case FIELD_TF_UNIPROT_ID:
                result.setTfUniprotId( parser.getText() );
                break;
            case FIELD_TF_TITLE:
                result.setTfTitle( parser.getText() );
                break;
            case FIELD_TF_CLASS_ID:
                result.setTfClassId( parser.getText() );
                break;
        }
    }

    @Override
    protected void writeFieldsAfterTreatment(T exp, JsonGenerator jGenerator) throws IOException
    {
        super.writeFieldsAfterTreatment( exp, jGenerator );
        if(!exp.isControlExperiment())
        {
            jGenerator.writeStringField( FIELD_TF_UNIPROT_ID, exp.getTfUniprotId() );
            if(exp.getTfTitle() != null)
                jGenerator.writeStringField( FIELD_TF_TITLE, exp.getTfTitle());
            if(exp.getTfClassId() != null)
                jGenerator.writeStringField( FIELD_TF_CLASS_ID, exp.getTfClassId());
        }
    }
}
