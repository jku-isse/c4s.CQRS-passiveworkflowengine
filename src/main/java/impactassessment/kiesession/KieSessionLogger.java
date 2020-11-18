package impactassessment.kiesession;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.event.rule.*;
import org.kie.api.runtime.KieSession;

@Slf4j
public class KieSessionLogger {

    public static void addRuleRuntimeEventListener(KieSession kieSession) {
        kieSession.addEventListener(new RuleRuntimeEventListener() {
            @Override
            public void objectInserted(ObjectInsertedEvent evt) {
                log.debug("[KB ] insert");
//                log.debug("[KB ] inserted {}", evt.getObject().toString());
            }

            @Override
            public void objectUpdated(ObjectUpdatedEvent evt) {
                log.debug("[KB ] update");
//                log.debug("[KB ] updated {}", evt.getObject().toString());
            }

            @Override
            public void objectDeleted(ObjectDeletedEvent evt) {
                log.debug("[KB ] delete");
//                log.debug("[KB ] deleted {}", evt.getOldObject().toString());
            }
        });
    }

    public static void addAgendaEventListener(KieSession kieSession) {
        kieSession.addEventListener(new AgendaEventListener() {
            @Override
            public void matchCreated(MatchCreatedEvent evt) {
                log.debug("[KB ] the rule {} can be fired in agenda", evt.getMatch().getRule().getName());
            }

            @Override
            public void matchCancelled(MatchCancelledEvent evt) {
                log.debug("[KB ] the rule {} cannot be in agenda", evt.getMatch().getRule().getName());
            }

            @Override
            public void beforeMatchFired(BeforeMatchFiredEvent evt) {
                log.debug("[KB ] the rule {} will be fired", evt.getMatch().getRule().getName());
            }

            @Override
            public void afterMatchFired(AfterMatchFiredEvent evt) {
                log.debug("[KB ] the rule {} has been fired", evt.getMatch().getRule().getName());
            }

            @Override
            public void agendaGroupPopped(AgendaGroupPoppedEvent evt) { }

            @Override
            public void agendaGroupPushed(AgendaGroupPushedEvent evt) { }

            @Override
            public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent evt) { }

            @Override
            public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent evt) { }

            @Override
            public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent evt) { }

            @Override
            public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent evt) { }
        });
    }
}
