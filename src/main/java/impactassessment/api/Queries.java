package impactassessment.api;

import lombok.Data;
import passiveprocessengine.instance.WorkflowInstance;

import java.util.Collection;

public class Queries {
    // QUERIES
    @Data
    public static class GetStateQuery {
        private final String id;
    }
    @Data
    public static class PrintKBQuery {
        private final String id;
    }

    // QUERY-RESPONSES
    @Data
    public static class GetStateResponse {
        private final Collection<WorkflowInstance> state;
    }
    @Data
    public static class PrintKBResponse {
        private final String kbString;
    }
}
