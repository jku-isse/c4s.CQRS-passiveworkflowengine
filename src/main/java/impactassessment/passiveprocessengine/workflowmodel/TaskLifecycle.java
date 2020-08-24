package impactassessment.passiveprocessengine.workflowmodel;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.github.oxo42.stateless4j.delegates.Func2;
import com.github.oxo42.stateless4j.delegates.Func3;
import com.github.oxo42.stateless4j.triggers.TriggerWithParameters1;
import com.github.oxo42.stateless4j.triggers.TriggerWithParameters2;

public class TaskLifecycle {
	
	// see as inspiration: https://docs.camunda.org/manual/7.4/reference/cmmn11/concepts/lifecycle/#task-stage-lifecycle
	public static enum State {
		AVAILABLE, // not used yet
		ENABLED, // input sufficient to be worked on, thus we would recommend this task, respectively expect to oberve work on it
		ACTIVE, // explicitly set by user after recommendation, or observed work output
		NO_WORK_EXPECTED, // user or rule/logic/etc set the the task to NOWORKEXPECTED, should not be executed/recommended, we might still see input for this
		REVOKED, // preconditions, etc input no longer fulfilled (reactivate/reenable upon input condition),
		COMPLETED, // output/post conditions fulfilled
		IGNORED, // ignore/drop existing output for other tasks
		PARTIALLY_COMPLETED, // prematurely ended, output usable for other tasks
		SUPERSTATE_ENDED
	}
	
	public static enum InputState {
		INPUT_UNKNOWN, // not yet checked
		INPUT_MISSING, // no input available so far,
		INPUT_PARTIAL, // no enough input available so far
		INPUT_SUFFICIENT // all required input available
	}
	
	public static enum OutputState {
		OUTPUT_UNKNOWN, // not yet checked
		OUTPUT_MISSING, // no output available so far,
		OUTPUT_PARTIAL, // no enough output available so far
		OUTPUT_SUFFICIENT // all required output available
	}
	
	public static enum Events {
		ADD_OUTPUT,
		ACTIVATE,
		DONT_WORK_ON_TASK,
		INPUTCONDITIONS_NO_LONGER_HOLD,
		INPUTCONDITIONS_FULFILLED,
		OUTPUT_REMOVED,
		OUTPUTCONDITIONS_FULFILLED,
		IGNORE_FOR_PROGRESS,
		PARTIALLY_COMPLETE,
	}
	
	private static StateMachineConfig<State, Events> smc;
	
	public static StateMachineConfig<State, Events> getStateMachineConfig() {
		// Alternative check: https://www.baeldung.com/spring-state-machine
		// http://projects.spring.io/spring-statemachine/
		if (smc == null) {
			// https://github.com/oxo42/stateless4j
			smc = new StateMachineConfig<>();
			smc.configure(State.AVAILABLE)
				.permit(Events.ADD_OUTPUT, State.ACTIVE)
				.permit(Events.INPUTCONDITIONS_FULFILLED, State.ENABLED);
			TriggerWithParameters2<OutputState, Boolean, State, Events> trigger2 = new TriggerWithParameters2<OutputState, Boolean, State, Events>(Events.OUTPUT_REMOVED, OutputState.class, Boolean.class);
			smc.configure(State.ACTIVE)
				.permitDynamic(trigger2, new Func3<OutputState, Boolean, State>() {
					@Override
					public State call(OutputState arg0, Boolean arg1) {
						switch(arg0) {
						// if task has be set to DISABLED/NOWORKEXPECTED, then setting back to enabled or available will forward it to disabled again
							case OUTPUT_UNKNOWN: //fallthrough
							case OUTPUT_MISSING:
								if (arg1) // assuming true means inputconditions fullfilled
									return State.ENABLED;
								else 
									return State.AVAILABLE;
							case OUTPUT_PARTIAL: // fallthrough
							case OUTPUT_SUFFICIENT: // fallthrough
							default:
								return State.ACTIVE;
						}
					}}  ) 
				.permit(Events.INPUTCONDITIONS_NO_LONGER_HOLD, State.REVOKED)
				.permit(Events.OUTPUTCONDITIONS_FULFILLED, State.COMPLETED)
				.permit(Events.PARTIALLY_COMPLETE, State.PARTIALLY_COMPLETED)
				.permit(Events.IGNORE_FOR_PROGRESS, State.IGNORED)
				.permitReentry(Events.ADD_OUTPUT);
			smc.configure(State.ENABLED)
				.permit(Events.ADD_OUTPUT, State.ACTIVE)
				.permit(Events.ACTIVATE, State.ACTIVE)
				.permit(Events.PARTIALLY_COMPLETE, State.PARTIALLY_COMPLETED) // fast forward: when user/rule signals completion without observable output,
				.permit(Events.OUTPUTCONDITIONS_FULFILLED, State.COMPLETED) // fast forward: when user/rule signals completion with single observable output, skips ACTIVE state
				.permit(Events.INPUTCONDITIONS_NO_LONGER_HOLD, State.REVOKED)
				.permit(Events.DONT_WORK_ON_TASK, State.NO_WORK_EXPECTED);
			smc.configure(State.NO_WORK_EXPECTED)
				.permit(Events.ADD_OUTPUT, State.ACTIVE)
				.permit(Events.IGNORE_FOR_PROGRESS, State.IGNORED);
			TriggerWithParameters1<Boolean, State, Events> trigger = new TriggerWithParameters1<Boolean, State, Events>(Events.INPUTCONDITIONS_FULFILLED, Boolean.class);	
			smc.configure(State.REVOKED)		
				//.permit(Events.INPUTCONDITIONS_FULFILLED, State.ENABLED)
				//.permit(Events.INPUTCONDITIONS_FULFILLED, State.ACTIVE)
				.permitDynamic(trigger, new Func2<Boolean, State>() {				
					@Override
					public State call(Boolean arg0) { 
						if (arg0) // assuming true means that OUTPUT DATA IS AVAILABLE
							return State.ACTIVE;
						else
							return State.ENABLED;
					}}  ) 
				.permit(Events.PARTIALLY_COMPLETE, State.PARTIALLY_COMPLETED)
				.permit(Events.IGNORE_FOR_PROGRESS, State.IGNORED)
				.permitReentry(Events.ADD_OUTPUT);		
			smc.configure(State.COMPLETED)
				.substateOf(State.SUPERSTATE_ENDED);
			smc.configure(State.PARTIALLY_COMPLETED)
				.substateOf(State.SUPERSTATE_ENDED)
				.permit(Events.OUTPUTCONDITIONS_FULFILLED, State.COMPLETED); // Delayed output completed
			smc.configure(State.IGNORED)
				.substateOf(State.SUPERSTATE_ENDED);
			smc.configure(State.SUPERSTATE_ENDED)
				.permitReentry(Events.ADD_OUTPUT);
		}
		return smc;	
	}
	
