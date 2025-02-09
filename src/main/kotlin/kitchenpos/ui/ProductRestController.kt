package kitchenpos.ui

import kitchenpos.application.ProductService
import kitchenpos.domain.Product
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RequestMapping("/api/products")
@RestController
class ProductRestController(
    private val productService: ProductService,
) {
    @PostMapping
    fun create(@RequestBody request: Product): ResponseEntity<Product> {
        val response = productService.create(request)
        return ResponseEntity.created(URI.create("/api/products/" + response.id))
            .body(response)
    }

    @PutMapping("/{productId}/price")
    fun changePrice(@PathVariable productId: UUID, @RequestBody request: Product): ResponseEntity<Product> {
        return ResponseEntity.ok(productService.changePrice(productId, request))
    }

    @GetMapping
    fun findAll(): ResponseEntity<List<Product>> {
        return ResponseEntity.ok(productService.findAll())
    }
}