package at.jku.isse.passiveprocessengine.frontend.security.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ProcessProxyRepository extends JpaRepository<ProcessProxy, Long>{
    
    @PostFilter("hasPermission(filterObject, 'READ')")
    List<ProcessProxy> findAll();
    
//    @PostAuthorize("hasPermission(returnObject, 'READ')")
//    ProcessProxy findById(Integer id);
    
//    @SuppressWarnings("unchecked")
//    @PreAuthorize("hasPermission(#processProxy, 'WRITE')")
//    ProcessProxy save(@Param("processProxy")ProcessProxy processProxy);

}
