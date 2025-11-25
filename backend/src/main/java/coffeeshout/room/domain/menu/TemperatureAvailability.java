package coffeeshout.room.domain.menu;

import java.util.Arrays;

public enum TemperatureAvailability {
    HOT_ONLY,
    ICE_ONLY,
    BOTH;

    public static TemperatureAvailability from(String temperatureAvailability) {
        return Arrays.stream(values())
                .filter(it -> it.name().equals(temperatureAvailability))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 타입입니다."));
    }
}
