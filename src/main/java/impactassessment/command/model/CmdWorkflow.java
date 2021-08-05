package impactassessment.command.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Data
public class CmdWorkflow {
    private String id;
    private List<CmdTask> tasks;

    public CmdWorkflow(String id) {
        this.id = id;
        tasks = new ArrayList<>();
    }

    public boolean add(CmdTask cmdTask) {
        return getTasks().add(cmdTask);
    }

    public boolean remove(CmdTask cmdTask) {
        return getTasks().remove(cmdTask);
    }

    public Optional<CmdTask> getTask(String id) {
        return tasks.stream()
                .filter(task -> task.getId().equals(id))
                .findAny();
    }

    public Optional<CmdConstraint> getConstraint(String wftId, String constrId) {
        return tasks.stream()
                .filter(task -> task.getId().equals(wftId))
                .map(CmdTask::getConstraints)
                .flatMap(Collection::stream)
                .filter(constr -> constr.getId().equals(constrId))
                .findAny();
    }
}
