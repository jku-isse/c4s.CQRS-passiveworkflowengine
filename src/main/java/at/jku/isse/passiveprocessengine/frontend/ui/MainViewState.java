package at.jku.isse.passiveprocessengine.frontend.ui;

import org.springframework.security.core.Authentication;

import com.vaadin.flow.component.UI;

import lombok.Data;

@Data
public class MainViewState {
    private final UI ui;
    private final MainView view;
    private final Authentication auth;
}
