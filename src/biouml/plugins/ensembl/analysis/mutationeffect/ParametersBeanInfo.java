package biouml.plugins.ensembl.analysis.mutationeffect;

import ru.biosoft.bsa.VCFSqlTrack;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ParametersBeanInfo extends BeanInfoEx2<Parameters>
{

    public ParametersBeanInfo()
    {
        super(Parameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(MessageBundle.CN_MUTATION_EFFECT);
        beanDescriptor.setShortDescription(MessageBundle.CD_MUTATION_EFFECT);
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        property( "inputTrack" ).inputElement( VCFSqlTrack.class ).title( "PN_INPUT_TRACK" ).description( "PD_INPUT_TRACK" ).add();
        add( "genome" );
        property( "outputTrack" ).outputElement( VCFSqlTrack.class ).auto( "$inputTrack$ with mutation effect" ).title( "PN_OUTPUT_TRACK" )
                .description( "PD_OUTPUT_TRACK" ).add();
    }
}
