package impactassessment.general;

import java.util.UUID;

public class IdGenerator {
    public static String getNewId() {
        long msb = System.currentTimeMillis();
        long lsb = System.currentTimeMillis();
        UUID uuid = new UUID(msb, lsb);
        return "WF-" + uuid;
    }
}
