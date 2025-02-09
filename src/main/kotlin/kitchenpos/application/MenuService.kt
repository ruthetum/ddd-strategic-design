package kitchenpos.application

import kitchenpos.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Service
class MenuService(
    private val menuRepository: MenuRepository,
    private val menuGroupRepository: MenuGroupRepository,
    private val productRepository: ProductRepository,
    private val purgomalumClient: PurgomalumClient,
) {
    @Transactional
    fun create(request: Menu): Menu {
        val price = request.price
        require(!(Objects.isNull(price) || price.compareTo(BigDecimal.ZERO) < 0))
        val menuGroup = menuGroupRepository.findById(request.menuGroupId)
            .orElseThrow { NoSuchElementException() }
        val menuProductRequests = request.menuProducts
        require(!(Objects.isNull(menuProductRequests) || menuProductRequests.isEmpty()))
        val products = productRepository.findAllByIdIn(
            menuProductRequests.stream()
                .map { obj: MenuProduct -> obj.productId }
                .toList()
        )
        require(products.size == menuProductRequests.size)
        val menuProducts: MutableList<MenuProduct> = ArrayList()
        var sum = BigDecimal.ZERO
        for (menuProductRequest in menuProductRequests) {
            val quantity = menuProductRequest.quantity
            require(quantity >= 0)
            val product = productRepository.findById(menuProductRequest.productId)
                .orElseThrow { NoSuchElementException() }
            sum = sum.add(
                product.price
                    .multiply(BigDecimal.valueOf(quantity))
            )
            val menuProduct = MenuProduct()
            menuProduct.product = product
            menuProduct.quantity = quantity
            menuProducts.add(menuProduct)
        }
        require(price.compareTo(sum) <= 0)
        val name = request.name
        require(!(Objects.isNull(name) || purgomalumClient.containsProfanity(name)))
        val menu = Menu()
        menu.id = UUID.randomUUID()
        menu.name = name
        menu.price = price
        menu.menuGroup = menuGroup
        menu.isDisplayed = request.isDisplayed
        menu.menuProducts = menuProducts
        return menuRepository.save(menu)
    }

    @Transactional
    fun changePrice(menuId: UUID, request: Menu): Menu {
        val price = request.price
        require(!(Objects.isNull(price) || price.compareTo(BigDecimal.ZERO) < 0))
        val menu = menuRepository.findById(menuId)
            .orElseThrow { NoSuchElementException() }
        var sum = BigDecimal.ZERO
        for (menuProduct in menu.menuProducts) {
            sum = sum.add(
                menuProduct.product
                    .price
                    .multiply(BigDecimal.valueOf(menuProduct.quantity))
            )
        }
        require(price.compareTo(sum) <= 0)
        menu.price = price
        return menu
    }

    @Transactional
    fun display(menuId: UUID): Menu {
        val menu = menuRepository.findById(menuId)
            .orElseThrow { NoSuchElementException() }
        var sum = BigDecimal.ZERO
        for (menuProduct in menu.menuProducts) {
            sum = sum.add(
                menuProduct.product
                    .price
                    .multiply(BigDecimal.valueOf(menuProduct.quantity))
            )
        }
        check(menu.price.compareTo(sum) <= 0)
        menu.isDisplayed = true
        return menu
    }

    @Transactional
    fun hide(menuId: UUID): Menu {
        val menu = menuRepository.findById(menuId)
            .orElseThrow { NoSuchElementException() }
        menu.isDisplayed = false
        return menu
    }

    @Transactional(readOnly = true)
    fun findAll(): List<Menu> {
        return menuRepository.findAll()
    }

}