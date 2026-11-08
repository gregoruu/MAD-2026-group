package ee.movara.app.ui.components

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.decode.DataSource
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import ee.movara.app.MovaraApplication
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovieImageCacheTest {
    @Test fun downloadedImage_isDecodedFromDiskWhenNetworkIsUnavailable() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<MovaraApplication>()
        val bytes = ByteArrayOutputStream().use { output ->
            val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
        val networkCalls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            if (networkCalls.incrementAndGet() > 1) throw IOException("Offline")
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(bytes.toResponseBody())
                .header("Content-Type", "image/png")
                .header("Cache-Control", "max-age=86400").build()
        }.build()
        // Retain the production disk cache and configuration; only replace HTTP transport.
        val productionLoader = context.newImageLoader(context)
        val loader = productionLoader.newBuilder()
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }.build()
        val url = "https://images.example.invalid/${UUID.randomUUID()}.png"
        val request = ImageRequest.Builder(context).data(url).size(20, 20)
            .memoryCachePolicy(CachePolicy.DISABLED).build()
        try {
            assertEquals(100L * 1024 * 1024, loader.diskCache!!.maxSize)
            val first = loader.execute(request)
            assertTrue("First load must decode the HTTP response: $first", first is SuccessResult)
            assertEquals(DataSource.NETWORK, (first as SuccessResult).dataSource)
            val second = loader.execute(request)
            assertTrue("Offline load must succeed: $second", second is SuccessResult)
            assertEquals(DataSource.DISK, (second as SuccessResult).dataSource)
            assertEquals("Disk hit must avoid HTTP entirely", 1, networkCalls.get())
        } finally {
            loader.diskCache?.remove(url)
            loader.shutdown()
            productionLoader.shutdown()
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }
}
