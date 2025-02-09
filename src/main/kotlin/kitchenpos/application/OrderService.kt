package kitchenpos.application

import kitchenpos.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val menuRepository: MenuRepository,
    private val orderTableRepository: OrderTableRepository,
    private val kitchenridersClient: KitchenridersClient,
) {
    @Transactional
    fun create(request: Order): Order {
        val type = request.type
        require(!Objects.isNull(type))
        val orderLineItemRequests = request.orderLineItems
        require(!(Objects.isNull(orderLineItemRequests) || orderLineItemRequests.isEmpty()))
        val menus = menuRepository.findAllByIdIn(
            orderLineItemRequests.stream()
                .map { obj: OrderLineItem -> obj.menuId }
                .toList()
        )
        require(menus.size == orderLineItemRequests.size)
        val orderLineItems: MutableList<OrderLineItem> = ArrayList()
        for (orderLineItemRequest in orderLineItemRequests) {
            val quantity = orderLineItemRequest.quantity
            if (type != OrderType.EAT_IN) {
                require(quantity >= 0)
            }
            val menu = menuRepository.findById(orderLineItemRequest.menuId)
                .orElseThrow { NoSuchElementException() }
            check(menu.isDisplayed)
            require(menu.price.compareTo(orderLineItemRequest.price) == 0)
            val orderLineItem = OrderLineItem()
            orderLineItem.menu = menu
            orderLineItem.quantity = quantity
            orderLineItems.add(orderLineItem)
        }
        val order = Order()
        order.id = UUID.randomUUID()
        order.type = type
        order.status = OrderStatus.WAITING
        order.orderDateTime = LocalDateTime.now()
        order.orderLineItems = orderLineItems
        if (type == OrderType.DELIVERY) {
            val deliveryAddress = request.deliveryAddress
            require(!(Objects.isNull(deliveryAddress) || deliveryAddress.isEmpty()))
            order.deliveryAddress = deliveryAddress
        }
        if (type == OrderType.EAT_IN) {
            val orderTable = orderTableRepository.findById(request.orderTableId)
                .orElseThrow { NoSuchElementException() }
            check(orderTable.isOccupied)
            order.orderTable = orderTable
        }
        return orderRepository.save(order)
    }

    @Transactional
    fun accept(orderId: UUID): Order {
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException() }
        check(order.status == OrderStatus.WAITING)
        if (order.type == OrderType.DELIVERY) {
            var sum = BigDecimal.ZERO
            for (orderLineItem in order.orderLineItems) {
                sum = orderLineItem.menu
                    .price
                    .multiply(BigDecimal.valueOf(orderLineItem.quantity))
            }
            kitchenridersClient.requestDelivery(orderId!!, sum!!, order.deliveryAddress)
        }
        order.status = OrderStatus.ACCEPTED
        return order
    }

    @Transactional
    fun serve(orderId: UUID): Order {
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException() }
        check(order.status == OrderStatus.ACCEPTED)
        order.status = OrderStatus.SERVED
        return order
    }

    @Transactional
    fun startDelivery(orderId: UUID): Order {
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException() }
        check(order.type == OrderType.DELIVERY)
        check(order.status == OrderStatus.SERVED)
        order.status = OrderStatus.DELIVERING
        return order
    }

    @Transactional
    fun completeDelivery(orderId: UUID): Order {
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException() }
        check(order.status == OrderStatus.DELIVERING)
        order.status = OrderStatus.DELIVERED
        return order
    }

    @Transactional
    fun complete(orderId: UUID): Order {
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException() }
        val type = order.type
        val status = order.status
        if (type == OrderType.DELIVERY) {
            check(status == OrderStatus.DELIVERED)
        }
        if (type == OrderType.TAKEOUT || type == OrderType.EAT_IN) {
            check(status == OrderStatus.SERVED)
        }
        order.status = OrderStatus.COMPLETED
        if (type == OrderType.EAT_IN) {
            val orderTable = order.orderTable
            if (!orderRepository.existsByOrderTableAndStatusNot(orderTable, OrderStatus.COMPLETED)) {
                orderTable.numberOfGuests = 0
                orderTable.isOccupied = false
            }
        }
        return order
    }

    @Transactional(readOnly = true)
    fun findAll(): List<Order> {
        return orderRepository.findAll()
    }
}