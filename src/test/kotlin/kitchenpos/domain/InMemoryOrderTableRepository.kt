package kitchenpos.domain

import java.util.*

class InMemoryOrderTableRepository : OrderTableRepository {
    private val orderTables: MutableMap<UUID, OrderTable> = HashMap()

    override fun save(orderTable: OrderTable): OrderTable {
        orderTables[orderTable.id] = orderTable
        return orderTable
    }

    override fun findById(id: UUID): Optional<OrderTable> {
        return Optional.ofNullable(orderTables[id])
    }

    override fun findAll(): List<OrderTable> {
        return orderTables.values.toList()
    }
}