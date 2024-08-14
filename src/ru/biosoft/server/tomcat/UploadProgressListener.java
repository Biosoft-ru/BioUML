/**
 * 
 */
package ru.biosoft.server.tomcat;

import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.ProgressListener;

public class UploadProgressListener implements ProgressListener
{
    long lastUpdate = System.currentTimeMillis();
    long updateInterval = 100; // ms
    Object servlet;
    Method uploadListener;
    HttpServletRequest req;
    Map params;
    
    public UploadProgressListener(Object servlet, HttpServletRequest req, Map params) throws Exception
    {
        this.servlet = servlet;
        this.uploadListener = servlet.getClass().getMethod("uploadListener",
                new Class<?>[] {Map.class, Object.class, Long.class, Long.class});
        this.req = req;
        this.params = params;
    }
    
    @Override
    public void update(long pBytesRead, long pContentLength, int pItems)
    {
        long curTime = System.currentTimeMillis();
        if(curTime-lastUpdate > updateInterval)
        {
            lastUpdate = curTime;
            try {
                uploadListener.invoke(servlet, new Object[] {
                        params, req.getSession(), pBytesRead, pContentLength
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}