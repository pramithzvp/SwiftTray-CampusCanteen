package lk.ac.ruh.swifttray;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface UserRepository extends JpaRepository<AppUser,Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
interface MenuRepository extends JpaRepository<MenuItem,Long> {
    List<MenuItem> findByActiveTrueOrderByIdAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MenuItem m where m.id=:id and m.active=true")
    Optional<MenuItem> lockActive(@Param("id") Long id);
}
interface OrderRepository extends JpaRepository<FoodOrder,Long> {
    @EntityGraph(attributePaths={"student","items"})
    List<FoodOrder> findByStudentIdOrderByIdDesc(Long id);
    @EntityGraph(attributePaths={"student","items"})
    List<FoodOrder> findByStatusInOrderByIdAsc(Collection<FoodOrder.Status> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from FoodOrder o where o.id=:id")
    Optional<FoodOrder> lockById(@Param("id") Long id);
}
