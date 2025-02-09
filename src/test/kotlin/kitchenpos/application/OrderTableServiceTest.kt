package kitchenpos.application

import kitchenpos.Fixtures.order
import kitchenpos.Fixtures.orderTable
import kitchenpos.domain.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.ValueSource

class OrderTableServiceTest {
    private lateinit var orderTableRepository: OrderTableRepository
    private lateinit var orderRepository: OrderRepository
    private lateinit var orderTableService: OrderTableService

    @BeforeEach
    fun setUp() {
        orderTableRepository = InMemoryOrderTableRepository()
        orderRepository = InMemoryOrderRepository()
        orderTableService = OrderTableService(orderTableRepository, orderRepository)
    }

    @DisplayName("주문 테이블을 등록할 수 있다.")
    @Test
    fun create() {
        val expected = createOrderTableRequest("1번")
        val actual = orderTableService.create(expected)
        Assertions.assertThat(actual).isNotNull()
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(actual.id).isNotNull() },
            Executable { Assertions.assertThat(actual.name).isEqualTo(expected.name) },
            Executable { Assertions.assertThat(actual.numberOfGuests).isZero() },
            Executable { Assertions.assertThat(actual.isOccupied).isFalse() }
        )
    }

    @DisplayName("주문 테이블의 이름이 올바르지 않으면 등록할 수 없다.")
    @EmptySource
    @ParameterizedTest
    fun create(name: String) {
        val expected = createOrderTableRequest(name)
        Assertions.assertThatThrownBy { orderTableService.create(expected) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @DisplayName("빈 테이블을 해지할 수 있다.")
    @Test
    fun sit() {
        val orderTableId = orderTableRepository.save(orderTable(false, 0)).id
        val actual = orderTableService.sit(orderTableId)
        Assertions.assertThat(actual.isOccupied).isTrue()
    }

    @DisplayName("빈 테이블로 설정할 수 있다.")
    @Test
    fun clear() {
        val orderTableId = orderTableRepository.save(orderTable(true, 4)).id
        val actual = orderTableService.clear(orderTableId)
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(actual.numberOfGuests).isZero() },
            Executable { Assertions.assertThat(actual.isOccupied).isFalse() }
        )
    }

    @DisplayName("완료되지 않은 주문이 있는 주문 테이블은 빈 테이블로 설정할 수 없다.")
    @Test
    fun clearWithUncompletedOrders() {
        val orderTable = orderTableRepository.save(orderTable(true, 4))
        val orderTableId = orderTable.id
        orderRepository.save(order(OrderStatus.ACCEPTED, orderTable))
        Assertions.assertThatThrownBy { orderTableService.clear(orderTableId) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @DisplayName("방문한 손님 수를 변경할 수 있다.")
    @Test
    fun changeNumberOfGuests() {
        val orderTableId = orderTableRepository.save(orderTable(true, 0)).id
        val expected = changeNumberOfGuestsRequest(4)
        val actual = orderTableService.changeNumberOfGuests(orderTableId, expected)
        Assertions.assertThat(actual.numberOfGuests).isEqualTo(4)
    }

    @DisplayName("방문한 손님 수가 올바르지 않으면 변경할 수 없다.")
    @ValueSource(ints = [-1])
    @ParameterizedTest
    fun changeNumberOfGuests(numberOfGuests: Int) {
        val orderTableId = orderTableRepository.save(orderTable(true, 0)).id
        val expected = changeNumberOfGuestsRequest(numberOfGuests)
        Assertions.assertThatThrownBy {
            orderTableService.changeNumberOfGuests(
                orderTableId,
                expected
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @DisplayName("빈 테이블은 방문한 손님 수를 변경할 수 없다.")
    @Test
    fun changeNumberOfGuestsInEmptyTable() {
        val orderTableId = orderTableRepository.save(orderTable(false, 0)).id
        val expected = changeNumberOfGuestsRequest(4)
        Assertions.assertThatThrownBy {
            orderTableService.changeNumberOfGuests(
                orderTableId,
                expected
            )
        }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @DisplayName("주문 테이블의 목록을 조회할 수 있다.")
    @Test
    fun findAll() {
        orderTableRepository.save(orderTable())
        val actual = orderTableService.findAll()
        Assertions.assertThat(actual).hasSize(1)
    }

    private fun createOrderTableRequest(name: String): OrderTable {
        val orderTable = OrderTable()
        orderTable.name = name
        return orderTable
    }

    private fun changeNumberOfGuestsRequest(numberOfGuests: Int): OrderTable {
        val orderTable = OrderTable()
        orderTable.numberOfGuests = numberOfGuests
        return orderTable
    }
}