package com.fitmate.app.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import java.awt.BorderLayout
import java.io.File
import java.util.concurrent.Executors
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

private object ChromiumRuntime {
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var cefClient: CefClient? = null

    @Volatile
    private var cefBrowser: CefBrowser? = null

    private fun getClient(): CefClient {
        cefClient?.let { return it }

        synchronized(this) {
            cefClient?.let { return it }

            val builder = CefAppBuilder()
            builder.setInstallDir(File("jcef-bundle"))
           builder.setSkipInstallation(true)
            builder.setAppHandler(object : MavenCefAppHandlerAdapter() {})
            builder.getCefSettings().windowless_rendering_enabled = false
            builder.getCefSettings().root_cache_path =
                File("jcef-cache").absolutePath

            val app = builder.build()
            val client = app.createClient()

            client.addRequestHandler(
                object : CefRequestHandlerAdapter() {
                    override fun getResourceRequestHandler(
                        browser: CefBrowser?,
                        frame: CefFrame?,
                        request: CefRequest?,
                        isNavigation: Boolean,
                        isDownload: Boolean,
                        requestInitiator: String?,
                        disableDefaultHandling: BoolRef?
                    ): CefResourceRequestHandler {
                        return object : CefResourceRequestHandlerAdapter() {
                            override fun onBeforeResourceLoad(
                                browser: CefBrowser?,
                                frame: CefFrame?,
                                request: CefRequest?
                            ): Boolean {
                                request?.let {
                                    it.setReferrer(
                                        "https://fitmate.app/",
                                        CefRequest.ReferrerPolicy.REFERRER_POLICY_ORIGIN
                                    )
                                    it.setHeaderByName(
                                        "Referer",
                                        "https://fitmate.app/",
                                        true
                                    )
                                }
                                return false
                            }
                        }
                    }
                }
            )

            cefClient = client
            return client
        }
    }

    fun load(url: String, panel: JPanel) {
        executor.execute {
            try {
                val client = getClient()
                var browser = cefBrowser

                if (browser == null) {
                    browser = client.createBrowser(url, false, false)
                    cefBrowser = browser
                } else {
                    browser.loadURL(url)
                }

                val activeBrowser = browser

                SwingUtilities.invokeLater {
                    panel.removeAll()
                    panel.add(activeBrowser.uiComponent, BorderLayout.CENTER)
                    panel.revalidate()
                    panel.repaint()
                }
            } catch (error: Exception) {
                SwingUtilities.invokeLater {
                    panel.removeAll()
                    panel.add(
                        JLabel(
                            "Chromium error: ${error.message}",
                            SwingConstants.CENTER
                        ),
                        BorderLayout.CENTER
                    )
                    panel.revalidate()
                    panel.repaint()
                }
            }
        }
    }
}

@Composable
fun TutorialPlayer(
    tutorial: Tutorial,
    modifier: Modifier = Modifier
) {
    SwingPanel(
        modifier = modifier,
        factory = {
            JPanel(BorderLayout()).apply {
                add(
                    JLabel("Starting Chromium player...", SwingConstants.CENTER),
                    BorderLayout.CENTER
                )
                ChromiumRuntime.load(tutorial.embedUrl, this)
            }
        },
        update = {}
    )
}
