# KMQTT crash demo

This is a minimal demo to reproduce [KMQTT/issues/70](https://github.com/davidepianca98/KMQTT/issues/70).

Steps:

1. Launch app
2. Wait for connection
3. Turn off device's screen
4. `adb shell dumpsys deviceidle force-idle`
5. `adb shell am set-inactive page.smirnov.kmqttcrash true`
6. Wait for ~minute

After client detects disconnection, it tries to reconnect and crashes:

```
2025-01-25 16:50:06.611 23843-23878 PlatformMqttClient      page.smirnov.kmqttcrash              D  onDisconnected
2025-01-25 16:50:06.621 23843-23878 PlatformMqttClient      page.smirnov.kmqttcrash              D  Disconnection was not initiated by us, will reconnect after 1000 ms
2025-01-25 16:50:06.637 23843-23878 PlatformMqttClient      page.smirnov.kmqttcrash              E  Exception occurred in MQTT: Read to buffer error End Of Stream (-1)
                                                                                                    io.github.davidepianca98.socket.SocketClosedException: Read to buffer error End Of Stream (-1)
                                                                                                    	at io.github.davidepianca98.socket.tcp.Socket.readToBuffer(Socket.kt:65)
                                                                                                    	at io.github.davidepianca98.socket.tcp.Socket.read--5HJl4c(Socket.kt:75)
                                                                                                    	at io.github.davidepianca98.ClientSocket.read--5HJl4c(ClientSocket.kt:40)
                                                                                                    	at io.github.davidepianca98.MQTTClient.check(MQTTClient.kt:389)
                                                                                                    	at io.github.davidepianca98.MQTTClient.step(MQTTClient.kt:466)
                                                                                                    	at io.github.davidepianca98.MQTTClient.run(MQTTClient.kt:476)
                                                                                                    	at io.github.davidepianca98.MQTTClient$runSuspend$2.invokeSuspend(MQTTClient.kt:491)
                                                                                                    	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
                                                                                                    	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:100)
                                                                                                    	at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:586)
                                                                                                    	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:829)
                                                                                                    	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:717)
                                                                                                    	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:704)
2025-01-25 16:50:07.635 23843-23878 PlatformMqttClient      page.smirnov.kmqttcrash              D  connectInternal
2025-01-25 16:50:07.637 23843-23878 TrafficStats            page.smirnov.kmqttcrash              D  tagSocket(5) with statsTag=0xffffffff, statsUid=-1
2025-01-25 16:50:07.863 23843-23878 AndroidRuntime          page.smirnov.kmqttcrash              E  FATAL EXCEPTION: DefaultDispatcher-worker-3
                                                                                                    Process: page.smirnov.kmqttcrash, PID: 23843
2025-01-25 16:50:07.885 23843-23878 Process                 page.smirnov.kmqttcrash              I  Sending signal. PID: 23843 SIG: 9
```

It will also instantly crash if you will re-launch it.

Seems that it's not handling network access absence properly while connecting.