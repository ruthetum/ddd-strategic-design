package kitchenpos.ui

import kitchenpos.application.MenuGroupService
import kitchenpos.domain.MenuGroup
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RequestMapping("/api/menu-groups")
@RestController
class MenuGroupRestController(
    private val menuGroupService: MenuGroupService,
) {
    @PostMapping
    fun create(@RequestBody request: MenuGroup?): ResponseEntity<MenuGroup> {
        val response = menuGroupService.create(request)
        return ResponseEntity.created(URI.create("/api/menu-groups/" + response.id))
            .body(response)
    }

    @GetMapping
    fun findAll(): ResponseEntity<List<MenuGroup>> {
        return ResponseEntity.ok(menuGroupService.findAll())
    }
}