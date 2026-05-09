package wbz.bili.cut

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import wbz.bili.cut.ui.theme.BiliCutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BiliCutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BiliCutApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiliCutApp(viewModel: MainViewModel = viewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showWelcomeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        if (isFirstLaunch) {
            showWelcomeDialog = true
            prefs.edit().putBoolean("is_first_launch", false).apply()
        }
    }

    if (showWelcomeDialog) {
        AlertDialog(
            onDismissRequest = { showWelcomeDialog = false },
            title = { Text("欢迎") },
            text = { Text("这是我开发的第一个APP，希望你能喜欢") },
            confirmButton = {
                TextButton(onClick = { showWelcomeDialog = false }) {
                    Text("我知道了")
                }
            }
        )
    }

    LaunchedEffect(viewModel.errorMessage.value) {
        viewModel.errorMessage.value?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.errorMessage.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (viewModel.currentScreen.value) {
                            MainViewModel.Screen.PREVIEW -> {
                                if (viewModel.hasCropped.value) "选择视频块" else "标记裁剪线"
                            }
                            MainViewModel.Screen.RESULT -> "拼接结果"
                            MainViewModel.Screen.ABOUT -> "关于我"
                            else -> "BiliCut"
                        }
                    )
                },
                navigationIcon = {
                    if (viewModel.currentScreen.value != MainViewModel.Screen.HOME &&
                        viewModel.currentScreen.value != MainViewModel.Screen.ABOUT
                    ) {
                        IconButton(onClick = { viewModel.goBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                },
                actions = {
                    if (viewModel.currentScreen.value == MainViewModel.Screen.HOME) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "主页"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (viewModel.currentScreen.value == MainViewModel.Screen.HOME ||
                viewModel.currentScreen.value == MainViewModel.Screen.ABOUT
            ) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Text("✂️") },
                        label = { Text("裁剪") },
                        selected = viewModel.currentScreen.value == MainViewModel.Screen.HOME,
                        onClick = { viewModel.currentScreen.value = MainViewModel.Screen.HOME }
                    )
                    NavigationBarItem(
                        icon = { Text("🐒") },
                        label = { Text("关于") },
                        selected = viewModel.currentScreen.value == MainViewModel.Screen.ABOUT,
                        onClick = { viewModel.currentScreen.value = MainViewModel.Screen.ABOUT }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (viewModel.currentScreen.value) {
                MainViewModel.Screen.HOME -> HomeScreen(viewModel)
                MainViewModel.Screen.PREVIEW -> PreviewScreen(viewModel)
                MainViewModel.Screen.RESULT -> ResultScreen(viewModel)
                MainViewModel.Screen.ABOUT -> AboutScreen()
            }
        }
    }
}

