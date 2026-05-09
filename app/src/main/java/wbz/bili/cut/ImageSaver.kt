package wbz.bili.cut

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException

/**
 * 图片保存工具
 * 将图片保存到系统相册
 */
object ImageSaver {

    /**
     * 保存Bitmap到系统相册
     * @param context 上下文
     * @param bitmap 要保存的图片
     * @param fileName 文件名（不含扩展名）
     * @return 是否保存成功
     */
    fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String = "BiliCut_${System.currentTimeMillis()}"): Boolean {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BiliCut")
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return false

        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
}
