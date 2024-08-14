package biouml.plugins.pharm;

import biouml.model.Node;

public class Util
{    
    public static boolean isArray(Node node)
    {
       return Type.TYPE_ARRAY.equals( node.getKernel().getType() ); 
    }

    public static boolean isVariable(Node node)
    {
       return Type.TYPE_VARIABLE.equals( node.getKernel().getType() ); 
    }
    
    public static boolean isPort(Node node)
    {
       return Type.TYPE_PORT.equals( node.getKernel().getType() ); 
    }
}
