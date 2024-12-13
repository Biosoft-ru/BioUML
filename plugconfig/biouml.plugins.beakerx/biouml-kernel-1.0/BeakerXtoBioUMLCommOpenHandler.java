package biouml.plugins.beakerx;

import com.twosigma.beakerx.kernel.KernelFunctionality;
import com.twosigma.beakerx.handler.Handler;
import com.twosigma.beakerx.kernel.comm.AutotranslationHandler;
import com.twosigma.beakerx.kernel.comm.GetCodeCellsHandler;
import com.twosigma.beakerx.kernel.comm.KernelControlCommandListHandler;
import com.twosigma.beakerx.kernel.comm.KernelControlInterrupt;
import com.twosigma.beakerx.kernel.comm.TargetNamesEnum;
import com.twosigma.beakerx.message.Message;
import com.twosigma.beakerx.kernel.handler.CommOpenHandler;

public class BeakerXtoBioUMLCommOpenHandler extends CommOpenHandler 
{
    private Handler<?>[] KERNEL_CONTROL_CHANNEL_HANDLERS = {
            new KernelControlInterrupt(kernel),
            new KernelControlCommandListHandler(kernel)};

    private Handler<?>[] KERNEL_GET_CODECELLS_CHANNEL_HANDLER = {
            new GetCodeCellsHandler(kernel)};

    private Handler<?>[] AUTOTRANSLATION_HANDLER = {
            new AutotranslationHandler(kernel)};

    public BeakerXtoBioUMLCommOpenHandler( KernelFunctionality kernel ) 
    {
        super( kernel );
    }

    public Handler<Message>[] getKernelControlChanelHandlers(String targetName) 
    {
        if (TargetNamesEnum.KERNEL_CONTROL_CHANNEL.getTargetName().equalsIgnoreCase(targetName)) 
        {
            return (Handler<Message>[]) KERNEL_CONTROL_CHANNEL_HANDLERS;
        } 
        else if (TargetNamesEnum.BEAKER_GETCODECELLS.getTargetName().equalsIgnoreCase(targetName)) 
        {
            return (Handler<Message>[]) KERNEL_GET_CODECELLS_CHANNEL_HANDLER;
        } 
        else if (TargetNamesEnum.BEAKER_AUTOTRANSLATION.getTargetName().equalsIgnoreCase(targetName)) 
        {
            return (Handler<Message>[]) AUTOTRANSLATION_HANDLER;
        } 
        else 
        {
            return (Handler<Message>[]) new Handler<?>[0];
        }
    }
}