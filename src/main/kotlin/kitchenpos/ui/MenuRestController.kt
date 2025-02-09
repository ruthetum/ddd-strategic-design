package kitchenpos.ui

import kitchenpos.application.MenuService
import kitchenpos.domain.Menu
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RequestMapping("/api/menus")
@RestController
class MenuRestController(
    private val menuService: MenuService,
) {
    @PostMapping
    fun create(@RequestBody request: Menu?): ResponseEntity<Menu> {
        val response = menuService.create(request)
        return ResponseEntity.created(URI.create("/api/menus/" + response.id))
            .body(response)
    }

    @PutMapping("/{menuId}/price")
    fun changePrice(@PathVariable menuId: UUID?, @RequestBody request: Menu?): ResponseEntity<Menu> {
        return ResponseEntity.ok(menuService.changePrice(menuId, request))
    }

    @PutMapping("/{menuId}/display")
    fun display(@PathVariable menuId: UUID?): ResponseEntity<Menu> {
        return ResponseEntity.ok(menuService.display(menuId))
    }

    @PutMapping("/{menuId}/hide")
    fun hide(@PathVariable menuId: UUID?): ResponseEntity<Menu> {
        return ResponseEntity.ok(menuService.hide(menuId))
    }

    @GetMapping
    fun findAll(): ResponseEntity<List<Menu>> {
        return ResponseEntity.ok(menuService.findAll())
    }
}