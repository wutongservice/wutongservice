package com.borqs.server.market.sfs;


import java.io.IOException;

public class ProxyStorage implements FileStorage {
    private FileStorage storage;

    public ProxyStorage() {
    }

    public ProxyStorage(FileStorage storage) {
        this.storage = storage;
    }

    public FileStorage getStorage() {
        return storage;
    }

    public void setStorage(FileStorage storage) {
        this.storage = storage;
    }

    @Override
    public void init() {
        storage.init();
    }

    @Override
    public String write(String fileId, FileContent content) throws IOException {
        return storage.write(fileId, content);
    }

    @Override
    public FileContent read(String fileId) throws IOException {
        return storage.read(fileId);
    }
}
