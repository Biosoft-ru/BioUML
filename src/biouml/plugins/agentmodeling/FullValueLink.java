package biouml.plugins.agentmodeling;

class FullValueLink extends Link
{
    private Message message;

    public FullValueLink(SimulationAgent sender,SimulationAgent reciever,  String nameAtSender, String nameAtReceiver)
    {
        super(sender, reciever, nameAtSender, nameAtReceiver);
    }

    @Override
    public void receiveMessage() throws Exception
    {
        message = new Message(getCurrentSenderTime(), getCurrentSenderValue());
    }

    @Override
    public void updateMessage() throws Exception
    {
    }

    @Override
    public void sendMessage() throws Exception
    {
        if( message == null )
            return;

        if( !message.isReady() )
            message.update(getCurrentSenderTime(), getCurrentSenderValue());
        double requestedTime = reciever.getScaledCurrentTime();
        double value = message.getValue(requestedTime);
        reciever.setCurrentValue(nameAtReciever, value);
    }
}