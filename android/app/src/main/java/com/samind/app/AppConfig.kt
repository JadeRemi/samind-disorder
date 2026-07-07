package com.samind.app

// Single lookup point for integration secrets injected at build time.
// All empty in the open-source build: chat stays on the local rule engine,
// models ship in assets, and the manifest requests no network permission.
// A cloud-enabled flavor only has to fill gradle.properties.
object AppConfig {
    val chatApiBaseUrl: String = BuildConfig.CHAT_API_BASE_URL
    val chatApiKey: String = BuildConfig.CHAT_API_KEY
    val modelUpdateUrl: String = BuildConfig.MODEL_UPDATE_URL
    val sentryDsn: String = BuildConfig.SENTRY_DSN

    val remoteChatConfigured: Boolean
        get() = chatApiBaseUrl.isNotEmpty() && chatApiKey.isNotEmpty()

    val modelUpdatesConfigured: Boolean
        get() = modelUpdateUrl.isNotEmpty()
}
