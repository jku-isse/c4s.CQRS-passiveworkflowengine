package impactassessment.passiveprocessengine.workflowmodel;

import java.util.UUID;

//import org.kie.api.runtime.KieSession;

public class DefaultWorkflowDefinition extends AbstractWorkflowDefinition{
		
		@Deprecated
		public DefaultWorkflowDefinition(){}
		
		public DefaultWorkflowDefinition(String id) {
			super(id);
		}

		@Override
		public WorkflowInstance createInstance(String withOptionalId) {
			String wfid = withOptionalId != null ? withOptionalId : this.getId()+"#"+UUID.randomUUID().toString();
			WorkflowInstance wfi = new WorkflowInstance(wfid, this, null); //FIXME: use actual EventPublisher instead of null
			
			//if (intoOptionalKSession != null)
			//	intoOptionalKSession.insert(wfi);
			return wfi;
		}

}
