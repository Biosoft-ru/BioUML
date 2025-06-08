package biouml.plugins.wdl.parser.validator;

import java.util.logging.Logger;


public abstract class VersionValidator
{
    protected Logger log;

    static VersionValidator getValidator(String version) throws Exception
    {
        switch( version )
        {
            case "1.0":
                return new VersionValidator1_0();
            case "1.1":
                return new VersionValidator1_1();
            case "1.2":
                return new VersionValidator1_2();
            case "2.0":
                return new VersionValidator2_0();
            default:
                throw new Exception("Specified version is not supported!");
        }
    }

    public abstract void checkRuntimeAttributes(String name, String type) throws Exception;

}
