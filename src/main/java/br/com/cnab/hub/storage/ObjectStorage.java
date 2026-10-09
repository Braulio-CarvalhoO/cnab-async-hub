package br.com.cnab.hub.storage;

public interface ObjectStorage {

    void ensureBuckets();

    void upload(String bucket, String key, byte[] content, String contentType);

    byte[] download(String bucket, String key);
}
