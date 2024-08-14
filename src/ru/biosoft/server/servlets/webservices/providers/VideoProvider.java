package ru.biosoft.server.servlets.webservices.providers;

import java.io.DataOutputStream;
import java.io.File;
import java.io.RandomAccessFile;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import ru.biosoft.access.VideoDataElement;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;

public class VideoProvider extends WebProviderSupport
{

    @Override
    public void process(BiosoftWebRequest arguments, BiosoftWebResponse resp) throws Exception
    {
        String action = arguments.getAction();
        VideoDataElement de = arguments.getDataElement(VideoDataElement.class);
        File deFile = de.getFile();
        if ( "format".equals(action) )
        {
            JSONObject result = new JSONObject();
            result.put("format", de.getFormat());
            result.put("description", de.getDescription());
            result.put("width", de.getWidthStr());
            result.put("height", de.getHeightStr());
            new JSONResponse(resp).sendJSON(result);
        }
        else
        {
            String range = arguments.getString("Range");
            RandomAccessFile file = new RandomAccessFile(deFile, "r");
            if ( range != null )
            {
                range = range.trim().substring("bytes=".length());
                String[] index = range.split("-");
                byte[] buffer = new byte[2048];
                DataOutputStream dos = new DataOutputStream(resp.getOutputStream());
                long start = Long.parseLong(index[0]);
                long end = index.length > 1 ? Long.parseLong(index[1]) : Math.min(file.length() - 1, start + 1024 * 1024);
                long contentLength = end - start + 1;
                if ( start < file.length() && end < file.length() && end > start )
                {
                    resp.setContentType("video/" + de.getFormat());
                    resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                    resp.setHeader("Accept-Ranges", "bytes");
                    resp.setContentLengthLong(contentLength);
                    resp.setHeader("Content-Range", new StringBuilder("bytes ").append(start).append("-").append(end).append("/").append(file.length()).toString());
                    file.seek(start);
                    long bytesleft = contentLength;
                    while ( file.getFilePointer() <= end )
                    {
                        if ( bytesleft >= buffer.length )
                        {
                            file.readFully(buffer);
                            dos.write(buffer);
                            dos.flush();
                            bytesleft = bytesleft - buffer.length;
                        }
                        else
                        {
                            file.readFully(buffer, 0, (int) bytesleft);
                            dos.write(buffer, 0, (int) bytesleft);
                            dos.flush();
                        }
                    }
                }
                else
                {
                    resp.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                }
            }
            file.close();
        }

    }

}
