package impactassessment.neo4j;

import java.util.List;

import impactassessment.passiveprocessengine.workflowmodel.AbstractArtifact;
import impactassessment.passiveprocessengine.workflowmodel.ArtifactType;
import impactassessment.passiveprocessengine.workflowmodel.DecisionNodeDefinition;
import impactassessment.passiveprocessengine.workflowmodel.DecisionNodeInstance;
import impactassessment.passiveprocessengine.workflowmodel.DefaultWorkflowDefinition;
import impactassessment.passiveprocessengine.workflowmodel.TaskDefinition;
import impactassessment.passiveprocessengine.workflowmodel.WorkflowInstance;
import impactassessment.passiveprocessengine.workflowmodel.WorkflowTask;

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
