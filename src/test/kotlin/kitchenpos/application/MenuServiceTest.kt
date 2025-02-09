package kitchenpos.application

import kitchenpos.Fixtures.INVALID_ID
import kitchenpos.Fixtures.menu
import kitchenpos.Fixtures.menuGroup
import kitchenpos.Fixtures.menuProduct
import kitchenpos.Fixtures.product
import kitchenpos.domain.*
import kitchenpos.infra.client.FakePurgomalumClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MenuServiceTest {
    private lateinit var menuRepository: MenuRepository
    private lateinit var menuGroupRepository: MenuGroupRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var purgomalumClient: PurgomalumClient
    private lateinit var menuService: MenuService
    private lateinit var menuGroupId: UUID
    private lateinit var product: Product

    @BeforeEach
    fun setUp() {
        menuRepository = InMemoryMenuRepository()
        menuGroupRepository = InMemoryMenuGroupRepository()
        productRepository = InMemoryProductRepository()
        purgomalumClient = FakePurgomalumClient()
        menuService = MenuService(menuRepository, menuGroupRepository, productRepository, purgomalumClient)
        menuGroupId = menuGroupRepository.save(menuGroup("두마리메뉴")).id
        product = productRepository.save(product("후라이드", 16000))
    }

    @DisplayName("1개 이상의 등록된 상품으로 메뉴를 등록할 수 있다.")
    @Test
    fun create() {
        val expected = createMenuRequest(
            "후라이드+후라이드", 19000L, menuGroupId, true, createMenuProductRequest(product.id, 2L)
        )
        val actual = menuService.create(expected)
        Assertions.assertThat(actual).isNotNull()
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(actual.id).isNotNull() },
            Executable { Assertions.assertThat(actual.name).isEqualTo(expected.name) },
            Executable { Assertions.assertThat(actual.price).isEqualTo(expected.price) },
            Executable { Assertions.assertThat(actual.menuGroup.id).isEqualTo(expected.menuGroupId) },
            Executable { Assertions.assertThat(actual.isDisplayed).isEqualTo(expected.isDisplayed) },
            Executable { Assertions.assertThat(actual.menuProducts).hasSize(1) }
        )
    }

    @DisplayName("상품이 없으면 등록할 수 없다.")
    @MethodSource("menuProducts")
    @ParameterizedTest
    fun create(menuProducts: List<MenuProduct>) {
        val expected = createMenuRequest("후라이드+후라이드", 19000L, menuGroupId, true, menuProducts)
        Assertions.assertThatThrownBy { menuService.create(expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @DisplayName("메뉴에 속한 상품의 수량은 0개 이상이어야 한다.")
    @Test
    fun createNegativeQuantity() {
        val expected = createMenuRequest(
            "후라이드+후라이드", 19000L, menuGroupId, true, createMenuProductRequest(product.id, -1L)
        )
        Assertions.assertThatThrownBy { menuService.create(expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @DisplayName("메뉴의 가격이 올바르지 않으면 등록할 수 없다.")
    @ValueSource(strings = ["-1000"])
    @ParameterizedTest
    fun create(price: BigDecimal) {
        val expected = createMenuRequest(
            "후라이드+후라이드", price, menuGroupId, true, createMenuProductRequest(product.id, 2L)
        )
        Assertions.assertThatThrownBy { menuService.create(expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @DisplayName("메뉴에 속한 상품 금액의 합은 메뉴의 가격보다 크거나 같아야 한다.")
    @Test
    fun createExpensiveMenu() {
        val expected = createMenuRequest(
            "후라이드+후라이드", 33000L, menuGroupId, true, createMenuProductRequest(product.id, 2L)
        )
        Assertions.assertThatThrownBy { menuService.create(expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @DisplayName("메뉴의 이름이 올바르지 않으면 등록할 수 없다.")
    @ValueSource(strings = ["비속어", "욕설이 포함된 이름"])
    @ParameterizedTest
    fun create(name: String) {
        val expected = createMenuRequest(
            name, 19000L, menuGroupId, true, createMenuProductRequest(product.id, 2L)
        )
        Assertions.assertThatThrownBy { menuService.create(expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @DisplayName("메뉴의 가격을 변경할 수 있다.")
    @Test
    fun changePrice() {
        val menuId = menuRepository.save(menu(19000L, menuProduct(product, 2L))).id
        val expected = changePriceRequest(16000L)
        val actual = menuService.changePrice(menuId, expected)
        Assertions.assertThat(actual.price).isEqualTo(expected.price)
    }

    @DisplayName("메뉴의 가격이 올바르지 않으면 변경할 수 없다.")
    @ValueSource(strings = ["-1000"])
    @ParameterizedTest
    fun changePrice(price: BigDecimal) {
        val menuId = menuRepository.save(menu(19000L, menuProduct(product, 2L))).id
        val expected = changePriceRequest(price)
        Assertions.assertThatThrownBy { menuService.changePrice(menuId, expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @DisplayName("메뉴에 속한 상품 금액의 합은 메뉴의 가격보다 크거나 같아야 한다.")
    @Test
    fun changePriceToExpensive() {
        val menuId = menuRepository.save(menu(19000L, menuProduct(product, 2L))).id
        val expected = changePriceRequest(33000L)
        Assertions.assertThatThrownBy { menuService.changePrice(menuId, expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @DisplayName("메뉴를 노출할 수 있다.")
    @Test
    fun display() {
        val menuId = menuRepository.save(menu(19000L, false, menuProduct(product, 2L))).id
        val actual = menuService.display(menuId)
        Assertions.assertThat(actual.isDisplayed).isTrue()
    }

    @DisplayName("메뉴의 가격이 메뉴에 속한 상품 금액의 합보다 높을 경우 메뉴를 노출할 수 없다.")
    @Test
    fun displayExpensiveMenu() {
        val menuId = menuRepository.save(menu(33000L, false, menuProduct(product, 2L))).id
        Assertions.assertThatThrownBy { menuService.display(menuId) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @DisplayName("메뉴를 숨길 수 있다.")
    @Test
    fun hide() {
        val menuId = menuRepository.save(menu(19000L, true, menuProduct(product, 2L))).id
        val actual = menuService.hide(menuId)
        Assertions.assertThat(actual.isDisplayed).isFalse()
    }

    @DisplayName("메뉴의 목록을 조회할 수 있다.")
    @Test
    fun findAll() {
        menuRepository.save(menu(19000L, true, menuProduct(product, 2L)))
        val actual = menuService.findAll()
        Assertions.assertThat(actual).hasSize(1)
    }

    private fun createMenuRequest(
        name: String,
        price: Long,
        menuGroupId: UUID,
        displayed: Boolean,
        vararg menuProducts: MenuProduct
    ): Menu {
        return createMenuRequest(name, BigDecimal.valueOf(price), menuGroupId, displayed, *menuProducts)
    }

    private fun createMenuRequest(
        name: String,
        price: BigDecimal,
        menuGroupId: UUID,
        displayed: Boolean,
        vararg menuProducts: MenuProduct
    ): Menu {
        return createMenuRequest(name, price, menuGroupId, displayed, Arrays.asList(*menuProducts))
    }

    private fun createMenuRequest(
        name: String,
        price: Long,
        menuGroupId: UUID,
        displayed: Boolean,
        menuProducts: List<MenuProduct>
    ): Menu {
        return createMenuRequest(name, BigDecimal.valueOf(price), menuGroupId, displayed, menuProducts)
    }

    private fun createMenuRequest(
        name: String,
        price: BigDecimal,
        menuGroupId: UUID,
        displayed: Boolean,
        menuProducts: List<MenuProduct>
    ): Menu {
        val menu = Menu()
        menu.name = name
        menu.price = price
        menu.menuGroupId = menuGroupId
        menu.isDisplayed = displayed
        menu.menuProducts = menuProducts
        return menu
    }



    private fun changePriceRequest(price: Long): Menu {
        return changePriceRequest(BigDecimal.valueOf(price))
    }

    private fun changePriceRequest(price: BigDecimal): Menu {
        val menu = Menu()
        menu.price = price
        return menu
    }

    companion object {
        private fun createMenuProductRequest(productId: UUID, quantity: Long): MenuProduct {
            val menuProduct = MenuProduct()
            menuProduct.productId = productId
            menuProduct.quantity = quantity
            return menuProduct
        }

        @JvmStatic
        private fun menuProducts(): List<Arguments> {
            return listOf(
                Arguments.of(emptyList<MenuProduct>()),
                Arguments.of(listOf(createMenuProductRequest(INVALID_ID, 2L)))
            )
        }
    }
}