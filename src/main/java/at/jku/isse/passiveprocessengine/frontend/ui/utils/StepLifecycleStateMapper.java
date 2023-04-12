package at.jku.isse.passiveprocessengine.frontend.ui.utils;

import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;

public class StepLifecycleStateMapper {

	public static String translateState(State state) {
		switch(state) {
		case ACTIVE:
			return "In Progress";			
		case AVAILABLE:
			return "Not Ready";			
		case CANCELED:
			return "Canceled";			
		case COMPLETED:
			return "Completed";			
		case ENABLED:
			return "Ready";			
		case NO_WORK_EXPECTED:
			return "Nothing to do";					
		}
		return state.toString();
	}
	
}
