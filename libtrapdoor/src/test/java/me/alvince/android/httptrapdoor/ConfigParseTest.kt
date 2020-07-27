package me.alvince.android.httptrapdoor

import me.alvince.android.httptrapdoor.util.ConfigParser
import okio.Okio
import org.json.JSONArray
import org.junit.Test
import java.io.File
import java.nio.charset.Charset

class ConfigParseTest {

    @Throws(Exception::class)
    @Test
    fun testParseJson() {
        javaClass.getResource("/test_config.json")
            ?.let {
                println("URL: $it")
                File(it.file)
            }
            ?.also { file ->
                Okio.buffer(Okio.source(file)).use { buffer ->
                    buffer.readString(Charset.forName("UTF-8"))
                }.let {
                    ConfigParser().parseJson(JSONArray(it))
                }.also {
                    println(it.toTypedArray().contentToString())
                }
            }
    }
}
