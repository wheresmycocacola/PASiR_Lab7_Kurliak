package pk.ak.pasir_andrii_kurliak.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pk.ak.pasir_andrii_kurliak.model.Group;
import pk.ak.pasir_andrii_kurliak.model.User;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByMemberships_User(User user);
}
