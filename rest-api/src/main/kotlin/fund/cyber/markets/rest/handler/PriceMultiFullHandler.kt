package fund.cyber.markets.rest.handler

import fund.cyber.markets.cassandra.repository.TickerRepository
import fund.cyber.markets.cassandra.repository.VolumeRepository
import fund.cyber.markets.common.Durations
import fund.cyber.markets.dto.TokensPair
import fund.cyber.markets.helpers.MILLIS_TO_SECONDS
import fund.cyber.markets.helpers.booleanValue
import fund.cyber.markets.helpers.closestSmallerMultiply
import fund.cyber.markets.helpers.convert
import fund.cyber.markets.helpers.stringValue
import fund.cyber.markets.model.Ticker
import fund.cyber.markets.rest.common.CrossConversion
import fund.cyber.markets.rest.configuration.AppContext
import fund.cyber.markets.rest.model.PriceMultiFullData
import fund.cyber.markets.rest.model.PriceMultiFullModel
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

class PriceMultiFullHandler(
    private val tickerRepository: TickerRepository = AppContext.tickerRepository,
    private val volumeRepository: VolumeRepository = AppContext.volumeRepository
    ) : AbstractHandler(), HttpHandler {

    override fun handleRequest(httpExchange: HttpServerExchange) {

        val params = httpExchange.queryParameters
        val bases = params["fsyms"]?.stringValue()?.split(",")
        val quotes = params["tsyms"]?.stringValue()?.split(",")
        val exchange = params["e"]?.stringValue() ?: "ALL"
        val tryConversion = params["tryConversion"]?.booleanValue() ?: true

        if (bases == null || quotes == null) {
            handleBadRequest("Bad parameters", httpExchange)
            return
        }

        val timestamp = closestSmallerMultiply(System.currentTimeMillis(), Durations.MINUTE)
        val raw = mutableMapOf<String, MutableMap<String, PriceMultiFullData>>()

        for (base in bases) {
            val quoteFullData = mutableMapOf<String, PriceMultiFullData>()
            for (quote in quotes) {

                if (base == quote) {
                    continue
                }

                var ticker24h: Ticker? = null
                var closePrice = tickerRepository.getTicker(TokensPair(base, quote), Durations.MINUTE, exchange, timestamp)?.close
                if (closePrice == null && tryConversion) {
                    val crossConversion = CrossConversion(base, quote, "ALL", Durations.MINUTE, timestamp).calculate()
                    if (crossConversion.success) {
                        closePrice = crossConversion.value
                    }
                } else {
                    ticker24h = tickerRepository.getTicker24h(TokensPair(base, quote), exchange)
                }

                val volumeBase = volumeRepository.getVolume24h(base, exchange)
                val volumeQuote = volumeRepository.getVolume24h(quote, exchange)

                var priceData: PriceMultiFullData? = null

                if (closePrice != null && ticker24h != null) {
                    priceData = PriceMultiFullData(
                            exchange,
                            base,
                            quote,
                            closePrice,
                            timestamp convert MILLIS_TO_SECONDS,
                            volumeBase?.value,
                            volumeQuote?.value,
                            ticker24h.open,
                            ticker24h.maxPrice,
                            ticker24h.minPrice
                    )
                } else if (closePrice != null) {
                    priceData = PriceMultiFullData(
                            exchange,
                            base,
                            quote,
                            closePrice,
                            timestamp convert MILLIS_TO_SECONDS,
                            volumeBase?.value,
                            volumeQuote?.value,
                            closePrice,
                            closePrice,
                            closePrice
                    )
                }

                if (priceData != null) {
                    quoteFullData.put(quote, priceData)
                }
            }
            raw.put(base, quoteFullData)
        }

        if (raw.isEmpty()) {
            handleNoData(httpExchange)
            return
        }

        send(PriceMultiFullModel(raw), httpExchange)
    }

}