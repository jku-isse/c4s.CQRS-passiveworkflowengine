package at.jku.isse.passiveprocessengine.frontend.ui;

import at.jku.isse.designspace.core.model.*;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.botsupport.HuggingFace;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OCLBot;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.plantuml.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.vaadin.gatanaso.MultiselectComboBox;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static at.jku.isse.passiveprocessengine.frontend.ui.components.ComponentUtils.convertToResourceLinkWithBlankTarget;


@Slf4j
@Route(value = "testdata", layout = AppView.class)
@CssImport(value = "./styles/grid-styles.css", themeFor = "vaadin-grid")
@CssImport(value = "./styles/theme.css")
@PageTitle("Test Data Playground")
@UIScope
//@SpringComponent
public class TestDataPlaygroundView extends VerticalLayout implements BeforeLeaveObserver /*implements PageConfigurator*/ {

    protected RequestDelegate commandGateway;
    private final MultiselectComboBox<InstanceType> instanceTypeComboBox =
            new MultiselectComboBox<>("Instance Type (rule starting point/context)");
    private final Select<String> outputFormatSelect = new Select<>();
    private static final Map<String, String> outputFormatMap = Map.ofEntries(
            Map.entry("JSON", "{"),
            Map.entry("XML", "<"),
            Map.entry("Protobuf", "message"));
    private final TextField customPromptTextField = new TextField();
    private HuggingFace huggingFace;
    ProgressBar spinner = new ProgressBar();
    ObjectMapper mapper = new ObjectMapper();

    private final TextArea arlArea = new TextArea();
    private MessageList botUI = new MessageList();
    private MessageInput input = new MessageInput();
    Grid<Anchor> grid = new Grid<>(Anchor.class, false);


    private Set<InstanceType> lastUsedInstanceTypes = null;


    private String user;

    public TestDataPlaygroundView(
            RequestDelegate commandGateway,
            @Qualifier("huggingface") HuggingFace huggingFace,
            SecurityService securityService) {
        this.commandGateway = commandGateway;
        this.huggingFace = huggingFace;
        setMargin(false);
        statePanel();
        user = securityService.getAuthenticatedUser() != null ? securityService.getAuthenticatedUser().getUsername() : "user";
    }


    private void statePanel() {
        VerticalLayout layout = this;


        Component testDataComponent = getTestDataComponent();

    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
    }

    private Dialog getConfirmDialog(String msg, ContinueNavigationAction action) {
        final Dialog dialog = new Dialog();
        H3 header = new H3("Changes to OCL/ARL rule detected");
        Span text = new Span(msg);
        Button btnStay = new Button("Stay");
        btnStay.addClickListener(event -> dialog.close());
        Button btnLeave = new Button("Leave");
        btnLeave.addClickListener(event -> {
            dialog.close();
            action.proceed();
        });

        HorizontalLayout buttonPart = new HorizontalLayout();
        buttonPart.add(btnStay, btnLeave);
        buttonPart.setJustifyContentMode(JustifyContentMode.BETWEEN);

        VerticalLayout layout = new VerticalLayout();
        layout.add(header, text, buttonPart);
        layout.setAlignSelf(Alignment.STRETCH, text);

        dialog.add(layout);
        dialog.setMaxWidth("300px");
        return dialog;

    }

