/* $Id$ */

package biouml.plugins.machinelearning.rscript;

import java.io.IOException;
import java.io.InputStream;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * @author yura
 *
 */
public class RHelper
{
    public static String getScript(String folderName, String scriptFileName)
    {
        InputStream is = RHelper.class.getResourceAsStream(folderName + '/' + scriptFileName + ".R");
        String str = null;
        try
        {
            str = ApplicationUtils.readAsString(is);
        }
        catch( IOException e )
        {
            e.printStackTrace();
        }
        return str;
    }
}
