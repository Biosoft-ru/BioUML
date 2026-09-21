package biouml.plugins.wdl.nextflow;

import com.developmentontheedge.beans.annot.PropertyName;

public class NextflowSettings
{
    public static final String MOVE = "move";
    public static final String COPY_NO_FOLLOW = "copyNoFollow";
    public static final String COPY = "copy";
    public static final String LINK = "link";
    public static final String RELLINK = "rellink";
    public static final String SYMLINK = "symlink";
    
    public final static String[] STAGE_IN_OPTIONS = new String[] {SYMLINK, RELLINK, LINK, COPY};
    public final static String[] PUBLISH_OUT_OPTIONS = new String[] {SYMLINK, RELLINK, LINK, COPY, COPY_NO_FOLLOW, MOVE};
    
    private String stageInput = SYMLINK;
    private String publishOutput = LINK;
    private boolean root = true;
    
    @PropertyName ( "Publish output mode")
    public String getPublishOutput()
    {
        return publishOutput;
    }
    public void setPublishOutput(String publishOutput)
    {
        this.publishOutput = publishOutput;
    }
    
    @PropertyName ( "Stage input mode")
    public String getStageInput()
    {
        return stageInput;
    }
    public void setStageInput(String stageInput)
    {
        this.stageInput = stageInput;
    }
    
    @PropertyName("Run as root")
    public boolean isRoot()
    {
        return root;
    }
    public void setRoot(boolean asRoot)
    {
        this.root = asRoot;
    }
}
