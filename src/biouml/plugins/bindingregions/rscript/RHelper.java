
package biouml.plugins.bindingregions.rscript;

import java.io.IOException;
import java.io.InputStream;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * @author yura
 *
 */
public class RHelper
{
    public static String getScript(String category, String scriptName) throws IOException
    {
        
        try( InputStream is = RHelper.class.getResourceAsStream(category + '/' + scriptName +".R") )
        {
            return ApplicationUtils.readAsString(is);
        }
    }
}
