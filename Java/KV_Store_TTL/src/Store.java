import java.time.Duration;

public interface Store {
    int put(String key, String value);
    int put(String key, String value, Duration ttl);
    String get(String key);
    int delete(String key);
}
