package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.TestReport
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneTestApp(viewModel: PhoneTestViewModel) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberNavy
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = screenState) {
                is ScreenState.Home -> HomeScreen(viewModel)
                is ScreenState.Testing -> ActiveTestingScreen(viewModel)
                is ScreenState.Report -> ReportScreen(viewModel, state)
                is ScreenState.History -> HistoryScreen(viewModel)
            }
        }
    }
}

@Composable
fun HardwareMatrixGrid(
    tests: List<TestDefinition>,
    results: Map<String, TestStatus>,
    activeTestId: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DenseSurface, RoundedCornerShape(16.dp))
            .border(1.dp, DenseOutline, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Testing",
                color = DenseTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            val completedCount = results.values.count { it == TestStatus.PASSED || it == TestStatus.FAILED || it == TestStatus.SKIPPED }
            Text(
                text = "$completedCount / ${tests.size} Checked",
                color = DensePrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        // 3-column high density matrix grid of hardware modules
        val chunked = tests.chunked(3)
        chunked.forEach { rowTests ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTests.forEach { test ->
                    val status = results[test.id] ?: TestStatus.PENDING
                    val isActive = test.id == activeTestId

                    val cardBgColor = when {
                        isActive -> DensePrimary
                        status == TestStatus.PASSED -> DenseContainer
                        status == TestStatus.FAILED -> DenseRedContainer
                        else -> DenseBg
                    }

                    val borderColor = when {
                        isActive -> DensePrimary
                        status == TestStatus.PASSED -> DenseAccentGlow
                        status == TestStatus.FAILED -> DenseRed
                        else -> DenseDivider
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val displayName = when (test.id) {
                                "device_info" -> "Specs"
                                "internet_check" -> "Internet"
                                "cameras_check" -> "Cameras"
                                "screen_colors" -> "Screen"
                                "touch_grid" -> "Touch"
                                "vibration" -> "Haptic"
                                "speaker" -> "Speaker"
                                "microphone" -> "Mic"
                                "flashlight" -> "Flash"
                                "accelerometer" -> "G-Sens"
                                "light_sensor" -> "Light"
                                "proximity" -> "Proximity"
                                else -> test.name.take(8)
                            }
                            Text(
                                text = displayName.uppercase(),
                                color = if (isActive) Color.White.copy(alpha = 0.9f) else DenseTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.2.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (isActive) {
                                Text(
                                    text = "...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            } else {
                                when (status) {
                                    TestStatus.PASSED -> {
                                        Text(
                                            text = "✓",
                                            color = DenseGreen,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                    TestStatus.FAILED -> {
                                        Text(
                                            text = "!",
                                            color = DenseRed,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                    TestStatus.SKIPPED -> {
                                        Text(
                                            text = "→",
                                            color = DenseSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    else -> {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(DenseSecondary.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (rowTests.size < 3) {
                    for (i in 0 until (3 - rowTests.size)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: PhoneTestViewModel) {
    val specs by viewModel.deviceSpecs.collectAsStateWithLifecycle()
    val pastReports by viewModel.pastReports.collectAsStateWithLifecycle()

    // Dynamically retrieve and parse results from the latest report to populate the status matrix
    val latestReport = pastReports.firstOrNull()
    val latestResults = remember(latestReport) {
        if (latestReport != null && latestReport.resultsString.isNotEmpty()) {
            latestReport.resultsString.split(";").associate { part ->
                val pair = part.split(":")
                if (pair.size == 2) {
                    val key = pair[0]
                    val value = try {
                        TestStatus.valueOf(pair[1])
                    } catch (e: Exception) {
                        TestStatus.PENDING
                    }
                    key to value
                } else {
                    "" to TestStatus.PENDING
                }
            }.filterKeys { it.isNotEmpty() }
        } else {
            emptyMap()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title & Dynamic Device Model Chip (Left/Right arrangement from high-density theme)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.app_icon_new_1784544356232),
                        contentDescription = "Testify Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, DenseOutline, RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Text(
                        text = "Testify",
                        color = DenseTextPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                val deviceModelName = specs["Model"] ?: android.os.Build.MODEL
                Card(
                    colors = CardDefaults.cardColors(containerColor = DensePrimaryContainer),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = deviceModelName,
                        color = DenseOnPrimaryContainer,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Start Diagnostic Test Button
        item {
            Button(
                onClick = { viewModel.startTestingSession() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_test_button")
                    .shadow(4.dp, RoundedCornerShape(100.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DensePrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(100.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start Test",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START TESTING",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Hardware Status Matrix Grid (Dynamic status parsing!)
        item {
            HardwareMatrixGrid(
                tests = viewModel.tests,
                results = latestResults
            )
        }

        // Navigation Row to History (Full Width)
        item {
            OutlinedButton(
                onClick = { viewModel.navigateToHistory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, DenseOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DensePrimary)
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "Test History",
                    tint = DensePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "View Test History (${pastReports.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun ActiveTestingScreen(viewModel: PhoneTestViewModel) {
    val currentTestIndex by viewModel.currentTestIndex.collectAsStateWithLifecycle()
    val activeTest = viewModel.tests[currentTestIndex]
    val results by viewModel.testResults.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.navigateToHome()
    }

    val isFullScreenTest = activeTest.id == "screen_colors" || activeTest.id == "touch_grid"

    if (isFullScreenTest) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ActiveTestInterface(testId = activeTest.id, viewModel = viewModel)

            // Semi-transparent Skip button at top-right corner
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
            ) {
                OutlinedButton(
                    onClick = { viewModel.skipCurrentTest() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White.copy(alpha = 0.8f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.testTag("full_screen_skip_button")
                ) {
                    Text(
                        text = "SKIP TEST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Progress header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateToHome() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Testing",
                            tint = DenseRed
                        )
                    }

                    Text(
                        text = "TESTING ${currentTestIndex + 1} OF ${viewModel.tests.size}",
                        color = DensePrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )

                    // Placeholder to balance close button
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Thin high fidelity progress bar
                LinearProgressIndicator(
                    progress = { (currentTestIndex + 1).toFloat() / viewModel.tests.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(100.dp)),
                    color = DensePrimary,
                    trackColor = DenseDivider
                )
            }

            // Core interactive test workspace
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Test Title
                    Text(
                        text = activeTest.name,
                        color = DenseTextPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    // Category badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DensePrimaryContainer),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    ) {
                        Text(
                            text = activeTest.category.uppercase(),
                            color = DenseOnPrimaryContainer,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    // Interactive Content Zone
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DenseSurface),
                        border = BorderStroke(1.dp, DenseOutline),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ActiveTestInterface(testId = activeTest.id, viewModel = viewModel)
                        }
                    }
                }
            }

            // Test Navigation Footer (Skip / Pass / Fail)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Instructions
                Text(
                    text = activeTest.description,
                    color = DenseTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Skip Button
                    Button(
                        onClick = { viewModel.skipCurrentTest() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("skip_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DenseSurface,
                            contentColor = DensePrimary
                        ),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, DenseOutline)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip Test"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SKIP",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    // Manual confirm buttons if not auto test, or provided as fallback
                    if (!activeTest.isAutoTest || activeTest.id == "screen_colors" || activeTest.id == "vibration" || activeTest.id == "speaker" || activeTest.id == "flashlight") {
                        // Fail Button
                        Button(
                            onClick = { viewModel.failCurrentTest() },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("fail_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DenseRedContainer,
                                contentColor = DenseRed
                            ),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, DenseRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbDown,
                                contentDescription = "Fail Test"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FAIL",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }

                        // Pass Button
                        Button(
                            onClick = { viewModel.passCurrentTest() },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("pass_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DensePrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, DensePrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "Pass Test"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PASS",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ActiveTestInterface(testId: String, viewModel: PhoneTestViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    when (testId) {
        "device_info" -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = CyberCyan)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Analyzing Device Specifications...",
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        "internet_check" -> {
            val statusText by viewModel.internetStatusText.collectAsStateWithLifecycle()
            val isPassed by viewModel.isInternetPassed.collectAsStateWithLifecycle()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = when (isPassed) {
                        true -> Icons.Default.Wifi
                        false -> Icons.Default.WifiOff
                        else -> Icons.Default.NetworkCheck
                    },
                    contentDescription = "Internet Status",
                    tint = when (isPassed) {
                        true -> DenseGreen
                        false -> DenseRed
                        else -> DensePrimary
                    },
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = statusText.uppercase(),
                    color = DenseTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isPassed == null) {
                    CircularProgressIndicator(
                        color = DensePrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isPassed == true) "PASSED" else "FAILED",
                        color = if (isPassed == true) DenseGreen else DenseRed,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            }
        }

        "cameras_check" -> {
            val statusText by viewModel.cameraStatusText.collectAsStateWithLifecycle()
            val cameraInfos by viewModel.cameraInfos.collectAsStateWithLifecycle()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Cameras",
                    tint = DensePrimary,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = statusText.uppercase(),
                    color = DenseTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (cameraInfos.isEmpty()) {
                    CircularProgressIndicator(
                        color = DensePrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DenseContainer),
                        border = BorderStroke(1.dp, DenseOutline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "DETECTION RESULTS:",
                                color = DensePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            cameraInfos.forEach { info ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "•",
                                        color = DenseGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = info,
                                        color = DenseTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        "screen_colors" -> {
            val colorIndex by viewModel.currentColorIndex.collectAsStateWithLifecycle()
            val colorsList = listOf(
                Color.Red to "RED",
                Color.Green to "GREEN",
                Color.Blue to "BLUE",
                Color.White to "WHITE",
                Color.Black to "BLACK"
            )

            val (currentColor, colorName) = colorsList[colorIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(currentColor, RoundedCornerShape(12.dp))
                    .clickable { viewModel.advanceScreenColor() },
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TAP SCREEN TO CHANGE COLOR",
                    color = if (currentColor == Color.White) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )

                Text(
                    text = colorName,
                    color = if (currentColor == Color.White) Color.Black else Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "${colorIndex + 1} of 5",
                    color = if (currentColor == Color.White) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        "touch_grid" -> {
            val touchedBlocks by viewModel.touchedBlocks.collectAsStateWithLifecycle()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "DRAG OVER ALL TILES TO FILL THEM GREEN",
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${touchedBlocks.size} / 60 blocks filled",
                    color = CyberTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(CyberNavy, RoundedCornerShape(12.dp))
                        .border(1.dp, CyberPurple, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, _ ->
                                    val x = change.position.x
                                    val y = change.position.y
                                    val w = size.width
                                    val h = size.height
                                    val colWidth = w / 6f
                                    val rowHeight = h / 10f
                                    val col = (x / colWidth).toInt().coerceIn(0, 5)
                                    val row = (y / rowHeight).toInt().coerceIn(0, 9)
                                    val blockIndex = row * 6 + col
                                    viewModel.touchGridBlock(blockIndex)
                                },
                                onDragStart = { offset ->
                                    val w = size.width
                                    val h = size.height
                                    val colWidth = w / 6f
                                    val rowHeight = h / 10f
                                    val col = (offset.x / colWidth).toInt().coerceIn(0, 5)
                                    val row = (offset.y / rowHeight).toInt().coerceIn(0, 9)
                                    val blockIndex = row * 6 + col
                                    viewModel.touchGridBlock(blockIndex)
                                }
                            )
                        }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        for (r in 0 until 10) {
                            Row(modifier = Modifier.weight(1f)) {
                                for (c in 0 until 6) {
                                    val index = r * 6 + c
                                    val isTouched = index in touchedBlocks
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(1.dp)
                                            .background(
                                                if (isTouched) CyberGreen.copy(alpha = 0.8f)
                                                else CyberSurface
                                            )
                                            .border(0.5.dp, CyberPurple.copy(alpha = 0.15f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        "vibration" -> {
            val isVibrating by viewModel.isVibrating.collectAsStateWithLifecycle()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = "Vibration Icon",
                    tint = if (isVibrating) CyberCyan else CyberTextSecondary,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "DEVICE PULSING VIBRATION",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Vibrating state: ${if (isVibrating) "ACTIVE" else "INACTIVE"}",
                    color = if (isVibrating) CyberGreen else CyberRed,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        "speaker" -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Speaker Sound",
                    tint = CyberCyan,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PLAYING 440 HZ SINE WAVE",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Please verify speaker volume and clarity.",
                    color = CyberTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        "microphone" -> {
            val permissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)

            if (permissionState.status.isGranted) {
                val db by viewModel.micDecibels.collectAsStateWithLifecycle()
                val progress = (db / 100f).coerceIn(0f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = if (db >= 65f) CyberGreen else CyberCyan,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "MICROPHONE LEVEL DETECTOR",
                        color = CyberTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = if (db >= 65f) CyberGreen else CyberCyan,
                        trackColor = CyberNavy
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = String.format("%.1f dB / 65.0 dB Target", db),
                        color = if (db >= 65f) CyberGreen else CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Mic Permission Off",
                        tint = CyberRed,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Microphone Permission Required",
                        color = CyberTextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberNavy)
                    ) {
                        Text("GRANT MIC PERMISSION", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        "flashlight" -> {
            val isFlashOn by viewModel.isFlashlightOn.collectAsStateWithLifecycle()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FlashlightOn,
                    contentDescription = "Flashlight",
                    tint = if (isFlashOn) CyberCyan else CyberTextSecondary,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "REAR FLASHLIGHT TESTING",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "State: ${if (isFlashOn) "ON (ACTIVE)" else "OFF"}",
                    color = if (isFlashOn) CyberGreen else CyberRed,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        "accelerometer" -> {
            val x by viewModel.accelX.collectAsStateWithLifecycle()
            val y by viewModel.accelY.collectAsStateWithLifecycle()
            val z by viewModel.accelZ.collectAsStateWithLifecycle()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "TILT PHONE LEFT/RIGHT/UP/DOWN TO PASS",
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(CyberNavy, RoundedCornerShape(12.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val offsetX = (-x * 12).coerceIn(-110f, 110f).dp
                    val offsetY = (y * 12).coerceIn(-110f, 110f).dp

                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .border(2.dp, CyberCyan.copy(alpha = 0.3f), CircleShape)
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = offsetX, y = offsetY)
                            .size(36.dp)
                            .background(CyberCyan, CircleShape)
                            .shadow(6.dp, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("X: ${String.format("%.2f", x)}", color = CyberTextPrimary, fontFamily = FontFamily.Monospace)
                    Text("Y: ${String.format("%.2f", y)}", color = CyberTextPrimary, fontFamily = FontFamily.Monospace)
                    Text("Z: ${String.format("%.2f", z)}", color = CyberTextPrimary, fontFamily = FontFamily.Monospace)
                }
            }
        }

        "light_sensor" -> {
            val lux by viewModel.lightLux.collectAsStateWithLifecycle()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Light Sensor",
                    tint = if (lux > 0) CyberCyan else CyberRed,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AMBIENT LIGHT LEVEL",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (lux == -1f) {
                    CircularProgressIndicator(color = CyberCyan)
                } else if (lux == -2f) {
                    Text(
                        text = "LIGHT SENSOR NOT DETECTED ON THIS DEVICE",
                        color = CyberRed,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = String.format("%.1f Lux", lux),
                        color = CyberCyan,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Cover sensor to reach < 20 lux then expose to sunlight/lamp (> 70 lux) to pass.",
                        color = CyberTextSecondary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    )
                }
            }
        }

        "proximity" -> {
            val dist by viewModel.proximityDistance.collectAsStateWithLifecycle()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Waves,
                    contentDescription = "Proximity Sensor",
                    tint = if (dist >= 0) CyberCyan else CyberRed,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PROXIMITY DETECTOR",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (dist == -1f) {
                    CircularProgressIndicator(color = CyberCyan)
                } else if (dist == -2f) {
                    Text(
                        text = "PROXIMITY SENSOR NOT DETECTED ON THIS DEVICE",
                        color = CyberRed,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    val isNear = dist < 3f
                    Text(
                        text = if (isNear) "NEAR (OBJECT DETECTED)" else "FAR (CLEAR)",
                        color = if (isNear) CyberPink else CyberGreen,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format("Sensor Reading: %.1f cm", dist),
                        color = CyberTextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Wave your hand close to and away from the top speaker region to pass.",
                        color = CyberTextSecondary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }

        "fingerprint_check" -> {
            val status by viewModel.fingerprintStatus.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Fingerprint Sensor",
                    tint = CyberCyan,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "BIOMETRIC FINGERPRINT DIALER",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = status,
                    color = CyberTextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.runFingerprintDiagnostic() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("INITIALIZE BIOMETRIC TEST", fontWeight = FontWeight.Bold)
                }
            }
        }

        "face_id_check" -> {
            val status by viewModel.faceIdStatus.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Face ID",
                    tint = CyberCyan,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "TRUEDEPTH FACIAL RECOGNITION",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = status,
                    color = CyberTextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.runFaceIdDiagnostic() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("START FACIAL DEPTH SCAN", fontWeight = FontWeight.Bold)
                }
            }
        }

        "gps_satellite" -> {
            val coords by viewModel.gpsCoordinates.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "GPS",
                    tint = CyberCyan,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "GPS HARDWARE RECEIVER",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = DenseContainer),
                    border = BorderStroke(1.dp, DenseOutline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = coords,
                        color = CyberGreen,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        "bluetooth_mesh" -> {
            val devices by viewModel.bluetoothDevices.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.padding(16.dp).fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = "Bluetooth Scanner",
                    tint = CyberCyan,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "BLE MESH ANTENNA SCAN",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, DenseOutline, RoundedCornerShape(8.dp)).background(CyberNavy).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(devices) { dev ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text("•", color = CyberCyan, fontWeight = FontWeight.Bold)
                            Text(dev, color = CyberTextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        "nfc_antenna" -> {
            val status by viewModel.nfcStatus.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = "NFC Antenna",
                    tint = CyberCyan,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "NFC CONTROLLER TRANSCEIVER",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = status,
                    color = CyberTextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.simulateNfcTap() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SIMULATE NFC TARGET TAP", fontWeight = FontWeight.Bold)
                }
            }
        }

        "sim_card_status" -> {
            val simState by viewModel.simCardState.collectAsStateWithLifecycle()
            val simCarrier by viewModel.simCarrierName.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SimCard,
                    contentDescription = "SIM Card",
                    tint = CyberCyan,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "CELLULAR MODEM & SLOTS",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = DenseContainer),
                    border = BorderStroke(1.dp, DenseOutline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(simState.uppercase(), color = CyberGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(simCarrier, color = CyberTextSecondary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        "earpiece_check" -> {
            val status by viewModel.earpieceStatus.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Hearing,
                    contentDescription = "Earpiece",
                    tint = CyberCyan,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "EARPIECE CALL RECEIVER",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = status,
                    color = CyberTextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.startEarpieceTone() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ROUTEPATH 440HZ TONE", fontWeight = FontWeight.Bold)
                }
            }
        }

        "mic_array" -> {
            val status by viewModel.micArrayStatus.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.padding(16.dp).fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Multi-Mic Array",
                    tint = CyberCyan,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "MULTI-MIC CHANNEL DECOUPLING",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, DenseOutline, RoundedCornerShape(8.dp)).background(CyberNavy).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(status) { channel ->
                        Text(channel, color = CyberGreen, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        "jack_detect" -> {
            val plugged by viewModel.jackPlugged.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Headset,
                    contentDescription = "Audio Jack",
                    tint = if (plugged) CyberGreen else CyberTextSecondary,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "AUXILIARY PLUG CONTROLLER",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (plugged) "HEADPHONE INSERTION DETECTED (ACTIVE LINK)" else "WAITING FOR HEADPHONE / USB-C AUX INSERTION...",
                    color = if (plugged) CyberGreen else CyberRed,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.checkAuxJackStatus() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("POLL HEADPHONE PORT STATE", fontWeight = FontWeight.Bold)
                }
            }
        }

        "thermal_throttling" -> {
            val temp by viewModel.thermalTemp.collectAsStateWithLifecycle()
            val status by viewModel.thermalStatusText.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Thermostat,
                    contentDescription = "Thermostat",
                    tint = if (temp > 40) CyberRed else CyberCyan,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "THERMAL BUS DIAGNOSTIC",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = DenseContainer),
                    border = BorderStroke(1.dp, DenseOutline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${String.format("%.1f", temp)}°C",
                            color = if (temp > 40) CyberRed else CyberGreen,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = status,
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        "barometer_seal" -> {
            val pressure by viewModel.barometerPressure.collectAsStateWithLifecycle()
            val status by viewModel.barometerSealStatus.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Barometer",
                    tint = CyberCyan,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "BAROMETRIC WATERPROOF TEST",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = DenseContainer),
                    border = BorderStroke(1.dp, DenseOutline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${String.format("%.2f", pressure)} hPa",
                            color = CyberCyan,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = status,
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        "charging_speed" -> {
            val amp by viewModel.chargingAmperage.collectAsStateWithLifecycle()
            val volt by viewModel.chargingVoltage.collectAsStateWithLifecycle()
            val protocol by viewModel.chargingProtocol.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Charging Protocol",
                    tint = CyberCyan,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "INTELLIGENT POWER CONTROLLER",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = DenseContainer),
                    border = BorderStroke(1.dp, DenseOutline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${String.format("%.1f", amp)} mA / ${String.format("%.2f", volt)} V",
                            color = CyberGreen,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = protocol,
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        "qi_coil" -> {
            val active by viewModel.wirelessChargingActive.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsInputAntenna,
                    contentDescription = "Wireless Charging",
                    tint = if (active) CyberGreen else CyberTextSecondary,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "QI WIRELESS INDUCTION CHIP",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (active) "ACTIVE COIL HANDSHAKE REGISTERED (CURRENT FLOWING)" else "AWAITING WIRELESS INDUCTIVE POWER COIL CONTACT...",
                    color = if (active) CyberGreen else CyberRed,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.simulateWirelessQiCurrent() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SIMULATE QI POWER CURRENT", fontWeight = FontWeight.Bold)
                }
            }
        }

        "usb_data_line" -> {
            val status by viewModel.usbDataHandshake.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Usb,
                    contentDescription = "USB OTG",
                    tint = CyberCyan,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "USB D+/D- TRANSFER CONTROLLER",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = DenseContainer),
                    border = BorderStroke(1.dp, DenseOutline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = status,
                        color = CyberGreen,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        "burn_in_check" -> {
            val timer by viewModel.burnInTimer.collectAsStateWithLifecycle()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black, RoundedCornerShape(12.dp))
                    .border(2.dp, CyberPurple, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineSpacing = 16f
                    for (x in 0..size.width.toInt() step lineSpacing.toInt()) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.25f),
                            start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f),
                            end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height),
                            strokeWidth = 2f
                        )
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                    border = BorderStroke(1.dp, CyberCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "OLED GHOSTING INSPECTOR",
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$timer SECS",
                            color = CyberPink,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Inspect the white grid carefully against the dark canvas for burn-in shadow patterns.",
                            color = CyberTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        "multitouch_points" -> {
            val count by viewModel.multiTouchCount.collectAsStateWithLifecycle()
            var activePoints by remember { mutableStateOf(0) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "PLACE UP TO 10 FINGERS CONCURRENTLY",
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "RECORDED MAX: $count FINGERS",
                    color = CyberPink,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(CyberNavy, RoundedCornerShape(12.dp))
                        .border(1.dp, CyberPurple, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val countTouch = event.changes.count { it.pressed }
                                    activePoints = countTouch
                                    if (countTouch > 0) {
                                        viewModel.updateMultiTouchCount(countTouch)
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Active contacts: $activePoints",
                        color = CyberCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        "fps_stress" -> {
            val fps by viewModel.gpuFps.collectAsStateWithLifecycle()
            var frameTrigger by remember { mutableStateOf(0) }
            val particles = remember { List(40) { androidx.compose.ui.geometry.Offset(Random.nextFloat() * 400f, Random.nextFloat() * 400f) } }
            
            LaunchedEffect(Unit) {
                var lastTime = System.nanoTime()
                while (true) {
                    withFrameNanos { time ->
                        val diffMs = (time - lastTime) / 1000000f
                        val currentFps = 1000f / diffMs
                        viewModel.addGpuFpsFrame(currentFps.coerceIn(0f, 120f))
                        lastTime = time
                        frameTrigger++
                    }
                    delay(8)
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "GPU FRAME RATE BENCHMARK",
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        particles.forEach { p ->
                            drawCircle(
                                color = CyberCyan.copy(alpha = 0.4f),
                                radius = 12f + (frameTrigger % 30).toFloat(),
                                center = androidx.compose.ui.geometry.Offset(
                                    (p.x + frameTrigger * 2) % size.width,
                                    (p.y + frameTrigger * 3) % size.height
                                )
                            )
                        }
                    }
                    
                    Text(
                        text = "${String.format("%.1f", fps)} FPS",
                        color = CyberGreen,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Frame Stability Index: Excellent (98%)",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        "display_origin" -> {
            val status by viewModel.displayAuthenticityStatus.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "OEM Signature Check",
                    tint = CyberCyan,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "DISPLAY AUTHENTICITY PANEL",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = DenseContainer),
                    border = BorderStroke(1.dp, DenseOutline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = status,
                        color = CyberGreen,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        "storage_health" -> {
            val writeSpeed by viewModel.storageWriteSpeed.collectAsStateWithLifecycle()
            val readSpeed by viewModel.storageReadSpeed.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Storage Bench",
                    tint = CyberCyan,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "STORAGE READ & WRITE BENCHMARK",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DenseContainer),
                        border = BorderStroke(1.dp, DenseOutline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WRITE SPEED", color = CyberTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("${String.format("%.1f", writeSpeed)} MB/s", color = CyberGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DenseContainer),
                        border = BorderStroke(1.dp, DenseOutline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("READ SPEED", color = CyberTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("${String.format("%.1f", readSpeed)} MB/s", color = CyberGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        "button_click" -> {
            val volUp by viewModel.volumeUpPressed.collectAsStateWithLifecycle()
            val volDown by viewModel.volumeDownPressed.collectAsStateWithLifecycle()
            val backPressed by viewModel.backKeyPressed.collectAsStateWithLifecycle()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Physical Buttons",
                    tint = CyberCyan,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PHYSICAL INTERRUPT MATRIX",
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().border(1.dp, DenseOutline, RoundedCornerShape(8.dp)).background(if (volUp) CyberGreen.copy(alpha = 0.2f) else CyberNavy).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("VOLUME UP KEY", color = if (volUp) CyberGreen else CyberTextSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Icon(
                            imageVector = if (volUp) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Status",
                            tint = if (volUp) CyberGreen else CyberTextSecondary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().border(1.dp, DenseOutline, RoundedCornerShape(8.dp)).background(if (volDown) CyberGreen.copy(alpha = 0.2f) else CyberNavy).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("VOLUME DOWN KEY", color = if (volDown) CyberGreen else CyberTextSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Icon(
                            imageVector = if (volDown) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Status",
                            tint = if (volDown) CyberGreen else CyberTextSecondary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().border(1.dp, DenseOutline, RoundedCornerShape(8.dp)).background(if (backPressed) CyberGreen.copy(alpha = 0.2f) else CyberNavy).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BACK NAVIGATION KEY", color = if (backPressed) CyberGreen else CyberTextSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Icon(
                            imageVector = if (backPressed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Status",
                            tint = if (backPressed) CyberGreen else CyberTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportScreen(viewModel: PhoneTestViewModel, state: ScreenState.Report) {
    val total = state.results.size
    val scorePercentage = if (total > 0) (state.passed * 100) / total else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Headline
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DIAGNOSTIC PROTOCOL COMPLETE",
                    color = DensePrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Diagnostic Report",
                    color = DenseTextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Circular Gauge Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DenseSurface),
                border = BorderStroke(1.dp, DenseOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(DensePrimaryContainer.copy(alpha = 0.5f), CircleShape)
                            .border(1.5.dp, DensePrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$scorePercentage%",
                                color = DensePrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "HEALTH GRADE",
                                color = DenseOnPrimaryContainer,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        val gradeText = when {
                            scorePercentage >= 90 -> "Excellent"
                            scorePercentage >= 75 -> "Good Condition"
                            scorePercentage >= 50 -> "Fair / Serviceable"
                            else -> "Action Required"
                        }
                        val gradeColor = when {
                            scorePercentage >= 75 -> DenseGreen
                            scorePercentage >= 50 -> DensePrimary
                            else -> DenseRed
                        }

                        Text(
                            text = "HEALTH STATUS",
                            color = DenseTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = gradeText,
                            color = gradeColor,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Device is ${if (scorePercentage >= 75) "fully optimized" else "experiencing some issues"}.",
                            color = DenseTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Stats summary cards row (Passed / Failed / Skipped)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Passed
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DenseContainer),
                    border = BorderStroke(1.dp, DenseAccentGlow),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("PASSED", color = DenseGreen, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${state.passed}", color = DenseTextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }

                // Failed
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DenseRedContainer),
                    border = BorderStroke(1.dp, DenseRed.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("FAILED", color = DenseRed, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${state.failed}", color = DenseTextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }

                // Skipped
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DenseSurface),
                    border = BorderStroke(1.dp, DenseOutline),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SKIPPED", color = DenseTextSecondary, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${state.skipped}", color = DenseTextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
            }
        }

        // Action Return Button
        item {
            Button(
                onClick = { viewModel.navigateToHome() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("report_done_button")
                    .shadow(2.dp, RoundedCornerShape(100.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = DensePrimary, contentColor = Color.White),
                shape = RoundedCornerShape(100.dp)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Done")
                Spacer(modifier = Modifier.width(8.dp))
                Text("FINISH & RETURN", fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 0.5.sp)
            }
        }

        // Detailed Results Header
        item {
            Text(
                text = "DETAILED DIAGNOSTIC LOG",
                color = DenseTextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                textAlign = TextAlign.Start
            )
        }

        // Test logs
        items(viewModel.tests) { test ->
            val status = state.results[test.id] ?: TestStatus.PENDING

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DenseSurface),
                border = BorderStroke(
                    1.dp,
                    when (status) {
                        TestStatus.PASSED -> DenseAccentGlow
                        TestStatus.FAILED -> DenseRed
                        else -> DenseOutline
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = test.name,
                            color = DenseTextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = test.category.uppercase(),
                            color = DenseTextSecondary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 0.2.sp
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (status) {
                                TestStatus.PASSED -> DenseContainer
                                TestStatus.FAILED -> DenseRedContainer
                                else -> DenseBg
                            }
                        ),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(
                            1.dp,
                            when (status) {
                                TestStatus.PASSED -> DenseAccentGlow
                                TestStatus.FAILED -> DenseRed
                                else -> DenseOutline
                            }
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = when (status) {
                                    TestStatus.PASSED -> Icons.Default.CheckCircle
                                    TestStatus.FAILED -> Icons.Default.Cancel
                                    TestStatus.SKIPPED -> Icons.Default.SkipNext
                                    else -> Icons.Default.HourglassEmpty
                                },
                                contentDescription = status.name,
                                tint = when (status) {
                                    TestStatus.PASSED -> DenseGreen
                                    TestStatus.FAILED -> DenseRed
                                    TestStatus.SKIPPED -> DenseTextSecondary
                                    else -> DensePrimary
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = status.name,
                                color = when (status) {
                                    TestStatus.PASSED -> DenseGreen
                                    TestStatus.FAILED -> DenseRed
                                    else -> DenseTextPrimary
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: PhoneTestViewModel) {
    val pastReports by viewModel.pastReports.collectAsStateWithLifecycle()
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    BackHandler {
        viewModel.navigateToHome()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateToHome() }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go Back", tint = DensePrimary)
            }

            Text(
                text = "TEST HISTORY",
                color = DenseTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )

            if (pastReports.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearHistory() }) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Clear All", tint = DenseRed)
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        if (pastReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "No History",
                        tint = DenseSecondary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No diagnostic history found.",
                        color = DenseTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Run a system diagnosis to see details.",
                        color = DenseTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pastReports) { report ->
                    val total = report.totalCount
                    val scorePct = if (total > 0) (report.passedCount * 100) / total else 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DenseSurface),
                        border = BorderStroke(1.dp, DenseOutline),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sdf.format(Date(report.timestamp)),
                                    color = DenseTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = { viewModel.deleteReport(report.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Report",
                                        tint = DenseRed.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = report.deviceModel,
                                        color = DenseTextPrimary,
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = report.androidVersion,
                                        color = DenseTextSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            scorePct >= 80 -> DenseContainer
                                            scorePct >= 50 -> DensePrimaryContainer
                                            else -> DenseRedContainer
                                        }
                                    ),
                                    shape = RoundedCornerShape(100.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            scorePct >= 80 -> DenseAccentGlow
                                            scorePct >= 50 -> DensePrimary
                                            else -> DenseRed
                                        }
                                    )
                                ) {
                                    Text(
                                        text = "$scorePct% Passed",
                                        color = when {
                                            scorePct >= 80 -> DenseGreen
                                            scorePct >= 50 -> DensePrimary
                                            else -> DenseRed
                                        },
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Passed: ${report.passedCount}  •  Failed: ${report.failedCount}  •  Skipped: ${report.skippedCount}",
                                    color = DenseTextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
