public final class DataValue {

    private final String key;
    private final String value;
    private final long version;
    private final long expireAt;

    public DataValue(String key, String value, long version, long expireAt) {
        this.key = key;
        this.value = value;
        this.version = version;
        this.expireAt = expireAt;
    }

    public String getKey() {
        return key;
    }

    public long getVersion() {
        return version;
    }

    public String getValue() {
        return value;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public boolean isExpired() {
        return expireAt != 0 && expireAt <= System.currentTimeMillis();
    }

}