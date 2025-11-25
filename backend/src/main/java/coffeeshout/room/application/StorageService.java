package coffeeshout.room.application;

public interface StorageService {

    String upload(String contents, byte[] data);

    String getUrl(String storageKey);
}
