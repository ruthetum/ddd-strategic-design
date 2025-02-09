package kitchenpos.application

import kitchenpos.Fixtures.menuGroup
import kitchenpos.domain.InMemoryMenuGroupRepository
import kitchenpos.domain.MenuGroup
import kitchenpos.domain.MenuGroupRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EmptySource

class MenuGroupServiceTest {
    private lateinit var menuGroupRepository: MenuGroupRepository
    private lateinit var menuGroupService: MenuGroupService

    @BeforeEach
    fun setUp() {
        menuGroupRepository = InMemoryMenuGroupRepository()
        menuGroupService = MenuGroupService(menuGroupRepository)
    }

    @Test
    fun `메뉴 그룹을 등록할 수 있다`() {
        val expected = createMenuGroupRequest("두마리메뉴")
        val actual = menuGroupService.create(expected)
        Assertions.assertThat(actual).isNotNull()
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(actual.id).isNotNull() },
            Executable { Assertions.assertThat(actual.name).isEqualTo(expected.name) }
        )
    }

    @EmptySource
    @ParameterizedTest
    fun `메뉴 그룹의 이름이 올바르지 않으면 등록할 수 없다`(name: String) {
        val expected = createMenuGroupRequest(name)
        Assertions.assertThatThrownBy { menuGroupService.create(expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `메뉴 그룹의 목록을 조회할 수 있다`() {
        menuGroupRepository.save(menuGroup("두마리메뉴"))
        val actual = menuGroupService.findAll()
        Assertions.assertThat(actual).hasSize(1)
    }

    private fun createMenuGroupRequest(name: String): MenuGroup {
        val menuGroup = MenuGroup()
        menuGroup.name = name
        return menuGroup
    }
}