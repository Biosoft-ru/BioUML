/**
 * 
 */
package biouml.plugins.research;

import java.text.SimpleDateFormat;
import java.util.Date;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.tasks.TaskInfo;

import com.developmentontheedge.beans.Option;

public class JournalInfoWrapper extends Option implements DataElement
{
    protected TaskInfo taskInfo;
    
    public JournalInfoWrapper()
    {
        this.taskInfo = new TaskInfo(null, "");
    }

    public JournalInfoWrapper(TaskInfo taskInfo)
    {
        this.taskInfo = taskInfo;
    }

    protected TaskInfo getTask()
    {
        return taskInfo;
    }

    public String getType()
    {
        return taskInfo.getType();
    }

    public String getSource()
    {
        return taskInfo.getSource() == null ? taskInfo.getData() : taskInfo.getSource().getName();
    }

    public String getEndTimeStr()
    {
        long endTime = taskInfo.getEndTime();
        if( endTime > 0 )
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            String endTimeStr = sdf.format(new Date(endTime));
            return endTimeStr;
        }
        return "-";
    }

    @Override
    public String getName()
    {
        return taskInfo.getName();
    }

    @Override
    public DataCollection getOrigin()
    {
        return taskInfo.getOrigin();
    }
}