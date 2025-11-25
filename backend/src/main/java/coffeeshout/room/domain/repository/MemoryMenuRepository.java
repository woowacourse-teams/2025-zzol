package coffeeshout.room.domain.repository;

import coffeeshout.room.domain.menu.ProvidedMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryMenuRepository implements MenuRepository {

    private final AtomicLong idGenerator;
    private final Map<Long, ProvidedMenu> menus;

    public MemoryMenuRepository() {
        this.menus = new ConcurrentHashMap<>();
        this.idGenerator = new AtomicLong(1);
    }

    @Override
    public Optional<ProvidedMenu> findById(Long menuId) {
        return Optional.ofNullable(menus.get(menuId));
    }

    @Override
    public List<ProvidedMenu> findAll() {
        return new ArrayList<>(menus.values());
    }

    @Override
    public ProvidedMenu save(ProvidedMenu menu) {
        if (menu.getId() != null && idGenerator.get() != menu.getId()) {
            throw new IllegalArgumentException("id가 올바르지 않습니다.");
        }
        menu.setId(idGenerator.getAndIncrement());
        menus.put(menu.getId(), menu);
        return menus.get(menu.getId());
    }
}
