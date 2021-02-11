package impactassessment.general;

import java.security.SecureRandom;
import java.util.UUID;

public class IdGenerator {
    private static volatile SecureRandom ng;
    public static String getNewId() {
        if (ng == null) {
            ng = new SecureRandom();
        }
        long lsb = System.currentTimeMillis();
        long msb = ng.nextLong();
        UUID uuid = new UUID(msb, lsb);
        return "WF-" + uuid;
    }
}
