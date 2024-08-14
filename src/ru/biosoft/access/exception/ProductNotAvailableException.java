package ru.biosoft.access.exception;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.LoggedException;

/**
 * @author lan
 *
 */
public class ProductNotAvailableException extends LoggedException
{
    private static final String KEY_PRODUCT = "product";
    private static final String KEY_USER = "user";

    public static final ExceptionDescriptor ED_PRODUCT_NOT_AVAILABLE = new ExceptionDescriptor( "Product", LoggingLevel.Summary,
            "Product '$product$' is not available for $user$. Please contact service administrator for subscription.");

    public ProductNotAvailableException(String productName)
    {
        super(ED_PRODUCT_NOT_AVAILABLE);
        properties.put( KEY_PRODUCT, productName );
        String user = SecurityManager.getSessionUser();
        properties.put( KEY_USER, user == null ? "current user" : user );
    }
}
