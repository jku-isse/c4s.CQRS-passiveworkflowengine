package impactassessment.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import impactassessment.api.Commands.*;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.definition.AbstractIdentifiableObject;
import passiveprocessengine.definition.ArtifactType;
import passiveprocessengine.definition.NoOpTaskDefinition;
import passiveprocessengine.instance.ArtifactIO;
import passiveprocessengine.instance.ArtifactInput;
import passiveprocessengine.instance.ArtifactOutput;
import passiveprocessengine.instance.QACheckDocument;
import passiveprocessengine.instance.ResourceLink;
import passiveprocessengine.instance.RuleEngineBasedConstraint;
import passiveprocessengine.instance.WorkflowInstance;
import passiveprocessengine.instance.WorkflowTask;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
@CssImport(value="./styles/grid-styles.css")
@CssImport(
        value= "./styles/dialog-overlay.css",
        themeFor = "vaadin-dialog-overlay"
)
public class WorkflowTreeGrid extends TreeGrid<AbstractIdentifiableObject> {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
    private Function<Object, Object> f;
    private boolean evalMode;


    public WorkflowTreeGrid(Function<Object, Object> f, boolean evalMode) {
        this.f = f;
        this.evalMode = evalMode;
    }

    public void initTreeGrid() {

        // Column "Workflow Instance"
        this.addHierarchyColumn(o -> {
            if (o instanceof WorkflowInstance) {
                WorkflowInstance wfi = (WorkflowInstance) o;
                int i = wfi.getId().indexOf("WF-");
                String id = i < 0 ? wfi.getId() : wfi.getId().substring(0, i+2).concat("...").concat(wfi.getId().substring(wfi.getId().length()-5));
                return wfi.getType().getId() + " (" + id + ")";
            } else if (o instanceof WorkflowTask) {
                WorkflowTask wft = (WorkflowTask) o;
                if (wft.getName() != null) {
                    return wft.getName();
                } else {
                    return wft.getType().getId();
                }
            } else if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                return rebc.getDescription();
            } else {
                return o.getClass().getSimpleName() + ": " + o.getId();
            }
        }).setHeader("Workflow Instance").setWidth("35%");

        // Column "Info"

        this.addColumn(new ComponentRenderer<Component, AbstractIdentifiableObject>(o -> {
            if (o instanceof WorkflowInstance) {
                return infoDialog((WorkflowInstance)o);
            } else if (o instanceof WorkflowTask) {
                return infoDialog((WorkflowTask)o);
            } else {
                return new Label("");
            }
        })).setWidth("5%").setFlexGrow(0);

        // Column "Last Evaluated"

