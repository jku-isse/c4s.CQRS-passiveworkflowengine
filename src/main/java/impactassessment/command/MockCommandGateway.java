package impactassessment.command;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import impactassessment.query.WorkflowProjection;
import impactassessment.registry.WorkflowDefinitionContainer;
import impactassessment.registry.WorkflowDefinitionRegistry;

import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandHandler;
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
import impactassessment.api.Commands.ChangeCanceledStateOfTaskCmd;
import impactassessment.api.Commands.CheckAllConstraintsCmd;
import impactassessment.api.Commands.CheckConstraintCmd;
import impactassessment.api.Commands.CreateWorkflowCmd;
import impactassessment.api.Commands.InstantiateTaskCmd;
import impactassessment.api.Commands.RemoveInputCmd;
import impactassessment.api.Commands.RemoveOutputCmd;
import impactassessment.api.Commands.SetPostConditionsFulfillmentCmd;
import impactassessment.api.Commands.SetPreConditionsFulfillmentCmd;
import impactassessment.api.Commands.SetPropertiesCmd;
import impactassessment.api.Commands.UpdateArtifactsCmd;
import impactassessment.api.Events.*;
import impactassessment.passiveprocessengine.LazyLoadingArtifactInput;
import impactassessment.passiveprocessengine.LazyLoadingArtifactOutput;

public class MockCommandGateway implements CommandGateway {

	WorkflowProjection proj;
	IArtifactRegistry artifactRegistry;
	WorkflowDefinitionRegistry workflowDefinitionRegistry;
	OffsetDateTime currentTime;
	
	public MockCommandGateway(IArtifactRegistry artReg, WorkflowDefinitionRegistry workflowDefinitionRegistry) {
		this.artifactRegistry = artReg;
		this.workflowDefinitionRegistry = workflowDefinitionRegistry;
	}
	
	public void setWorkflowProjection(WorkflowProjection proj) {
		this.proj = proj;
	}
	
	public void setNewCurrentTime(OffsetDateTime timestamp) {
		this.currentTime = timestamp;
	}
	
