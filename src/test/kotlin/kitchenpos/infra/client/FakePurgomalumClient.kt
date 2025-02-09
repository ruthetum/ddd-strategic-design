package kitchenpos.infra.client

import kitchenpos.infra.PurgomalumClient

class FakePurgomalumClient  : PurgomalumClient {

    override fun containsProfanity(text: String): Boolean {
        return profanities.any { text.contains(it) }
    }

    companion object {
        private val profanities = listOf("비속어", "욕설")
    }
}