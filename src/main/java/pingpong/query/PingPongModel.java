package pingpong.query;

import lombok.extern.slf4j.XSlf4j;
import org.apache.commons.lang3.SerializationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XSlf4j
public class PingPongModel {

    private static PingPongModel instance;

    Map<String, Integer> liveDB;
    HashMap<String, Integer> historyDB;
    List<Map<String, Integer>> snapshots;

    private PingPongModel() {
        liveDB = new HashMap<>();
        historyDB = new HashMap<>();
        snapshots = new ArrayList<>();
    }

    public static PingPongModel getInstance() {
        if (instance == null) {
            instance = new PingPongModel();
        }
        return instance;
    }

    public void createLive(String id, int amount) {
        instance.liveDB.put(id, amount);
    }
    public void increaseLive(String id, int amount) {
        instance.liveDB.put(id, instance.liveDB.get(id) + amount);
    }
    public void decreaseLive(String id, int amount) {
        instance.liveDB.put(id, instance.liveDB.get(id) - amount);
    }

    public void createHistory(String id, int amount) {
        instance.historyDB.put(id, amount);
    }
    public void increaseHistory(String id, int amount) {
        instance.historyDB.put(id, instance.historyDB.get(id) + amount);
    }
    public void decreaseHistory(String id, int amount) {
        instance.historyDB.put(id, instance.historyDB.get(id) - amount);
    }

    public void resetLive() {
        instance.liveDB = new HashMap<>();
    }

    public void resetHistory() {
        instance.historyDB = new HashMap<>();
    }

    public void createSnapshot(){
        instance.snapshots.add(SerializationUtils.clone(instance.historyDB));
    }

    public void print() {
        log.info("*****************LIVE******************");
        for (Map.Entry<String, Integer> e : instance.liveDB.entrySet()) {
            log.info("id: {}, amount: {}", e.getKey(), e.getValue());
        }
        log.info("****************HISTORY****************");
        for (Map.Entry<String, Integer> e : instance.historyDB.entrySet()) {
            log.info("id: {}, amount: {}", e.getKey(), e.getValue());
        }
        for (Map<String, Integer> m : instance.snapshots) {
            log.info("*****************SNAP******************");
            for (Map.Entry<String, Integer> e : m.entrySet()) {
                log.info("id: {}, amount: {}", e.getKey(), e.getValue());
            }
        }
        log.info("***************************************");
    }
}
