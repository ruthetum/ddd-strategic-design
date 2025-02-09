package kitchenpos.application

import kitchenpos.domain.MenuRepository
import kitchenpos.domain.Product
import kitchenpos.domain.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val menuRepository: MenuRepository,
    private val purgomalumClient: PurgomalumClient,
) {
    @Transactional
    fun create(request: Product): Product {
        val price = request.price
        require(!(Objects.isNull(price) || price.compareTo(BigDecimal.ZERO) < 0))
        val name = request.name
        require(!(Objects.isNull(name) || purgomalumClient.containsProfanity(name)))
        val product = Product()
        product.id = UUID.randomUUID()
        product.name = name
        product.price = price
        return productRepository.save(product)
    }

    @Transactional
    fun changePrice(productId: UUID?, request: Product): Product {
        val price = request.price
        require(!(Objects.isNull(price) || price.compareTo(BigDecimal.ZERO) < 0))
        val product = productRepository.findById(productId)
            .orElseThrow { NoSuchElementException() }
        product.price = price
        val menus = menuRepository.findAllByProductId(productId)
        for (menu in menus) {
            var sum = BigDecimal.ZERO
            for (menuProduct in menu.menuProducts) {
                sum = sum.add(
                    menuProduct.product
                        .price
                        .multiply(BigDecimal.valueOf(menuProduct.quantity))
                )
            }
            if (menu.price.compareTo(sum) > 0) {
                menu.isDisplayed = false
            }
        }
        return product
    }

    @Transactional(readOnly = true)
    fun findAll(): List<Product> {
        return productRepository.findAll()
    }
}