package impactassessment.artifactconnector.jama;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import artifactapi.IArtifactService;
import artifactapi.jama.IJamaArtifact;
import artifactapi.jama.subtypes.IJamaProjectArtifact;
import artifactapi.jama.subtypes.IJamaRelease;
import artifactapi.jama.subtypes.IJamaUserArtifact;
import com.jamasoftware.services.restclient.jamadomain.TestCaseStep;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.*;
import com.jamasoftware.services.restclient.jamadomain.values.*;
import impactassessment.SpringUtil;
import impactassessment.artifactconnector.jama.subtypes.JamaProjectArtifact;
import impactassessment.artifactconnector.jama.subtypes.JamaRelease;
import impactassessment.artifactconnector.jama.subtypes.JamaUserArtifact;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.instance.ResourceLink;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class JamaArtifact implements IJamaArtifact {
    public void setJamaService(IJamaService jamaService) {
		this.jamaService = jamaService;
	}

	// id for axon application
    private ArtifactIdentifier artifactIdentifier;
    // fields of jama item
    private Integer id;
    private Boolean isProject;
    private String name;
    private String globalId;
    private String itemType;
    private String documentKey;
    private Date createdDate;
    private Date modifiedDate;
    private Date lastActivityDate;

    private List<String> children;
    private List<String> prefetchItems;
    private List<Integer> downstreamItems;
    private List<Integer> upstreamItems;

    private Map<String, String> stringValues = new HashMap<>();
    private Map<String, List<String>> stringListValues = new HashMap<>();
    private Map<String, Date> dateValues = new HashMap<>();
    private Map<String, Integer> intValues = new HashMap<>();
    private Map<String, Boolean> booleanValues = new HashMap<>();
    private Map<String, IJamaProjectArtifact> projectValues = new HashMap<>();
    private Map<String, IJamaRelease> releaseValues = new HashMap<>();
    private Map<String, IJamaUserArtifact> userValues = new HashMap<>();

    private IJamaProjectArtifact jamaProjectArtifact;
    private IJamaUserArtifact userCreated;
    private IJamaUserArtifact userModified;

    private String resourceUrl;

    private transient IJamaService jamaService;

    public JamaArtifact(JamaItem jamaItem, IJamaService jamaService) {
        // id for axon application
    	this.jamaService = jamaService;
        this.artifactIdentifier = new ArtifactIdentifier(String.valueOf(jamaItem.getId()), IJamaArtifact.class.getSimpleName());
        // simple fields of jama item
        this.id = jamaItem.getId();
        this.isProject = jamaItem.isProject();
        this.name = jamaItem.getName() == null ? null : jamaItem.getName().getValue();
        this.globalId = jamaItem.getGlobalId();
        this.itemType = jamaItem.getItemType() == null ? null : jamaItem.getItemType().getTypeKey();
        this.documentKey = jamaItem.getDocumentKey();
        this.createdDate = jamaItem.getCreatedDate();
        this.modifiedDate = jamaItem.getModifiedDate();
        this.lastActivityDate = jamaItem.getLastActivityDate();
//        try {
//            this.children = jamaItem.getChildren().stream()
//                    .map(JamaItem::getDocumentKey)
//                    .map(String::valueOf)
//                    .collect(Collectors.toList());
//            this.prefetchItems = jamaItem.prefetchDownstreamItems().stream()
//                    .map(JamaItem::getDocumentKey)
//                    .map(String::valueOf)
//                    .collect(Collectors.toList());
//        } catch (RestClientException e) {
//            e.printStackTrace();
//        }

        this.downstreamItems = jamaItem.getDownstreamItemIds() == null ? new ArrayList<>() : jamaItem.getDownstreamItemIds();
        this.upstreamItems = jamaItem.getUpstreamItemIds() == null ? new ArrayList<>() : jamaItem.getUpstreamItemIds();

        for (JamaFieldValue jfv : jamaItem.getFieldValues()) {
            getString(jfv).ifPresent(s -> stringValues.put(jfv.getName(), s));
            getStringList(jfv).ifPresent(sl -> stringListValues.put(jfv.getName(), sl));
            getInt(jfv).ifPresent(i -> intValues.put(jfv.getName(), i));
            getBoolean(jfv).ifPresent(b -> booleanValues.put(jfv.getName(), b));
            getDate(jfv).ifPresent(d -> dateValues.put(jfv.getName(), d));
            getProject(jfv).ifPresent(p -> projectValues.put(jfv.getName(), p));
            getUser(jfv).ifPresent(u -> userValues.put(jfv.getName(), u));
            getRelease(jfv).ifPresent(r -> releaseValues.put(jfv.getName(), r));
        }
  
        // complex field of jama item

//        this.jamaProjectArtifact = jamaItem.getProject() != null ? new JamaProjectArtifact(jamaItem.getProject()) : null;
//        this.userCreated = jamaItem.getCreatedBy() != null ? new JamaUserArtifact(jamaItem.getCreatedBy()) : null;
//        this.userModified = jamaItem.getModifiedBy() != null ? new JamaUserArtifact(jamaItem.getModifiedBy()) : null;
        this.jamaProjectArtifact = jamaItem.getProject() != null ? jamaService.convertProject(jamaItem.getProject()) : null;
        this.userCreated = jamaItem.getCreatedBy() != null ? jamaService.convertUser(jamaItem.getCreatedBy()) : null;
        this.userModified = jamaItem.getModifiedBy() != null ? jamaService.convertUser(jamaItem.getModifiedBy()) : null;
        this.resourceUrl = jamaService.getJamaServerUrl(jamaItem);
    }

//    protected Optional<IJamaArtifact> fetch(String artifactId, String workflowId) {
//        log.info("Artifact fetching linked item: {}", artifactId);
//        if (artifactRegistry == null)
//            artifactRegistry = SpringUtil.getBean(IArtifactRegistry.class);
//        ArtifactIdentifier ai = new ArtifactIdentifier(artifactId, IJamaArtifact.class.getSimpleName());
//        Optional<IArtifact> a = artifactRegistry.get(ai, workflowId);
//        return a.map(artifact -> (IJamaArtifact) artifact);
//    }

    public void setArtifactIdentifier(ArtifactIdentifier artifactIdentifier) {
      this.artifactIdentifier = artifactIdentifier;
    }

	protected Optional<IJamaArtifact> fetch(Integer id) {
    	return jamaService.get(id);
    }

    @Override
    public ArtifactIdentifier getArtifactIdentifier() {
        return artifactIdentifier;
    }

    @Override
    public ResourceLink convertToResourceLink() {
        String context = String.valueOf(getId());
        String href = resourceUrl;
        String rel = "self";
        String as = getName();
        String linkType = "html";
        String title = getDocumentKey();
        return new ResourceLink(context, href, rel, as, linkType, title);
    }

    @Override
    public void injectArtifactService(IArtifactService service) {
        if (jamaService == null) {
            if (service instanceof IJamaService) {
                jamaService = (IJamaService) service;
            } else {
                log.warn("Injection of {} into JamaArtifact not possible.", service.getClass().getSimpleName());
            }
        }
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public boolean isProject() {
        return isProject;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getGlobalId() {
        return globalId;
    }

    @Override
    public IJamaProjectArtifact getProject() {
        return jamaProjectArtifact;
    }

    @Override
    public String getItemType() {
        return itemType;
    }

    @Override
    public IJamaUserArtifact getCreatedBy() {
        return userCreated;
    }

    @Override
    public IJamaUserArtifact getModifiedBy() {
        return userModified;
    }

    @Override
    public String getDocumentKey() {
        return documentKey;
    }

    @Override
    public Date getCreatedDate() {
        return createdDate;
    }

    @Override
    public Date getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public Date getLastActivityDate() {
        return lastActivityDate;
    }

    public List<String> getChildrenIds() {
        return children;
    }

    @Override
    public List<IJamaArtifact> getChildren(String workflowId) {
        return children.stream()
         //       .map(child -> fetch(child, workflowId))
        		.map(child -> fetch(Integer.parseInt(child)))
        		.filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public List<String> getPrefetchItemIds() {
        return prefetchItems;
    }

    @Override
    public List<IJamaArtifact> prefetchDownstreamItems(String workflowId) {
        return prefetchItems.stream()
               // .map(child -> fetch(child, workflowId))
        		.map(child -> fetch(Integer.parseInt(child)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public List<Integer> getDownstreamItemIds() {
        return downstreamItems;
    }

    @Override
    public List<IJamaArtifact> getDownstreamItems(String workflowId) {
        return downstreamItems.stream()
               // .map(child -> fetch(String.valueOf(child), workflowId))
        		.map(child -> fetch(child))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public List<Integer> getUpstreamItemIds() {
        return upstreamItems;
    }

    @Override
    public List<IJamaArtifact> getUpstreamItems(String workflowId) {
        return upstreamItems.stream()
              //  .map(parent -> fetch(String.valueOf(parent), workflowId))
        		.map(child -> fetch(child))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public String getStringValue(String fieldName) {
        return stringValues.get(fieldName);
    }

    @Override
    public List<String> getStringListValue(List<String> fieldName) {
        return stringListValues.get(fieldName);
    }

    @Override
    public Date getDateValue(String fieldName) {
        return dateValues.get(fieldName);
    }

    @Override
    public Boolean getBooleanValue(String fieldName) {
        return booleanValues.get(fieldName);
    }

    @Override
    public Integer getIntegerValue(String fieldName) {
        return intValues.get(fieldName);
    }

    @Override
    public IJamaProjectArtifact getJamaProjectValue(String fieldName) {
        return projectValues.get(fieldName);
    }

    @Override
    public IJamaRelease getJamaReleaseValue(String fieldName) {
        return releaseValues.get(fieldName);
    }

    @Override
    public IJamaUserArtifact getJamaUserValue(String fieldName) {
        return userValues.get(fieldName);
    }

    @Override
    public String toString() {
        return "JamaArtifact{" +
                "documentKey=" + getDocumentKey() +
                ", artifactIdentifier=" + artifactIdentifier +
                ", createdBy=" + getCreatedBy() +
                ", createdDate=" + getCreatedDate() +
                ", globalId=" + getGlobalId() +
                ", id=" + getId() +
                ", itemType=" + getItemType() +
                ", lastActivityDate=" + getLastActivityDate() +
                ", modifiedBy=" + getModifiedBy() +
                ", modifiedDate=" + getModifiedDate() +
                ", name=" + getName() +
                ", project=" + getProject() +
                '}';
    }

    private Optional<String> getString(JamaFieldValue jfv) {
        if (jfv instanceof CalculatedFieldValue) {
            return Optional.ofNullable((String) jfv.getValue());
        } else if (jfv instanceof TextFieldValue) {
            return Optional.ofNullable(((TextFieldValue) jfv).getValue());
        } else if (jfv instanceof URLFieldValue) {
            return Optional.ofNullable(((URLFieldValue) jfv).getValue());
        } else if (jfv instanceof TimeFieldValue) {
            return Optional.ofNullable((String) jfv.getValue());
        } else if (jfv instanceof TextBoxFieldValue) {
            return Optional.ofNullable(((TextBoxFieldValue) jfv).getValue());
        } else if (jfv instanceof TestCaseStatusFieldValue) {
            return Optional.ofNullable(((TestCaseStatusFieldValue) jfv).getValue());
        } else if (jfv instanceof RichTextFieldValue) {
            return Optional.ofNullable(((RichTextFieldValue) jfv).getValue().getValue());
        } else if (jfv instanceof PickListFieldValue) {
            PickListOption opt = ((PickListFieldValue) jfv).getValue();
        	return Optional.ofNullable(opt != null ? opt.getName() : null);
        } else {
            return Optional.empty();
        }
    }

    private Optional<List<String>> getStringList(JamaFieldValue jfv) {
         if (jfv instanceof TestCaseStepsFieldValue) {
            List<TestCaseStep> list = ((TestCaseStepsFieldValue) jfv).getValue();
            if (list != null) {
                List<String> strList = list.stream().map(TestCaseStep::getAction).collect(Collectors.toList());
                return Optional.of(strList);
            } else {
                return Optional.empty();
            }
        } else if (jfv instanceof MultiSelectFieldValue) {
             List<PickListOption> list = ((MultiSelectFieldValue) jfv).getValue();
             if (list != null) {
                 List<String> strList = list.stream().map(PickListOption::getName).collect(Collectors.toList());
                 return Optional.of(strList);
             } else {
                 return Optional.empty();
             }
         } else {
             return Optional.empty();
         }

    }

    private Optional<Date> getDate(JamaFieldValue jfv) {
        if (jfv instanceof DateFieldValue) {
            return Optional.ofNullable((Date) jfv.getValue());
        } else {
            return Optional.empty();
        }
    }

    private Optional<Integer> getInt(JamaFieldValue jfv) {
        if (jfv instanceof IntegerFieldValue) {
            return Optional.ofNullable((Integer) jfv.getValue());
        } else if (jfv instanceof RollupFieldValue) {
            return Optional.ofNullable(((RollupFieldValue) jfv).getValue());
        } else {
            return Optional.empty();
        }
    }

    private Optional<Boolean> getBoolean(JamaFieldValue jfv) {
        if (jfv instanceof FlagFieldValue) {
            return Optional.ofNullable((Boolean) jfv.getValue());
        } else {
            return Optional.empty();
        }
    }

    private Optional<IJamaProjectArtifact> getProject(JamaFieldValue jfv) {
        if (jfv instanceof ProjectFieldValue) {
            JamaProject p = ((ProjectFieldValue) jfv).getValue();
            if (p != null) {
                return Optional.of(new JamaProjectArtifact(p));
            }
        }
        return Optional.empty();
    }

    private Optional<IJamaRelease> getRelease(JamaFieldValue jfv) {
        if (jfv instanceof ReleaseFieldValue) {
            Release r = ((ReleaseFieldValue) jfv).getValue();
            if (r != null) {
                return Optional.of(new JamaRelease(r)); //TODO: also do caching here
            }
        }
        return Optional.empty();
    }

    private Optional<IJamaUserArtifact> getUser(JamaFieldValue jfv) {
        if (jfv instanceof UserFieldValue) {
            JamaUser u = ((UserFieldValue) jfv).getValue();
            if (u != null) {
           // return Optional.of(new JamaUserArtifact(u));
            	return Optional.ofNullable(jamaService.convertUser(u));
            }
        }
        return Optional.empty();
    }
}
