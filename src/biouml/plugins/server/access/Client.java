package biouml.plugins.server.access;

import javax.annotation.Nonnull;

import ru.biosoft.access.security.Permission;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.server.ClientConnection;

public interface Client 
{
	/**
	 * Returns true if corresponding client data collection (or module) is principal.
	 * 
	 * Principal holds connection credentials (client connection, login, password and session id). 
	 * @return
	 */
	public boolean isPrincipal();

   
	/**
	 * Returns permissions. 
	 */
    public Permission getPermission();

    //public @Nonnull ClientConnection getClientConnection() throws LoggedException;

}