// ============================================================
//  首页
// ============================================================

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                bitmap?.let { bmp ->
                    viewModel.setOriginalBitmap(bmp)
                    viewModel.currentScreen.value = MainViewModel.Screen.PREVIEW
                }
            } catch (e: Exception) {
                viewModel.errorMessage.value = "加载图片失败: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "BiliCut",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "上传B站截图，手动标记裁剪线分割视频块",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "在图上添加竖线和横线，自动按网格裁剪为多个视频块",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("选择截图")
        }
    }

    if (viewModel.isProcessing.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ============================================================
//  预览/编辑/选择 屏幕（双模式）
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(viewModel: MainViewModel) {
    if (viewModel.hasCropped.value) {
        BlockSelectorScreen(viewModel)
    } else {
        LineEditorScreen(viewModel)
    }
}

// ===================== 子模式 A：画线编辑器 =====================

@Composable
fun LineEditorScreen(viewModel: MainViewModel) {
    val bitmap = viewModel.originalBitmap.value
    if (bitmap == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("未加载图片")
        }
        return
    }

    var isAddingVertical by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "点击图片添加${if (isAddingVertical) "竖线" else "横线"}，线将穿过点击位置",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "原图",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isAddingVertical) {
                        detectTapGestures { offset ->
                            val size = this.size
                            val relativeX = offset.x / size.width
                            val relativeY = offset.y / size.height
                            if (isAddingVertical) {
                                viewModel.addVerticalLine(relativeX)
                            } else {
                                viewModel.addHorizontalLine(relativeY)
                            }
                        }
                    },
                contentScale = ContentScale.Fit
            )

            Canvas(modifier = Modifier.matchParentSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                for (pos in viewModel.sortedVerticalLines) {
                    val x = pos * canvasWidth
                    drawLine(
                        color = Color.Red,
                        start = Offset(x, 0f),
                        end = Offset(x, canvasHeight),
                        strokeWidth = 4f
                    )
                    drawCircle(
                        color = Color.Red,
                        radius = 10f,
                        center = Offset(x, 20f)
                    )
                }

                for (pos in viewModel.sortedHorizontalLines) {
                    val y = pos * canvasHeight
                    drawLine(
                        color = Color.Blue,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 4f
                    )
                    drawCircle(
                        color = Color.Blue,
                        radius = 10f,
                        center = Offset(20f, y)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "竖线: ${viewModel.verticalLines.size}",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "横线: ${viewModel.horizontalLines.size}",
                color = Color.Blue,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = isAddingVertical,
                onClick = { isAddingVertical = true },
                label = { Text("竖线") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.Red.copy(alpha = 0.2f)
                )
            )
            FilterChip(
                selected = !isAddingVertical,
                onClick = { isAddingVertical = false },
                label = { Text("横线") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.Blue.copy(alpha = 0.2f)
                )
            )
            OutlinedButton(
                onClick = { viewModel.removeLastLine() },
                enabled = viewModel.verticalLines.isNotEmpty() || viewModel.horizontalLines.isNotEmpty()
            ) {
                Text("撤销")
            }
            OutlinedButton(
                onClick = { viewModel.clearLines() },
                enabled = viewModel.verticalLines.isNotEmpty() || viewModel.horizontalLines.isNotEmpty()
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("清除")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.cropByLines() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = viewModel.verticalLines.isNotEmpty() || viewModel.horizontalLines.isNotEmpty()
        ) {
            Text("执行裁剪")
        }
    }
}

// ===================== 子模式 B：区块选择器 =====================

@Composable
fun BlockSelectorScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "已裁剪出 ${viewModel.croppedImages.size} 个区块，点击选择要拼接的块",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                TextButton(onClick = { viewModel.selectAll() }) {
                    Text("全选")
                }
                TextButton(onClick = { viewModel.deselectAll() }) {
                    Text("全不选")
                }
            }

            Text("已选: ${viewModel.selectedIndices.size}")
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(viewModel.croppedImages) { index, bitmap ->
                val isSelected = index in viewModel.selectedIndices

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.8f)
                        .clickable { viewModel.toggleSelection(index) }
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "区块 $index",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(48.dp),
                                    tint = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.backToLineEditor() },
                modifier = Modifier.weight(1f)
            ) {
                Text("返回画线")
            }

            Button(
                onClick = { viewModel.stitchSelected() },
                modifier = Modifier.weight(2f),
                enabled = viewModel.selectedIndices.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("拼接选中项 (${viewModel.selectedIndices.size})")
            }
        }
    }
}

// ============================================================
//  结果屏幕
// ============================================================

@Composable
fun ResultScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var saveSuccess by remember { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "拼接结果",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        viewModel.stitchedResult.value?.let { bitmap ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "拼接结果",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            saveSuccess?.let { success ->
                Text(
                    text = if (success) "保存成功！" else "保存失败，请检查权限",
                    color = if (success) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.stitchedResult.value = null
                        viewModel.currentScreen.value = MainViewModel.Screen.PREVIEW
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("返回调整")
                }

                Button(
                    onClick = {
                        saveSuccess = ImageSaver.saveToGallery(context, bitmap)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存到相册")
                }
            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("拼接结果为空")
            }
        }
    }
}

// ============================================================
//  关于页面
// ============================================================

@Composable
fun AboutScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 应用信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.icon).asImageBitmap(),
                    contentDescription = "应用图标",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "BiliCut",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "1.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "看到B站上好玩的封面拼接想分享出去？来这里裁剪吧！",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 作者介绍卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.my).asImageBitmap(),
                        contentDescription = "头像",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(32.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "WalkerBaoZhi",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "喜欢开发小东西的大学生",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://github.com/WalkerBaoZhi/"))
                            context.startActivity(intent)
                        }
                    ) {
                        Image(
                            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.github).asImageBitmap(),
                            contentDescription = "GitHub",
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
