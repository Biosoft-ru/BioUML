package ru.biosoft.server.servlets.webservices.imports;

import java.util.Optional;

import org.json.JSONObject;

import ru.biosoft.access.security.PrivilegedAction;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.server.servlets.webservices.messages.Message;
import ru.biosoft.server.servlets.webservices.messages.ServerMessages;

public class UploadProgressReporter extends JobControlListenerAdapter
{
    private ImportDAO dao = new ImportDAO();

    private int recId;
    private String user;

    public UploadProgressReporter(int recId, String user)
    {
        this.recId = recId;
        this.user = user;
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
                rec.setUploadProgress( event.getPreparedness() );
                dao.updateRecord( rec );
            }
            messageContent.put( "progress", event.getPreparedness() );
            SecurityManager.runPrivileged( new PrivilegedAction()
            {
                @Override
                public Object run() throws Exception
                {
                    ServerMessages.sendMessageToUser( new Message( "uploadProgress/" + recId, messageContent ), user );
                    return null;
                }
            });
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }

    }

    private Optional<ImportRecord> findImportRecord()
    {
        return dao.findByIdAndUser( recId, user );
    }
    
    private long totalSize = -1;
    public void setTotalSize(long size)
    {
        this.totalSize = size;
    }

    @Override
    public void jobTerminated(JobControlEvent event)
    {
        try
        {
            JSONObject messageContent = new JSONObject();

            String statusText = AbstractJobControl.getTextStatus( event.getStatus() );
            messageContent.put( "status", statusText );
            if(totalSize != -1)
                messageContent.put( "size", totalSize );

            SecurityManager.runPrivileged( new PrivilegedAction()
            {
                @Override
                public Object run() throws Exception
                {
                    ServerMessages.sendMessageToUser( new Message( "uploadDone/" + recId, messageContent ), user );
                    return null;
                }
            } );
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

}
