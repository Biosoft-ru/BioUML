package biouml.plugins.downloadext;

import java.util.ListResourceBundle;


public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
                {"CN_CLASS", "Parameters"},
                {"CD_CLASS", "Parameters"},
                {"PN_METHOD_NAME", "Method name"},
                {"PD_METHOD_NAME", "Method name"},
                {"PN_METHOD_DESCRIPTION", "Method description"},
                {"PD_METHOD_DESCRIPTION", "Method description"},
        
                //Import
                {"PN_URL", "FTP file URL"},
                {"PD_URL", "FTP file address in correct format ftp://user:pass@ftp.somehost.org/file.txt"},
                {"PN_IMPORT_RESULT_PATH", "Target"},
                {"PD_IMPORT_RESULT_PATH", "Path in repository to put imported file to"},
                {"PN_IMPORT_FORMAT", "Importer for uploaded file"},
                {"PD_IMPORT_FORMAT", "Import element in the specified format"},
                {"PN_IMPORT_PROPERTIES", "Importer properties"},
                {"PD_IMPORT_PROPERTIES", "Properties for importer"}
        
        };
    }
}
