package kitchenpos.application

import kitchenpos.Fixtures.INVALID_ID
import kitchenpos.Fixtures.menu
import kitchenpos.Fixtures.menuProduct
import kitchenpos.Fixtures.order
import kitchenpos.Fixtures.orderTable
import kitchenpos.domain.*
import kitchenpos.infra.client.FakeKitchenridersClient
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.*
import java.math.BigDecimal
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderServiceTest {
    private lateinit var orderRepository: OrderRepository
    private lateinit var menuRepository: MenuRepository
    private lateinit var orderTableRepository: OrderTableRepository
    private lateinit var kitchenridersClient: FakeKitchenridersClient
    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        orderRepository = InMemoryOrderRepository()
        menuRepository = InMemoryMenuRepository()
        orderTableRepository = InMemoryOrderTableRepository()
        kitchenridersClient = FakeKitchenridersClient()
        orderService = OrderService(orderRepository, menuRepository, orderTableRepository, kitchenridersClient)
    }

    @Test
    fun `1개 이상의 등록된 메뉴로 배달 주문을 등록할 수 있다`() {
        val menuId = menuRepository.save(menu(19000L, true, menuProduct())).id
        val expected = createOrderRequest(
            OrderType.DELIVERY, "서울시 송파구 위례성대로 2", createOrderLineItemRequest(menuId, 19000L, 3L)
        )
        val actual = orderService.create(expected)
        Assertions.assertThat(actual).isNotNull()
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(actual.id).isNotNull() },
            Executable { Assertions.assertThat(actual.type).isEqualTo(expected.type) },
            Executable { Assertions.assertThat(actual.status).isEqualTo(OrderStatus.WAITING) },
            Executable { Assertions.assertThat(actual.orderDateTime).isNotNull() },
            Executable { Assertions.assertThat(actual.orderLineItems).hasSize(1) },
            Executable { Assertions.assertThat(actual.deliveryAddress).isEqualTo(expected.deliveryAddress) }
        )
    }

    @Test
    fun `1개 이상의 등록된 메뉴로 포장 주문을 등록할 수 있다`() {
        val menuId = menuRepository.save(menu(19000L, true, menuProduct())).id
        val expected = createOrderRequest(OrderType.TAKEOUT, createOrderLineItemRequest(menuId, 19000L, 3L))
        val actual = orderService.create(expected)
        Assertions.assertThat(actual).isNotNull()
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(actual.id).isNotNull() },
            Executable { Assertions.assertThat(actual.type).isEqualTo(expected.type) },
            Executable { Assertions.assertThat(actual.status).isEqualTo(OrderStatus.WAITING) },
            Executable { Assertions.assertThat(actual.orderDateTime).isNotNull() },
            Executable { Assertions.assertThat(actual.orderLineItems).hasSize(1) }
        )
    }

    @Test
    fun `1개 이상의 등록된 메뉴로 매장 주문을 등록할 수 있다`() {
        val menuId = menuRepository.save(menu(19000L, true, menuProduct())).id
        val orderTableId = orderTableRepository.save(orderTable(true, 4)).id
        val expected =
            createOrderRequest(OrderType.EAT_IN, orderTableId, createOrderLineItemRequest(menuId, 19000L, 3L))
        val actual = orderService.create(expected)
        Assertions.assertThat(actual).isNotNull()
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(actual.id).isNotNull() },
            Executable { Assertions.assertThat(actual.type).isEqualTo(expected.type) },
            Executable { Assertions.assertThat(actual.status).isEqualTo(OrderStatus.WAITING) },
            Executable { Assertions.assertThat(actual.orderDateTime).isNotNull() },
            Executable { Assertions.assertThat(actual.orderLineItems).hasSize(1) },
            Executable { Assertions.assertThat(actual.orderTable.id).isEqualTo(expected.orderTableId) }
        )
    }

    @MethodSource("orderLineItems")
    @ParameterizedTest
    fun `메뉴가 없으면 등록할 수 없다`(orderLineItems: List<OrderLineItem>) {
        val expected = createOrderRequest(OrderType.TAKEOUT, orderLineItems)
        assertThatThrownBy { orderService.create(expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @ValueSource(longs = [-1L])
    @ParameterizedTest
    fun `매장 주문은 주문 항목의 수량이 0 미만일 수 있다`(quantity: Long) {
        val menuId = menuRepository.save(menu(19000L, true, menuProduct())).id
        val orderTableId = orderTableRepository.save(orderTable(true, 4)).id
        val expected = createOrderRequest(
            OrderType.EAT_IN, orderTableId, createOrderLineItemRequest(menuId, 19000L, quantity)
        )
        assertDoesNotThrow<Order> { orderService.create(expected) }
    }

    @ValueSource(longs = [-1L])
    @ParameterizedTest
    fun `매장 주문을 제외한 주문의 경우 주문 항목의 수량은 0 이상이어야 한다`(quantity: Long) {
        val menuId = menuRepository.save(menu(19000L, true, menuProduct())).id
        val expected = createOrderRequest(
            OrderType.TAKEOUT, createOrderLineItemRequest(menuId, 19000L, quantity)
        )

        assertThatThrownBy { orderService.create(expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @EmptySource
    @ParameterizedTest
    fun `배달 주소가 올바르지 않으면 배달 주문을 등록할 수 없다`(deliveryAddress: String) {
        val menuId = menuRepository.save(menu(19000L, true, menuProduct())).id
        val expected = createOrderRequest(
            OrderType.DELIVERY, deliveryAddress, createOrderLineItemRequest(menuId, 19000L, 3L)
        )

        assertThatThrownBy { orderService.create(expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `빈 테이블에는 매장 주문을 등록할 수 없다`() {
        val menuId = menuRepository.save(menu(19000L, true, menuProduct())).id
        val orderTableId = orderTableRepository.save(orderTable(false, 0)).id
        val expected = createOrderRequest(
            OrderType.EAT_IN, orderTableId, createOrderLineItemRequest(menuId, 19000L, 3L)
        )
        assertThatThrownBy { orderService.create(expected) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `숨겨진 메뉴는 주문할 수 없다`() {
        val menuId = menuRepository.save(menu(19000L, false, menuProduct())).id
        val expected = createOrderRequest(OrderType.TAKEOUT, createOrderLineItemRequest(menuId, 19000L, 3L))
        assertThatThrownBy { orderService.create(expected) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `주문한 메뉴의 가격은 실제 메뉴 가격과 일치해야 한다`() {
        val menuId = menuRepository.save(menu(19000L, true, menuProduct())).id
        val expected = createOrderRequest(OrderType.TAKEOUT, createOrderLineItemRequest(menuId, 16000L, 3L))
        assertThatThrownBy { orderService.create(expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `주문을 접수한다`() {
        val orderId = orderRepository.save(order(OrderStatus.WAITING, orderTable(true, 4))).id
        val actual = orderService.accept(orderId)
        Assertions.assertThat(actual.status).isEqualTo(OrderStatus.ACCEPTED)
    }

    @EnumSource(value = OrderStatus::class, names = ["WAITING"], mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    fun `접수 대기 중인 주문만 접수할 수 있다`(status: OrderStatus) {
        val orderId = orderRepository.save(order(status, orderTable(true, 4))).id
        assertThatThrownBy { orderService.accept(orderId) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `배달 주문을 접수되면 배달 대행사를 호출한다`() {
        val orderId = orderRepository.save(order(OrderStatus.WAITING, "서울시 송파구 위례성대로 2")).id
        val actual = orderService.accept(orderId)
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(actual.status).isEqualTo(OrderStatus.ACCEPTED) },
            Executable { Assertions.assertThat(kitchenridersClient.orderId).isEqualTo(orderId) },
            Executable { Assertions.assertThat(kitchenridersClient.deliveryAddress).isEqualTo("서울시 송파구 위례성대로 2") }
        )
    }

    @Test
    fun `주문을 서빙한다`() {
        val orderId = orderRepository.save(order(OrderStatus.ACCEPTED)).id
        val actual = orderService.serve(orderId)
        Assertions.assertThat(actual.status).isEqualTo(OrderStatus.SERVED)
    }

    @EnumSource(value = OrderStatus::class, names = ["ACCEPTED"], mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    fun `접수된 주문만 서빙할 수 있다`(status: OrderStatus) {
        val orderId = orderRepository.save(order(status)).id
        assertThatThrownBy { orderService.serve(orderId) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `주문을 배달한다`() {
        val orderId = orderRepository.save(order(OrderStatus.SERVED, "서울시 송파구 위례성대로 2")).id
        val actual = orderService.startDelivery(orderId)
        Assertions.assertThat(actual.status).isEqualTo(OrderStatus.DELIVERING)
    }

    @Test
    fun `배달 주문만 배달할 수 있다`() {
        val orderId = orderRepository.save(order(OrderStatus.SERVED)).id
        assertThatThrownBy { orderService.startDelivery(orderId) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @EnumSource(value = OrderStatus::class, names = ["SERVED"], mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    fun `서빙된 주문만 배달할 수 있다`(status: OrderStatus) {
        val orderId = orderRepository.save(order(status, "서울시 송파구 위례성대로 2")).id
        assertThatThrownBy { orderService.startDelivery(orderId) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `주문을 배달 완료한다`() {
        val orderId = orderRepository.save(order(OrderStatus.DELIVERING, "서울시 송파구 위례성대로 2")).id
        val actual = orderService.completeDelivery(orderId)
        Assertions.assertThat(actual.status).isEqualTo(OrderStatus.DELIVERED)
    }

    @EnumSource(value = OrderStatus::class, names = ["DELIVERING"], mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    fun `배달 중인 주문만 배달 완료할 수 있다`(status: OrderStatus) {
        val orderId = orderRepository.save(order(status, "서울시 송파구 위례성대로 2")).id
        assertThatThrownBy { orderService.completeDelivery(orderId) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `주문을 완료한다`() {
        val expected = orderRepository.save(order(OrderStatus.DELIVERED, "서울시 송파구 위례성대로 2"))
        val actual = orderService.complete(expected.id)
        Assertions.assertThat(actual.status).isEqualTo(OrderStatus.COMPLETED)
    }

    @EnumSource(value = OrderStatus::class, names = ["DELIVERED"], mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    fun `배달 주문의 경우 배달 완료된 주문만 완료할 수 있다`(status: OrderStatus) {
        val orderId = orderRepository.save(order(status, "서울시 송파구 위례성대로 2")).id
        assertThatThrownBy { orderService.complete(orderId) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @EnumSource(value = OrderStatus::class, names = ["SERVED"], mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    fun `포장 및 매장 주문의 경우 서빙된 주문만 완료할 수 있다`(status: OrderStatus) {
        val orderId = orderRepository.save(order(status)).id
        assertThatThrownBy { orderService.complete(orderId) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `주문 테이블의 모든 매장 주문이 완료되면 빈 테이블로 설정한다`() {
        val orderTable = orderTableRepository.save(orderTable(true, 4))
        val expected = orderRepository.save(order(OrderStatus.SERVED, orderTable))
        val actual = orderService.complete(expected.id)
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(actual.status).isEqualTo(OrderStatus.COMPLETED) },
            Executable {
                Assertions.assertThat(orderTableRepository.findById(orderTable.id).get().isOccupied).isFalse()
            },
            Executable {
                Assertions.assertThat(orderTableRepository.findById(orderTable.id).get().numberOfGuests).isEqualTo(0)
            }
        )
    }

    @Test
    fun `완료되지 않은 매장 주문이 있는 주문 테이블은 빈 테이블로 설정하지 않는다`() {
        val orderTable = orderTableRepository.save(orderTable(true, 4))
        orderRepository.save(order(OrderStatus.ACCEPTED, orderTable))
        val expected = orderRepository.save(order(OrderStatus.SERVED, orderTable))
        val actual = orderService.complete(expected.id)
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(actual.status).isEqualTo(OrderStatus.COMPLETED) },
            Executable {
                Assertions.assertThat(orderTableRepository.findById(orderTable.id).get().isOccupied).isTrue()
            },
            Executable {
                Assertions.assertThat(orderTableRepository.findById(orderTable.id).get().numberOfGuests).isEqualTo(4)
            }
        )
    }

    @Test
    fun `주문의 목록을 조회할 수 있다`() {
        val orderTable = orderTableRepository.save(orderTable(true, 4))
        orderRepository.save(order(OrderStatus.SERVED, orderTable))
        orderRepository.save(order(OrderStatus.DELIVERED, "서울시 송파구 위례성대로 2"))
        val actual = orderService.findAll()
        Assertions.assertThat(actual).hasSize(2)
    }

    private fun createOrderRequest(
        type: OrderType,
        deliveryAddress: String,
        vararg orderLineItems: OrderLineItem
    ): Order {
        val order = Order()
        order.type = type
        order.deliveryAddress = deliveryAddress
        order.orderLineItems = Arrays.asList(*orderLineItems)
        return order
    }

    private fun createOrderRequest(orderType: OrderType, vararg orderLineItems: OrderLineItem): Order {
        return createOrderRequest(orderType, Arrays.asList(*orderLineItems))
    }

    private fun createOrderRequest(orderType: OrderType, orderLineItems: List<OrderLineItem>): Order {
        val order = Order()
        order.type = orderType
        order.orderLineItems = orderLineItems
        return order
    }

    private fun createOrderRequest(
        type: OrderType,
        orderTableId: UUID,
        vararg orderLineItems: OrderLineItem
    ): Order {
        val order = Order()
        order.type = type
        order.orderTableId = orderTableId
        order.orderLineItems = Arrays.asList(*orderLineItems)
        return order
    }

    companion object {
        private fun createOrderLineItemRequest(menuId: UUID, price: Long, quantity: Long): OrderLineItem {
            val orderLineItem = OrderLineItem()
            orderLineItem.seq = Random().nextLong()
            orderLineItem.menuId = menuId
            orderLineItem.price = BigDecimal.valueOf(price)
            orderLineItem.quantity = quantity
            return orderLineItem
        }

        @JvmStatic
        private fun orderLineItems(): List<Arguments> {
            return listOf(
                Arguments.of(emptyList<OrderLineItem>()),
                Arguments.of(listOf(createOrderLineItemRequest(INVALID_ID, 19000L, 3L)))
            )
        }
    }
}