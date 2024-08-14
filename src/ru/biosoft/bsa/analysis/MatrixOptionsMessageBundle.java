package ru.biosoft.bsa.analysis;

import java.util.ListResourceBundle;

public class MatrixOptionsMessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents =
    {
        {"CN_CLASS"                         , "matrix options"},
        {"CD_CLASS"                         , "Weight matrix options"},

        {"PN_NAME"                          , "matrixID"},
        {"PD_NAME"                          , "Matrix ID"},

        {"PN_CUTOFF"                        , "cutOff"},
        {"PD_CUTOFF"                        , "Matrix cutoff"},

        {"PN_CORECUTOFF"                    , "core cutOff"},
        {"PD_CORECUTOFF"                    , "Matrix core cutoff"},

        {"PN_MATRIX"                        , "matrix"},
        {"PD_MATRIX"                        , "Weight matrix"},

        {"PN_MATRIXFREQUENCY"               , "matrix frequency"       },
        {"PD_MATRIXFREQUENCY"               , "Weight matrix frequency"},

        {"PN_BINDINGELEMENT"                , "transcription factor"         },
        {"PD_BINDINGELEMENT"                , "matrix Transcription Factor"  },

        {"PN_LIBRARY"                       , "library name"         },
        {"PD_LIBRARY"                       , "Matrix library name"  },

    };
}
