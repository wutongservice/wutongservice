package com.borqs.server.market.sfs;


import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface FileStorage {

    void init();

    String write(String fileId, FileContent content) throws IOException;

    FileContent read(String fileId) throws IOException;
}
