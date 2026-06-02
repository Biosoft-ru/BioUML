package biouml.plugins.wdl._test;

public class TestResult
{
    private String name;
    private String wdlPath;
    private String title = null;
    private String description = "";
    private String wdlGenerated = "Failed";
    private String wdlValidated = "N/A";
    private String diagramGenerated = "Failed";
    private String nextflowGenerated = "Failed";
    private String nextflowExecuted = "N/A";
    private String roundTest = "Failed";
    private String error;

    public TestResult(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
    
    public void setWDLPath(String wdlPath)
    {
        this.wdlPath = wdlPath;
    }
    public String getWDLPath()
    {
        return wdlPath;
    }


    public void setTitle(String title)
    {
        this.title = title;
    }
    public String getTitle()
    {
        return title;
    }

    public void setDescrption(String description)
    {
        this.description = description;
    }
    public String getDescription()
    {
        return description;
    }

    public String getDiagramGenerated()
    {
        return diagramGenerated;
    }
    public void setDiagramGenerated(String diagramGeneration)
    {
        this.diagramGenerated = diagramGeneration;
    }

    public String getWDLGenerated()
    {
        return wdlGenerated;
    }
    public void setWDLGenerated(String wdlGeneration)
    {
        this.wdlGenerated = wdlGeneration;
    }

    public String getRoundTest()
    {
        return roundTest;
    }
    public void setRoundTest(String roundTest)
    {
        this.roundTest = roundTest;
    }

    public String getWDLValidated()
    {
        return wdlValidated;
    }
    public void setWDLValidated(String wdlValidation)
    {
        this.wdlValidated = wdlValidation;
    }

    public String getNextflowGenerated()
    {
        return nextflowGenerated;
    }
    public void setNextflowGenerated(String nextflowGeneration)
    {
        this.nextflowGenerated = nextflowGeneration;
    }

    public String getNextflowExecuted()
    {
        return nextflowExecuted;
    }
    public void setNextflowExecuted(String nextflowExecution)
    {
        this.nextflowExecuted = nextflowExecution;
    }

    public String getError()
    {
        return error;
    }

    public void setError(String error)
    {
        this.error = error;
    }
}