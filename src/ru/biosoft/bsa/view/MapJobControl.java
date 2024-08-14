package ru.biosoft.bsa.view;

import java.util.logging.Logger;

import ru.biosoft.jobcontrol.FunctionJobControl;

public class MapJobControl extends FunctionJobControl
{
    protected    int mapNumber = 1 ;
    protected    int layoutPercent = 100 ;
    protected    int sequenceLength ;
    protected    int currentMap = 0;
    protected    int currentLength ;

    public void  setMapNumber(int value)
    {
        mapNumber      = value;
    }

    public void  setSequenceLength(int value)
    {
        sequenceLength = value;

    }

    public void  setCurrentMap(int value)
    {
        currentMap = value;
    }

    public void  setCurrentLength(int value)
    {
        currentLength  = value;
        calcPreparedness();
    }

    public void  setLayoutPercent(int value)
    {
        layoutPercent  = value;
        calcPreparedness();
    }

    public MapJobControl(Logger l)
    {
        super( l );
    }

    private int              oldValue = 0;
    private final static int STEP = 1;
    @Override
    public void setPreparedness(int percent)
    {
//        cat.debug("AbstractJobControl setPreparedness("+percent+")");
        //System.out.println( "MapJobControl.setPreparedness: percent = " + percent );
        //System.out.println( "" );
        super.setPreparedness(percent);
        if( CurrentJobControl.getInstance().getValue() == this )
            fireValueChanged();
    }

    protected void calcPreparedness()
    {
        double value = ((double)currentMap)/mapNumber + (((double)layoutPercent)/100) * (((double)currentLength)/sequenceLength/mapNumber);
        value *= 100;
        //cat.debug( "value="+value );
        //System.out.println( "MapJobControl.calcPreparedness: value = " + value );
        int delta = (int)Math.abs(oldValue - value);
        if(delta >= STEP )
        {
            oldValue = (int)value;
            setPreparedness((int)Math.round(value));
        }
    }

    @Override
    public void functionStarted(String msg)
    {
        CurrentJobControl.getInstance().setValue( this );
        if(currentMap==0)
          super.functionStarted(msg);
    }

    @Override
    public void functionFinished(String msg)
    {
        if(currentMap >= mapNumber-1)
          super.functionFinished(msg);
    }

    @Override
    protected void end(String msg)
    {
        if (isStatusTerminated()) return;
        String str = "";

        if (msg != null)
            str = msg;

        log.fine( "end(" + str + ")" );

        setTerminated(runStatus);
        if( CurrentJobControl.getInstance().getValue() == this )
            fireJobTerminated(msg);
        resetFlags();
    }
}
