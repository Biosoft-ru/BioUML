package ru.biosoft.access;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.RepositoryAccessDeniedException;
import ru.biosoft.access.security.Permission;

/**
 * Implementing this interface means that given element wraps normal element with some kind of protection
 * @author lan
 */
public interface ProtectedElement extends DataElement
{
    /**
     * Fetch original element with desired access. Will throw SecurityException if access is not granted
     * Warning: currently this allows you to work with original element directly, removing any protection
     * @param access desired access. See {@link Permission} for details.
     * @return unprotected element
     * @throws SecurityException if desired access cannot be granted
     */
    public DataElement getUnprotectedElement(int access) throws RepositoryAccessDeniedException;
}
