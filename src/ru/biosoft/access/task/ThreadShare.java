package ru.biosoft.access.task;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

/**
 * How many threads each user's task pool gets.
 *
 * <p>Without a budget every user's pool has {@code threadsNumber} threads, as it always had: several users running
 * analyses then run several times that many threads, and one user alone leaves the rest of the machine idle. With
 * {@value #BUDGET_PREFERENCE} set in preferences.xml, the budget is divided among the users who have work (queued tasks,
 * or running ones that are not waiting):</p>
 *
 * <pre>threads per user = max( 1, min( budget / users with work, threadsNumber ) )</pre>
 *
 * <p>The budget caps the sum of the shares, except that every user with work gets at least one thread: with more such
 * users than the budget, the sum is the number of users. It caps the shares, not the threads that run at a given moment:</p>
 * <ul>
 * <li>a task waiting on another ({@link TaskPool#markCurrentThreadInactive}) gets one thread more on top of its user's
 * share, as it always did, so that the task it waits for can run;</li>
 * <li>a smaller share interrupts no running task. Work submitted through {@link TaskPool#iterate} goes back to the queue
 * every {@link TaskPool#ITERATE_SWITCH_MILLIS} and so follows a new share within seconds; a task that holds its thread
 * until it is done (for example {@link TaskPool#executeMultiple}) keeps it until then.</li>
 * </ul>
 *
 * <p>The preferences are read at each rebalance; preferences.xml itself is read when the server starts.</p>
 */
public class ThreadShare
{
    /** Threads for all users' analyses together; 0 or absent: no budget, each user gets threadsNumber. */
    public static final String BUDGET_PREFERENCE = "threadsBudget";

    private ThreadShare()
    {
    }

    /**
     * Threads per user.
     * @param budget threads for all users together; 0 or less: no budget, {@code maxPerUser}
     * @param usersWithWork users with work (counted as at least 1)
     * @param maxPerUser the most per user ({@code threadsNumber})
     */
    public static int perUser(int budget, int usersWithWork, int maxPerUser)
    {
        int max = Math.max( 1, maxPerUser );
        if( budget <= 0 )
            return max;
        int share = budget / Math.max( 1, usersWithWork );
        return Math.max( 1, Math.min( share, max ) );
    }

    /** The budget from preferences.xml, 0 when not set. */
    static int budget()
    {
        Preferences preferences = Application.getPreferences();
        if( preferences == null )
            return 0;
        DynamicProperty property = preferences.getProperty( BUDGET_PREFERENCE );
        if( property == null || property.getValue() == null )
            return 0;
        Object value = property.getValue();
        if( value instanceof Number )
            return ( (Number)value ).intValue();
        try
        {
            return Integer.parseInt( value.toString().trim() );
        }
        catch( NumberFormatException e )
        {
            return 0;
        }
    }
}
