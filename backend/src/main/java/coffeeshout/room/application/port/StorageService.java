package coffeeshout.room.application.port;

public interface StorageService {

    String upload(String contents, byte[] data);

    String getUrl(String storageKey);
}
