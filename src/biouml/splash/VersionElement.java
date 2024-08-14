package biouml.splash;

public enum VersionElement
{
    V_ONE ( Constants.ONE ),
    V_TWO ( Constants.TWO ),
    V_THREE ( Constants.THREE ),
    V_FOUR ( Constants.FOUR ),
    V_FIVE ( Constants.FIVE ),
    V_SIX ( Constants.SIX ),
    V_SEVEN ( Constants.SEVEN ),
    V_EIGHT ( Constants.EIGHT ),
    V_NINE ( Constants.NINE ),
    V_ZERO ( Constants.ZERO ),
    V_DOT ( Constants.DOT );

    private char[][] representation;
    private int reprShift;
    VersionElement(char[][] repr)
    {
        this.representation = repr;
        this.reprShift = repr[0].length;
    }

    public int getShift()
    {
        return reprShift;
    }
    public int addElement(char[][] array, int shift)
    {
        //TODO: validate in a better way
        if( array.length == 0 || array[0].length < shift || array[0].length < shift + reprShift )
            return shift;
        for( int i = 0; i < representation.length; i++ )
            for( int j = 0; j < reprShift; j++ )
                array[i][j + shift] = representation[i][j];

        return shift + reprShift;
    }
}