    private Component getTestDataComponent() {
        VerticalLayout layout = new VerticalLayout();
        // field to provide context: instance type
        List<InstanceType> instTypes = commandGateway.getWorkspace().debugInstanceTypes().stream()
                .filter(iType -> !iType.isDeleted)
                .sorted(new InstanceTypeComparator())
                .collect(Collectors.toList());
        instanceTypeComboBox.setItems(instTypes);
        instanceTypeComboBox.setItemLabelGenerator(iType -> String.format("%s (DSid: %s FQN: %s ) ", iType.name(), iType.id().toString(), iType.getQualifiedName()));
        instanceTypeComboBox.setWidthFull();
        instanceTypeComboBox.setMinWidth("100px");
        layout.add(instanceTypeComboBox);

        outputFormatSelect.setLabel("Format");
        outputFormatSelect.setItems(outputFormatMap.keySet());
        outputFormatSelect.setValue("JSON");

        layout.add(outputFormatSelect);

        customPromptTextField.setPlaceholder("Enter Prompt");
        customPromptTextField.setWidthFull();
        layout.add(customPromptTextField);

        // Button to send
        Button generateButton = new Button("Generate");

        Button validateButton = new Button("Validate");

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.add(generateButton);
        buttonLayout.add(validateButton);

        layout.add(buttonLayout);

        TextArea textArea = new TextArea();
        textArea.setWidthFull();
        textArea.setHeight("400px");
        layout.add(textArea);


        spinner.setIndeterminate(true);
        spinner.setVisible(false); // Hide spinner initially
        layout.add(spinner);
        generateButton.setEnabled(false);
        instanceTypeComboBox.addSelectionListener(multiSelectionEvent ->
                generateButton.setEnabled(!multiSelectionEvent.getAllSelectedItems().isEmpty()));

        generateButton.addClickListener(buttonClickEvent -> {
            // reset components
            grid.setVisible(false);
            spinner.setVisible(true);
            textArea.setValue("");
            // send request to huggingface
            huggingFace.sendAsync(
                            new OCLBot.TestDataBotRequest(
                                    Instant.now(),
                                    "user", customPromptTextField.getValue(),
                                    instanceTypeComboBox.getValue(),
                                    Map.entry(outputFormatSelect.getValue(), outputFormatMap.get(outputFormatSelect.getValue()))))
                    .thenAccept(botResult -> {
                        if (botResult != null) {
                            lastUsedInstanceTypes = instanceTypeComboBox.getValue();
                            getUI().ifPresent(ui -> ui.access(() -> {
                                log.info("Test data: " + ((OCLBot.TestDataBotResult) botResult).getTestData());
                                textArea.setValue(((OCLBot.TestDataBotResult) botResult).getTestData());
                                spinner.setVisible(false);
                                Notification.show("Test data generated successfully.");
                                ui.push();
                            }));
                        } else {
                            Notification.show("Test data generation failed.");
                        }
                    })
                    .exceptionally(throwable -> {
                        Notification.show("Test data generation failed.");
                        return null;
                    });
        });

        validateButton.setEnabled(false);
        validateButton.addClickListener(buttonClickEvent -> {
            validateTestData(textArea.getValue());
        });

        Button exportButton = new Button("Export");
        layout.add(exportButton);
        grid.removeAllColumns();
        grid.setVisible(false);
        layout.add(grid);

        //if textarea has no value -> disable export button
        textArea.addValueChangeListener(textAreaValueChangeEvent -> {
            validateButton.setEnabled(!textAreaValueChangeEvent.getValue().isEmpty());
            exportButton.setEnabled(!textAreaValueChangeEvent.getValue().isEmpty());
        });
        exportButton.setEnabled(false);
        // map text from text area (as Jsonnode) to selected InstanceType as new InstanceType object
        exportButton.addClickListener(buttonClickEvent -> {
            var createdInstances = new ArrayList<Instance>();
            var resultInstances = new ArrayList<Instance>();
            var instanceTypeList = new HashSet<InstanceType>();
            instanceTypeComboBox.getValue()
                    .forEach(type -> type
                            .getPropertyTypes(true)
                            .stream()
                            .forEach(propertyType -> {
                                if (propertyType.workspace.META_INSTANCE_TYPE.equals(propertyType.referencedInstanceType().getInstanceType())) {
                                    instanceTypeList.add(propertyType.referencedInstanceType());
                                }
                            }));
            if (outputFormatSelect.getValue().equals("JSON")) {
                try {
                    JsonNode readTree = mapper.readTree(textArea.getValue());
                    instanceTypeList.stream().iterator().forEachRemaining(instanceType -> {
                        JsonNode instanceTypeJsonNode = readTree.get(instanceType.name());
                        if (instanceTypeJsonNode == null) {
                            log.warn(String.format("Instance type %s not found in JSON data.", instanceType.name()));
                            return;
                        }

                        if (instanceTypeJsonNode.getNodeType().name().equals("ARRAY")) {
                            // object is an array
                            log.info("Instance type is an array.");
                            for (JsonNode node : instanceTypeJsonNode) {
                                var instance = searchForReference(instanceType, node, createdInstances);
                                if (instance == null) {
                                    instance = this.commandGateway.getWorkspace().createInstance(instanceType, instanceType.name());
                                    resultInstances.add(instance);
                                    createdInstances.add(instance);
                                }
                                Instance finalInstance = instance;
                                node.fields().forEachRemaining(entry -> {
                                    mapJsonNodeToInstance(instanceType, entry, finalInstance, createdInstances);
                                });
                            }
                        } else {
                            var instance = searchForReference(instanceType, instanceTypeJsonNode, createdInstances);
                            if (instance == null) {
                                instance = this.commandGateway.getWorkspace().createInstance(instanceType, instanceType.name());
                                resultInstances.add(instance);
                                createdInstances.add(instance);
                            }
                            Instance finalInstance = instance;
                            instanceTypeJsonNode.fields().forEachRemaining(entry -> {
                                mapJsonNodeToInstance(instanceType, entry, finalInstance, createdInstances);
                            });
                        }
                    });
                } catch (JsonProcessingException e) {
                    Notification.show("Invalid JSON data.");
                    throw new RuntimeException(e);
                }
            } else
            {
                Notification.show("Export to " + outputFormatSelect.getValue() + " not supported.");
            }
//            workspace.concludeTransaction();

            // display result instances
            List<Anchor> instanceLinks = new ArrayList();
            resultInstances.forEach(instance -> {
                Anchor anchor = convertToResourceLinkWithBlankTarget(instance);
                anchor.setText(instance.name());
                instanceLinks.add(anchor);
            });
            grid.setItems(instanceLinks);
            grid.removeAllColumns();
            grid.addColumn(new ComponentRenderer<>(item -> item)).setHeader("Result Instances");
            grid.setVisible(true);

        });
        return layout;
    }

