package impactassessment.passiveprocessengine.definition;

import impactassessment.passiveprocessengine.instance.WorkflowInstance;

import java.util.UUID;

public class DefaultWorkflowDefinition extends AbstractWorkflowDefinition{
		
		@Deprecated
		public DefaultWorkflowDefinition(){}
		
		public DefaultWorkflowDefinition(String id) {
			super(id);
		}

		@Override
		public WorkflowInstance createInstance(String withOptionalId) {
			String wfid = withOptionalId != null ? withOptionalId : this.getId()+"#"+UUID.randomUUID().toString();
			WorkflowInstance wfi = new WorkflowInstance(wfid, this, pub);

			return wfi;
		}

}
