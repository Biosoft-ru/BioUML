
package ru.biosoft.bsa;

public class ErrorLetterException extends RuntimeException
{
    public ErrorLetterException(char letter, int seqPosition)
    {
        super("Error letter \'" + letter +
              "\' in sequence position " + (seqPosition+1) + ".");
    }

    public ErrorLetterException(char letter, int seqPosition, int line, int position)
    {
        super("Error letter \'" + letter +
              "\' in sequence position " + (seqPosition+1) +
              "; line " + line + ", position " + (position+1) + ".");
    }
}
