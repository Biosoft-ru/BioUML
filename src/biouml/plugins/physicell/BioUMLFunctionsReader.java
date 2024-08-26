package biouml.plugins.physicell;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.xml.FunctionsReader;

public class BioUMLFunctionsReader extends FunctionsReader
{
    /**
     * For each cell definition we may associate function with file path
     * Later given file will be imported to BioUML as ScriptDataElement
     */
    Map<String, FunctionsInfo> cellFunctions = new HashMap<>();

    public void readFunctions(Element element, CellDefinition cd)
    {
        FunctionsInfo info = new FunctionsInfo();
        for( Element functionElement : getAllElements( element ) )
        {
            String name = functionElement.getAttribute( "name" );
            String type = functionElement.getAttribute( "type" );
            String path = functionElement.getAttribute( "file" );
            info.addFunction( name, type, path );
        }
        if( info != null )
            cellFunctions.put( cd.name, info );
    }

    public Map<String, FunctionInfo> getFunctionsInfo(String cd)
    {
        FunctionsInfo info = cellFunctions.get( cd );
        if( info == null )
            return new HashMap<String, FunctionInfo>();
        return info.functionPaths;
    }

    public static class FunctionsInfo
    {
        Map<String, FunctionInfo> functionPaths = new HashMap<String, FunctionInfo>();

        public void addFunction(String name, String type, String path)
        {
            functionPaths.put( name, new FunctionInfo(name, type, path) );
        }

        public boolean isEmpty()
        {
            return functionPaths.isEmpty();
        }
    }
    
    public static class FunctionInfo
    {
        String name;
        String type;
        String path;
     
        public FunctionInfo(String name, String type, String path)
        {
            this.name = name;
            this.type = type;
            this.path = path;
        }
    }
}