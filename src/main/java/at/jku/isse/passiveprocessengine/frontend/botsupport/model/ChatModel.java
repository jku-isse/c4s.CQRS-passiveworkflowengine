package at.jku.isse.passiveprocessengine.frontend.botsupport.model;

import at.jku.isse.passiveprocessengine.frontend.botsupport.HuggingFace;

import java.util.List;

/**
 * Interface for the AI chat model
 */
public interface ChatModel {
    String getModelName();

    String applyChatTemplate(List<HuggingFace.Message> messages);
}
