package kitchenpos.application

import kitchenpos.Fixtures.menu
import kitchenpos.Fixtures.menuProduct
import kitchenpos.Fixtures.product
import kitchenpos.domain.*
import kitchenpos.infra.PurgomalumClient
import kitchenpos.infra.client.FakePurgomalumClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

class ProductServiceTest {
    private lateinit var productRepository: ProductRepository
    private lateinit var menuRepository: MenuRepository
    private lateinit var purgomalumClient: PurgomalumClient
    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        productRepository = InMemoryProductRepository()
        menuRepository = InMemoryMenuRepository()
        purgomalumClient = FakePurgomalumClient()
        productService = ProductService(productRepository, menuRepository, purgomalumClient)
    }

    @DisplayName("상품을 등록할 수 있다.")
    @Test
    fun create() {
        val expected = createProductRequest("후라이드", 16000L)
        val actual = productService.create(expected)
        Assertions.assertThat(actual).isNotNull()
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(actual.id).isNotNull() },
            Executable { Assertions.assertThat(actual.name).isEqualTo(expected.name) },
            Executable { Assertions.assertThat(actual.price).isEqualTo(expected.price) }
        )
    }

    @DisplayName("상품의 가격이 올바르지 않으면 등록할 수 없다.")
    @ValueSource(strings = ["-1000"])
    @ParameterizedTest
    fun create(price: BigDecimal) {
        val expected = createProductRequest("후라이드", price)
        Assertions.assertThatThrownBy { productService.create(expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @DisplayName("상품의 이름이 올바르지 않으면 등록할 수 없다.")
    @ValueSource(strings = ["비속어", "욕설이 포함된 이름"])
    @ParameterizedTest
    fun create(name: String) {
        val expected = createProductRequest(name, 16000L)
        Assertions.assertThatThrownBy { productService.create(expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @DisplayName("상품의 가격을 변경할 수 있다.")
    @Test
    fun changePrice() {
        val productId = productRepository.save(product("후라이드", 16000L)).id
        val expected = changePriceRequest(15000L)
        val actual = productService.changePrice(productId, expected)
        Assertions.assertThat(actual.price).isEqualTo(expected.price)
    }

    @DisplayName("상품의 가격이 올바르지 않으면 변경할 수 없다.")
    @ValueSource(strings = ["-1000"])
    @ParameterizedTest
    fun changePrice(price: BigDecimal) {
        val productId = productRepository.save(product("후라이드", 16000L)).id
        val expected = changePriceRequest(price)
        Assertions.assertThatThrownBy { productService.changePrice(productId, expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @DisplayName("상품의 가격이 변경될 때 메뉴의 가격이 메뉴에 속한 상품 금액의 합보다 크면 메뉴가 숨겨진다.")
    @Test
    fun changePriceInMenu() {
        val product = productRepository.save(product("후라이드", 16000L))
        val menu = menuRepository.save(menu(19000L, true, menuProduct(product, 2L)))
        productService.changePrice(product.id, changePriceRequest(8000L))
        Assertions.assertThat(menuRepository.findById(menu.id).get().isDisplayed).isFalse()
    }

    @DisplayName("상품의 목록을 조회할 수 있다.")
    @Test
    fun findAll() {
        productRepository.save(product("후라이드", 16000L))
        productRepository.save(product("양념치킨", 16000L))
        val actual = productService.findAll()
        Assertions.assertThat(actual).hasSize(2)
    }

    private fun createProductRequest(name: String, price: Long): Product {
        return createProductRequest(name, BigDecimal.valueOf(price))
    }

    private fun createProductRequest(name: String, price: BigDecimal): Product {
        val product = Product()
        product.name = name
        product.price = price
        return product
    }

    private fun changePriceRequest(price: Long): Product {
        return changePriceRequest(BigDecimal.valueOf(price))
    }

    private fun changePriceRequest(price: BigDecimal): Product {
        val product = Product()
        product.price = price
        return product
    }
}