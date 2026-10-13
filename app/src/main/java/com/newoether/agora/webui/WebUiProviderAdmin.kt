package com.newoether.agora.webui

import com.newoether.agora.data.CustomEndpointProtocol
import com.newoether.agora.data.CustomProviderNamePolicy
import com.newoether.agora.data.repository.SettingsRepository
import com.newoether.agora.model.ModelId
import com.newoether.agora.viewmodel.ProviderRegistry
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Lets a signed-in agent (e.g. one running in Termux on the same phone) add an LLM provider and
 * read the provider list back, instead of a human typing it into Settings.
 *
 * Routes sit behind the same session + same-origin gate as `/api/sync`, so this grants nothing a
 * WebUI session could not already reach. API keys are write-only: the list never returns them,
 * only whether one is set and its last four characters.
 */
internal interface WebUiProviders {
    suspend fun list(): List<ProviderSummary>

    /** Adds or updates one custom provider, its active key and one model. */
    suspend fun add(request: AddProviderRequest): AddProviderResult
}

@Serializable
internal data class AddProviderRequest(
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    val alias: String = "",
    val protocol: String = "openai",
)

@Serializable
internal data class ProviderSummary(
    val name: String,
    val custom: Boolean,
    val baseUrl: String? = null,
    val protocol: String? = null,
    val hasKey: Boolean,
    val keyHint: String? = null,
    val models: List<ProviderModel> = emptyList(),
)

@Serializable
internal data class ProviderModel(val id: String, val alias: String? = null)

internal sealed interface AddProviderResult {
    data class Ok(val provider: ProviderSummary) : AddProviderResult
    data class Rejected(val status: HttpStatusCode, val error: String) : AddProviderResult
}

@Serializable private data class ProviderError(val error: String)

@Serializable private data class ProviderList(val providers: List<ProviderSummary>)

private val providerJson = Json { ignoreUnknownKeys = true; explicitNulls = false }
private const val MAX_PROVIDER_BODY_BYTES = 16L * 1024L

/** Installed under a route that already enforces a valid session and same origin. */
internal fun Route.providerAdminRoutes(admin: WebUiProviders) {
    get {
        call.respondProviderJson(HttpStatusCode.OK, providerJson.encodeToString(ProviderList(admin.list())))
    }
    post {
        if (!call.request.contentType().match(ContentType.Application.Json)) {
            return@post call.providerError(HttpStatusCode.UnsupportedMediaType, "invalid_request")
        }
        val length = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (length == null || length > MAX_PROVIDER_BODY_BYTES) {
            return@post call.providerError(HttpStatusCode.PayloadTooLarge, "invalid_request")
        }
        val request = runCatching { providerJson.decodeFromString<AddProviderRequest>(call.receiveText()) }
            .getOrNull()
            ?: return@post call.providerError(HttpStatusCode.BadRequest, "invalid_request")
        validateAddProvider(request)?.let { return@post call.providerError(HttpStatusCode.BadRequest, it) }
        when (val result = admin.add(request.normalized())) {
            is AddProviderResult.Ok -> call.respondProviderJson(
                HttpStatusCode.Created,
                providerJson.encodeToString(result.provider),
            )
            is AddProviderResult.Rejected -> call.providerError(result.status, result.error)
        }
    }
}

private fun AddProviderRequest.normalized() = copy(
    name = name.trim(),
    baseUrl = baseUrl.trim().trimEnd('/'),
    modelId = modelId.trim(),
    alias = alias.trim(),
    protocol = protocol.trim().lowercase(),
)

/** Returns an error code, or null when [request] is acceptable. */
internal fun validateAddProvider(request: AddProviderRequest): String? {
    val name = request.name.trim()
    if (name.isEmpty() || name.length > 64 || name.any { it.isISOControl() }) return "invalid_name"
    if (!CustomProviderNamePolicy.isAllowed(name)) return "reserved_name"
    if (!isAcceptableBaseUrl(request.baseUrl.trim())) return "invalid_base_url"
    val key = request.apiKey
    if (key.isEmpty() || key.length > 4096 || key.any { it.isWhitespace() || it.isISOControl() }) {
        return "invalid_api_key"
    }
    val model = request.modelId.trim()
    if (model.isEmpty() || model.length > 256 || model.any { it.isISOControl() }) return "invalid_model_id"
    if (request.alias.length > 128 || request.alias.any { it.isISOControl() }) return "invalid_alias"
    if (protocolOf(request.protocol) == null) return "invalid_protocol"
    return null
}