        this.addColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                try {
                    return formatter.format(rebc.getLastEvaluated());
                } catch (DateTimeException e) {
                    return "not available";
                }
            } else {
                return "";
            }
        }).setHeader("Last Evaluated");

        // Column "Last Changed"

        this.addColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                try {
                    return formatter.format(rebc.getLastChanged());
                } catch (DateTimeException e) {
                    return "not available";
                }
            } else {
                return "";
            }
        }).setHeader("Last Changed");

        // Column delete

        this.addColumn(new ComponentRenderer<Component, AbstractIdentifiableObject>(o -> {
            if (o instanceof WorkflowInstance) {
                WorkflowInstance wfi = (WorkflowInstance) o;
                Icon icon = new Icon(VaadinIcon.TRASH);
                icon.setColor("red");
                icon.getStyle().set("cursor", "pointer");
                icon.addClickListener(e -> {
                    f.apply(new DeleteCmd(wfi.getId()));
                });
                icon.getElement().setProperty("title", "Remove this workflow");
                return icon;
            } else {
                return new Label("");
            }
        })).setClassNameGenerator(x -> "column-center").setWidth("5%").setFlexGrow(0);

        // Column "Reevaluate"

        if (evalMode) {
            this.addColumn(new ComponentRenderer<Component, AbstractIdentifiableObject>(o -> {
                if (o instanceof WorkflowInstance) {
                    WorkflowInstance wfi = (WorkflowInstance) o;
                    Icon icon = new Icon(VaadinIcon.REPLY_ALL);
                    icon.setColor("#1565C0");
                    icon.getStyle().set("cursor", "pointer");
                    icon.addClickListener(e -> {
                        f.apply(new CheckAllConstraintsCmd(wfi.getId()));
                        Notification.show("Evaluation of "+wfi.getId()+" requested");
                    });
                    icon.getElement().setProperty("title", "Request a explicit re-evaluation of all rules for this artifact..");
                    return icon;
                } else if (o instanceof RuleEngineBasedConstraint) {
                    RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                    Icon icon = new Icon(VaadinIcon.REPLY);
                    icon.setColor("#1565C0");
                    icon.getStyle().set("cursor", "pointer");
                    icon.addClickListener(e -> {
                        f.apply(new CheckConstraintCmd(rebc.getWorkflow().getId(), rebc.getId()));
                        Notification.show("Evaluation of "+rebc.getId()+" requested");
                    });
                    icon.getElement().setProperty("title", "Request a explicit re-evaluation of this rule for this artifact..");
                    return icon;
                } else {
                    return new Label("");
                }
            })).setClassNameGenerator(x -> "column-center").setWidth("5%").setFlexGrow(0);
        }

        // Column "Unsatisfied" or "Fulfilled"

        this.addColumn(new ComponentRenderer<Component, AbstractIdentifiableObject>(o -> {
            if (o instanceof WorkflowInstance) {
                WorkflowInstance wfi = (WorkflowInstance) o;
                boolean unsatisfied = wfi.getWorkflowTasksReadonly().stream()
                        .anyMatch(wft -> wft.getOutput().stream()
                                .map(ArtifactIO::getArtifact)
                                .filter(a -> a instanceof passiveprocessengine.instance.QACheckDocument)
                                .map(a -> (QACheckDocument) a)
                                .map(QACheckDocument::getConstraintsReadonly)
                                .anyMatch(a -> a.stream()
                                        .anyMatch(c -> !c.isFulfilled()))
                        );
                boolean fulfilled = wfi.getWorkflowTasksReadonly().stream()
                        .anyMatch(wft -> wft.getOutput().stream()
                                .map(ArtifactIO::getArtifact)
                                .filter(a -> a instanceof QACheckDocument)
                                .map(a -> (QACheckDocument) a)
                                .map(QACheckDocument::getConstraintsReadonly)
                                .anyMatch(a -> a.stream()
                                        .anyMatch(QACheckDocument.QAConstraint::isFulfilled))
                        );
                return getIcon(unsatisfied, fulfilled);
            } else if (o instanceof WorkflowTask) {
                WorkflowTask wft = (WorkflowTask) o;
                boolean unsatisfied = wft.getOutput().stream()
                                .map(ArtifactIO::getArtifact)
                                .filter(a -> a instanceof QACheckDocument)
                                .map(a -> (QACheckDocument) a)
                                .map(QACheckDocument::getConstraintsReadonly)
                                .anyMatch(a -> a.stream()
                                        .anyMatch(c -> !c.isFulfilled()));
                boolean fulfilled = wft.getOutput().stream()
                        .map(ArtifactIO::getArtifact)
                        .filter(a -> a instanceof QACheckDocument)
                        .map(a -> (QACheckDocument) a)
                        .map(QACheckDocument::getConstraintsReadonly)
                        .anyMatch(a -> a.stream()
                                .anyMatch(QACheckDocument.QAConstraint::isFulfilled));
                return getIcon(unsatisfied, fulfilled);
            } else if (o instanceof RuleEngineBasedConstraint) {
                return infoDialog((RuleEngineBasedConstraint)o);
            } else {
                return new Label("");
            }
        })).setClassNameGenerator(x -> "column-center").setWidth("5%").setFlexGrow(0);
    }

    private Icon getIcon(boolean unsatisfied, boolean fulfilled) {
        Icon icon;
        if (unsatisfied && fulfilled) {
            icon = new Icon(VaadinIcon.WARNING);
            icon.setColor("#E24C00");
            icon.getElement().setProperty("title", "This contains unsatisfied and fulfilled constraints");
        } else if (unsatisfied) {
            icon = new Icon(VaadinIcon.CLOSE_CIRCLE);
            icon.setColor("red");
            icon.getElement().setProperty("title", "This contains unsatisfied constraints");
        } else if (fulfilled){
            icon = new Icon(VaadinIcon.CHECK_CIRCLE);
            icon.setColor("green");
            icon.getElement().setProperty("title", "This contains fulfilled constraints");
        } else {
            icon = new Icon(VaadinIcon.QUESTION_CIRCLE);
            icon.setColor("#1565C0");
            icon.getElement().setProperty("title", "Constraints not evaluated");
        }
        return icon;
    }

    private Component infoDialog(WorkflowInstance wfi) {
        VerticalLayout l = new VerticalLayout();
        l.setClassName("scrollable");
        Paragraph p = new Paragraph("Process Instance ID:");
        p.setClassName("info-header");
        l.add(p);
        H3 h3 = new H3(wfi.getId());
        h3.setClassName("info-header");
        l.add(h3);
        if (wfi.getPropertiesReadOnly().size() > 0) {
            H4 h4 = new H4("Properties");
            h4.setClassName("no-margin");
            l.add(h4);
            UnorderedList list = new UnorderedList();
            for (Map.Entry<String, String> e : wfi.getPropertiesReadOnly()) {
                list.add(new ListItem(e.getKey() + ": " + e.getValue()));
            }
            l.add(list);
        }
        infoDialogInputOutput(l, wfi.getInput(), wfi.getOutput(), wfi.getType().getExpectedInput(), wfi.getType().getExpectedOutput());
        Dialog dialog = new Dialog();
        dialog.setWidth("80%");
        dialog.setMaxHeight("80%");

        Icon icon = new Icon(VaadinIcon.INFO_CIRCLE);
        icon.setColor("#1565C0");
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> dialog.open());
        icon.getElement().setProperty("title", "Show more information about this workflow instance");

        dialog.add(l);

        return icon;
    }

    private Component infoDialog(WorkflowTask wft) {
        VerticalLayout l = new VerticalLayout();
        l.setClassName("scrollable");
        Paragraph p = new Paragraph("Process Step ID:");
        p.setClassName("info-header");
        l.add(p);
        H3 h3 = new H3(wft.getId());
        h3.setClassName("info-header");
        l.add(h3);

        if (wft.getLifecycleState() != null)
            l.add(new Paragraph("Process Step Lifecycle State: "+wft.getLifecycleState().name()));
        infoDialogInputOutput(l, wft.getInput(), wft.getOutput(), wft.getType().getExpectedInput(), wft.getType().getExpectedOutput());
        Dialog dialog = new Dialog();
        dialog.setMaxHeight("80%");
        dialog.setWidth("80%");

        Icon icon = new Icon(VaadinIcon.INFO_CIRCLE_O);
        icon.setColor("#1565C0");
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> dialog.open());
        icon.getElement().setProperty("title", "Show more information about this workflow task");

        dialog.add(l);

        return icon;
    }

    private void infoDialogInputOutput(VerticalLayout l, List<ArtifactInput> inputs, List<ArtifactOutput> outputs, Map<String, ArtifactType> expectedInput, Map<String, ArtifactType> expectedOutput) {
        HorizontalLayout line = new HorizontalLayout();
        line.setClassName("line");
        Button addInput = new Button("Add Input", evt -> {
            Notification.show("coming soon");
        });
        H4 h4 = new H4("Inputs");
        h4.setClassName("left-margin");
        line.add(h4, addInput);
        l.add(line);
        VerticalLayout inLayout = new VerticalLayout();
        inLayout.setClassName("card-border");
        inLayout.add(new H5("Expected"));
        UnorderedList list = new UnorderedList();
        list.setClassName("no-margin");
        if (expectedInput.size() > 0) {
            for (Map.Entry<String, ArtifactType> entry : expectedInput.entrySet()) {
                list.add(new ListItem(entry.getKey() + " (" + entry.getValue().getArtifactType() + ")"));
            }
        } else {
            ListItem li = new ListItem("no input expected");
            li.setClassName("italic");
            list.add(li);
        }
        inLayout.add(list);
        inLayout.add(new H5("Present"));
        UnorderedList list2 = new UnorderedList();
        list2.setClassName("no-margin");
        if (inputs.size() > 0) {
            for (ArtifactInput ai : inputs) {
                list2.add(new ListItem(ai.getRole() + " (" + ai.getArtifactType().getArtifactType() + "): " + ai.getArtifact().getId()));
            }
        } else {
            ListItem li = new ListItem("no input present");
            li.setClassName("italic");
            list2.add(li);
        }
        inLayout.add(list2);
        l.add(inLayout);

        HorizontalLayout line2 = new HorizontalLayout();
        line2.setClassName("line");
        Button addOutput = new Button("Add Output", evt -> {
            Notification.show("coming soon");
        });
        H4 h41 = new H4("Outputs");
        h41.setClassName("left-margin");
        line2.add(h41, addOutput);
        l.add(line2);
        VerticalLayout outLayout = new VerticalLayout();
        outLayout.setClassName("card-border");
        outLayout.add(new H5("Expected"));
        UnorderedList list3 = new UnorderedList();
        list3.setClassName("no-margin");
        if (expectedOutput.size() > 0) {
            for (Map.Entry<String, ArtifactType> entry : expectedOutput.entrySet()) {
                list3.add(new ListItem(entry.getKey() + " (" + entry.getValue().getArtifactType() + ")"));
            }
        } else {
            ListItem li = new ListItem("no output expected");
            li.setClassName("italic");
            list3.add(li);
        }
        outLayout.add(list3);
        outLayout.add(new H5("Present"));
        UnorderedList list4 = new UnorderedList();
        list4.setClassName("no-margin");
        if (outputs.size() > 0) {
            for (ArtifactOutput ao : outputs) {
                list4.add(new ListItem(ao.getRole() + " (" + ao.getArtifactType().getArtifactType() + "): " + ao.getArtifact().getId()));
            }
        } else {
            ListItem li = new ListItem("no output present");
            li.setClassName("italic");
            list4.add(li);
        }
        outLayout.add(list4);
        l.add(outLayout);
    }

    private Component infoDialog(RuleEngineBasedConstraint rebc) {
        VerticalLayout l = new VerticalLayout();
        l.setClassName("scrollable");

        Paragraph p = new Paragraph("Quality Assurance Document ID:");
        p.setClassName("info-header");
        l.add(p);
        H3 h3 = new H3(rebc.getParentArtifact().getId());
        h3.setClassName("info-header");
        l.add(h3);

        l.add(h3);
        l.add(new H4(rebc.getDescription()));
        // Unsatisfied resources
        List<Anchor> unsatisfiedLinks = new ArrayList<>();
        for (ResourceLink rl : rebc.getUnsatisfiedForReadOnly()) {
            Anchor a = new Anchor(rl.getHref(), rl.getTitle());
            a.setTarget("_blank");
            unsatisfiedLinks.add(a);
        }
        if (unsatisfiedLinks.size() > 0) {
            l.add(new H5("Unsatisfied by:"));
            for (Anchor a : unsatisfiedLinks) {
                HorizontalLayout h = new HorizontalLayout();
                h.setWidthFull();
                h.setMargin(false);
                h.setPadding(false);
                Icon icon = new Icon(VaadinIcon.CLOSE_CIRCLE_O);
                icon.setColor("red");
                h.add(icon, a);
                l.add(h);
            }
        }
        // Fulfilled resources
        List<Anchor> fulfilledLinks = new ArrayList<>();
        for (ResourceLink rl : rebc.getFulfilledForReadOnly()) {
            Anchor a = new Anchor(rl.getHref(), rl.getTitle());
            a.setTarget("_blank");
            fulfilledLinks.add(a);
        }
        if (fulfilledLinks.size() > 0) {
            l.add(new H5("Fulfilled by:"));
            for (Anchor a : fulfilledLinks) {
                HorizontalLayout h = new HorizontalLayout();
                h.setWidthFull();
                h.setMargin(false);
                h.setPadding(false);
                Icon icon = new Icon(VaadinIcon.CHECK_CIRCLE_O);
                icon.setColor("green");
                h.add(icon, a);
                l.add(h);
            }
        }

        Dialog dialog = new Dialog();
        dialog.add(l);
        dialog.setMaxHeight("80%");
        dialog.setMaxWidth("80%");

        Icon icon;
        if (!rebc.getEvaluationStatus().equals(QACheckDocument.QAConstraint.EvaluationState.SUCCESS)) {
            icon = new Icon(VaadinIcon.QUESTION_CIRCLE);
            icon.setColor("#1565C0");
            return icon;
        } else if (fulfilledLinks.size() > 0 && unsatisfiedLinks.size() > 0) {
            icon = new Icon(VaadinIcon.WARNING);
            icon.setColor("#E24C00");
        } else if (fulfilledLinks.size() > 0) {
            icon = new Icon(VaadinIcon.CHECK_CIRCLE);
            icon.setColor("green");
        } else if (unsatisfiedLinks.size() > 0) {
            icon = new Icon(VaadinIcon.CLOSE_CIRCLE);
            icon.setColor("red");
        } else {
            icon = new Icon(VaadinIcon.QUESTION_CIRCLE);
            icon.setColor("#1565C0");
            return icon;
        }
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> dialog.open());
        icon.getElement().setProperty("title", "Show all resources of this rule");

        return icon;
    }

    public void updateTreeGrid(List<WorkflowInstanceWrapper> content) {
        if (content != null) {
            this.setItems(content.stream().map(WorkflowInstanceWrapper::getWorkflowInstance), o -> {
                if (o instanceof WorkflowInstance) {
                    WorkflowInstance wfi = (WorkflowInstance) o;
                    return wfi.getWorkflowTasksReadonly().stream()
                            .filter(wft -> !(wft.getType() instanceof NoOpTaskDefinition))
                            .map(wft -> (AbstractIdentifiableObject) wft);
                } else if (o instanceof WorkflowTask) {
                    WorkflowTask wft = (WorkflowTask) o;
                    Optional<QACheckDocument> qacd =  wft.getOutput().stream()
                            .map(ArtifactIO::getArtifact)
                            .filter(io -> io instanceof QACheckDocument)
                            .map(io -> (QACheckDocument) io)
                            .findFirst();
                    return qacd.map(qaCheckDocument -> qaCheckDocument.getConstraintsReadonly().stream()
                            .map(x -> (AbstractIdentifiableObject) x))
                            .orElseGet(Stream::empty);
                }/* else if (o instanceof QACheckDocument) {
                    QACheckDocument qacd = (QACheckDocument) o;
                    return qacd.getConstraintsReadonly().stream().map(x -> (IdentifiableObject) x);
                }*/ else if (o instanceof RuleEngineBasedConstraint) {
                    return Stream.empty();
                } else {
                    log.error("TreeGridPanel got unknown artifact: " + o.getClass().getSimpleName());
                    return Stream.empty();
                }
            });
            this.getDataProvider().refreshAll();
        }
    }
}
