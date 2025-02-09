package kitchenpos.infra.client

import kitchenpos.infra.KitchenridersClient
import java.math.BigDecimal
import java.util.*

class FakeKitchenridersClient : KitchenridersClient {
    var orderId: UUID? = null
        private set

    var amount: BigDecimal? = null
        private set

    var deliveryAddress: String? = null
        private set

    override fun requestDelivery(orderId: UUID, amount: BigDecimal, deliveryAddress: String) {
        this.orderId = orderId
        this.amount = amount
        this.deliveryAddress = deliveryAddress
    }
}