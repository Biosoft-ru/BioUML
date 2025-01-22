package biouml.plugins.beakerx;

import com.twosigma.beakerx.kernel.restserver.impl.BeakerXServerJavalin;
import com.twosigma.beakerx.kernel.KernelFunctionality;
import com.twosigma.beakerx.kernel.restserver.impl.GetUrlArgHandler;
import io.javalin.Javalin;

public class BeakerXtoBioUMLServer extends BeakerXServerJavalin 
{
    public BeakerXtoBioUMLServer( GetUrlArgHandler urlArgHandler ) 
    {
        super( urlArgHandler );
    }

    @Override
    public void createMapping( Javalin app, KernelFunctionality kernel )
    {
    }
}
