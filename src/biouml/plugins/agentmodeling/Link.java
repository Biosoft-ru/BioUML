package biouml.plugins.agentmodeling;

/**
 *  Link between two agents it is a part of AgentBasedModel.<br> 
 *  Is used to pass messages about variables changes between agents.<br>
 *  One link connects one variable of one agent (sender) with one variable of another agent (receiver)<br>
 *  Link works in 3 steps process
 *  <ul><li> receive message from sender and store it in the link
 *  <li> update message so it is complete and can be used
 *  <li> send stored message to receiver </ul>
 *  @see AgentBasedModel
 *  @see Message
 *  @see SimulationAgent
 *  @author Ilya
 */
abstract class Link
{
    /**Agent which sends messages through this link*/
    protected SimulationAgent sender;
    
    /**Agent which receives messages through this link*/
    protected SimulationAgent reciever;
    
    /**Name of the variable in sender agent*/
    protected String nameAtSender;
    
    /**Name of the variable in receiver agent*/
    protected String nameAtReciever;

    /**
     * Create new Link from agent sender to agent receiver
     * @param sender agent which sends message
     * @param nameAtSender inner sender name of variable which value change is sending
     */
    protected Link(SimulationAgent sender, SimulationAgent recievier, String nameAtSender, String nameAtReciever)
    {
        this.sender = sender;
        this.nameAtSender = nameAtSender;
        this.nameAtReciever = nameAtReciever;
        this.reciever = recievier;
    }

    /** Method should be used to delete any history so simulation of the model may started again*/
    public void init() throws Exception
    {
        //nothing by default
    }
    
    /**Send stored messages to receiver*/
    public abstract void sendMessage() throws Exception;
    
    /**Receive message from sender and store it in the link*/
    public abstract void receiveMessage() throws Exception;
    
    /**Finalize message from receiver*/
    public abstract void updateMessage() throws Exception;
    
    /**delete history which is out dated - marked with time stamps before given minimumTime*/
    public void checkOutdatedMessages(double minimumTime)
    {
        //nothing by default
    }

    // utility methods
    protected double getCurrentSenderTime()
    {
        return sender.getScaledCurrentTime();
    }

    protected double getCurrentSenderValue() throws Exception
    {
        return sender.getCurrentValue(nameAtSender);
    }
    
    public SimulationAgent getSender()
    {
        return sender;
    }
}