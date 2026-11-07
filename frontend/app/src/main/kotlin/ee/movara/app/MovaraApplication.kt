package ee.movara.app

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath

class MovaraApplication : Application(), SingletonImageLoader.Factory {
  val container: AppContainer by lazy { AppContainer(applicationContext) }

  override fun newImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
      .crossfade(true)
      .diskCache {
        DiskCache.Builder()
          .directory(context.cacheDir.resolve("movie_images").toOkioPath())
          .maxSizeBytes(100L * 1024 * 1024)
          .build()
      }
      .build()
  }
}
