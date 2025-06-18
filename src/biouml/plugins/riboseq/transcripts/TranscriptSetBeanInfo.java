package biouml.plugins.riboseq.transcripts;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.bean.BeanInfoEx2;

public class TranscriptSetBeanInfo extends BeanInfoEx2<TranscriptSet>
{
    public TranscriptSetBeanInfo()
    {
        super( TranscriptSet.class );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        addWithTags( "annotationSource", TranscriptSet.ANNOTATION_SOURCE_ENSEMBL, TranscriptSet.ANNOTATION_SOURCE_BED_FILE, TranscriptSet.ANNOTATION_SOURCE_GTF_FILE );
        
        PropertyDescriptorEx pde = DataElementPathEditor.registerInput( "transcriptsTrack", beanClass, Track.class );
        addHidden( pde, "isTranscriptsTrackHidden" );
        
        pde = DataElementPathEditor.registerInput( "sequencesCollection", beanClass, SequenceCollection.class );
        addHidden( pde, "isSequencesCollectionHidden" );
        
        addHidden( "ensembl", "isEnsemblHidden" );
        
        property( "transcriptSubset" ).inputElement( ru.biosoft.access.core.DataCollection.class ).canBeNull().hidden( "isTranscriptSubsetHidden" ).add();
        
        property("gtfFile").inputElement( FileDataElement.class ).hidden( "isGtfFileHidden" ).add();;
    }
}
