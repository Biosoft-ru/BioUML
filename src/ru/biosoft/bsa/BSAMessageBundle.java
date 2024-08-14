package ru.biosoft.bsa;

import java.util.ListResourceBundle;

public class BSAMessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents =
    {
        {"CN_CLASS"         , "simple weight matrix"},
        {"CD_CLASS"         , "Simple weight matrix ..."},

        {"PN_NAME"              , "Matrix ID"},//get
        {"PD_NAME"              , "Matrix ID"},

        {"PN_ACCESSION"              , "Accession"},//get
        {"PD_ACCESSION"              , "Matrix accession"},

        {"PN_LENGTH"              , "Length"},//get
        {"PD_LENGTH"              , "Matrix length"},

        {"PN_CORE_START"              , "Core start"},//get
        {"PD_CORE_START"              , "Start of core"},

        {"PN_CORE_LENGTH"              , "Core length"},//get
        {"PD_CORE_LENGTH"              , "Length of core"},

        {"PN_MIN_WEIGHT"              , "Min weight"},//get
        {"PD_MIN_WEIGHT"              , "Minimal matrix weight"},

        {"PN_MAX_WEIGHT"              , "Max weight"},//get
        {"PD_MAX_WEIGHT"              , "Maximal matrix weight"},

        {"PN_BINDINGELEMENT"          , "Transcription factor"},
        {"PD_BINDINGELEMENT"          , "Matrix transcription factor"},

        {"PN_THRESHOLD"          , "Threshold"},
        {"PD_THRESHOLD"          , "Threshold"},

        {"PN_ENTITY_URL"              , "TF URL"},
        {"PD_ENTITY_URL"              , "TF URL..."},

        {"CN_BINDING"         , "Binding elements"},
        {"CD_BINDING"         , "Binding elements"},
        {"PN_BINDING_NAME", "Name"},
        {"PD_BINDING_NAME", "Name of the factor"},
        {"PN_BINDING_FACTORS", "Factors"},
        {"PD_BINDING_FACTORS", "Factors list"},

        {"CN_FACTOR"         , "Transcription factor"},
        {"CD_FACTOR"         , "Transcription factor"},

        {"PN_FACTOR_NAME", "Accession"},
        {"PD_FACTOR_NAME", "Accession number"},
        {"PN_FACTOR_DISPLAY_NAME", "Factor name"},
        {"PD_FACTOR_DISPLAY_NAME", "Factor name"},
        {"PN_FACTOR_TAXON", "Species or taxon"},
        {"PD_FACTOR_TAXON", "Species or taxon"},
        {"PN_FACTOR_GENERAL_CLASS", "General class"},
        {"PD_FACTOR_GENERAL_CLASS", "General class"},
        {"PN_FACTOR_BINDING_DOMAIN", "DNA binding domain"},
        {"PD_FACTOR_BINDING_DOMAIN", "DNA binding domain"},
        {"PN_FACTOR_NEGATIVE_TISSUE_SPECIFICITY", "Negative tissue specificity"},
        {"PD_FACTOR_NEGATIVE_TISSUE_SPECIFICITY", "Negative tissue specificity"},
        {"PN_FACTOR_POSITIVE_TISSUE_SPECIFICITY", "Positive tissue specificity"},
        {"PD_FACTOR_POSITIVE_TISSUE_SPECIFICITY", "Positive tissue specificity"},
        {"PN_FACTOR_REFERENCES", "References"},
        {"PD_FACTOR_REFERENCES", "References"}
        /*
        {"PN_ALPHABET"             , "start"},
        {"PD_ALPHABET"             , "Start ..."},
        */
    };
}