	public static StateMachine<State, Events> buildStatemachine() {
		StateMachine<State, Events> sm = new StateMachine<>(State.AVAILABLE, getStateMachineConfig());
		return sm;
	}
}

/*

@startuml

title TaskLifecycle State Model
[*] --> AVAILABLE: createTask
AVAILABLE --> ENABLED : inputConditionsFulfilled
ENABLED --> ACTIVE : addOutput / activate
AVAILABLE --> ACTIVE : addOutput
ACTIVE --> ACTIVE : addOutput
ENABLED --> NOWORKEXPECTED : DONTWORKONTASK
ENABLED --> REVOKED : inputConditionsNoLongerHold
NOWORKEXPECTED --> ACTIVE : addOutput
NOWORKEXPECTED --> CANCELED : IGNORE_FOR_PROGRESS
REVOKED --> ENABLED : inputConditionsFulfilled
REVOKED --> CANCELED : IGNORE_FOR_PROGRESS
REVOKED --> PARTIALLY_COMPLETED : PARTIALLY_COMPLETE
ACTIVE --> REVOKED : inputConditionsNoLongerHold
ACTIVE --> COMPLETED : outputConditionsFulfilled
ACTIVE --> CANCELED : IGNORE_FOR_PROGRESS
ACTIVE --> PARTIALLY_COMPLETED : PARTIALLY_COMPLETE

COMPLETED --> [*] 
CANCELED --> [*] 
PARTIALLY_COMPLETED --> [*] 



note left of AVAILABLE
    whenever task is added to knowledge base
end note 

note right of ENABLED
    while input conditions are met, 
    but no work observed yet
end note

note left of ACTIVE
    some but insufficient user output 
    or after explicit activation by user
    might not imply fulfilled inputconditions
end note

note top of NOWORKEXPECTED
    e.g. when alternative task in XOR becomes active instead
    (but input conditions could still hold)
    user might still provide output
end note

note right of REVOKED
    when input conditions no longer hold 
    (similar to AVAILABLE but engine needs 
    to expect potential output from user)
end note

note top of COMPLETED
     when user provided sufficient output and conditions hold
     task and its output can now be used to trigger process progress
end note

note right of CANCELED
    explicitly set by user or 
    e.g. when two task were wrongfully worked on in an XOR split, 
    and one triggers completion, the other is canceled, 
    we still might see output 
    that should not be available to further tasks
    but that will be ignored
end note

note right of PARTIALLY_COMPLETED
    output conditions don't hold but rules 
    or user signal no more work,
    and output should be available to further tasks
end note 

note "once in COMPLETED, CANCELED, or PARTIALLY_COMPLETED, further user output will be recorded but not acted upon" as N1

note "External actions by the user include addOutput, activate, PARTIALLY_COMPLETE, and cancel; engine actionc include: createTask, inputConditionsFulfilled, inputConditionsNoLongerHold, outputConditionsFulfilled, DONTWORKONTASK" as N2

@enduml

 * 
 * */

