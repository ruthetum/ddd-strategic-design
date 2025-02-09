package kitchenpos.domain

import java.util.*

class InMemoryMenuRepository : MenuRepository {
    private val menus: MutableMap<UUID, Menu> = HashMap()

    override fun save(menu: Menu): Menu {
        menus[menu.id] = menu
        return menu
    }

    override fun findById(id: UUID): Optional<Menu> {
        return Optional.ofNullable(menus[id])
    }

    override fun findAll(): List<Menu> {
        return menus.values.toList()
    }

    override fun findAllByIdIn(ids: List<UUID>): List<Menu> {
        return menus.filterKeys { ids.contains(it) }.values.toList()
    }

    override fun findAllByProductId(productId: UUID): List<Menu> {
        return menus.values.filter { it.menuProducts.any { menuProduct -> menuProduct.productId == productId } }
    }
}