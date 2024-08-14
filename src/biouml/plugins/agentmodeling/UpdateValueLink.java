
package biouml.plugins.agentmodeling;

import java.util.LinkedList;

import one.util.streamex.StreamEx;

/**
 * History contains changes in variable value achieved by some agent
 * @author axec
 *
 */
class UpdateValueLink extends Link
{
    private final LinkedList<Message> history = new LinkedList<>();
    private String conversionFactor = null;
    private SimulationAgent mainAgent = null;
    private boolean senderIsMain;

    public UpdateValueLink(SimulationAgent sender, SimulationAgent reciever, String nameAtSender, String nameAtReciever)
    {
        super(sender, reciever, nameAtSender, nameAtReciever);
    }

    public void setConversion(boolean senderIsMain, String conversionFactor)
    {
        this.senderIsMain = senderIsMain;
        this.conversionFactor = conversionFactor;
        mainAgent = senderIsMain ? sender : reciever;
    }

    @Override
    public void receiveMessage() throws Exception
    {
        history.add(new Message(getCurrentSenderTime(), getCurrentSenderValue()));
    }

    @Override
    public void updateMessage() throws Exception
    {
        history.getLast().update(getCurrentSenderTime(), getCurrentSenderValue());
    }

    /**
     * Calculate from messages value for sending to agent
     * @param startTime
     * @param finishTime
     * @return delta achieved from startTime till finishTime
     */
    private double getValueChange(double startTime, double finishTime) throws Exception
    {
        double result = 0;
        
        if( history.isEmpty() )
            return result;

        //TODO: actually we need only two messages here
        for( Message m : StreamEx.of(history).filter(m -> m.getStartTime() <= finishTime || m.getFinishTime() >= startTime) )
            result += m.getValueChange(startTime, finishTime );//getValue(finishTime) - m.getValue(startTime);
        return convertValue(result);
    }

    @Override
    public void sendMessage() throws Exception
    {
        double update = getValueChange(reciever.getScaledPreviousTime(), reciever.getScaledCurrentTime());
        //        System.out.println(sender.getName()+" -[ "+update+" ]-> "+reciever.getName());
        //        log.info(sender.getName()+" -[ "+update+" ]-> "+reciever.getName());
        reciever.setCurrentValueUpdate(nameAtReciever, update);
    }

    /**
     * Delete messages that are out dated for all possible requests
     * (older than memorizedTime)
     */
    @Override
    public void checkOutdatedMessages(double minimumTime)
    {
        if( history.isEmpty() )
            return;
        while( history.element().getFinishTime() < minimumTime )
            history.remove();
    }

    private double convertValue(double value) throws Exception
    {
        if( mainAgent == null || conversionFactor == null || conversionFactor.isEmpty() )
            return value;

        double conversionValue = mainAgent.getCurrentValue(conversionFactor);
        return senderIsMain ? value / conversionValue : value * conversionValue;
    }
    
    @Override
    public void init() throws Exception
    {
        history.clear();
        if (senderIsMain)
        {
            double value = convertValue(sender.getCurrentValue(nameAtSender));
            reciever.setCurrentValue(this.nameAtReciever, value);
        }
        super.init();
    }
}