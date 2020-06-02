package impactassessment.analytics;

public class CorrelationTuple {
    String correlationId;
    String correlationObjectType;
    public String getCorrelationId() {
        return correlationId;
    }
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    public String getCorrelationObjectType() {
        return correlationObjectType;
    }
    public void setCorrelationObjectType(String correlationObjectType) {
        this.correlationObjectType = correlationObjectType;
    }
    public CorrelationTuple(String correlationId, String correlationObjectType) {
        super();
        this.correlationId = correlationId;
        this.correlationObjectType = correlationObjectType;
    }

    public CorrelationTuple(){}

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CorrelationTuple{");
        sb.append("correlationId='").append(correlationId).append('\'');
        sb.append(", correlationObjectType='").append(correlationObjectType).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
