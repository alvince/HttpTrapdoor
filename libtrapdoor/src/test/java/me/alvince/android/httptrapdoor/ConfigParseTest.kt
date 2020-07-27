package me.alvince.android.httptrapdoor

import org.junit.Test
import java.io.File

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
                /*Okio.buffer(Okio.source(file)).use { buffer ->
                    buffer.readString(Charset.forName("UTF-8"))
                }.let {
                    ConfigParser().parseJson(JSONArray(it))
                }.also {
                    println(it.toTypedArray().contentToString())
                }*/
            }
    }
}
