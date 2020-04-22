package impactassessment.rulebase;

import lombok.extern.slf4j.XSlf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.event.rule.*;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@XSlf4j
public class RuleBaseFactory {

    private static final String RULES_PATH = "rules/";
    private static final String INPUT_RULES_FILE = "input.drl";
    private static final String EXECUTION_RULES_FILE = "execution.drl";
    private static final String CONSTRAINTS_RULES_FILE = "constraints.drl";

    private KieServices kieServices = KieServices.Factory.get();

    private void getKieRepository() {
        final KieRepository kieRepository = kieServices.getRepository();
        kieRepository.addKieModule(() -> kieRepository.getDefaultReleaseId());
    }

    public KieSession getKieSession() {
        getKieRepository();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_PATH+INPUT_RULES_FILE));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_PATH+EXECUTION_RULES_FILE));
        KieBuilder kb = kieServices.newKieBuilder(kieFileSystem);
        kb.buildAll();
        KieModule kieModule = kb.getKieModule();
        KieContainer kContainer = kieServices.newKieContainer(kieModule.getReleaseId());
        KieSession kieSession = kContainer.newKieSession();
        addRuleRuntimeEventListender(kieSession);
        addAgendaEventListender(kieSession);
        return kieSession;
    }

    public KieSession getKieSession(Resource dt) {
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem().write(dt);
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem).buildAll();
        KieRepository kieRepository = kieServices.getRepository();
        ReleaseId krDefaultReleaseId = kieRepository.getDefaultReleaseId();
        KieContainer kieContainer = kieServices.newKieContainer(krDefaultReleaseId);
        KieSession kiesession = kieContainer.newKieSession();
        return kiesession;
    }

    private void addRuleRuntimeEventListender(KieSession kieSession) {
        kieSession.addEventListener(new RuleRuntimeEventListener() {
            @Override
            public void objectInserted(ObjectInsertedEvent evt) {
                log.debug("[KB ] inserted {}", evt.getObject().toString());
            }

            @Override
            public void objectUpdated(ObjectUpdatedEvent evt) {
                log.debug("[KB ] updated {}", evt.getObject().toString());
            }

            @Override
            public void objectDeleted(ObjectDeletedEvent evt) {
                log.debug("[KB ] deleted {}", evt.getOldObject().toString());
            }
        });
    }

    private void addAgendaEventListender(KieSession kieSession) {
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
