package fund.cyber.markets.rest.handler

import com.fasterxml.jackson.databind.ObjectMapper
import fund.cyber.markets.rest.configuration.AppContext
import fund.cyber.markets.rest.model.ErrorMessage
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers

open class AbstractHandler(
        private val jsonSerializer: ObjectMapper = AppContext.jsonSerializer
) {

    fun handleBadRequest(errorMessage: String, httpExchange: HttpServerExchange) {
        val response = ErrorMessage(
                "Error",
                errorMessage
        )
        httpExchange.statusCode = 400
        httpExchange.responseHeaders.put(Headers.CONTENT_TYPE, "application/json")
        httpExchange.responseSender.send(jsonSerializer.writeValueAsString(response))
    }

    fun handleNoData(httpExchange: HttpServerExchange) {
        val response = ErrorMessage(
                "Error",
                "Sorry no data available"
        )
        httpExchange.statusCode = 404
        httpExchange.responseHeaders.put(Headers.CONTENT_TYPE, "application/json")
        httpExchange.responseSender.send(jsonSerializer.writeValueAsString(response))
    }

    fun send(result: Any, httpExchange: HttpServerExchange) {
        httpExchange.responseHeaders.put(Headers.CONTENT_TYPE, "application/json")
        httpExchange.responseSender.send(jsonSerializer.writeValueAsString(result))
    }

}