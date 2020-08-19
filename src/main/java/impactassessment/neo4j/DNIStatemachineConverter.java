package impactassessment.neo4j;

import org.neo4j.ogm.typeconversion.AttributeConverter;

import com.github.oxo42.stateless4j.StateMachine;

import impactassessment.passiveprocessengine.workflowmodel.DecisionNodeDefinition;
import impactassessment.passiveprocessengine.workflowmodel.DecisionNodeDefinition.Events;
import impactassessment.passiveprocessengine.workflowmodel.DecisionNodeDefinition.States;

public class DNIStatemachineConverter implements AttributeConverter<StateMachine<DecisionNodeDefinition.States, DecisionNodeDefinition.Events>,String>{

	@Override
	public StateMachine<States, Events> toEntityAttribute(String arg0) {
		States s = States.valueOf(arg0);
		StateMachine<States, Events> sm = new StateMachine<>(s, DecisionNodeDefinition.getStateMachineConfig());
		return sm;
	}

	@Override
	public String toGraphProperty(StateMachine<States, Events> arg0) {
		return arg0.getState().toString();
	}

}
