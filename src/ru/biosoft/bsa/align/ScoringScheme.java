package ru.biosoft.bsa.align;

import ru.biosoft.bsa.align.Alignment.Element;

public interface ScoringScheme
{
    double getScore(byte[] seq1, int pos1, byte[] seq2, int pos2, Element elem);
    double getMaxScore(int seqLen);
}