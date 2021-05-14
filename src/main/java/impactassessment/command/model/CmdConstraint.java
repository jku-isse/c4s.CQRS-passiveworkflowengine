package impactassessment.command.model;

import artifactapi.ResourceLink;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class CmdConstraint {
    private String id;
    private List<String> fulfilled;
    private List<String> unsatisfied;

    public CmdConstraint(String id) {
        this.id = id;
        fulfilled = new ArrayList<>();
        unsatisfied = new ArrayList<>();
    }

    public boolean hasChanged(Map<ResourceLink, Boolean> evaluationResult) {
        if (fulfilled.isEmpty() && unsatisfied.isEmpty()) return true;
        boolean fulfilledChanged = evaluationResult.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(entry -> entry.getKey().getId())
                .anyMatch(resId -> !fulfilled.contains(resId));
        boolean unsatisfiedChanged = evaluationResult.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(entry -> entry.getKey().getId())
                .anyMatch(resId -> !unsatisfied.contains(resId));
        return fulfilledChanged || unsatisfiedChanged;
    }

    public void update(Map<ResourceLink, Boolean> evaluationResult) {
        evaluationResult.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(entry -> entry.getKey().getId())
                .filter(resId -> !fulfilled.contains(resId))
                .forEach(resId -> fulfilled.add(resId));
        evaluationResult.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(entry -> entry.getKey().getId())
                .filter(resId -> !unsatisfied.contains(resId))
                .forEach(resId -> unsatisfied.add(resId));
    }
}