	public OffsetDateTime getCurrentTime() {
		if (this.currentTime == null)
			return OffsetDateTime.now();
		else
			return currentTime;
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
			AddedEvaluationResultToConstraintEvt evt = new AddedEvaluationResultToConstraintEvt(cmd.getId(), cmd.getWftId(), cmd.getQacId(), cmd.getRes(), cmd.getCorr(), cmd.getTime());
			evt.setTimestamp(getCurrentTime());
			proj.on(evt, ReplayStatus.REGULAR);
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
	        AddedInputEvt evt = new AddedInputEvt(cmd.getId(), cmd.getWftId(), opt.get().getArtifactIdentifier(), cmd.getRole());
	        evt.setTimestamp(getCurrentTime());
	        proj.on(evt);

		} else	
		if (command instanceof AddOutputCmd) {
			AddOutputCmd cmd = (AddOutputCmd)command;
			ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
	        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
			if (opt.isPresent()) {
				AddedOutputEvt evt = new AddedOutputEvt(cmd.getId(), cmd.getWftId(), ai, cmd.getRole());
				evt.setTimestamp(getCurrentTime());
				proj.on(evt, ReplayStatus.REGULAR);
			}
		} else
		if (command instanceof RemoveOutputCmd) {
			RemoveOutputCmd cmd = (RemoveOutputCmd)command;
			RemovedOutputEvt evt = new RemovedOutputEvt(cmd.getId(), cmd.getWftId(), cmd.getArtifactId(), cmd.getRole());
			evt.setTimestamp(getCurrentTime());
			proj.on(evt, ReplayStatus.REGULAR);
		} else
		if (command instanceof RemoveInputCmd) {
				RemoveInputCmd cmd = (RemoveInputCmd)command;
				RemovedInputEvt evt = new RemovedInputEvt(cmd.getId(), cmd.getWftId(), cmd.getArtifactId(), cmd.getRole());
				evt.setTimestamp(getCurrentTime());
				proj.on(evt, ReplayStatus.REGULAR);
		} else
		if (command instanceof AddInputToWorkflowCmd) {
			AddInputToWorkflowCmd cmd = (AddInputToWorkflowCmd) command;
			ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
	        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
	        opt.ifPresent(artifact -> { 
	        	AddedInputToWorkflowEvt evt = new AddedInputToWorkflowEvt(cmd.getId(), artifact.getArtifactIdentifier(), cmd.getRole());
	        	evt.setTimestamp(getCurrentTime());
	        	proj.on(evt);
	        	});
		} else
		if (command instanceof AddOutputToWorkflowCmd) {
			AddOutputToWorkflowCmd cmd = (AddOutputToWorkflowCmd)command;
			ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
	        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
	        opt.ifPresent(artifact -> { 
	        	AddedOutputToWorkflowEvt evt = new AddedOutputToWorkflowEvt(cmd.getId(), artifact.getArtifactIdentifier(), cmd.getRole());
	        	evt.setTimestamp(getCurrentTime());
	        	proj.on(evt); } );
		} else
		if(command instanceof UpdateArtifactsCmd) {
			UpdateArtifactsCmd cmd = (UpdateArtifactsCmd)command;
			UpdatedArtifactsEvt evt = new UpdatedArtifactsEvt(cmd.getId(), cmd.getArtifacts().stream().map(art -> art.getArtifactIdentifier()).collect(Collectors.toList()));
			evt.setTimestamp(getCurrentTime());
			proj.on(evt);
		} else
		if(command instanceof SetPreConditionsFulfillmentCmd) {
			SetPreConditionsFulfillmentCmd cmd = (SetPreConditionsFulfillmentCmd)command;
			SetPreConditionsFulfillmentEvt evt = new SetPreConditionsFulfillmentEvt(cmd.getId(), cmd.getWftId(), cmd.isFulfilled());
			evt.setTimestamp(getCurrentTime());
			proj.on(evt, ReplayStatus.REGULAR);
		} else
		if(command instanceof SetPostConditionsFulfillmentCmd) {
			SetPostConditionsFulfillmentCmd cmd = (SetPostConditionsFulfillmentCmd)command;
			SetPostConditionsFulfillmentEvt evt = new SetPostConditionsFulfillmentEvt(cmd.getId(), cmd.getWftId(), cmd.isFulfilled());
			evt.setTimestamp(getCurrentTime());
			proj.on(evt, ReplayStatus.REGULAR);
		} else
		if (command instanceof ActivateTaskCmd) {
			ActivateTaskCmd cmd = (ActivateTaskCmd)command;
			ActivatedTaskEvt evt = new ActivatedTaskEvt(cmd.getId(), cmd.getWftId());
			evt.setTimestamp(getCurrentTime());
			proj.on(evt);
		} else
	    if (command instanceof ChangeCanceledStateOfTaskCmd) {		        
	    	ChangeCanceledStateOfTaskCmd cmd = (ChangeCanceledStateOfTaskCmd)command;
	    	ChangedCanceledStateOfTaskEvt evt = new ChangedCanceledStateOfTaskEvt(cmd.getId(), cmd.getWftId(), cmd.isCanceled());
	    	evt.setTimestamp(getCurrentTime());
	    	proj.on(evt);
		} else					
		if (command instanceof SetPropertiesCmd) {
			SetPropertiesCmd cmd = (SetPropertiesCmd)command;
			proj.on(new SetPropertiesEvt(cmd.getId(), cmd.getIwftId(), cmd.getProperties()));
		} else 
		if (command instanceof InstantiateTaskCmd) {
			InstantiateTaskCmd cmd = (InstantiateTaskCmd)command;
			InstantiatedTaskEvt evt = new InstantiatedTaskEvt(cmd.getId(), cmd.getTaskDefinitionId(), 
					cmd.getOptionalInputs().stream().map(in -> LazyLoadingArtifactInput.generateFrom(in, artifactRegistry, cmd.getId())).collect(Collectors.toList())  , 
					cmd.getOptionalOutputs().stream().map(out -> LazyLoadingArtifactOutput.generateFrom(out, artifactRegistry, cmd.getId())).collect(Collectors.toList()) );
			evt.setTimestamp(getCurrentTime());
			proj.on(evt, ReplayStatus.REGULAR);
		} else
		if (command instanceof CreateWorkflowCmd) {
			CreateWorkflowCmd cmd = (CreateWorkflowCmd)command;
			WorkflowDefinitionContainer wfdContainer = workflowDefinitionRegistry.get(cmd.getDefinitionName());
			CreatedWorkflowEvt evt = new CreatedWorkflowEvt(cmd.getId(), cmd.getInput(), cmd.getDefinitionName(), wfdContainer.getWfd());
			evt.setTimestamp(getCurrentTime());
			proj.on(evt, ReplayStatus.REGULAR);
		}
		else {
		
			System.err.println("Received unsupported command: "+command.toString());
		}
			
		return null;
	}

}
