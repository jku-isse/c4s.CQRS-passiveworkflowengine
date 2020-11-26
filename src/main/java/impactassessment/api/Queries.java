package impactassessment.api;

import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import lombok.Data;

import java.util.List;

public class Queries {
    // QUERIES
    @Data
    public static class GetStateQuery {
        private final int depth;
    }
    @Data
    public static class PrintKBQuery {
        private final String id;
    }

    // QUERY-RESPONSES
    @Data
    public static class GetStateResponse {
        private final List<WorkflowInstanceWrapper> state;
    }
    @Data
    public static class PrintKBResponse {
        private final String kbString;
    }
}