    /**
     * Maps the JSON node to the instance
     * @param instanceType
     * @param entry
     * @param instance
     * @param createdInstances
     */
    private void mapJsonNodeToInstance(InstanceType instanceType,
                                       Map.Entry<String, JsonNode> entry,
                                       Instance instance,
                                       ArrayList<Instance> createdInstances) {

        instanceType.getPropertyTypes(false, true).stream()
                .filter(propertyType -> propertyType.name().equals(entry.getKey()))
                .findFirst()
                .ifPresent(propertyType -> {
                            log.info("PropertyType: " + propertyType.name());
                            log.info("PropertyType cardinality: " + propertyType.cardinality());
                            log.info("PropertyType referencedInstanceType: " + propertyType.referencedInstanceType().name());
                            switch (propertyType.cardinality()) {
                                case SINGLE -> {
                                    if (instance.getProperty(entry.getKey()).propertyType.referencedInstanceType().name() == "String") {
                                        if (entry.getValue() != null && !StringUtils.isEmpty(entry.getValue().asText()))
                                            instance.getProperty(entry.getKey()).set(entry.getValue().asText());
                                    } else if (instance.getProperty(entry.getKey()).propertyType.referencedInstanceType().name() == "Integer") {
                                        if (entry.getValue() != null)
                                            instance.getProperty(entry.getKey()).set(entry.getValue().asInt());
                                    } else if (instance.getProperty(entry.getKey()).propertyType.referencedInstanceType().name() == "Boolean") {
                                        if (entry.getValue() != null)
                                            instance.getProperty(entry.getKey()).set(entry.getValue().asBoolean());
                                    } else if (instance.getProperty(entry.getKey()).propertyType.referencedInstanceType().name() == "Double") {
                                        if (entry.getValue() != null)
                                            instance.getProperty(entry.getKey()).set(entry.getValue().asDouble());
                                    } else {
                                        // nested Object
                                        var subInstance = searchForReference(propertyType.referencedInstanceType(), entry.getValue(), createdInstances);
                                        if (subInstance == null) {
                                            subInstance = this.commandGateway.getWorkspace().createInstance(propertyType.referencedInstanceType(), entry.getKey());
                                            createdInstances.add(subInstance);
                                        }
                                        Instance finalSubInstance = subInstance;
                                        entry.getValue().fields().forEachRemaining(subEntry -> {
                                            mapJsonNodeToInstance(propertyType.referencedInstanceType(), subEntry, finalSubInstance, createdInstances);
                                        });
                                        instance.getProperty(entry.getKey()).set(subInstance);
                                    }
                                }
                                case LIST -> {
                                    ListProperty listProperty = (ListProperty) instance.getProperty(entry.getKey());
                                    if (entry.getValue().getNodeType().name().equals("ARRAY")) {
                                        entry.getValue().elements().forEachRemaining(e -> {
                                            if (propertyType.workspace.META_INSTANCE_TYPE.equals(propertyType.referencedInstanceType().getInstanceType())) {
                                                var listEntryInstance = searchForReference(propertyType.referencedInstanceType(), e, createdInstances);
                                                if (listEntryInstance == null && propertyType.workspace.META_INSTANCE_TYPE.equals(propertyType.referencedInstanceType().getInstanceType())) {
                                                    listEntryInstance = this.commandGateway.getWorkspace().createInstance(propertyType.referencedInstanceType(), entry.getKey());
                                                    createdInstances.add(listEntryInstance);
                                                }
                                                Instance finalListEntryInstance = listEntryInstance;
                                                e.fields().forEachRemaining(setEntry -> {
                                                    mapJsonNodeToInstance(propertyType.referencedInstanceType(), setEntry, finalListEntryInstance, createdInstances);
                                                });
                                                listProperty.add(listEntryInstance);
                                            } else {
                                                if (propertyType.referencedInstanceType().name().equals("String"))
                                                    listProperty.add(e.asText());
                                                else if (propertyType.referencedInstanceType().name().equals("Integer"))
                                                    listProperty.add(e.asInt());
                                                else if (propertyType.referencedInstanceType().name().equals("Boolean"))
                                                    listProperty.add(e.asBoolean());
                                                else if (propertyType.referencedInstanceType().name().equals("Double"))
                                                    listProperty.add(e.asDouble());
                                            }
                                        });
                                    }
                                    instance.getProperty(entry.getKey()).set(listProperty);
                                }
                                case SET -> {
                                    SetProperty setProperty = (SetProperty) instance.getProperty(entry.getKey());
                                    if (entry.getValue().getNodeType().name().equals("ARRAY")) {
                                        entry.getValue().elements().forEachRemaining(e -> {
                                            if (propertyType.workspace.META_INSTANCE_TYPE.equals(propertyType.referencedInstanceType().getInstanceType())) {
                                                var setEntryInstance = searchForReference(propertyType.referencedInstanceType(), e, createdInstances);
                                                if (setEntryInstance == null) {
                                                    setEntryInstance = this.commandGateway.getWorkspace().createInstance(propertyType.referencedInstanceType(), entry.getKey());
                                                    createdInstances.add(setEntryInstance);
                                                }
                                                Instance finalSetEntryInstance = setEntryInstance;
                                                e.fields().forEachRemaining(setEntry -> {
                                                    mapJsonNodeToInstance(propertyType.referencedInstanceType(), setEntry, finalSetEntryInstance, createdInstances);
                                                });
                                                setProperty.add(setEntryInstance);
                                            } else {
                                                if (propertyType.referencedInstanceType().name().equals("String"))
                                                    setProperty.add(e.asText());
                                                else if (propertyType.referencedInstanceType().name().equals("Integer"))
                                                    setProperty.add(e.asInt());
                                                else if (propertyType.referencedInstanceType().name().equals("Boolean"))
                                                    setProperty.add(e.asBoolean());
                                                else if (propertyType.referencedInstanceType().name().equals("Double"))
                                                    setProperty.add(e.asDouble());
                                            }
                                        });
                                    }
                                    instance.getProperty(entry.getKey()).set(setProperty);
                                }
                            }
                        }
                );
    }

