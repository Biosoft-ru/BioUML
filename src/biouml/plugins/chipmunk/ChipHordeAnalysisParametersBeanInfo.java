package biouml.plugins.chipmunk;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 *
 */
public class ChipHordeAnalysisParametersBeanInfo extends BeanInfoEx2<ChipHordeAnalysisParameters>
{
    public ChipHordeAnalysisParametersBeanInfo()
    {
        super( ChipHordeAnalysisParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "inputSequences" ).inputElement( Track.class ).value( DataElementPathEditor.CHILD_CLASS, AnnotatedSequence.class ).add();
        add( "startLength" );
        add( "stopLength" );
        add( "nMotifs" );
        addWithTags( "mode", AbstractChipMunkParameters.HORDE_MODES );
        addExpert( "threadCount" );
        addExpert( "stepLimit" );
        addExpert( "tryLimit" );
        addExpert( "gcPercent" );
        addExpert( "zoopsFactor" );
        property( "shape" ).expert().tags( AbstractChipMunkParameters.SHAPES ).add();
        addExpert( "useProfiles" );

        property( "outputLibrary" ).outputElement( WeightMatrixCollection.class )
                .value( DataElementPathEditor.CHILD_CLASS, FrequencyMatrix.class ).auto( "$inputSequences/parent$/Matrix library" ).add();

        property( "matrixName" ).auto( "$inputSequences/name$" ).add();
    }
}
