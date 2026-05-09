package wbz.bili.cut

import android.graphics.Bitmap
import android.graphics.Canvas


/**
 * 图片拼接工具
 * 支持网格拼接，可指定列数
 */
object ImageStitcher {

    /**
     * 网格拼接，无边框无间距，图片直接拼接
     * @param bitmaps 要拼接的图片列表
     * @param columns 列数
     * @return 拼接后的图片
     */
    fun stitchInGrid(
        bitmaps: List<Bitmap>,
        columns: Int = 2
    ): Bitmap {
        if (bitmaps.isEmpty()) {
            throw IllegalArgumentException("图片列表不能为空")
        }

        val rows = (bitmaps.size + columns - 1) / columns

        val cellWidth = bitmaps.maxOf { it.width }
        val cellHeight = bitmaps.maxOf { it.height }

        val totalWidth = columns * cellWidth
        val totalHeight = rows * cellHeight

        val result = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        bitmaps.forEachIndexed { index, bitmap ->
            val row = index / columns
            val col = index % columns

            val x = col * cellWidth
            val y = row * cellHeight

            val scaledBitmap = if (bitmap.width != cellWidth || bitmap.height != cellHeight) {
                Bitmap.createScaledBitmap(bitmap, cellWidth, cellHeight, true)
            } else {
                bitmap
            }

            canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
        }

        return result
    }
}
