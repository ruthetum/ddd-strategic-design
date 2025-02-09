package kitchenpos.application

import kitchenpos.domain.OrderRepository
import kitchenpos.domain.OrderStatus
import kitchenpos.domain.OrderTable
import kitchenpos.domain.OrderTableRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class OrderTableService(
    private val orderTableRepository: OrderTableRepository,
    private val orderRepository: OrderRepository,
) {
    @Transactional
    fun create(request: OrderTable): OrderTable {
        val name = request.name
        require(!(Objects.isNull(name) || name.isEmpty()))
        val orderTable = OrderTable()
        orderTable.id = UUID.randomUUID()
        orderTable.name = name
        orderTable.numberOfGuests = 0
        orderTable.isOccupied = false
        return orderTableRepository.save(orderTable)
    }

    @Transactional
    fun sit(orderTableId: UUID): OrderTable {
        val orderTable = orderTableRepository.findById(orderTableId)
            .orElseThrow { NoSuchElementException() }
        orderTable.isOccupied = true
        return orderTable
    }

    @Transactional
    fun clear(orderTableId: UUID): OrderTable {
        val orderTable = orderTableRepository.findById(orderTableId)
            .orElseThrow { NoSuchElementException() }
        check(!orderRepository.existsByOrderTableAndStatusNot(orderTable, OrderStatus.COMPLETED))
        orderTable.numberOfGuests = 0
        orderTable.isOccupied = false
        return orderTable
    }

    @Transactional
    fun changeNumberOfGuests(orderTableId: UUID, request: OrderTable): OrderTable {
        val numberOfGuests = request.numberOfGuests
        require(numberOfGuests >= 0)
        val orderTable = orderTableRepository.findById(orderTableId)
            .orElseThrow { NoSuchElementException() }
        check(orderTable.isOccupied)
        orderTable.numberOfGuests = numberOfGuests
        return orderTable
    }

    @Transactional(readOnly = true)
    fun findAll(): List<OrderTable> {
        return orderTableRepository.findAll()
    }
}