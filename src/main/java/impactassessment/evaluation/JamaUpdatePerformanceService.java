package impactassessment.evaluation;

import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jamaconnector.cache.InternalActivity;
import c4s.jamaconnector.cache.JamaCache;
import c4s.jamaconnector.changehandling.ChangeParser;
import c4s.jamaconnector.changehandling.ChangeReplayer;
import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import impactassessment.artifactconnector.jama.JamaChangeSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
//@Service
@RequiredArgsConstructor
@ConditionalOnExpression("${jama.enabled:true}")
public class JamaUpdatePerformanceService {

    private final JamaCache cache;
    private final JamaInstance jamaInst;
    private final JamaChangeSubscriber changeSubscriber;

    public void replayUpdates() {
        List<Long> timeStamps = new LinkedList<>();
        StopWatch stopW = new StopWatch();
        stopW.start();

        int posFrom = 0;
        //int posFrom = AnalyseSubWPs.subwpIds.length-1;
        //int posTo = 1;
        int posTo = subwpIds.length;


        timeStamps.add(stopW.getTime());

        List<Integer> ids = Arrays.asList(subwpIds);
        //List<Integer> ids = Arrays.asList(new Integer[]{14464164}); // jama15178842-->jira559181,  14464166-->545754
        //List<Integer> jiraIds = Arrays.asList(new Integer[]{545752 }); // first jama id maps to jira id
        // filter out: WPs without SRS

        // filter out items without SRS links
        Map<Integer, JamaItem> allItems = ids.subList(posFrom, posTo).stream()
                .map(id -> {
                    try {
                        return jamaInst.getBasicItem(id);
                    } catch (RestClientException e2) {
                        e2.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(item -> item.getDownstreamItems().stream()
                        .filter(dsi -> dsi.getItemType() != null)
                        .anyMatch(dsi -> dsi.getItemType().getId() == 43)) // there will be only one entry
                .flatMap(item -> {
                    try {
                        return extendReplayScope(item.getId(), jamaInst).stream();
                    } catch (RestClientException e) {
                        e.printStackTrace();
                        return Stream.empty();
                    }
                }).filter(Objects::nonNull)
                .collect(Collectors.toMap(JamaItem::getId, Function.identity(), (existing, replacement) -> existing));

        ChangeReplayer jamaRP = replayJamaItems(allItems.values());
        //ChangeReplayer jamaRP = noReplay(allItems.values());

        timeStamps.add(stopW.getTime());

        LocalDateTime jamaTime = LocalDateTime.of(2000, 1,1,1,1);

        Map<String,LocalDateTime> firstLastDates = new HashMap<>();

        long updateCount = 0;
        int failedChanges = 0;
        boolean continueJama = true;


//        BufferedWriter writer = null;
//        try {
//            writer = new BufferedWriter(new FileWriter("C:\\Users\\stefan\\Desktop\\changes.txt", true));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        Set<JamaItem> updatedItems = new HashSet<>();


        while(continueJama) {
            Optional<ZonedDateTime> optTime = jamaRP.getDateOfNextFutureChange();
            if (optTime.isPresent()) {// another change
                jamaTime = optTime.get().toLocalDateTime();
                firstLastDates.computeIfAbsent("JAMABEGIN", key -> optTime.get().toLocalDateTime());
            } else {
                continueJama = false;
                if (!firstLastDates.containsKey("JAMAEND")) {
                    firstLastDates.put("JAMAEND", jamaTime);
                }
                jamaTime = LocalDateTime.of(3000, 1,1,1,1); // some distant future
            }
            updateCount++;

            InternalActivity jamaChange = null;

            try {
                jamaChange = jamaRP.applyNextForwardChange();
            } catch(Exception e) {
                failedChanges++;
                log.error("Error while apllying update: "+e.getClass().getSimpleName());
            }
            if (jamaChange == null) {
                continueJama = false;
                System.out.println("Jama Replay Complete");
            } else {
                JamaItem jamaItem = allItems.get(jamaChange.getItemId()); //item must exist as otherwise we wouldnt be replaying it

//                try {
//                    String s = jamaItem.getId()+": "+jamaItem.getDocumentKey()+"\n";
//                    writer.append(s);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                updatedItems.add(jamaItem);
                changeSubscriber.handleChangedJamaItems(Set.of(jamaItem), new CorrelationTuple());
            }

        }

//        changeSubscriber.handleChangedJamaItems(updatedItems, new CorrelationTuple());

        timeStamps.add(stopW.getTime());
        stopW.stop();

        log.info("UpdateCount: "+updateCount);
        log.info("FailedChangeCount: "+failedChanges);
        log.info(timeStamps+"");

        firstLastDates.forEach((key, value) -> System.out.println(key + " " + value.toString()));
    }

    private ChangeReplayer replayJamaItems(Collection<JamaItem> items) {
        ChangeReplayer player = new ChangeReplayer(items);
        long countNonresolvable = player.getChanges().stream()
                .filter(ChangeParser.RelationChange.class::isInstance)
                .map(ChangeParser.RelationChange.class::cast)
                .flatMap(rc -> Arrays.stream(new String[]{rc.getDestinationDocKey(), rc.getSourceDocKey()}))
                .filter(dkey -> cache.getIdForDocKey(dkey) == null)
                .distinct()
                .peek(dkey -> System.out.println("No Item for Key: "+dkey))
                .count();
        //if (countNonresolvable > 0) return;
        System.out.println("Non resolvable items cound: "+countNonresolvable);
        long count = player.revertToBegin();
        System.out.println("Reverted change count: "+count);
        return player;
    }

    // this scope is custom made just for the constraint checks we implemented
    private List<JamaItem>  extendReplayScope(int itemId, JamaInstance ji) throws RestClientException {
        //prefetchItemTypesAndPickListOptions()
        Set<Integer> fetchedIds = new HashSet<Integer>();
        fetchedIds.add(itemId);
        JamaItem jamaItem = ji.getBasicItem(itemId);
        //JamaUtils.getJamaItemDetails(jamaItem);
        List<JamaItem> firstDegree = extendScopeViaRelationsFrom(jamaItem, fetchedIds);
        firstDegree.forEach(ji2 -> {
            extendScopeViaRelationsFrom(ji2, fetchedIds); }); //second degree relations just need to be available but not replayed (for our purpose)
        firstDegree.add(jamaItem);
        return firstDegree;
    }

    private List<JamaItem> extendScopeViaRelationsFrom(JamaItem item, Set<Integer> fetchedIds) {
        List<JamaItem> list = item.getDownstreamItems().stream()
                .filter(ji -> !fetchedIds.contains(ji.getId())) // we only fetch when not yet fetched
                .peek(ji -> fetchedIds.add(ji.getId()))
                .collect(Collectors.toList());
        list.addAll(item.getUpstreamItems().stream()
                .filter(ji -> !fetchedIds.contains(ji.getId())) // we only fetch when not yet fetched
                .peek(ji -> fetchedIds.add(ji.getId()))
                .collect(Collectors.toList()));
        return list;
    }

    public static Integer[] subwpIds = {
//8323559, no downstream SRS
            8379876,
            8459763,
            8505744,
            8505745,
            8505746,
            8505747,
            8510535,
            8510536,
            8510537,
            8510538,
            8884791,
            9531187,
            9584033,
            9584034,
            9675922,
            9675923,
            9675924,
            9675925,
            9675926,
            7341194,
            9675928,
            7341195,
            9675930,
            9675931,
            9675932,
            7169347,
            7341196,
            9734168,
            7341197,
            7341198,
            7341199,
            7341200,
            10021367,
            7346167,
            10209034,
            10209035,
            10269113,
            10269114,
            10270374,
            10349103,
            10429022,
            10534524,
            10534525,
            10534526,
            10555518,
            10914018,
            11555760,
            11555761,
            7367862,
            7367863,
            11942379,
            11947100,
            12044126,
            7374274,
            7375539,
            12497003,
            12994844,
            12994845,
            12994846,
            13348825,
            13348826,
            13351621,
            13351622,
            13373302,
            13563604,
            13793488,
            7230585,
            7541866,
            7541867,
            7541868,
// drop 411 onwards below
            14494337,
            14500058,
            14500947,
            14619816,
            14619817,
            14619818,
            14624079,
            14624080,
            14624081,
            15079383,
            15104162,
            15104163,
            15128680,
            15178841,
            15178842,
            15178843,
            15178845,
            15178846,
            15178847,
            15408422,
            15539532,
            15539533,
            15540718,
            15540719,
            15540720,
            15540721,
            15550132,
            15550133,
            15550134,
            15551554,
            15551555,
            15551556,
            11539214,
            11539215,
            11539217,
            11591733,
            13348823,
            13348824,
            13373299,
            13373300,
            13373301,
            13422794,
            14359309,
            14360397,
            14464163,
            14464164,
            14464165,
            14464166
    };
}