/** HTTPS anywhere; plain HTTP only to this device (a local relay never leaves loopback). */
internal fun isAcceptableBaseUrl(url: String): Boolean {
    if (url.isEmpty() || url.length > 2048) return false
    val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false
    val host = uri.host ?: return false
    if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) return false
    return when (uri.scheme?.lowercase()) {
        "https" -> true
        "http" -> host == "localhost" || host == "127.0.0.1" || host == "[::1]" || host == "::1"
        else -> false
    }
}

private fun protocolOf(value: String): CustomEndpointProtocol? = when (value.trim().lowercase()) {
    "openai" -> CustomEndpointProtocol.OPENAI
    "anthropic" -> CustomEndpointProtocol.ANTHROPIC
    "google" -> CustomEndpointProtocol.GOOGLE
    else -> null
}

/** Last four characters only, and only for keys long enough that four reveal nothing useful. */
internal fun keyHintOf(key: String?): String? =
    key?.takeIf { it.length >= 16 }?.let { "…" + it.takeLast(4) }

private suspend fun ApplicationCall.respondProviderJson(status: HttpStatusCode, body: String) {
    response.header(HttpHeaders.CacheControl, "no-store")
    respondText(body, ContentType.Application.Json, status)
}

private suspend fun ApplicationCall.providerError(status: HttpStatusCode, error: String) =
    respondProviderJson(status, providerJson.encodeToString(ProviderError(error)))

/** Production [WebUiProviders] over the app's live provider registry and settings. */
internal class AppWebUiProviders(
    private val registry: ProviderRegistry,
    private val settings: SettingsRepository,
) : WebUiProviders {

    override suspend fun list(): List<ProviderSummary> {
        val custom = settings.customProviders.value
        val urls = settings.providerBaseUrls.value
        val keys = settings.apiKeys.value
        val active = settings.activeApiKeyIds.value
        val models = settings.customModels.value
        val aliases = settings.modelAliases.value
        val names = custom.map { it.name } + keys.map { it.provider }.filter { p -> custom.none { it.name == p } }
        return names.distinct().map { name ->
            val config = custom.firstOrNull { it.name == name }
            val owners = setOfNotNull(name, config?.providerId)
            val key = keys.firstOrNull { it.id == active[name] }?.key
            ProviderSummary(
                name = name,
                custom = config != null,
                baseUrl = urls[name]?.takeIf { it.isNotBlank() },
                protocol = config?.protocol?.wireValue,
                hasKey = key != null,
                keyHint = keyHintOf(key),
                models = models.filter { ModelId.parse(it).providerName in owners }.sorted()
                    .map { ProviderModel(ModelId.parse(it).modelName, aliases[it]) },
            )
        }
    }

    override suspend fun add(request: AddProviderRequest): AddProviderResult {
        val protocol = protocolOf(request.protocol)
            ?: return AddProviderResult.Rejected(HttpStatusCode.BadRequest, "invalid_protocol")
        val existing = settings.customProviders.value.firstOrNull { it.name.equals(request.name, ignoreCase = true) }
        if (existing != null) {
            val currentUrl = settings.providerBaseUrls.value[existing.name].orEmpty().trimEnd('/')
            // Never silently repoint a provider (and its stored key) at a different host.
            if (!currentUrl.equals(request.baseUrl, ignoreCase = true) || existing.protocol != protocol) {
                return AddProviderResult.Rejected(HttpStatusCode.Conflict, "provider_exists_with_different_endpoint")
            }
        } else {
            withContext(Dispatchers.Main.immediate) { registry.addCustom(request.name, request.baseUrl, protocol) }
            awaitOrNull { settings.customProviders.first { list -> list.any { it.name == request.name } } }
                ?: return AddProviderResult.Rejected(HttpStatusCode.InternalServerError, "provider_create_failed")
        }
        val name = existing?.name ?: request.name
        settings.upsertApiKey(name, request.apiKey, name)
        awaitOrNull {
            combine(settings.apiKeys, settings.activeApiKeyIds) { keys, ids ->
                keys.any { it.id == ids[name] && it.key == request.apiKey }
            }.first { it }
        } ?: return AddProviderResult.Rejected(HttpStatusCode.InternalServerError, "key_save_failed")
        settings.addCustomModel(name, request.modelId, request.alias)
        val modelKey = ModelId(settings.stableProviderReference(name), request.modelId).prefixed
        awaitOrNull { settings.customModels.first { modelKey in it } }
            ?: return AddProviderResult.Rejected(HttpStatusCode.InternalServerError, "model_save_failed")
        val summary = list().firstOrNull { it.name == name }
            ?: return AddProviderResult.Rejected(HttpStatusCode.InternalServerError, "provider_missing")
        return AddProviderResult.Ok(summary)
    }

    private suspend fun <T> awaitOrNull(block: suspend () -> T): T? = withTimeoutOrNull(5_000L) { block() }
}
