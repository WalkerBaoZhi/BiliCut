package wbz.bili.cut

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlin.math.roundToInt

/**
 * 主界面 ViewModel
 * 管理应用状态和业务逻辑
 */
class MainViewModel : ViewModel() {

    val originalBitmap = mutableStateOf<Bitmap?>(null)

    // ---- 手动画线裁剪相关 ----
    val verticalLines = mutableStateListOf<Float>()        // 竖线位置（归一化 0.0~1.0，相对图片宽度）
    val horizontalLines = mutableStateListOf<Float>()      // 横线位置（归一化 0.0~1.0，相对图片高度）
    val hasCropped = mutableStateOf(false)                 // 是否已执行裁剪，用于切换画线/选择子模式
    private val lineHistory = mutableListOf<Boolean>()     // 添加历史：true=竖线, false=横线，用于撤销

    // ---- 裁剪结果相关 ----
    val croppedImages = mutableStateListOf<Bitmap>()
    val selectedIndices = mutableStateListOf<Int>()
    val stitchedResult = mutableStateOf<Bitmap?>(null)

    // ---- 通用状态 ----
    val isProcessing = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val currentScreen = mutableStateOf(Screen.HOME)
    val gridColumns = mutableStateOf(2)

    enum class Screen {
        HOME, PREVIEW, RESULT, ABOUT
    }

    fun setOriginalBitmap(bitmap: Bitmap) {
        originalBitmap.value = bitmap
        verticalLines.clear()
        horizontalLines.clear()
        lineHistory.clear()
        hasCropped.value = false
        croppedImages.clear()
        selectedIndices.clear()
        stitchedResult.value = null
        errorMessage.value = null
    }

    // ======================== 线条管理 ========================

    /**
     * 添加竖线，自动排序。
     * @param position 归一化位置 0.0~1.0（相对图片宽度）
     */
    fun addVerticalLine(position: Float) {
        val clamped = position.coerceIn(0.05f, 0.95f)
        // 避免与已有线太近
        if (verticalLines.any { kotlin.math.abs(it - clamped) < 0.02f }) return
        verticalLines.add(clamped)
        lineHistory.add(true)
        // 不排序！保持插入顺序以便撤销
    }

    /**
     * 添加横线，自动排序。
     * @param position 归一化位置 0.0~1.0（相对图片高度）
     */
    fun addHorizontalLine(position: Float) {
        val clamped = position.coerceIn(0.05f, 0.95f)
        if (horizontalLines.any { kotlin.math.abs(it - clamped) < 0.02f }) return
        horizontalLines.add(clamped)
        lineHistory.add(false)
        // 不排序！保持插入顺序以便撤销
    }

    /**
     * 撤销最后添加的一条线
     */
    fun removeLastLine() {
        if (lineHistory.isEmpty()) return
        val wasVertical = lineHistory.removeAt(lineHistory.lastIndex)
        if (wasVertical && verticalLines.isNotEmpty()) {
            verticalLines.removeAt(verticalLines.lastIndex) // 按插入顺序删除最后一条
        } else if (!wasVertical && horizontalLines.isNotEmpty()) {
            horizontalLines.removeAt(horizontalLines.lastIndex) // 按插入顺序删除最后一条
        }
    }

    /**
     * 获取排序后的竖线（用于裁剪和绘制）
     */
    val sortedVerticalLines: List<Float>
        get() = verticalLines.sorted()

    /**
     * 获取排序后的横线（用于裁剪和绘制）
     */
    val sortedHorizontalLines: List<Float>
        get() = horizontalLines.sorted()

    /**
     * 清除所有线条
     */
    fun clearLines() {
        verticalLines.clear()
        horizontalLines.clear()
        lineHistory.clear()
        hasCropped.value = false
        croppedImages.clear()
        selectedIndices.clear()
    }

    // ======================== 手动裁剪 ========================

