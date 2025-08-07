package biouml.plugins.wdl;

import biouml.plugins.wdl.WDLEditor.WorkflowSettings;

public class NextFlowPreprocessor
{
    private WorkflowSettings settings;
    public void NextFlowPreprocessor()
    {

    }

    public void setExportPath(WorkflowSettings settings)
    {
        this.settings = settings;
    }

    public String preprocess(String s) throws Exception
    {
        return s.replace( "~{", "${" );
        //            String[] lines = s.split( "\n" );
        //            for( int i = 0; i < lines.length; i++ )
        //            {
        //                String line = lines[i];
        //                if( line.contains( "biouml.get(" ) )
        //                {
        //                    line = line.replace( "\"", "" );
        //                    String paramName = line.substring( line.indexOf( "." ) + 1, line.indexOf( "=" ) ).trim();
        //                    String path = line.substring( line.indexOf( "(" ) + 1, line.lastIndexOf( ")" ) ).trim();
        //                    DataElement de = DataElementPath.create( path ).getDataElement();
        //                    export( de, new File( outputDir ) );
        //                    lines[i] = "params." + paramName + " = file(\"" + de.getName() + "\")";
        //                }
        //            }
        //            return StreamEx.of( lines ).joining( "\n" );
    }


}