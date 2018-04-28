package fund.cyber.markets.connector.api

import fund.cyber.markets.common.model.TokensPair
import fund.cyber.markets.connector.ConnectorRunner
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderbookEndpoint(
        private val connectorRunner: ConnectorRunner
) {

    val connectors by lazy { connectorRunner.orderbookConnectors }

    @GetMapping("/orderbook")
    fun getOrderbook(
        @RequestParam(value = "exchange", required = true) exchange: String,
        @RequestParam(value = "pair", required = true) tokensPair: String
    ): ResponseEntity<Any> {

        val pair = TokensPair(tokensPair.substringBefore("_"), tokensPair.substringAfter("_"))
        val orderbook = connectors[exchange.toUpperCase()]?.getOrderBookSnapshot(pair)

        return if (orderbook == null) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } else {
            ResponseEntity.ok(orderbook)
        }
    }

}