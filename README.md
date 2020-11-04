# Framework for passive Engineering-Process Management

The foundation for this project is a quality-assurance-process support framework, which facilitates [Drools](https://www.drools.org/) as business rules management system in order to show engineering process state and check process constraints on artifacts of traceability information systems, like [Jira](https://www.atlassian.com/de/software/jira).

The main new aspect added in this work is the ability to get the process state of the past.
To provide this information an event-sourcing framework named [Axon](https://axoniq.io/) is used.
Axon comes with the CQRS pattern (Command Query Responsibility Segregation) of Domain-Driven Design, which had to be incooperated into the existing framework.
Alongside this redesign of the architecture and the new features for replay, the various reused components of the framework were enhanced too and some of the software components are located in other repositories of our institute (c4s.passiveprocessengine.v2 and c4s.impactassessment.components).

## Command Model
In this application an aggregate manages the workflow state, which is modelled in the passive-process-engine. This is about modelling an engineering process, adding constraints and passively update its state. But you can think of it just as the (workflow) model of the framework.
The user initiates changes to the state of the workflow model by sending commands to the aggregate. This can happen for example via a user interface or via the result of a rule in the knowledge base (Drools). These commands trigger events, that cause the change of the aggregate and are stored in the event-store. From this events the current state of an aggregate can be recreated, replayed and so on.

## Query Model
The workflow-projection receives state update events via the Axon framework and projects the changes onto the view model (which is in our case also represented by the passive process engine). The projection not only build its own workflow state but additionally puts selected workflow artifacts into the knowledgebase where certain checks are triggered and may send new commands. Additionally the Snapshotter can open new event streams of the event store until a given point in time, to recreate a state from the past.
A user can access the view model via queries to the projection.

## Architecture
![](doc/architecture.PNG)
The architecture is structured into four main parts. On the left and right are the command- and query-side of the CQRS-pattern located, indicated by those black dotted boxes. Inbetween the event store persists all events and enables us to recreate the state of the workflow. On the bottom the data connector is located, which is currently only connected to Jira but will be extended to a more generic interface soon. On the top we have the "Workflow Instance Wrapper", which is an instance of a workflow definition from the passive process engine.

Additionally the command/event flow between the various components is indicated by those blue dotted lines.

## Workflow
For a more detailled explanation how the workflow is modelled look into the wiki.
