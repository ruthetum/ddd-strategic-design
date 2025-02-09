package kitchenpos.ui

import kitchenpos.application.OrderTableService
import kitchenpos.domain.OrderTable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RequestMapping("/api/order-tables")
@RestController
class OrderTableRestController(
    private val orderTableService: OrderTableService,
) {
    @PostMapping
    fun create(@RequestBody request: OrderTable?): ResponseEntity<OrderTable> {
        val response = orderTableService.create(request)
        return ResponseEntity.created(URI.create("/api/order-tables/" + response.id))
            .body(response)
    }

    @PutMapping("/{orderTableId}/sit")
    fun sit(@PathVariable orderTableId: UUID?): ResponseEntity<OrderTable> {
        return ResponseEntity.ok(orderTableService.sit(orderTableId))
    }

    @PutMapping("/{orderTableId}/clear")
    fun clear(@PathVariable orderTableId: UUID?): ResponseEntity<OrderTable> {
        return ResponseEntity.ok(orderTableService.clear(orderTableId))
    }

    @PutMapping("/{orderTableId}/number-of-guests")
    fun changeNumberOfGuests(
        @PathVariable orderTableId: UUID?,
        @RequestBody request: OrderTable?
    ): ResponseEntity<OrderTable> {
        return ResponseEntity.ok(orderTableService.changeNumberOfGuests(orderTableId, request))
    }

    @GetMapping
    fun findAll(): ResponseEntity<List<OrderTable>> {
        return ResponseEntity.ok(orderTableService.findAll())
    }
}