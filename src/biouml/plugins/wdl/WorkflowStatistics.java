package biouml.plugins.wdl;

import biouml.plugins.wdl.model.ScriptInfo;

public class WorkflowStatistics
{
    private int workflows = 0;
    private int tasks = 0;
    private int calls = 0;
    private int importedCalls = 0;
    private int inputs = 0;
    private int outputs = 0;
    private int expressions = 0;
    private int structures = 0;
    private int cycles = 0;
    private int conditionals = 0;
    private String[] wdlFunctions = new String[0];


    public WorkflowStatistics(ScriptInfo script)
    {

    }


    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        append( "Tasks definitions: " + tasks, sb );
        append( "Struct definitions: " + structures, sb );
        append( "Workflows: " + workflows, sb );
        append( "Calls: " + calls, sb );
        append( "Inputs: " + inputs, sb );
        append( "Outputs: " + outputs, sb );
        append( "Calls: " + calls, sb );
        return sb.toString();
    }

    private void append(String line, StringBuilder sb)
    {
        sb.append( line );
        sb.append( "\n" );
    }


}
