package com.newoether.agora.webui

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WebUiProviderAdminTest {
    @get:Rule val temporary = TemporaryFolder()
    private val hasher = WebUiPasswordHasher(iterations = 1_000)
    private val secret = "sk-test-0123456789abcdefWXYZ"

    private class FakeProviders : WebUiProviders {
        val added = mutableListOf<AddProviderRequest>()
        override suspend fun list() = added.map {
            ProviderSummary(it.name, custom = true, baseUrl = it.baseUrl, protocol = it.protocol, hasKey = true,
                keyHint = keyHintOf(it.apiKey), models = listOf(ProviderModel(it.modelId, it.alias)))
        }
        override suspend fun add(request: AddProviderRequest): AddProviderResult {
            added += request
            return AddProviderResult.Ok(list().last())
        }
    }

    private fun body(name: String = "Space Bunny", url: String = "https://opencode.ai/zen/go/v1/") =
        """{"name":"$name","baseUrl":"$url","apiKey":"$secret","modelId":"glm-5.1","alias":"Bunny"}"""

    @Test
    fun refusesWithoutASession() = webUi { _ ->
        assertEquals(HttpStatusCode.Forbidden, client.get("/api/providers").status)
        val post = client.post("/api/providers") { contentType(ContentType.Application.Json); setBody(body()) }
        assertEquals(HttpStatusCode.Forbidden, post.status)
        assertTrue(fake.added.isEmpty())
    }

    @Test
    fun refusesCrossOriginEvenWithASession() = webUi { token ->
        val post = client.post("/api/providers") {
            header(HttpHeaders.Cookie, "${WebUiServer.SESSION_COOKIE}=$token")
            header(HttpHeaders.Origin, "https://evil.example")
            contentType(ContentType.Application.Json)
            setBody(body())
        }
        assertEquals(HttpStatusCode.Forbidden, post.status)
        assertTrue(fake.added.isEmpty())
    }

    @Test
    fun addsThenListsWithoutEverReturningTheKey() = webUi { token ->
        val post = client.post("/api/providers") {
            header(HttpHeaders.Cookie, "${WebUiServer.SESSION_COOKIE}=$token")
            contentType(ContentType.Application.Json)
            setBody(body())
        }
        assertEquals(HttpStatusCode.Created, post.status)
        assertEquals("https://opencode.ai/zen/go/v1", fake.added.single().baseUrl)
        val list = client.get("/api/providers") { header(HttpHeaders.Cookie, "${WebUiServer.SESSION_COOKIE}=$token") }
        assertEquals(HttpStatusCode.OK, list.status)
        val text = list.bodyAsText()
        assertTrue(text.contains("\"Space Bunny\"") && text.contains("glm-5.1") && text.contains("WXYZ"))
        assertFalse(text.contains(secret))
        assertFalse(post.bodyAsText().contains(secret))
        assertEquals("no-store", list.headers[HttpHeaders.CacheControl])
    }

    @Test
    fun rejectsBadInputBeforeTouchingSettings() = webUi { token ->
        for (bad in listOf(body(url = "http://evil.example/v1"), body(name = "OpenCode Go"), "{\"name\":1}")) {
            val post = client.post("/api/providers") {
                header(HttpHeaders.Cookie, "${WebUiServer.SESSION_COOKIE}=$token")
                contentType(ContentType.Application.Json)
                setBody(bad)
            }
            assertEquals(HttpStatusCode.BadRequest, post.status)
        }
        assertTrue(fake.added.isEmpty())
    }

    @Test
    fun validationRules() {
        assertTrue(isAcceptableBaseUrl("https://api.example.com/v1"))
        assertTrue(isAcceptableBaseUrl("http://127.0.0.1:11434/v1"))
        assertFalse(isAcceptableBaseUrl("http://192.168.1.5/v1"))
        assertFalse(isAcceptableBaseUrl("https://user:pw@api.example.com"))
        assertFalse(isAcceptableBaseUrl("https://api.example.com/v1?k=1"))
        assertFalse(isAcceptableBaseUrl("file:///etc/passwd"))
        val ok = AddProviderRequest("Relay", "https://r.example/v1", secret, "m")
        assertNull(validateAddProvider(ok))
        assertEquals("invalid_api_key", validateAddProvider(ok.copy(apiKey = "has space")))
        assertEquals("invalid_protocol", validateAddProvider(ok.copy(protocol = "soap")))
        assertNull(keyHintOf("short"))
    }

    private lateinit var fake: FakeProviders

    private fun webUi(block: suspend ApplicationTestBuilder.(String) -> Unit) {
        fake = FakeProviders()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val path = temporary.newFolder().resolve("auth.preferences_pb")
        val store = WebUiSettingsStore(PreferenceDataStoreFactory.create(scope = scope,
            storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) { path.absolutePath.toPath() },
        ))
        runBlocking { store.savePasswordHash(hasher.hash("pw")) }
        val auth = WebUiAuth(store = store, hasher = hasher, clock = { 0L })
        val server = WebUiServer(
            auth = auth,
            readAsset = { null },
            syncSession = { _, _, _ -> },
            clock = { 0L },
            providers = fake,
        )
        try {
            testApplication {
                application { server.install(this) }
                val login = client.post("/api/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"password":"pw"}""")
                }
                val token = login.headers.getAll(HttpHeaders.SetCookie)!!.single()
                    .substringAfter('=').substringBefore(';')
                block(token)
            }
        } finally { runBlocking { scope.cancel(); scope.coroutineContext[Job]!!.join() } }
    }
}
