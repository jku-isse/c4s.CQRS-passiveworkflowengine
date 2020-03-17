package impactassessment.neo4j;

import java.util.List;

import impactassessment.workflowmodel.AbstractArtifact;
import impactassessment.workflowmodel.ArtifactType;
import impactassessment.workflowmodel.DecisionNodeDefinition;
import impactassessment.workflowmodel.DecisionNodeInstance;
import impactassessment.workflowmodel.DefaultWorkflowDefinition;
import impactassessment.workflowmodel.TaskDefinition;
import impactassessment.workflowmodel.WorkflowInstance;
import impactassessment.workflowmodel.WorkflowTask;

public class BasicServices {

	public interface TaskDefinitionService extends IPersistable<TaskDefinition>{}
	public static interface ArtifactTypeService extends IPersistable<ArtifactType> {}
	public static interface WorkflowDefinitionService extends IPersistable<DefaultWorkflowDefinition> {}
	
	public static interface WorkflowInstanceService extends IPersistable<WorkflowInstance> {
		public void deleteAllAbstractWorkflowInstanceObjectsByWorkflowInstanceId(String workflowInstanceId);
		public void deleteWorkflowInstanceViaQuery(String workflowInstanceId);
	}
	public static interface WorkflowTaskService extends IPersistable<WorkflowTask> {
		public List<WorkflowTask> deleteDetachedPlaceHolders();
	}
	public static interface DecisionNodeInstanceService extends IPersistable<DecisionNodeInstance> {}
	public static interface DecisionNodeDefinitionService extends IPersistable<DecisionNodeDefinition> {}
	public static interface ArtifactService extends IPersistable<AbstractArtifact>{
		public List<AbstractArtifact> deleteArtifactsByWorkflowInstanceId(String workflowInstanceId);
	}
	
}
