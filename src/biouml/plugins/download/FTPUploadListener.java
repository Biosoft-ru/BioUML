
package biouml.plugins.download;

import ru.biosoft.jobcontrol.JobControl;

import it.sauronsoftware.ftp4j.FTPDataTransferListener;

/**
 * @author anna
 *
 */
public class FTPUploadListener implements FTPDataTransferListener
{
    private long bytesTransferred = 0;
    private JobControl job = null;
    private long totalBytesToTransfer = 0;
    private int percentFrom = 0;
    private int percentTo = 100;
    
    public FTPUploadListener(JobControl jc, int from, int to)
    {
        job = jc;
        percentFrom = from;
        percentTo = to;
    }
    @Override
    public void aborted()
    {
    }

    @Override
    public void completed()
    {
    }

    @Override
    public void failed()
    {
    }

    @Override
    public void started()
    {
    }

    @Override
    public void transferred(int length)
    {
        bytesTransferred += length;
        if( job != null )
        {
            job.setPreparedness(percentFrom + (int)((percentTo - percentFrom)*bytesTransferred/totalBytesToTransfer));
        }
    }
    public long getTotalBytesToTransfer()
    {
        return totalBytesToTransfer;
    }
    public void setTotalBytesToTransfer(long totalBytesToTransfer)
    {
        this.totalBytesToTransfer = totalBytesToTransfer;
    }
    
    public void setBytesTransferred(long n)
    {
        this.bytesTransferred = n;
    }

}
