package ru.biosoft.util;

import java.io.IOException;
import java.io.InputStream;

public class SizeLimitInputStream extends InputStream {

    private InputStream stream;
    private long remaining;

    public SizeLimitInputStream(InputStream stream, long sizeLimit) {
        this.stream = stream;
        this.remaining = sizeLimit;
    }

    @Override
    public int read() throws IOException {
        if (remaining <= 0)
            return -1;
        remaining--;
        return stream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0)
            return len > 0 ? -1 : 0;
        if (remaining < len)
            len = (int) remaining;
        int readen = stream.read(b, off, len);
        if (readen > 0)
            remaining -= readen;
        return readen;
    }
    
    @Override
    public void close() throws IOException {
        stream.close();
    }

}
