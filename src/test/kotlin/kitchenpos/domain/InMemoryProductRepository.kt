package kitchenpos.domain

import java.util.*

class InMemoryProductRepository : ProductRepository {
    private val products: MutableMap<UUID, Product> = HashMap()

    override fun save(product: Product): Product {
        products[product.id] = product
        return product
    }

    override fun findById(id: UUID): Optional<Product> {
        return Optional.ofNullable(products[id])
    }

    override fun findAll(): List<Product> {
        return products.values.toList()
    }

    override fun findAllByIdIn(ids: List<UUID>): List<Product> {
        return products.filterKeys { ids.contains(it) }.values.toList()
    }
}