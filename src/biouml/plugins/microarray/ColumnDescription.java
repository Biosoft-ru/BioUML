package biouml.plugins.microarray;

import biouml.standard.filter.Action;

/**
 * One filter component description
 */
public class ColumnDescription
{
    public ColumnDescription()
    {
        this.experimentID = "";
        this.column = null;
        this.action = null;
        this.comment = "";
    }
    public ColumnDescription(String experimentID, String column, Action action, String comment)
    {
        this.experimentID = experimentID;
        this.column = column;
        this.action = action;
        this.comment = comment;
    }
    protected String experimentID;
    public String getExperimentID()
    {
        return experimentID;
    }
    public void setExperimentID(String experimentID)
    {
        this.experimentID = experimentID;
    }

    protected String column;
    public String getColumn()
    {
        return column;
    }
    public void setColumn(String column)
    {
        this.column = column;
    }

    protected Action action;
    public Action getAction()
    {
        return action;
    }
    public void setAction(Action action)
    {
        this.action = action;
    }

    protected String comment;
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        this.comment = comment;
    }
    
    @Override
    public ColumnDescription clone()
    {
        return new ColumnDescription(experimentID, column, action, comment);
    }
}

