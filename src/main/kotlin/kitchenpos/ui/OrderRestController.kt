package kitchenpos.ui

import kitchenpos.application.OrderService
import kitchenpos.domain.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RequestMapping("/api/orders")
@RestController
class OrderRestController(
    private val orderService: OrderService,
) {
    @PostMapping
    fun create(@RequestBody request: Order?): ResponseEntity<Order> {
        val response = orderService.create(request)
        return ResponseEntity.created(URI.create("/api/orders/" + response.id))
            .body(response)
    }

    @PutMapping("/{orderId}/accept")
    fun accept(@PathVariable orderId: UUID?): ResponseEntity<Order> {
        return ResponseEntity.ok(orderService.accept(orderId))
    }

    @PutMapping("/{orderId}/serve")
    fun serve(@PathVariable orderId: UUID?): ResponseEntity<Order> {
        return ResponseEntity.ok(orderService.serve(orderId))
    }

    @PutMapping("/{orderId}/start-delivery")
    fun startDelivery(@PathVariable orderId: UUID?): ResponseEntity<Order> {
        return ResponseEntity.ok(orderService.startDelivery(orderId))
    }

    @PutMapping("/{orderId}/complete-delivery")
    fun completeDelivery(@PathVariable orderId: UUID?): ResponseEntity<Order> {
        return ResponseEntity.ok(orderService.completeDelivery(orderId))
    }

    @PutMapping("/{orderId}/complete")
    fun complete(@PathVariable orderId: UUID?): ResponseEntity<Order> {
        return ResponseEntity.ok(orderService.complete(orderId))
    }

    @GetMapping
    fun findAll(): ResponseEntity<List<Order>> {
        return ResponseEntity.ok(orderService.findAll())
    }
}