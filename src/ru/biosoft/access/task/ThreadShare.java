package ru.biosoft.access.task;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

/**
 * How many threads each user's task pool gets.
 *
 * <p>Without a budget every user's pool has {@code threadsNumber} threads, as it always had: several users running
 * analyses then run several times that many threads, and one user alone leaves the rest of the machine idle. With
 * {@value #BUDGET_PREFERENCE} set in preferences.xml, the budget is shared among the users who have work (running or
 * queued tasks):</p>
 *
 * <pre>threads per user = clamp( budget / users with work, threadsMinPerUser, threadsNumber )</pre>
 *
 * <p>The preferences are read at each rebalance, so a change applies without a restart.</p>
 */
public class ThreadShare
{
    /** Threads for all users' analyses together; 0 or absent: no budget, each user gets threadsNumber. */
    public static final String BUDGET_PREFERENCE = "threadsBudget";
    /** The fewest threads a user with work gets under a budget, however many users there are. */
    public static final String MIN_PER_USER_PREFERENCE = "threadsMinPerUser";
    public static final int DEFAULT_MIN_PER_USER = 8;

    private ThreadShare()
    {
    }

    /**
     * Threads per user.
     * @param budget threads for all users together; 0 or less: no budget, {@code maxPerUser}
     * @param usersWithWork users with running or queued tasks (counted as at least 1)
     * @param minPerUser the fewest per user (at least 1, at most {@code maxPerUser})
     * @param maxPerUser the most per user ({@code threadsNumber})
     */
    public static int perUser(int budget, int usersWithWork, int minPerUser, int maxPerUser)
    {
        int max = Math.max( 1, maxPerUser );
        if( budget <= 0 )
            return max;
        int min = Math.max( 1, Math.min( minPerUser, max ) );
        int share = budget / Math.max( 1, usersWithWork );
        return Math.max( min, Math.min( share, max ) );
    }

    /** The budget from preferences.xml, 0 when not set. */
    static int budget()
    {
        return intPreference( BUDGET_PREFERENCE, 0 );
    }

    /** The per-user minimum from preferences.xml. */
    static int minPerUser()
    {
        return intPreference( MIN_PER_USER_PREFERENCE, DEFAULT_MIN_PER_USER );
    }

    private static int intPreference(String name, int defaultValue)
    {
        Preferences preferences = Application.getPreferences();
        if( preferences == null )
            return defaultValue;
        DynamicProperty property = preferences.getProperty( name );
        if( property == null || property.getValue() == null )
            return defaultValue;
        Object value = property.getValue();
        if( value instanceof Number )
            return ( (Number)value ).intValue();
        try
        {
            return Integer.parseInt( value.toString().trim() );
        }
        catch( NumberFormatException e )
        {
            return defaultValue;
        }
    }
}
