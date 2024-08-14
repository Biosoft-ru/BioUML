package ru.biosoft.server.servlets.webservices.imports;

import java.util.Optional;

import org.json.JSONObject;

import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.servlets.webservices.messages.Message;
import ru.biosoft.server.servlets.webservices.messages.ServerMessages;

public class ImportProgressReporter extends JobControlListenerAdapter
{
    private ImportDAO dao = new ImportDAO();

    private int importRecordId;

    public ImportProgressReporter(int importRecordId)
    {
        this.importRecordId = importRecordId;
    }

    @Override
    public void valueChanged(JobControlEvent event)
    {
        JSONObject messageContent = new JSONObject();
        try
        {
            Optional<ImportRecord> optRec = findImportRecord();
            if( optRec.isPresent() )
            {
                ImportRecord rec = optRec.get();
                rec.setImportProgress( event.getPreparedness() );
                dao.updateRecord( rec );
            }
            messageContent.put( "progress", event.getPreparedness() );
            ServerMessages.sendMessageToCurrentUser( new Message( "importProgress/" + importRecordId, messageContent ) );
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }

    }

    private Optional<ImportRecord> findImportRecord()
    {
        return dao.findByIdAndUser( importRecordId, SecurityManager.getSessionUser() );
    }

    @Override
    public void jobTerminated(JobControlEvent event)
    {
        try
        {
            Optional<ImportRecord> optRec = findImportRecord();
            if(optRec.isPresent()) {
                ImportRecord rec = optRec.get();
                rec.setStatus( event.getStatus() == JobControl.COMPLETED ? ImportStatus.DONE : ImportStatus.IMPORT_ERROR );
                dao.updateRecord( rec );
                JSONObject messageContent = new JSONObject();

                String statusText = AbstractJobControl.getTextStatus( event.getStatus() );
                messageContent.put( "status", statusText );
                DataElementPath targetElementPath = rec.getTargetFolder().getChildPath( rec.getTargetElementName() );
                if( event.getStatus() == JobControl.COMPLETED)
                    messageContent.put( "result", targetElementPath.toString() );
                ServerMessages.sendMessageToCurrentUser( new Message( "importDone/" + rec.getId(), messageContent ) );
            }
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

}
