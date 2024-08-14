package ru.biosoft.bsa.gui;

import java.util.ResourceBundle;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.repository.DataElementPathDialog;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.ProjectAsLists;
import ru.biosoft.bsa.project.ProjectDocument;
import ru.biosoft.bsa.project.TrackInfo;
import ru.biosoft.gui.GUI;
import biouml.model.Module;
import biouml.workbench.OpenDocumentAction;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * Open ensemble sequence as BSA project action
 */
@SuppressWarnings ( "serial" )
public class OpenSequenceAction extends OpenDocumentAction
{
    private AnnotatedSequence getAnnotatedSequence(DataElement de)
    {
        try
        {
            if(de instanceof AnnotatedSequence) return (AnnotatedSequence)de;
            if(!(de instanceof DataCollection)) return null;
            DataCollection<?> dc = (DataCollection<?>)de;
            if(!AnnotatedSequence.class.isAssignableFrom(dc.getDataElementType())) return null;
            if(dc.isEmpty()) return null;
            de = dc.iterator().next();
            if(de instanceof AnnotatedSequence) return (AnnotatedSequence)de;
        }
        catch( Exception e )
        {
        }
        return null;
    }
    
    private DataCollection<Track> getTracksCollection(AnnotatedSequence de)
    {
        try
        {
            Module module = Module.optModule(de);
            if(module == null) return null;
            DataElement tracksDE = module.get("Tracks");
            if(!(tracksDE instanceof DataCollection)) return null;
            @SuppressWarnings ( "unchecked" )
            DataCollection<Track> tracksDC = (DataCollection<Track>)tracksDE;
            if(!Track.class.isAssignableFrom(tracksDC.getDataElementType()) || tracksDC.isEmpty()) return null;
            return tracksDC;
        }
        catch( Exception e )
        {
            return null;
        }
    }
    
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        AnnotatedSequence as = getAnnotatedSequence(de);
        DataCollection<Track> tracksCollection = getTracksCollection(as);
        if(tracksCollection != null)
        {
            ResourceBundle resources = ResourceBundle.getBundle(MessageBundle.class.getName());
            DataElementPathDialog dialog = new DataElementPathDialog(resources.getString("NEW_PROJECT_DIALOG_TITLE"));
            dialog.setMultiSelect(true);
            dialog.setElementMustExist(true);
            dialog.setElementClass(Track.class);
            dialog.setValue(tracksCollection.getCompletePath().getChildPath(""));
            if(dialog.doModal())
            {
                Project project = ProjectAsLists.createProjectByMap(as.getOrigin(), as.getName(), as);
                
                int order = 0;
                DataElementPathSet selected = dialog.getValues();
                for(Track track: tracksCollection)
                {
                    DataElementPath path = DataElementPath.create(track);
                    if(selected.contains(path))
                    {
                        TrackInfo trackInfo = new TrackInfo(track);
                        trackInfo.setOrder(++order);
                        trackInfo.setTitle(track.getName());
                        project.addTrack(trackInfo);
                        selected.remove(path);
                    }
                }
                if(!selected.isEmpty())
                {
                    ApplicationUtils.errorBox("The following tracks weren't added:\n"+selected.toString());
                }
                GUI.getManager().addDocument(new ProjectDocument(project, true));
            }
        } else
        {
            super.performAction(as);
        }
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        AnnotatedSequence as = getAnnotatedSequence(de);
        return as != null && (as != de || getTracksCollection(as) != null); // The rest actions are handled by "Open document"
    }
}
