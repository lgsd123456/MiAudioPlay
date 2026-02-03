package com.miaudioplay.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miaudioplay.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsTestScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    var testResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("歌词API测试") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.PlayArrow, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "测试歌词API是否工作",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    isRunning = true
                    testResults = listOf("正在测试所有API...")
                    viewModel.testLyricsApis()
                    // 等待一段时间后显示提示
                    testResults = listOf(
                        "测试已开始！",
                        "请打开Android Studio的Logcat查看详细结果",
                        "搜索标签: MiAudioPlay",
                        "",
                        "或使用命令:",
                        "adb logcat | grep MiAudioPlay"
                    )
                    isRunning = false
                },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isRunning) "测试中..." else "开始测试所有API")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (testResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        items(testResults) { result ->
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "测试说明",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• 将测试所有7个API和5个网页爬虫\n" +
                        "• 测试歌曲: Taylor Swift - Love Story (英文)\n" +
                        "• 测试歌曲: 周杰伦 - 晴天 (中文)\n" +
                        "• 查看Logcat获取详细结果",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
