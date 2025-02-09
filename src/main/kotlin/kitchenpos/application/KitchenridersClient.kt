package kitchenpos.application

import java.math.BigDecimal
import java.util.*

interface KitchenridersClient {
    fun requestDelivery(orderId: UUID, amount: BigDecimal, deliveryAddress: String)
}