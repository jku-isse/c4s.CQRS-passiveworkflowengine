package impactassessment.jama;

import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jamaconnector.*;
import c4s.jamaconnector.analytics.JamaUpdateTracingInstrumentation;
import c4s.jamaconnector.cache.CachedResourcePool;
import c4s.jamaconnector.cache.CachingJsonHandler;
import c4s.jamaconnector.cache.CouchDBJamaCache;
import c4s.jamaconnector.cache.JamaCache;
import com.jamasoftware.services.restclient.JamaConfig;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import impactassessment.artifactconnector.jama.JamaChangeSubscriber;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.evaluation.JamaUpdatePerformanceService;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

public class OnlyJamaConfig {

    @Bean
    public JamaUpdatePerformanceService getJamaUpdatePerformanceService(JamaCache jamaCache, JamaInstance jamaInstance, JamaChangeSubscriber jamaChangeSubscriber) {
        return new JamaUpdatePerformanceService(jamaCache, jamaInstance, jamaChangeSubscriber);
    }

    @Bean
    public JamaService getJamaService(JamaInstance jamaInstance, JamaChangeSubscriber jamaChangeSubscriber) {
        return new JamaService(jamaInstance, jamaChangeSubscriber);
    }

    @Bean
    public JamaConnector getJamaConnector(AutowireCapableBeanFactory beanFactory) {
        JamaConnector jamaConn = new OfflineJamaConnector(1); // pollInterval is not used
        beanFactory.autowireBean(jamaConn);
        return jamaConn;
    }

    @Bean
    public CouchDBJamaCache getCache(CouchDbClient dbClient) {
        return new CouchDBJamaCache(dbClient);
    }

    @Bean
    public JamaInstance getJamaInstance(CouchDBJamaCache cache) {
        JamaConfig jamaConf = new JamaConfig();
        jamaConf.setJson(new CachingJsonHandler(cache));
        jamaConf.setApiKey("SUPERSECRETKEY");
        String url = "http://localhost";
        jamaConf.setBaseUrl(url);
        jamaConf.setResourceTimeOut(Integer.MAX_VALUE);
        jamaConf.setOpenUrlBase(url);
        jamaConf.setUsername("OFFLINE");
        jamaConf.setPassword("OFFLINE");
        jamaConf.setResourceTimeOut(60);
        jamaConf.setHttpClient(new OfflineHttpClientMock());
        JamaInstance jamaInst = new JamaInstance(jamaConf, true);
        cache.setJamaInstance(jamaInst);
        jamaInst.setResourcePool(new CachedResourcePool(cache));
        return jamaInst;
    }

    @Bean
    public CouchDbClient getCouchDbClient() {
        CouchDbProperties dbprops = new CouchDbProperties()
                .setDbName("jamaitems3")
                .setCreateDbIfNotExist(true)
                .setProtocol("http")
                .setHost("localhost")
                .setPort(Integer.parseInt("5984"))
                .setUsername("admin")
                .setPassword("password")
                .setMaxConnections(100)
                .setConnectionTimeout(0);
        return new CouchDbClient(dbprops);
    }

    // ------------------------- JAMA MOCKS -------------------------

    @Bean
    public JamaChangeSubscriber getJamaChangeSubscriber() {
        return new JamaChangeSubscriber(null){
            @Override
            public void handleChangedJamaItems(Set<JamaItem> set, CorrelationTuple correlationTuple) {
                // do nothing
            }
        };
    }

    @Bean
    public ProjectMonitoringState getProjectMonitoringState() {
        return new ProjectMonitoringState() {
            @Override
            public Set<Integer> getMonitoredProjectIds() {
                return null;
            }

            @Override
            public void removeMonitoredProject(Integer integer) {

            }

            @Override
            public void addMonitoredProject(Integer integer) {

            }
        };
    }

    @Bean
    public ItemMonitoringState getItemMonitoringState() {
        return new ItemMonitoringState() {
            @Override
            public Set<Integer> getMonitoredItemIds() {
                return null;
            }

            @Override
            public boolean removeMonitoredItem(Integer integer) {
                return false;
            }

            @Override
            public void addMonitoredItem(Integer integer) {

            }
        };
    }

    @Bean
    public JamaUpdateTracingInstrumentation getUpdaeTraceInstrumentation() {
        return new JamaUpdateTracingInstrumentation() {
            @Override
            public void logJamaPollResult(CorrelationTuple correlationTuple, int i, Map<String, Set<Integer>> map) {

            }

            @Override
            public void logJamaUpdateResult(CorrelationTuple correlationTuple, int i, Set<JamaItem> set) {

            }
        };
    }
}
