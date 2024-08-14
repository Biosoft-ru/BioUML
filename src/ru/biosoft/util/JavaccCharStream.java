package ru.biosoft.util;

import java.io.IOException;

public interface JavaccCharStream
{

    int getBeginLine();

    int getBeginColumn();

    String GetImage();

    int getEndLine();

    int getEndColumn();

    char BeginToken() throws IOException;

    void backup(int i);

    char readChar() throws IOException;

}
