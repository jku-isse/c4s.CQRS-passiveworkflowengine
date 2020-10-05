package impactassessment.registry;

public interface IRegisterService {

    boolean register(String workflowName);

    int registerAll();
}
