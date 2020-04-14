package impactassessment.neo4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import impactassessment.neo4j.BasicServices.ArtifactTypeService;
import impactassessment.neo4j.BasicServices.ArtifactService;
import impactassessment.neo4j.BasicServices.DecisionNodeDefinitionService;
import impactassessment.neo4j.BasicServices.DecisionNodeInstanceService;
import impactassessment.neo4j.BasicServices.TaskDefinitionService;
import impactassessment.neo4j.BasicServices.WorkflowDefinitionService;
import impactassessment.neo4j.BasicServices.WorkflowInstanceService;
import impactassessment.neo4j.BasicServices.WorkflowTaskService;
import impactassessment.model.workflowmodel.AbstractArtifact;
import impactassessment.model.workflowmodel.AbstractWorkflowInstanceObject;
import impactassessment.model.workflowmodel.ArtifactType;
import impactassessment.model.workflowmodel.DecisionNodeDefinition;
import impactassessment.model.workflowmodel.DecisionNodeInstance;
import impactassessment.model.workflowmodel.DefaultWorkflowDefinition;
import impactassessment.model.workflowmodel.TaskDefinition;
import impactassessment.model.workflowmodel.TaskStateTransitionEventPublisher;
import impactassessment.model.workflowmodel.WorkflowDefinition;
import impactassessment.model.workflowmodel.WorkflowInstance;
import impactassessment.model.workflowmodel.WorkflowTask;

public class BasicServiceImpl {

	public static class ArtifactTypeServiceImpl extends Persistable<ArtifactType> implements ArtifactTypeService {
		
		@Override
		Class<ArtifactType> getEntityType() {
			return ArtifactType.class;
		}		
	}
	
	public static class ArtifactServiceImpl extends Persistable<AbstractArtifact> implements ArtifactService {

		public ArtifactServiceImpl() {
			DEPTH_ENTITY = 2;
		}
		
		
		@Override
		Class<AbstractArtifact> getEntityType() {
			return AbstractArtifact.class;
		}


		@Override
		public List<AbstractArtifact> deleteArtifactsByWorkflowInstanceId(String workflowInstanceId) {
//			Filter filterByWFI = new Filter("wfi", ComparisonOperator.EQUALS, workflowInstance);
//			Iterable<AbstractArtifact> iter = super.getSession().loadAll(AbstractArtifact.class, filterByWFI);
//			List<AbstractArtifact> deletedWFArts = new ArrayList<AbstractArtifact>();
//			iter.forEach(wft -> { 	super.getSession().delete(wft);
//									deletedWFArts.add(wft);		
//								} );
//			return deletedWFArts;
			List<AbstractArtifact> deletedWFArts = new ArrayList<AbstractArtifact>();
			String query = "MATCH (aa:AbstractArtifact) WHERE aa.wfi = $wfid DETACH DELETE aa";	
			HashMap<String,String> paraMap = new HashMap<String, String>();
			paraMap.put("wfid", workflowInstanceId);
			Iterable<AbstractArtifact> iter = getSession().query(AbstractArtifact.class, query, paraMap);
			iter.forEach(aa -> deletedWFArts.add(aa));
			return deletedWFArts; // will never return something as query doesnt return anything, even when adding RETURN aa 
		}
		
		
		
	}
	
	public static class WorkflowDefinitionServiceImpl extends Persistable<DefaultWorkflowDefinition> implements WorkflowDefinitionService {
		
		@Inject TaskStateTransitionEventPublisher pub;
		
		public WorkflowDefinitionServiceImpl() {
			DEPTH_ENTITY = 2;
		}
		
		@Override
		public DefaultWorkflowDefinition find(String id) {
			DefaultWorkflowDefinition wfd = super.find(id);
			if (wfd != null) {
				wfd.setTaskStateTransitionEventPublisher(pub); 
			}
			return wfd;
		}



		@Override
		Class<DefaultWorkflowDefinition> getEntityType() {
			return DefaultWorkflowDefinition.class;
		}		
	}
	
	public static class WorkflowInstanceServiceImpl extends Persistable<WorkflowInstance> implements WorkflowInstanceService {
		
		@Inject TaskStateTransitionEventPublisher pub;
		@Inject WorkflowDefinitionService wfdService;
		
		public WorkflowInstanceServiceImpl() {
			DEPTH_ENTITY = 4;
		}
		
		@Override
		Class<WorkflowInstance> getEntityType() {
			return WorkflowInstance.class;
		}

		@Override
		public WorkflowInstance find(String id) {
			WorkflowInstance wfi = super.find(id);
			if (wfi != null) {
				WorkflowDefinition wfd = wfi.getWorkflowDefinition();
				if (wfd != null) {
					wfd = wfdService.find(wfd.getId());
				}
				wfi.initAfterPersistance(pub, wfd);
				
			}
			return wfi;
		}

		@Override
		public void deleteAllAbstractWorkflowInstanceObjectsByWorkflowInstanceId(String workflowInstanceId) {
			String query = "MATCH (aa:AbstractWorkflowInstanceObject) WHERE aa.wfi = $wfid DETACH DELETE aa";	
			HashMap<String,String> paraMap = new HashMap<String, String>();
			paraMap.put("wfid", workflowInstanceId);
			getSession().query(AbstractWorkflowInstanceObject.class, query, paraMap);
		}

		@Override
		public void deleteWorkflowInstanceViaQuery(String workflowInstanceId) {
			String query = "MATCH (aa:WorkflowInstance) WHERE aa.id = $wfid DETACH DELETE aa";	
			HashMap<String,String> paraMap = new HashMap<String, String>();
			paraMap.put("wfid", workflowInstanceId);
			getSession().query(WorkflowInstance.class, query, paraMap);
		}		
		
		
	}
	
	
	
	public static class WorkflowTaskServiceImpl extends Persistable<WorkflowTask> implements WorkflowTaskService {
		@Override
		Class<WorkflowTask> getEntityType() {
			return WorkflowTask.class;
		}

		@Override
		public List<WorkflowTask> deleteDetachedPlaceHolders() {
			String query = "MATCH (pht:PlaceHolderTask) OPTIONAL MATCH ()-[r:BRANCH_INSTANCE]->(pht) with pht, r where r is null RETURN pht";		
			Iterable<WorkflowTask> iter = super.getSession().query(WorkflowTask.class, query, Collections.emptyMap());
			List<WorkflowTask> deletedWFTs = new ArrayList<WorkflowTask>();
			iter.forEach(wft -> { 	super.getSession().delete(wft);
									deletedWFTs.add(wft);		
								} );
			return deletedWFTs;
		}		
	}
	
	public static class  DecisionNodeInstanceServiceImpl extends Persistable< DecisionNodeInstance> implements  DecisionNodeInstanceService {
		@Override
		Class< DecisionNodeInstance> getEntityType() {
			return  DecisionNodeInstance.class;
		}		
	}
	
	public static class DecisionNodeDefinitionServiceImpl extends Persistable<DecisionNodeDefinition> implements DecisionNodeDefinitionService {
		@Override
		Class<DecisionNodeDefinition> getEntityType() {
			return DecisionNodeDefinition.class;
		}
	}
	
	public static class TaskDefinitionServiceImpl extends Persistable<TaskDefinition> implements TaskDefinitionService{

		@Override
		Class<TaskDefinition> getEntityType() {
			return TaskDefinition.class;
		}

	}
}
