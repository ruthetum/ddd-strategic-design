package kitchenpos.domain

import java.util.*

class InMemoryMenuGroupRepository : MenuGroupRepository {
    private val menuGroups: MutableMap<UUID, MenuGroup> = HashMap()

    override fun save(menuGroup: MenuGroup): MenuGroup {
        menuGroups[menuGroup.id] = menuGroup
        return menuGroup
    }

    override fun findById(id: UUID): Optional<MenuGroup> {
        return Optional.ofNullable(menuGroups[id])
    }

    override fun findAll(): List<MenuGroup> {
        return menuGroups.values.toList()
    }
}