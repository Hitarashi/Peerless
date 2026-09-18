package org.shilpo.peerless

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.shilpo.peerless.auth.DeepLinkHandler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

private const val SINGLE_INSTANCE_PORT = 49444

fun main(args: Array<String>) {
    val deepLinkArg = args.firstOrNull { it.startsWith("peerless://") || it.contains("data=") }

    val serverSocket: ServerSocket = try {
        ServerSocket(SINGLE_INSTANCE_PORT, 50, InetAddress.getByName("127.0.0.1"))
    } catch (_: Exception) {
        // Another instance is already running
        if (deepLinkArg != null) {
            try {
                Socket("127.0.0.1", SINGLE_INSTANCE_PORT).use { socket ->
                    val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), true)
                    writer.println(deepLinkArg)
                }
            } catch (_: Exception) {
            }
        }
        return
    }

    if (deepLinkArg != null) {
        DeepLinkHandler.handleUri(deepLinkArg)
    }

    thread(isDaemon = true, name = "Peerless-SingleInstanceListener") {
        while (!serverSocket.isClosed) {
            try {
                val client = serverSocket.accept()
                thread(isDaemon = true) {
                    try {
                        client.use { s ->
                            val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                            val line = reader.readLine()
                            if (!line.isNullOrBlank()) {
                                DeepLinkHandler.handleUri(line)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
                break
            }
        }
    }

    application {
        Window(
            onCloseRequest = {
                try {
                    serverSocket.close()
                } catch (_: Exception) {
                }
                exitApplication()
            },
            title = "Peerless",
        ) {
            App()
        }
    }
}
