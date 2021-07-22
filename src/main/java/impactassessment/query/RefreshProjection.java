package impactassessment.query;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import impactassessment.api.Events.RefreshedTriggerEvent;
import impactassessment.ui.IFrontendPusher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@Component
@Slf4j
@RequiredArgsConstructor
@Profile("query")
@ProcessingGroup("projection")
public class RefreshProjection {
	private final IFrontendPusher pusher;
	private final ProjectionModel projection;
	
	@DisallowReplay
	@EventHandler
	public void on(RefreshedTriggerEvent evt) {
		log.debug("[PRJ] projecting {}", evt);		
		if (projection.getDb().size() > 0) {
			pusher.update(projection.getWfis());
		}
	}
}
