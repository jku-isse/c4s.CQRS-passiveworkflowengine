package impactassessment.jiraartifact;

public interface IJiraIssueLinkType {

    enum Direction {
        OUTBOUND,
        INBOUND
    }

    String getName();

    String getDescription();

    Direction getDirection();

}
