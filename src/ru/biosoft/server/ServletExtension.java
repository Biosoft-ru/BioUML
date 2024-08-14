package ru.biosoft.server;

import java.io.OutputStream;
import java.util.Map;

public interface ServletExtension
{
    public void init(String[] args) throws Exception;
    public String service(String localAddress, Object session, Map params, OutputStream out, Map<String, String> header);
}
