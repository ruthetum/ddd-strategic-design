package kitchenpos

import kitchenpos.domain.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

object Fixtures {

    val INVALID_ID = UUID(0L, 0L);

    fun menu(): Menu {
        return menu(19000L, true, menuProduct())
    }

    fun menu(
        price: Long,
        vararg menuProducts: MenuProduct = arrayOf(menuProduct()),
    ): Menu {
        return menu(price, false, *menuProducts)
    }

    fun menu(
        price: Long,
        displayed: Boolean,
        vararg menuProducts: MenuProduct = arrayOf(menuProduct()),
    ): Menu {
        val menu = Menu()
        menu.id = INVALID_ID
        menu.name = "후라이드+후라이드"
        menu.price = BigDecimal.valueOf(price)
        menu.menuGroup = menuGroup()
        menu.isDisplayed = displayed
        menu.menuProducts = listOf(*menuProducts)
        return menu
    }

    fun menuGroup(name: String = "두마리메뉴"): MenuGroup {
        val menuGroup = MenuGroup()
        menuGroup.id = UUID.randomUUID()
        menuGroup.name = name
        return menuGroup
    }

    fun menuProduct(product: Product = product(), quantity: Long = 2L): MenuProduct {
        val menuProduct = MenuProduct()
        menuProduct.seq = Random().nextLong()
        menuProduct.product = product
        menuProduct.quantity = quantity
        return menuProduct
    }

    fun order(status: OrderStatus, deliveryAddress: String): Order {
        val order = Order()
        order.id = UUID.randomUUID()
        order.type = OrderType.DELIVERY
        order.status = status
        order.orderDateTime = LocalDateTime.of(2020, 1, 1, 12, 0)
        order.orderLineItems = listOf(orderLineItem())
        order.deliveryAddress = deliveryAddress
        return order
    }

    fun order(status: OrderStatus): Order {
        val order = Order()
        order.id = UUID.randomUUID()
        order.type = OrderType.TAKEOUT
        order.status = status
        order.orderDateTime = LocalDateTime.of(2020, 1, 1, 12, 0)
        order.orderLineItems = listOf(orderLineItem())
        return order
    }

    fun order(status: OrderStatus, orderTable: OrderTable): Order {
        val order = Order()
        order.id = UUID.randomUUID()
        order.type = OrderType.EAT_IN
        order.status = status
        order.orderDateTime = LocalDateTime.of(2020, 1, 1, 12, 0)
        order.orderLineItems = listOf(orderLineItem())
        order.orderTable = orderTable
        return order
    }

    fun orderLineItem(): OrderLineItem {
        val orderLineItem = OrderLineItem()
        orderLineItem.seq = Random().nextLong()
        orderLineItem.menu = menu()
        return orderLineItem
    }

    fun orderTable(
        occupied: Boolean = false,
        numberOfGuests: Int = 0,
    ): OrderTable {
        val orderTable = OrderTable()
        orderTable.id = UUID.randomUUID()
        orderTable.name = "1번"
        orderTable.numberOfGuests = numberOfGuests
        orderTable.isOccupied = occupied
        return orderTable
    }

    fun product(
        name: String = "후라이드",
        price: Long = 16000L,
    ): Product {
        val product = Product()
        product.id = UUID.randomUUID()
        product.name = name
        product.price = BigDecimal.valueOf(price)
        return product
    }
}