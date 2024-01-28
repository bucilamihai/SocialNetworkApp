package ir.map.utils;

import java.time.format.DateTimeFormatter;

public final class Constants {

    public static final String url = "jdbc:postgresql://localhost:5432/SocialNetwork";

    public static final String username = "postgres";

    public static final String password = "postgres";

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
