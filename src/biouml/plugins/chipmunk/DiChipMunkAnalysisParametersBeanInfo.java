package biouml.plugins.chipmunk;

import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 *
 */
public class DiChipMunkAnalysisParametersBeanInfo extends BeanInfoEx2<DiChipMunkAnalysisParameters>
{
    public DiChipMunkAnalysisParametersBeanInfo()
    {
        super(DiChipMunkAnalysisParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property("inputSequences").inputElement( Track.class ).value( DataElementPathEditor.CHILD_CLASS, AnnotatedSequence.class ).add();
        add("startLength");
        add("stopLength");
        addExpert( "threadCount" );
        addExpert( "stepLimit" );
        addExpert( "tryLimit" );
        addExpert( "localBackground" );
        addExpert( "zoopsFactor" );
        property( "shape" ).expert().tags( AbstractChipMunkParameters.SHAPES ).add();
        addExpert( "useProfiles" );

        property( "outputLibrary" ).outputElement( TransformedDataCollection.class )
                .value( DataElementPathEditor.CHILD_CLASS, FrequencyMatrix.class ).auto( "$inputSequences/parent$/Matrix library" ).add();

        property( "matrixName" ).auto( "$inputSequences/name$" ).add();
        property( "matrixPath" ).outputElement( FrequencyMatrix.class ).hidden().add();
    }
}
