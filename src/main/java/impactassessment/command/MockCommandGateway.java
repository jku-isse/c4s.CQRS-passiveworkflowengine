package impactassessment.command;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import impactassessment.query.WorkflowProjection;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.common.Registration;
import org.axonframework.eventhandling.ReplayStatus;
import org.axonframework.messaging.MessageDispatchInterceptor;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import impactassessment.api.Commands.ActivateTaskCmd;
import impactassessment.api.Commands.AddConstraintsCmd;
import impactassessment.api.Commands.AddEvaluationResultToConstraintCmd;
import impactassessment.api.Commands.AddInputCmd;
import impactassessment.api.Commands.AddInputToWorkflowCmd;
import impactassessment.api.Commands.AddOutputCmd;
import impactassessment.api.Commands.AddOutputToWorkflowCmd;
import impactassessment.api.Commands.CheckAllConstraintsCmd;
import impactassessment.api.Commands.CheckConstraintCmd;
import impactassessment.api.Commands.InstantiateTaskCmd;
import impactassessment.api.Commands.SetPostConditionsFulfillmentCmd;
import impactassessment.api.Commands.SetPreConditionsFulfillmentCmd;
import impactassessment.api.Commands.SetPropertiesCmd;
import impactassessment.api.Commands.UpdateArtifactsCmd;
import impactassessment.api.Events.ActivatedTaskEvt;
import impactassessment.api.Events.AddedConstraintsEvt;
import impactassessment.api.Events.AddedEvaluationResultToConstraintEvt;
import impactassessment.api.Events.AddedInputEvt;
import impactassessment.api.Events.AddedInputToWorkflowEvt;
import impactassessment.api.Events.AddedOutputEvt;
import impactassessment.api.Events.AddedOutputToWorkflowEvt;
import impactassessment.api.Events.CheckedAllConstraintsEvt;
import impactassessment.api.Events.CheckedConstraintEvt;
import impactassessment.api.Events.InstantiatedTaskEvt;
import impactassessment.api.Events.SetPostConditionsFulfillmentEvt;
import impactassessment.api.Events.SetPreConditionsFulfillmentEvt;
import impactassessment.api.Events.SetPropertiesEvt;
import impactassessment.api.Events.UpdatedArtifactsEvt;
import impactassessment.passiveprocessengine.LazyLoadingArtifactInput;
import impactassessment.passiveprocessengine.LazyLoadingArtifactOutput;

public class MockCommandGateway implements CommandGateway {

	WorkflowProjection proj;
	IArtifactRegistry artifactRegistry;
	
	public MockCommandGateway(IArtifactRegistry artReg) {
		this.artifactRegistry = artReg;
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
		

		if (command instanceof AddConstraintsCmd) {
			AddConstraintsCmd cmd = (AddConstraintsCmd)command;
			proj.on(new AddedConstraintsEvt(cmd.getId(), cmd.getWftId(), cmd.getRules()), ReplayStatus.REGULAR);
		} else
		if (command instanceof AddEvaluationResultToConstraintCmd) {
			AddEvaluationResultToConstraintCmd cmd = (AddEvaluationResultToConstraintCmd)command;
			proj.on(new AddedEvaluationResultToConstraintEvt(cmd.getId(), cmd.getWftId(), cmd.getQacId(), cmd.getRes(), cmd.getCorr(), cmd.getTime()), ReplayStatus.REGULAR);
		} else
		if (command instanceof CheckConstraintCmd) {
			CheckConstraintCmd cmd = (CheckConstraintCmd)command;
	        proj.on(new CheckedConstraintEvt(cmd.getId(), cmd.getConstrId()));
	    } else	
		if (command instanceof CheckAllConstraintsCmd) {
			CheckAllConstraintsCmd cmd = (CheckAllConstraintsCmd) command;
			proj.on(new CheckedAllConstraintsEvt(cmd.getId()));
		} else
		if (command instanceof	AddInputCmd) {
			AddInputCmd cmd = (AddInputCmd)command;
			ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
	        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
			proj.on(new AddedInputEvt(cmd.getId(), cmd.getWftId(), opt.get().getArtifactIdentifier(), cmd.getRole()));
		} else	
		if (command instanceof AddOutputCmd) {
			AddOutputCmd cmd = (AddOutputCmd)command;
			ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
	        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
			if (opt.isPresent())
				proj.on(new AddedOutputEvt(cmd.getId(), cmd.getWftId(), ai, cmd.getRole()), ReplayStatus.REGULAR);
		} else
		if (command instanceof AddInputToWorkflowCmd) {
			AddInputToWorkflowCmd cmd = (AddInputToWorkflowCmd) command;
			ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
	        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
	        opt.ifPresent(artifact -> proj.on(new AddedInputToWorkflowEvt(cmd.getId(), artifact.getArtifactIdentifier(), cmd.getRole())));
		} else
		if (command instanceof AddOutputToWorkflowCmd) {
			AddOutputToWorkflowCmd cmd = (AddOutputToWorkflowCmd)command;
			ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
	        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
	        opt.ifPresent(artifact -> proj.on(new AddedOutputToWorkflowEvt(cmd.getId(), artifact.getArtifactIdentifier(), cmd.getRole())));
		} else
		if(command instanceof UpdateArtifactsCmd) {
			UpdateArtifactsCmd cmd = (UpdateArtifactsCmd)command;
			proj.on(new UpdatedArtifactsEvt(cmd.getId(), cmd.getArtifacts().stream().map(art -> art.getArtifactIdentifier()).collect(Collectors.toList())));
		} else
		if(command instanceof SetPreConditionsFulfillmentCmd) {
			SetPreConditionsFulfillmentCmd cmd = (SetPreConditionsFulfillmentCmd)command;
			proj.on(new SetPreConditionsFulfillmentEvt(cmd.getId(), cmd.getWftId(), cmd.isFulfilled()));
		} else
		if(command instanceof SetPostConditionsFulfillmentCmd) {
			SetPostConditionsFulfillmentCmd cmd = (SetPostConditionsFulfillmentCmd)command;
			proj.on(new SetPostConditionsFulfillmentEvt(cmd.getId(), cmd.getWftId(), cmd.isFulfilled()));
		} else
		if (command instanceof ActivateTaskCmd) {
			ActivateTaskCmd cmd = (ActivateTaskCmd)command;
			proj.on(new ActivatedTaskEvt(cmd.getId(), cmd.getWftId()));
		} else
		if (command instanceof SetPropertiesCmd) {
			SetPropertiesCmd cmd = (SetPropertiesCmd)command;
			proj.on(new SetPropertiesEvt(cmd.getId(), cmd.getIwftId(), cmd.getProperties()));
		} else 
		if (command instanceof InstantiateTaskCmd) {
			InstantiateTaskCmd cmd = (InstantiateTaskCmd)command;
			proj.on(new InstantiatedTaskEvt(cmd.getId(), cmd.getTaskDefinitionId(), 
					cmd.getOptionalInputs().stream().map(in -> LazyLoadingArtifactInput.generateFrom(in, artifactRegistry, cmd.getId())).collect(Collectors.toList())  , 
					cmd.getOptionalOutputs().stream().map(out -> LazyLoadingArtifactOutput.generateFrom(out, artifactRegistry, cmd.getId())).collect(Collectors.toList()) ));
		}
		else {
		
			System.err.println("Received unsupported command: "+command.toString());
		}
			
		return null;
	}

}
