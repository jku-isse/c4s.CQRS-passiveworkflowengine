package at.jku.isse.passiveprocessengine.frontend.security.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.prepost.PostFilter;

public interface RestrictionProxyRepository extends JpaRepository<RestrictionProxy, Long>{
       
	@PostFilter("hasPermission(filterObject, 'READ')")
    List<RestrictionProxy> findAll();
  
}
