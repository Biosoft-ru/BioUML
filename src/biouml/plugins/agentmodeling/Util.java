package biouml.plugins.agentmodeling;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;

public class Util
{

    private Util()
    {
    }

    /**
     * Represents Queue as double[] array
     * @param vector
     * @return
     */
    public static double[] asArray(Queue<Double> vector)
    {
        double[] result = new double[vector.size()];
        int i = 0;
        Iterator<Double> iterator = vector.iterator();
        while( iterator.hasNext() )
            result[i++] = iterator.next();
        return result;
    }

    /**
     * Statitical mean value
     * @param sample
     * @return
     */
    public static double mean(double[] sample)
    {
        double sum = 0;
        for( double s : sample )
            sum += s;
        return sum / sample.length;
    }


    public static double linearInterpolation(double t0, double t1, double x0, double x1, double t)
    {
        if( t >= t1 )
            return x1;
        if( t <= t0 )
            return x0;
        return x0 + ( x1 - x0 ) / ( t1 - t0 ) * ( t - t0 );
    }
    
    protected static String joinArray(double[] array)
    {
        StringBuilder result = new StringBuilder("[ ");
        for (double val: array)
            result.append(val+", ");
        result.append(" ]");
        
        return result.toString();
    }
    

    private static String generateModelText(AgentBasedModel model)
    {
        StringBuilder builder = new StringBuilder("");
        builder.append("Agent model");
        for( SimulationAgent agent : model.getAgents() )
        {
            builder.append(agent.getName());
            builder.append(",");
        }
        builder.append("\n");
        for( SimulationAgent agent : model.getAgents() )
        {
            builder.append(agent.getName() + ":\n");
            HashSet<Link> Inlinks = model.agentToInputLinks.get(agent);
            HashSet<Link> Outlinks = model.agentToOutputLinks.get(agent);

            if( Inlinks != null )
            {
                for( Link link : Inlinks )
                {
                    builder.append(
                            link.sender.getName() + " (" + link.nameAtSender + ")" + ( link instanceof FullValueLink ? " -> " : " -- " )
                                    + link.reciever.getName() + " (" + link.nameAtReciever + ")");
                    builder.append("\n");
                }
            }
            builder.append("\n");
            if( Outlinks != null )
            {
                for( Link link : Outlinks )
                {
                    builder.append(link.getSender().getName() + ( link instanceof FullValueLink ? " -> " : " -- " ) + "Scheduler");
                    builder.append("\n");
                }
            }
        }
        return builder.toString();
    }
}
