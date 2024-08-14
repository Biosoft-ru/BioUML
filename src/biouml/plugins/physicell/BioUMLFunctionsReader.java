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
            if( type.equals( "NONE" ) )
            {
                info.addFunction( name, null );
            }
            else if( type.equals( "CUSTOM" ) )
            {
                String path = functionElement.getAttribute( "path" );
                info.addFunction( name, path );
            }
        }
        if( info != null )
            cellFunctions.put( cd.name, info );
    }

    public Map<String, String> getFunctionsInfo(String cd)
    {
        FunctionsInfo info = cellFunctions.get( cd );
        if( info == null )
            return new HashMap<String, String>();
        return info.functionPaths;
    }

    public static class FunctionsInfo
    {
        Map<String, String> functionPaths = new HashMap<String, String>();

        public void addFunction(String name, String path)
        {
            functionPaths.put( name, path );
        }

        public boolean isEmpty()
        {
            return functionPaths.isEmpty();
        }
    }
}
