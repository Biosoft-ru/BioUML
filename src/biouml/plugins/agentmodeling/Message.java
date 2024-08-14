package biouml.plugins.agentmodeling;

class Message
{
    private double startTime;
    private double finishTime;
    private double startValue;
    private double finishValue;
    private boolean readyToSend;

    protected Message(double startTime, double startValue) throws IllegalArgumentException
    {

        this.startTime = startTime;
        this.startValue = startValue;
        this.readyToSend = false;
    }

    public void update(double time, double value)
    {
        finishTime = time;
        finishValue = value;
        readyToSend = true;
    }

    public double getValue(double time) throws Exception
    {
        if( !readyToSend )
            throw new Exception("Message is not ready.");
        return Util.linearInterpolation(startTime, finishTime, startValue, finishValue, time);
    }
    
    public double getValueChange(double startTime, double endTime) throws Exception
    {
        if (startTime == endTime)
            return finishValue - startValue;
        return getValue(endTime) - getValue(startTime);
    }

    public boolean isReady()
    {
        return readyToSend;
    }

    public double getFinishTime()
    {
        return finishTime;
    }
    
    public double getStartTime()
    {
        return startTime;
    }
}