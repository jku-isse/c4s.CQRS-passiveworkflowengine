package impactassessment.artifact.base;

public interface IIssueLinkType {

    enum Direction {
        OUTBOUND,
        INBOUND
    }

    String getName();

    String getDescription();

    Direction getDirection();
}
