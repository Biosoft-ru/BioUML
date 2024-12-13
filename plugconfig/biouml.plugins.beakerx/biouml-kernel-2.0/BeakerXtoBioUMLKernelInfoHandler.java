package biouml.plugins.beakerx;

import com.twosigma.beakerx.BeakerImplementationInfo;
import com.twosigma.beakerx.KernelInfoHandler;
import com.twosigma.beakerx.kernel.KernelFunctionality;

import java.io.Serializable;
import java.util.HashMap;

public class BeakerXtoBioUMLKernelInfoHandler extends KernelInfoHandler
{
    public BeakerXtoBioUMLKernelInfoHandler( KernelFunctionality kernel )
    {
        super( kernel );
    }

    @Override
    protected HashMap<String, Serializable> doLanguageInfo(HashMap<String, Serializable> languageInfo)
    {
        languageInfo.put("name", "ECMAScript");
        languageInfo.put("version", "ECMA - 262 Edition 5.1" );
        languageInfo.put("mimetype", "text/javascript");
        languageInfo.put("file_extension", ".js");
        languageInfo.put("codemirror_mode", "javascript");
        languageInfo.put("nbconverter_exporter", "");
        return languageInfo;
    }

    @Override
    protected HashMap<String, Serializable> doContent(HashMap<String, Serializable> content)
    {
        content.put("implementation", "javascript");
        content.put("banner", String.format(BeakerImplementationInfo.IMPLEMENTATION_VERSION, "BioUML", "Dev"));
        return content;
    }
}
