@file:OptIn(ExperimentalUnsignedTypes::class)

package page.smirnov.kmqttcrash

import co.touchlab.kermit.Logger
import io.github.davidepianca98.MQTTClient
import io.github.davidepianca98.mqtt.MQTTVersion
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTConnack
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTDisconnect
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import kotlinx.coroutines.*

class MqttClient(
    private val ioDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher,
) {

    private val logger = Logger.withTag(LOG_TAG)

    private val coroutineScope = CoroutineScope(defaultDispatcher + SupervisorJob())

    private var connectionOptions: ConnectionOptions? = null

    private var client: MQTTClient? = null

    private var wasDisconnectInitiated = false

    fun connect(clientId: String, host: String, port: Int) {
        logger.d { "Connect: clientId=$clientId, host=$host, port=$port" }

        connectionOptions = ConnectionOptions(
            clientId = clientId,
            host = host,
            port = port,
            userName = null,
            password = null,
        )

        connectInternal()
    }

    private fun connectInternal() {
        logger.d { "connectInternal" }

        wasDisconnectInitiated = false

        val connectionOptions = requireNotNull(this.connectionOptions) {
            "Called connectInternal with null ConnectionOptions"
        }

        val client = MQTTClient(
            mqttVersion = MQTTVersion.MQTT3_1_1,
            address = connectionOptions.host,
            port = connectionOptions.port,
            keepAlive = KEEP_ALIVE_INTERVAL_SECONDS,
            cleanStart = true,
            clientId = connectionOptions.clientId,
            userName = connectionOptions.userName,
            password = connectionOptions.password?.toByteArray()?.toUByteArray(),
            tls = null, // TODO: Use TLS,
            onConnected = ::onConnected,
            onDisconnected = ::onDisconnected,
            publishReceived = {},
        )

        this.client = client

        client.runSuspend(
            dispatcher = defaultDispatcher,
            exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                logger.e(throwable) { "Exception occurred in MQTT: ${throwable.message}" }
            }
        )
    }

    fun disconnect() {
        logger.d { "disconnect" }

        wasDisconnectInitiated = true

        client?.disconnect(ReasonCode.DISCONNECT_WITH_WILL_MESSAGE)
        client = null
    }

    private fun onConnected(connAck: MQTTConnack) {
        logger.d { "onConnected" }
    }

    private fun onDisconnected(disconnect: MQTTDisconnect?) {
        logger.d { "onDisconnected" }

        if (!wasDisconnectInitiated) {
            logger.d { "Disconnection was not initiated by us, will reconnect after $RECONNECT_DELAY_MS ms" }

            coroutineScope.launch {
                delay(RECONNECT_DELAY_MS)
                connectInternal()
            }
        } else {
            logger.d { "Disconnection was initiated by us, will not reconnect" }
        }
    }

    private data class ConnectionOptions(
        val clientId: String,
        val host: String,
        val port: Int,
        val userName: String?,
        val password: String?,
    )


    private companion object {
        private const val LOG_TAG = "PlatformMqttClient"

        private const val KEEP_ALIVE_INTERVAL_SECONDS = 30

        private const val RECONNECT_DELAY_MS = 1000L * 1
    }
}