    /**
     * 根据当前所有竖线和横线形成的网格，裁剪原图。
     * 每组相邻的竖线 + 每组相邻的横线构成一个裁剪区块。
     * 图片边缘（0.0 和 1.0）被隐式加入作为边界。
     */
    fun cropByLines() {
        val bitmap = originalBitmap.value ?: return

        if (verticalLines.size < 1 && horizontalLines.size < 1) {
            errorMessage.value = "请至少添加一条竖线或一条横线"
            return
        }

        isProcessing.value = true
        errorMessage.value = null

        try {
            // 构建列边界，包含图片边缘
            val colBoundaries = mutableListOf(0f)
            colBoundaries.addAll(sortedVerticalLines)
            colBoundaries.add(1f)

            // 构建行边界，包含图片边缘
            val rowBoundaries = mutableListOf(0f)
            rowBoundaries.addAll(sortedHorizontalLines)
            rowBoundaries.add(1f)

            val bw = bitmap.width.toFloat()
            val bh = bitmap.height.toFloat()

            val newCropped = mutableListOf<Bitmap>()

            for (row in 0 until rowBoundaries.size - 1) {
                for (col in 0 until colBoundaries.size - 1) {
                    val left = (colBoundaries[col] * bw).roundToInt()
                    val right = (colBoundaries[col + 1] * bw).roundToInt()
                    val top = (rowBoundaries[row] * bh).roundToInt()
                    val bottom = (rowBoundaries[row + 1] * bh).roundToInt()

                    val width = (right - left).coerceAtLeast(1)
                    val height = (bottom - top).coerceAtLeast(1)

                    // 确保在图片范围内
                    val safeLeft = left.coerceIn(0, bitmap.width - 1)
                    val safeTop = top.coerceIn(0, bitmap.height - 1)
                    val safeWidth = (safeLeft + width).coerceAtMost(bitmap.width) - safeLeft
                    val safeHeight = (safeTop + height).coerceAtMost(bitmap.height) - safeTop

                    if (safeWidth >= 10 && safeHeight >= 10) {
                        val cropped = Bitmap.createBitmap(
                            bitmap, safeLeft, safeTop, safeWidth, safeHeight
                        )
                        newCropped.add(cropped)
                    }
                }
            }

            if (newCropped.isEmpty()) {
                errorMessage.value = "裁剪区域过小，请重新调整线条位置"
                isProcessing.value = false
                return
            }

            croppedImages.clear()
            croppedImages.addAll(newCropped)
            selectedIndices.clear()
            hasCropped.value = true
        } catch (e: Exception) {
            errorMessage.value = "裁剪失败: ${e.message}"
        } finally {
            isProcessing.value = false
        }
    }

    // ======================== 区块选择 ========================

    fun toggleSelection(index: Int) {
        if (index in selectedIndices) {
            selectedIndices.remove(index)
        } else {
            selectedIndices.add(index)
        }
    }

    fun selectAll() {
        selectedIndices.clear()
        selectedIndices.addAll(croppedImages.indices)
    }

    fun deselectAll() {
        selectedIndices.clear()
    }

    // ======================== 拼接 ========================

    fun stitchSelected() {
        if (selectedIndices.isEmpty()) {
            errorMessage.value = "请至少选择一个视频块"
            return
        }

        val selectedImages = selectedIndices.map { croppedImages[it] }

        stitchedResult.value = try {
            ImageStitcher.stitchInGrid(selectedImages, gridColumns.value)
        } catch (e: Exception) {
            errorMessage.value = "拼接失败: ${e.message}"
            null
        }

        if (stitchedResult.value != null) {
            currentScreen.value = Screen.RESULT
        }
    }

    /**
     * 返回画线模式（保留线条，清空裁剪结果）
     */
    fun backToLineEditor() {
        hasCropped.value = false
        croppedImages.clear()
        selectedIndices.clear()
        stitchedResult.value = null
    }

    // ======================== 导航 ========================

    fun goBack() {
        when (currentScreen.value) {
            Screen.PREVIEW -> {
                if (hasCropped.value) {
                    backToLineEditor()
                } else {
                    currentScreen.value = Screen.HOME
                }
            }
            Screen.RESULT -> currentScreen.value = Screen.PREVIEW
            Screen.ABOUT -> currentScreen.value = Screen.HOME
            else -> {}
        }
    }

    fun reset() {
        originalBitmap.value = null
        verticalLines.clear()
        horizontalLines.clear()
        lineHistory.clear()
        hasCropped.value = false
        croppedImages.clear()
        selectedIndices.clear()
        stitchedResult.value = null
        errorMessage.value = null
        currentScreen.value = Screen.HOME
    }
}
