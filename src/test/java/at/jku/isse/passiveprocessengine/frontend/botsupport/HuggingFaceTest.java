package at.jku.isse.passiveprocessengine.frontend.botsupport;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class HuggingFaceTest {

    private HuggingFace huggingFace;

    @Test
    void send() {
        huggingFace = new HuggingFace("NOTRELEVANT");
//        OCLBot.TestDataBotRequest testDataBotRequest = new OCLBot.TestDataBotRequest(
//                Instant.now());
//
//        assertDoesNotThrow(() -> huggingFace.send(testDataBotRequest));
    }
}