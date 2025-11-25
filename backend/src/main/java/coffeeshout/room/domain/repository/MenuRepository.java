package coffeeshout.room.domain.repository;

import coffeeshout.room.domain.menu.ProvidedMenu;
import java.util.List;
import java.util.Optional;

public interface MenuRepository {

    Optional<ProvidedMenu> findById(Long menuId);

    List<ProvidedMenu> findAll();

    ProvidedMenu save(ProvidedMenu menu);
}
