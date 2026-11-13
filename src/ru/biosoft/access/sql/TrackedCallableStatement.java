package ru.biosoft.access.sql;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;

/**
 * Makes a CallableStatement of a {@link PersistentConnection} take part in its statement lifecycle: it is
 * registered while open, so idle validation never pings or replaces the connection under it, every call counts
 * as activity, and getConnection() returns the persistent connection.
 *
 * Unlike PersistentStatement it does not reconnect on failure. Nothing in BioUML calls prepareCall, and a full
 * wrapper would have to delegate the ~100 extra methods of CallableStatement; a raw callable statement never
 * had recovery either. Executing a stored procedure again after a lost connection is also not safe in general.
 */
final class TrackedCallableStatement
{
    private TrackedCallableStatement()
    {
    }

    /** Called under the connection's replacement lock (from PersistentConnection.withRetry) */
    static CallableStatement wrap(PersistentConnection conn, CallableStatement statement)
    {
        CallableStatement tracked = (CallableStatement)Proxy.newProxyInstance( TrackedCallableStatement.class.getClassLoader(),
                new Class<?>[] {CallableStatement.class}, (proxy, method, args) -> {
                    switch( method.getName() )
                    {
                        case "equals":
                            return proxy == args[0];
                        case "hashCode":
                            return System.identityHashCode( proxy );
                        case "toString":
                            return "TrackedCallableStatement: " + statement;
                        case "getConnection":
                            return conn;
                        case "close":
                            conn.touch();
                            // same protocol as PersistentStatement.close: under the replacement lock, unregistered
                            // only after the JDBC close has returned
                            conn.closeStatement( (CallableStatement)proxy, statement::close );
                            return null;
                        default:
                            break;
                    }
                    conn.touch();
                    try
                    {
                        return method.invoke( statement, args );
                    }
                    catch( InvocationTargetException e )
                    {
                        throw e.getCause();
                    }
                } );
        conn.addStatement( tracked );
        return tracked;
    }
}
