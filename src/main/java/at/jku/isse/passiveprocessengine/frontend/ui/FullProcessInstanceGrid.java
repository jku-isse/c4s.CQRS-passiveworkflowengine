package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.stream.Stream;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.treegrid.TreeGrid;

import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CssImport(value="./styles/grid-styles.css")
@CssImport(
        value= "./styles/dialog-overlay.css",
        themeFor = "vaadin-dialog-overlay"
)
 
public class FullProcessInstanceGrid extends TreeGrid<ProcessInstanceScopedElement> {/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public FullProcessInstanceGrid() {
		this.addComponentHierarchyColumn(o -> {
            if (o instanceof ProcessInstance) {
                ProcessInstance wfi = (ProcessInstance) o;
                Span span= new Span(wfi.getName());
                span.getElement().setProperty("title", wfi.getDefinition().getName() + " (" + wfi.getName() + ")");
                return span;
            } else if (o instanceof ProcessStep) {
                ProcessStep wft = (ProcessStep) o;
                    Span span = new Span(wft.getDefinition().getName());
                    span.getElement().setProperty("title", wft.getDefinition().getName());
                    return span;
                //}            
            } else if (o instanceof DecisionNodeInstance) {
            	DecisionNodeInstance dni = (DecisionNodeInstance)o;
                Span span = new Span(dni.getDefinition().getInFlowType().toString());                
                return span;
            //}            
        }
            else {
                return new Paragraph(o.getClass().getSimpleName() +": " + o.getName());
            }
        }).setHeader("Process Structure").setWidth("80%");
	}
	
	public void updateProcessGrid(ProcessInstance pi) {
		this.setItems(pi.getProcessSteps().stream()                
                .map(x->x),
        o -> {
            if (o instanceof ProcessInstance) {
                ProcessInstance wfi = (ProcessInstance) o;
                return wfi.getProcessSteps().stream()                      
                		//.sorted(new StepComparator())
                		.map(wft -> (ProcessInstanceScopedElement) wft);
            } else if (o instanceof ProcessStep) {                
            	return Stream.empty();
            } else {
                log.error("TreeGridPanel got unexpected artifact: " + o.getClass().getSimpleName());
                return Stream.empty();
            }
        });
		this.getDataProvider().refreshAll();
		
		
	}
}
