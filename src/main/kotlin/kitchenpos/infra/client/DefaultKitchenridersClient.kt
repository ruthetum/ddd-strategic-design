package kitchenpos.infra.client

import kitchenpos.application.KitchenridersClient
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.*

@Component
class DefaultKitchenridersClient : KitchenridersClient {
    override fun requestDelivery(orderId: UUID, amount: BigDecimal, deliveryAddress: String) {
        // do nothing
    }
}