package ru.biosoft.bsa.gui;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.AbstractElementAction;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.project.ProjectDocument;
import ru.biosoft.bsa.project.TrackInfo;
import ru.biosoft.gui.DocumentManager;

/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public class OpenTrackInGenomeBrowser extends AbstractElementAction
{
    private AnnotatedSequence getAnnotatedSequence(DataElement de)
    {
        try
        {
            if(!(de instanceof Track)) return null;
            DataElementPath sequenceCollectionPath = TrackUtils.getTrackSequencesPath((Track)de);
            DataCollection<AnnotatedSequence> dc = sequenceCollectionPath.getDataCollection(AnnotatedSequence.class);
            if(dc.isEmpty()) return null;
            return dc.iterator().next();
        }
        catch( Exception e )
        {
        }
        return null;
    }
    

    @Override
    protected void performAction(DataElement de) throws Exception
    {
        AnnotatedSequence as = getAnnotatedSequence(de);
        DocumentManager documentManager = DocumentManager.getDocumentManager();
        ProjectDocument document = (ProjectDocument)documentManager.openDocument(as);
        document.getProject().addTrack(new TrackInfo((Track)de));
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return getAnnotatedSequence(de) != null;
    }

}
