package biouml.plugins.wdl;

import java.nio.file.Path;

public class GeneSpaceContext
{
    //Path to all user projects
    protected Path projectsDir;
    //Path to worklfows 
    protected Path workflowsDir;
    //Path to supporting data like genome references
    protected Path genomeDir;
    //Path to folder where process run will be performed
    protected Path outputDir;

    public GeneSpaceContext(Path projectDir, Path workflowsDir, Path genomeDir, Path outputDir)
    {
        this.projectsDir = projectDir;
        this.workflowsDir = workflowsDir;
        this.genomeDir = genomeDir;
        this.outputDir = outputDir;
    }

    public Path getProjectsDir()
    {
        return projectsDir;
    }

    public void setProjectDir(Path projectDir)
    {
        this.projectsDir = projectDir;
    }

    public Path getWorkflowsDir()
    {
        return workflowsDir;
    }

    public void setWorkflowsDir(Path workflowsDir)
    {
        this.workflowsDir = workflowsDir;
    }

    public Path getGenomeDir()
    {
        return genomeDir;
    }

    public void setGenomeDir(Path genomeDir)
    {
        this.genomeDir = genomeDir;
    }

    public Path getOutputDir()
    {
        return outputDir;
    }

    public void setOutputDir(Path outputDir)
    {
        this.outputDir = outputDir;
    }

}