    /**
     * Searches for the reference in the created instances by id or name
     * @param instanceType
     * @param entry
     * @param createdInstances
     * @return
     */
    private Instance searchForReference(InstanceType instanceType, JsonNode entry, ArrayList<Instance> createdInstances) {
        return createdInstances.stream()
                .filter(instance -> instance.getInstanceType().id().equals(instanceType.id()))
                .filter(instance -> {
                    if (instance.getProperty("id") != null && entry.get("id") != null &&
                            instance.getProperty("id").getValue() != null)
                        return instance.getProperty("id").getValue().toString().equals(entry.get("id").asText());
                    else if (instance.getProperty("name") != null && entry.get("name") != null &&
                            instance.getProperty("name").getValue() != null)
                        return instance.getProperty("name").getValue().toString().equals(entry.get("name").asText());
                    else
                        return false;
                })
                .findFirst()
                .orElse(null);
    }

    private void validateTestData(String testData) {
        if (outputFormatSelect.getValue().equals("JSON")) {
            try {
                JsonNode actualObj = mapper.readTree(testData);
                Notification.show("Test data validated successfully.");
            } catch (JsonProcessingException e) {
                Notification.show("Invalid JSON data.");
                throw new RuntimeException(e);
            }
        }
    }



    public static class InstanceTypeComparator implements Comparator<InstanceType> {

        @Override
        public int compare(InstanceType o1, InstanceType o2) {
            if (o1.getQualifiedName() == null || o2.getQualifiedName() == null)
                return o1.name().compareTo(o2.name());
            else
                return o1.getQualifiedName().compareTo(o2.getQualifiedName());
        }

    }
}
