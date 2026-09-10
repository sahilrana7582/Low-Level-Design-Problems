public final class ExpireKey {

    private final String key;
    private final long expireAt;
    private final long version;

    public ExpireKey(String key, long expireAt, long version) {
        this.key = key;
        this.expireAt = expireAt;
        this.version = version;
    }

    public String getKey() {
        return key;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public long getVersion() {
        return version;
    }

    public boolean isExpired() {
        return expireAt != 0 &&
                expireAt <= System.currentTimeMillis();
    }
}