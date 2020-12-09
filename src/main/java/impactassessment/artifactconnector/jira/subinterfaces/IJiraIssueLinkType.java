package impactassessment.artifactconnector.jira.subinterfaces;

public interface IJiraIssueLinkType {

    enum Direction {
        OUTBOUND,
        INBOUND
    }

    String getName();

    String getDescription();

    Direction getDirection();

}
