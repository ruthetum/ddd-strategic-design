package kitchenpos.application

import kitchenpos.domain.MenuGroup
import kitchenpos.domain.MenuGroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class MenuGroupService(
    private val menuGroupRepository: MenuGroupRepository,
) {
    @Transactional
    fun create(request: MenuGroup): MenuGroup {
        val name = request.name
        if (request.name.isBlank()) {
            throw IllegalArgumentException("메뉴 그룹의 이름은 빈 값이 될 수 없습니다.")
        }

        val menuGroup = MenuGroup()
        menuGroup.id = UUID.randomUUID()
        menuGroup.name = name
        return menuGroupRepository.save(menuGroup)
    }

    @Transactional(readOnly = true)
    fun findAll(): List<MenuGroup> {
        return menuGroupRepository.findAll()
    }
}