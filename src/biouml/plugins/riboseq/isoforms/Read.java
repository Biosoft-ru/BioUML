package biouml.plugins.riboseq.isoforms;

import ru.biosoft.bsa.Nucleotide5LetterAlphabet;

public class Read
{
    public byte[] seq;
    public Read(String str)
    {
        this(str.getBytes());
    }
    
    public Read(byte[] letters) {
        seq = new byte[letters.length];
        Nucleotide5LetterAlphabet alphabet = Nucleotide5LetterAlphabet.getInstance();
        for(int i = 0; i < letters.length; i++)
        {
            byte code = alphabet.lettersToCode( letters, i );
            if(code == Nucleotide5LetterAlphabet.ERROR_CHAR)
                throw new IllegalArgumentException("Invalid character '" + ((char)letters[i]) + "' in read sequence");
            seq[i] = code;
        }
    }
    
    public static enum Type
    {
        UNMAPPED,
        MAPPED,
        FILTERED //filtered due to too many alignments
    }
}