package at.jku.isse.passiveprocessengine.frontend.botsupport.model;

import at.jku.isse.passiveprocessengine.frontend.botsupport.HuggingFace;

import java.util.List;

public class MistralModel implements ChatModel {
    private static final String MODEL_NAME = "mistralai/Mistral-7B-Instruct-v0.2";

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    /**
     * <a href="https://www.promptingguide.ai/models/mistral-7b">Docs</a>
     * <p>
     * ###### Default (but Error-Raising) Chat Template ######
     * <p>
     * jinja2.exceptions.TemplateError: Conversation roles must alternate user/assistant/user/assistant/...<p>
     * ###### Corrected Chat Template ######<p>
     * &lt;s&gt;[INST] What is the capital of Australia? [/INST]The capital city of Australia is Canberra...</s>
     * [INST] But I thought it was Sydney! [/INST]Sydney is indeed a large and famous city in Australia, but it is not the capital city...</s>
     * [INST] Thanks for putting me straight. [/INST]
     * <p>
     * <a href="https://community.aws/content/2dFNOnLVQRhyrOrMsloofnW0ckZ/how-to-prompt-mistral-ai-models-and-why">Further readings</a>
     * @param messages List of messages to apply the chat template to
     * @return The chat template applied to the messages
     */
    @Override
    public String applyChatTemplate(List<HuggingFace.Message> messages) {
        StringBuilder result = new StringBuilder();
        boolean firstUserMessage = true;

        for (int i = 0; i < messages.size(); i++) {
            HuggingFace.Message message = messages.get(i);

            if (message.getRole().equals("user")) {
                if (firstUserMessage) {
                    result.append("<s>");
                    firstUserMessage = false;
                }
                result.append("[INST] ").append(message.getContent()).append(" [/INST] ");
            } else if (message.getRole().equals("assistant")) {
                result.append(message.getContent());
                if (i + 1 < messages.size() && messages.get(i + 1).getRole().equals("user")) {
                    result.append("</s>");
                }
            }
        }

        return result.toString();
    }
}
