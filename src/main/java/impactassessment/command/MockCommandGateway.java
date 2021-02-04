package impactassessment.command;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import impactassessment.query.WorkflowProjection;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.common.Registration;
import org.axonframework.eventhandling.ReplayStatus;
import org.axonframework.messaging.MessageDispatchInterceptor;

import impactassessment.api.Commands.ActivateInOutBranchCmd;
import impactassessment.api.Commands.AddConstraintsCmd;
import impactassessment.api.Commands.AddEvaluationResultToConstraintCmd;
import impactassessment.api.Commands.AddOutputCmd;
import impactassessment.api.Commands.CompleteDataflowCmd;
import impactassessment.api.Events.ActivatedInOutBranchEvt;
import impactassessment.api.Events.AddedConstraintsEvt;
import impactassessment.api.Events.AddedEvaluationResultToConstraintEvt;
import impactassessment.api.Events.AddedOutputEvt;
import impactassessment.api.Events.CompletedDataflowEvt;

public class MockCommandGateway implements CommandGateway {

	WorkflowProjection proj;
	
	public MockCommandGateway() {
		
	}
	
	public void setWorkflowProjection(WorkflowProjection proj) {
		this.proj = proj;
	}
	
	@Override
	public Registration registerDispatchInterceptor(
			MessageDispatchInterceptor<? super CommandMessage<?>> dispatchInterceptor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <C, R> void send(C command, CommandCallback<? super C, ? super R> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <R> R sendAndWait(Object command) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> R sendAndWait(Object command, long timeout, TimeUnit unit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> CompletableFuture<R> send(Object command) {
		
		if (command instanceof CompleteDataflowCmd) {
			CompleteDataflowCmd cmd = (CompleteDataflowCmd)command;
			proj.on(new CompletedDataflowEvt(cmd.getId(), cmd.getDniId(), cmd.getRes()), ReplayStatus.REGULAR);
		} else 
		if (command instanceof ActivateInOutBranchCmd) {
			ActivateInOutBranchCmd cmd = (ActivateInOutBranchCmd)command;
			proj.on(new ActivatedInOutBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getWftId(), cmd.getBranchId()), ReplayStatus.REGULAR);
		} else
		if (command instanceof AddConstraintsCmd) {
			AddConstraintsCmd cmd = (AddConstraintsCmd)command;
			proj.on(new AddedConstraintsEvt(cmd.getId(), cmd.getWftId(), cmd.getRules()), ReplayStatus.REGULAR);
		} else
		if (command instanceof AddEvaluationResultToConstraintCmd) {
			AddEvaluationResultToConstraintCmd cmd = (AddEvaluationResultToConstraintCmd)command;
			proj.on(new AddedEvaluationResultToConstraintEvt(cmd.getId(), cmd.getQacId(), cmd.getRes(), cmd.getCorr(), cmd.getTime()), ReplayStatus.REGULAR);
		} 
		if (command instanceof AddOutputCmd) {
			AddOutputCmd cmd = (AddOutputCmd)command;
			proj.on(new AddedOutputEvt(cmd.getId(), cmd.getWftId(), cmd.getArtifact(), cmd.getRole(), cmd.getType()), ReplayStatus.REGULAR);
		}
		else {
			System.err.println("Received unsupported command: "+command.toString());
		}
			
		return null;
	}

}
