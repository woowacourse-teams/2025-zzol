package coffeeshout.room.domain.service;

import java.io.IOException;

public interface QrCodeGenerator {

    byte[] generate(String url) throws IOException;
}
