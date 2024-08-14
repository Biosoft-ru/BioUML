
package biouml.plugins.biopax.writer;

import biouml.plugins.biopax.BioPAXSupport;


/**
 * Utility class to write diagrams and other owl files in BioPAX format.
 *
 * Factory design pattern is used to process different levels of BioPAX format.
 */

public class BioPAXWriterFactory
{
    public static BioPAXWriter getWriter(String format)
    {
        if(format.equals(BioPAXSupport.BIOPAX_LEVEL_3))
        {
            return new BioPAXWriter_level3();
        }
        else if(format.equals(BioPAXSupport.BIOPAX_LEVEL_2))
        {
            return new BioPAXWriter_level2();
        }
        else
        {
            return null;
        }
    }
}
