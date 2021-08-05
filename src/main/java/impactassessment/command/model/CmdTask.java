package impactassessment.command.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CmdTask {
    private String id;
    private List<CmdConstraint> constraints;

    public CmdTask(String id) {
        this.id = id;
        constraints = new ArrayList<>();
    }

    public boolean add(CmdConstraint cmdConstraint) {
        return getConstraints().add(cmdConstraint);
    }

    public boolean remove(CmdConstraint cmdConstraint) {
        return getConstraints().remove(cmdConstraint);
    }
}
