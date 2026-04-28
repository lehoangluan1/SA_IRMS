package SA.irms.common.context;

public record RequestMetadata(
        String remoteIp,
        String userAgent,
        String requestId,
        String locale
) {
    public static RequestMetadata empty() {
        return new RequestMetadata(null, null, null, null);
    }
}
