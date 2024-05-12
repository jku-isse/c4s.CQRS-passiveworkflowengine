package at.jku.isse.passiveprocessengine.frontend.botsupport;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.passiveprocessengine.frontend.botsupport.model.ChatModel;
import at.jku.isse.passiveprocessengine.frontend.botsupport.model.MistralModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@Scope("session")
@ConditionalOnExpression(value = "${huggingface.enabled:false}")
@Qualifier("huggingface")
public class HuggingFace implements OCLBot {

    public static final String TEST_DATA_PROMPT = "You are an AI language model tasked with generating test data in %s format based on a specified data schema. " +
            "The data schema and the specific task are outlined below. Use the schema to create accurate and structured test data that fulfills the given task.";
    private static final String API_URL = "https://api-inference.huggingface.co/models/";

    private final ChatModel chatModel = new MistralModel();

    private static int maxInteractions = 20;

    private final String apiKey;

    protected List<Message> interaction = new ArrayList<>();


    public HuggingFace(@Value("${huggingface.apikey}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public CompletableFuture<BotResult> sendAsync(BotRequest request) {
        return OCLBot.super.sendAsync(request);
    }

    @Override
    public BotResult send(BotRequest request) {

        try {
            ChatRequest chatRequest = compileRequest(request);
            log.info(chatRequest.toString());
            String requestBody = chatRequest.toJsonString();
            log.info(requestBody);
            URL url = new URL(API_URL + chatModel.getModelName());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + this.apiKey);
            conn.setRequestProperty("Cache-control", "max-age=0");
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            conn.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));
            Scanner scanner = new Scanner(conn.getInputStream());

            // used for mock response - comment out conn.getOutputStream().write(...) and replace scanner for mocking
            // Scanner scanner = new Scanner(getMockAnswer());
            StringBuilder responseBuilder = new StringBuilder();
            while (scanner.hasNextLine()) {
                responseBuilder.append(scanner.nextLine());
            }
            String responseBody = responseBuilder.toString();
            ObjectMapper objectMapper = new ObjectMapper();
            ChatResponse chatResponse = objectMapper
                    .readValue(responseBody, new TypeReference<List<ChatResponse>>() {
                    }).get(0);
            log.info(chatResponse.getGeneratedText());
            if (interaction.get(interaction.size() - 1).role.equals("assistant"))
                interaction.get(interaction.size() - 1).content += chatResponse.getGeneratedText();
            else
                interaction.add(new Message("assistant", chatResponse.getGeneratedText()));
            conn.disconnect();
            return new TestDataBotResult(interaction.get(interaction.size() - 1).content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Mock response for testing purposes
     * @return InputStream
     */
    private InputStream getMockAnswer() {
        String mockResponse = "[{\"generated_text\":\"\\n  \\\"azure_workitem\\\": {\\n    \\\"id\\\": \\\"WIT-001\\\",\\n    \\\"name\\\": \\\"Test requirement 1\\\",\\n    \\\"state\\\": \\\"open\\\",\\n    \\\"description\\\": \\\"This is a test requirement for the new feature.\\\",\\n    \\\"project\\\": {\\n      \\\"id\\\": \\\"PROJ-001\\\",\\n      \\\"name\\\": \\\"Test project\\\"\\n    },\\n    \\\"successorItems\\\": [\\n      {\\n        \\\"id\\\": \\\"WIT-002\\\",\\n        \\\"name\\\": \\\"Test case 1.1\\\",\\n        \\\"state\\\": \\\"draft\\\"\\n      },\\n      {\\n        \\\"id\\\": \\\"WIT-003\\\",\\n        \\\"name\\\": \\\"Test case 1.2\\\",\\n        \\\"state\\\": \\\"draft\\\"\\n      },\\n      {\\n        \\\"id\\\": \\\"WIT-004\\\",\\n        \\\"name\\\": \\\"Test case 1.3\\\",\\n        \\\"state\\\": \\\"draft\\\"\\n      }\\n    ],\\n    \\\"modifiedBy\\\": [\\\"John Doe\\\", \\\"Jane Doe\\\"],\\n    \\\"createdBy\\\": {\\n      \\\"userDescriptor\\\": \\\"user1@example.com\\\",\\n      \\\"name\\\": \\\"User 1\\\",\\n      \\\"displayName\\\": \\\"User One\\\",\\n      \\\"html_url\\\": \\\"https://dev.azure.com/org/_users/user1\\\"\\n    }\\n  }\\n}\"}]";

        // Convert the predefined string to a byte array
        return new ByteArrayInputStream(mockResponse.getBytes());

    }

    @Override
    public void resetSession() {
        interaction.clear();
    }

    /**
     * Compiles the request for the chat model in the following format:
     * <p>
     * You are an AI language model tasked with generating test data in %s format based on a specified data schema.
     * The data schema and the specific task are outlined below. Use the schema to create accurate and structured test data that fulfills the given task.
     * <p>
     * Data Schema:
     * instanceType1
     * instanceType2
     * ...
     * <p>
     * Task:
     * userPrompt
     *
     * @param request
     * @return ChatRequest
     */
    private ChatRequest compileRequest(BotRequest request) {
        while (interaction.size() > maxInteractions) {
            interaction.remove(0);
        }
        ArrayList<HuggingFace.Message> messages = new ArrayList<>();
        String prompt = String.format(TEST_DATA_PROMPT, ((TestDataBotRequest) request).outputFormat.getKey());
        HumanReadableSchemaExtractor extractor = new HumanReadableSchemaExtractor();
        Map<InstanceType, String> instanceTypeMap = new HashMap<>();
        ((TestDataBotRequest) request).instanceTypes
                .forEach(type -> instanceTypeMap
                        .putAll(extractor.getSchemaForInstanceTypeAndOneHop(type, true)));

        String joinedSchemaStr = String.join("\r\n", instanceTypeMap.values());
        joinedSchemaStr = String.format("Data Schema:\r\n%s", joinedSchemaStr);
        String taskPromptStr = String.format("\nTask:\r\n%s Ensure that the result only includes the fields from the schema. " +
                        "Ensure the result includes only the necessary fields, i.e exclude empty fields.",
                request.userPrompt); //generate me a requirement in status ‘open’ that has three successor test cases, all of them in state ‘draft’.
        String instr = String.join("\n", prompt, joinedSchemaStr, taskPromptStr);
        messages.add(new Message(request.role, instr));
        // Here we "prefill" the response of the AI, so we can decide how the response starts with (can be char or a string).
        // In case of JSON e.g. the response shall start with a curly brace '{'
        messages.add(new Message("assistant", ((TestDataBotRequest) request).outputFormat.getValue()));
        interaction = messages;
        return new ChatRequest(chatModel.applyChatTemplate(messages), new ChatRequest.Parameters(false, 3000));
    }

    @Data
    @AllArgsConstructor
    public static class ChatRequest {
        private String inputs;
        private Parameters parameters;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Parameters {
            @JsonProperty("return_full_text")
            private boolean returnFullText;
            @JsonProperty("max_new_tokens")
            private int maxNewTokens;
        }


        public String toJsonString() throws IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(this);
        }
    }

    @Data
    public static class ChatResponse {
        @JsonProperty("generated_text")
        private String generatedText;
    }

    @Data
    public static class Message {

        @JsonIgnore
        private Instant time;
        private String role;
        private String content;

        public Message() {
            this.time = Instant.now();
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
            this.time = Instant.now();
        }

        public Message(String role, String content, Instant time) {
            this.role = role;
            this.content = content;
            this.time = time;
        }
    }
}

