package mx.trazable.gps.core.config;

import java.util.Locale;

public final class PlateUtils {

    private PlateUtils() {}

    public static String normalize(String plate) {
        if (plate == null) return null;

        return plate.replaceAll("[\\s-]", "")
                .toUpperCase(Locale.ROOT);
    }
}