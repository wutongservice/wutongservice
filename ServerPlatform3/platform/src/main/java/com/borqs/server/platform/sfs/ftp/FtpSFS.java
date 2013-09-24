package com.borqs.server.platform.sfs.ftp;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sfs.AbstractSFS;
import com.borqs.server.platform.util.NetAddress;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;


import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;


public class FtpSFS extends AbstractSFS {

    private static final Logger L = Logger.get(FtpSFS.class);

    private String server;
    private String username;
    private String password;
    private String root = "";

    public FtpSFS() {
    }

    public FtpSFS(String ftpRoot) {
        setFtpRoot(ftpRoot);
    }

    public FtpSFS(String server, String username, String password, String root) {
        setServer(server);
        setUsername(username);
        setPassword(password);
        setRoot(root);
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public void setFtpRoot(String ftpRoot) {
        URL url;
        try {
            url = new URL(ftpRoot);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Parse ftp url error " + ftpRoot);
        }

        if (!"ftp".equalsIgnoreCase(url.getProtocol()))
            throw new IllegalArgumentException();

        setServer(url.getHost() + ":" + (url.getPort() < 0 ? 21 : url.getPort()));
        String userInfo = url.getUserInfo();
        setUsername(StringUtils.substringBefore(userInfo, ":"));
        setPassword(StringUtils.substringAfter(userInfo, ":"));
        setRoot(StringUtils.removeStart(url.getPath(), "/"));
    }

    public String getFtpRoot() {
        if (StringUtils.isNotEmpty(username)) {
            return String.format("ftp://%s:%s@%s/%s",
                    username, ObjectUtils.toString(password), ObjectUtils.toString(server), ObjectUtils.toString(root));
        } else {
            return String.format("ftp://anonymous@%s/%s", ObjectUtils.toString(server), ObjectUtils.toString(root));
        }
    }

    private <T> T openFtp(FTPHandler<T> handler) throws IOException {
        NetAddress addr = NetAddress.parse(server);
        FTPClient ftp = null;
        try {
            ftp = new FTPClient();
            ftp.connect(addr.host, addr.port);
            if (StringUtils.isNotEmpty(username)) {
                ftp.login(username, ObjectUtils.toString(password));
            } else {
                ftp.login("anonymous", "");
            }
            int r = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(r))
                throw new ServerException(E.SFS, "Open ftp error");

            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            return handler.handle(ftp);
        } finally {
            if (ftp != null) {
                try {
                    ftp.logout();
                    ftp.disconnect();
                } catch (IOException ignored) {
                }
            }
        }
    }

    protected String getAbsolutePath(String name) {
        return StringUtils.isNotEmpty(name) ? root + "/" + name : name;
    }

    protected String checkName(String name) {
        if (StringUtils.isEmpty(name))
            throw new ServerException(E.SFS, "Invalid name '%s'", name);
        return name;
    }

    @Override
    public boolean exists(String name) {
        final String path = getAbsolutePath(checkName(name));
        try {
            return openFtp(new FTPHandler<Boolean>() {
                @Override
                public Boolean handle(FTPClient ftp) throws IOException {
                    String[] a = ftp.listNames(path);
                    return ArrayUtils.isNotEmpty(a);
                }
            });
        } catch (IOException e) {
            throw new ServerException(E.SFS, e);
        }
    }

    @Override
    public InputStream read(final String name) throws IOException {
        final String path = getAbsolutePath(checkName(name));
        return openFtp(new FTPHandler<InputStream>() {
            @Override
            public InputStream handle(FTPClient ftp) throws IOException {
                L.debug(null, "FTP: read %s <= %s", name, path);
                return ftp.retrieveFileStream(path);
            }
        });
    }

    @Override
    public void write(final String name, final InputStream in) throws IOException {
        final String path = getAbsolutePath(checkName(name));
        try {
            openFtp(new FTPHandler<Object>() {
                @Override
                public Object handle(FTPClient ftp) throws IOException {
                    L.debug(null, "FTP: write %s => %s", name, path);
                    ftp.storeFile(path, in);
                    return null;
                }
            });
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public void delete(final String name) throws IOException {
        final String path = getAbsolutePath(checkName(name));
        openFtp(new FTPHandler<Object>() {
            @Override
            public Object handle(FTPClient ftp) throws IOException {
                L.debug(null, "FTP: delete %s => %s", name, path);
                ftp.deleteFile(path);
                return null;
            }
        });
    }

    private static interface FTPHandler<T> {
        T handle(FTPClient ftp) throws IOException;
    }
}
