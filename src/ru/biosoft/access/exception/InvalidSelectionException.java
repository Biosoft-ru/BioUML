package ru.biosoft.access.exception;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.LoggedException;

/**
 * @author lan
 *
 */
public class InvalidSelectionException extends LoggedException
{
    public static final int SELECTED_ZERO = 0;
    public static final int SELECTED_ONE = 1;
    public static final int SELECTED_ANY = 2;
    public static final int SELECTED_ZERO_OR_ANY = 3;
    public static final int SELECTED_UNDEFINED = -1;

    public static final ExceptionDescriptor ED_SELECTION_ONE_REQUIRED = new ExceptionDescriptor( "OneRequired", LoggingLevel.None,
            "Please select exactly one $object$");
    public static final ExceptionDescriptor ED_SELECTION_MULTIPLE_REQUIRED = new ExceptionDescriptor( "MultipleRequired", LoggingLevel.None,
            "Please select one or more $objects$");
    public static final ExceptionDescriptor ED_SELECTION_NONE_REQUIRED = new ExceptionDescriptor( "NoneRequired", LoggingLevel.None,
            "This operation doesn't need any selection. Please deselect all $objects$ to continue");

    private static final String KEY_OBJECT = "object";
    private static final String KEY_OBJECTS = "objects";

    public InvalidSelectionException(int wantedMode, String objectSingular, String objectPlural)
    {
        super(wantedMode == SELECTED_ONE ? ED_SELECTION_ONE_REQUIRED : wantedMode == SELECTED_ANY ? ED_SELECTION_MULTIPLE_REQUIRED
                : ED_SELECTION_NONE_REQUIRED);
        properties.put( KEY_OBJECT, objectSingular );
        properties.put( KEY_OBJECTS, objectPlural );
    }

    public InvalidSelectionException(int wantedMode)
    {
        this(wantedMode, "item", "items");
    }

    public static void checkSelection(int wantedMode, int numberOfSelected)
    {
        switch(wantedMode)
        {
            case SELECTED_ZERO:
                if(numberOfSelected != 0)
                    throw new InvalidSelectionException(wantedMode);
                break;
            case SELECTED_ONE:
                if(numberOfSelected != 1)
                    throw new InvalidSelectionException(wantedMode);
                break;
            case SELECTED_ANY:
                if(numberOfSelected <= 0)
                    throw new InvalidSelectionException(wantedMode);
                break;
        }
    }
}
