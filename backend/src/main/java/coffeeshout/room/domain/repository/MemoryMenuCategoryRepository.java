package coffeeshout.room.domain.repository;

import coffeeshout.room.domain.menu.MenuCategory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryMenuCategoryRepository implements MenuCategoryRepository {

    private final AtomicLong idGenerator;
    private final Map<Long, MenuCategory> menuCategories;

    public MemoryMenuCategoryRepository() {
        this.menuCategories = new ConcurrentHashMap<>();
        this.idGenerator = new AtomicLong(1);
    }

    @Override
    public List<MenuCategory> getAll() {
        return menuCategories.values().stream().toList();
    }

    @Override
    public MenuCategory save(MenuCategory category) {
        if (category.getId() != null && idGenerator.get() != category.getId()) {
            throw new IllegalArgumentException("id가 올바르지 않습니다.");
        }
        category.setId(idGenerator.getAndIncrement());
        menuCategories.put(category.getId(), category);
        return menuCategories.get(category.getId());
    }

    @Override
    public Optional<MenuCategory> findById(Long id) {
        return Optional.ofNullable(menuCategories.get(id));
    }
}
