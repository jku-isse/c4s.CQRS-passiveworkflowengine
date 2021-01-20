package impactassessment.evaluation;

import impactassessment.api.Commands;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static impactassessment.general.IdGenerator.getNewId;

@Service
@RequiredArgsConstructor
public class JamaWorkflowCreationPerformanceService {

    private final CommandGateway commandGateway;
    // 14464163
    private List<Integer> wpIds = List.of(14494337,14500058,14500947,14619816,14619817,14619818,14624079,14624080,14624081
        ,15079383,15104162,15104163,15128680,15178841,15178842,15178843,15178845,15178846,15178847,15408422,15539532
        ,15539533,15540718,15540719,15540720,15540721,15550132,15550133,15550134,15551554,15551555,15551556,11539214
        ,11539215,11539217,11591733,13348823,13348824,13373299,13373300,13373301,13422794,14359309,14360397
        ,14464163,14464164,14464165,14464166);

    public void createAll() {
        for (Integer id : wpIds) {
            Map<String, String> input = new HashMap<>();
            input.put(String.valueOf(id), "JAMA");
            commandGateway.send(new Commands.CreateWorkflowCmd(getNewId(), input, "JAMA_PERFORMANCE"));
        }
    }
}
