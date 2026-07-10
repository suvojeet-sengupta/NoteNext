@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.drawing

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress

@Composable
fun DrawingScreen(
    windowSizeClass: WindowSizeClass,
    onSave: (Uri) -> Unit,
    onDismiss: () -> Unit,
    viewModel: DrawingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var currentPath by remember { mutableStateOf<Path?>(null) }
    val context = LocalContext.current
    
    val isExpanded = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    val colors = listOf(
        Color.Black, Color(0xFF424242), Color(0xFF757575), Color.White,
        Color(0xFFD32F2F), Color(0xFFC2185B), Color(0xFF7B1FA2), Color(0xFF512DA8),
        Color(0xFF303F9F), Color(0xFF1976D2), Color(0xFF0288D1), Color(0xFF0097A7),
        Color(0xFF00796B), Color(0xFF388E3C), Color(0xFF689F38), Color(0xFFFBC02D),
        Color(0xFFFFA000), Color(0xFFF57C00), Color(0xFFE64A19), Color(0xFF5D4037)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.drawing),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.drawing_close_cd))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(DrawingEvent.Undo) },
                        enabled = state.paths.isNotEmpty(),
                        modifier = Modifier.springPress()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(id = R.string.drawing_undo_cd))
                    }
                    IconButton(
                        onClick = { viewModel.onEvent(DrawingEvent.Redo) },
                        enabled = state.undonePaths.isNotEmpty(),
                        modifier = Modifier.springPress()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = stringResource(id = R.string.drawing_redo_cd))
                    }
                    IconButton(
                        onClick = { viewModel.onEvent(DrawingEvent.ClearAll) },
                        enabled = state.paths.isNotEmpty(),
                        modifier = Modifier.springPress()
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(id = R.string.drawing_clear_all_cd))
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    if (state.isSaving) {
                        LoadingIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp)
                        )
                    } else {
                        Button(
                            onClick = {
                                viewModel.onEvent(DrawingEvent.SaveDrawing(context) { uri ->
                                    if (uri != null) onSave(uri)
                                })
                            },
                            enabled = state.paths.isNotEmpty(),
                            modifier = Modifier.padding(end = 8.dp).springPress(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(id = R.string.save))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (!isExpanded) {
                DrawingBottomBar(state, viewModel, colors)
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            if (isExpanded) {
                DrawingSideBar(state, viewModel, colors)
            }

            // Drawing Area (The White Sheet)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(if (isExpanded) 24.dp else 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = 6.dp,
                    tonalElevation = 2.dp
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .pointerInput(state.isEraserMode, state.currentColor, state.currentStrokeWidth) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                    },
                                    onDrag = { change, _ ->
                                        currentPath?.let { existing ->
                                            existing.lineTo(change.position.x, change.position.y)
                                            currentPath = Path().apply { addPath(existing) }
                                        }
                                    },
                                    onDragEnd = {
                                        currentPath?.let { viewModel.onEvent(DrawingEvent.PathAdded(it)) }
                                        currentPath = null
                                    },
                                    onDragCancel = { currentPath = null }
                                )
                            }
                    ) {
                        state.paths.forEach { drawingPath ->
                            drawPath(
                                path = drawingPath.path,
                                color = drawingPath.color,
                                style = Stroke(
                                    width = drawingPath.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                ),
                                blendMode = if (drawingPath.isEraser) BlendMode.Clear else BlendMode.SrcOver
                            )
                        }
                        
                        currentPath?.let {
                            drawPath(
                                path = it,
                                color = if (state.isEraserMode) Color.Transparent else state.currentColor,
                                style = Stroke(
                                    width = state.currentStrokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                ),
                                blendMode = if (state.isEraserMode) BlendMode.Clear else BlendMode.SrcOver
                            )
                        }
                    }
                    
                    if (state.paths.isEmpty() && currentPath == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(id = R.string.drawing_placeholder),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.LightGray.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawingBottomBar(
    state: DrawingState,
    viewModel: DrawingViewModel,
    colors: List<Color>
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 12.dp,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Settings Row (Slider)
            AnimatedVisibility(
                visible = state.showBrushSettings,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LineWeight, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Slider(
                            value = state.currentStrokeWidth,
                            onValueChange = { viewModel.onEvent(DrawingEvent.ChangeStrokeWidth(it)) },
                            valueRange = 5f..100f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(id = R.string.drawing_stroke_size_label, state.currentStrokeWidth.toInt()),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.width(45.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tool Selection (Segmented Button)
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = !state.isEraserMode,
                        onClick = { viewModel.onEvent(DrawingEvent.ToggleEraserMode(false)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Default.Brush, contentDescription = null) }
                    ) {
                        Text(stringResource(id = R.string.drawing_tool_brush), style = MaterialTheme.typography.labelSmall)
                    }
                    SegmentedButton(
                        selected = state.isEraserMode,
                        onClick = { viewModel.onEvent(DrawingEvent.ToggleEraserMode(true)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) }
                    ) {
                        Text(stringResource(id = R.string.drawing_tool_eraser), style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Color Selection
                Box(modifier = Modifier.weight(1f)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(
                            items = colors,
                            key = { it.toArgb() }
                        ) { color ->
                            ColorItem(
                                color = color,
                                isSelected = state.currentColor == color && !state.isEraserMode,
                                onClick = { viewModel.onEvent(DrawingEvent.ChangeColor(color)) }
                            )
                        }
                    }
                }

                // Settings Toggle
                FilledTonalIconButton(
                    onClick = { viewModel.onEvent(DrawingEvent.ToggleBrushSettings) },
                    modifier = Modifier.size(48.dp).springPress()
                ) {
                    Icon(
                        if (state.showBrushSettings) Icons.Default.KeyboardArrowDown else Icons.Default.Settings,
                        contentDescription = stringResource(id = R.string.drawing_settings_cd)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawingSideBar(
    state: DrawingState,
    viewModel: DrawingViewModel,
    colors: List<Color>
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier
            .width(100.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 12.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Brush / Eraser
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconToggleButton(
                    checked = !state.isEraserMode,
                    onCheckedChange = { viewModel.onEvent(DrawingEvent.ToggleEraserMode(false)) },
                    modifier = Modifier.size(56.dp).springPress()
                ) {
                    Icon(Icons.Default.Brush, contentDescription = stringResource(id = R.string.drawing_tool_brush))
                }
                Text(stringResource(id = R.string.drawing_tool_brush), style = MaterialTheme.typography.labelSmall)
                
                Spacer(Modifier.height(8.dp))
                
                FilledTonalIconToggleButton(
                    checked = state.isEraserMode,
                    onCheckedChange = { viewModel.onEvent(DrawingEvent.ToggleEraserMode(true)) },
                    modifier = Modifier.size(56.dp).springPress()
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = stringResource(id = R.string.drawing_tool_eraser))
                }
                Text(stringResource(id = R.string.drawing_tool_eraser), style = MaterialTheme.typography.labelSmall)
            }

            HorizontalDivider()

            // Settings Toggle & Width
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { viewModel.onEvent(DrawingEvent.ToggleBrushSettings) }) {
                    Icon(Icons.Default.LineWeight, contentDescription = stringResource(id = R.string.drawing_stroke_width_cd))
                }
                if (state.showBrushSettings) {
                    Slider(
                        value = state.currentStrokeWidth,
                        onValueChange = { viewModel.onEvent(DrawingEvent.ChangeStrokeWidth(it)) },
                        valueRange = 5f..100f,
                        modifier = Modifier
                            .graphicsLayer { rotationZ = 270f }
                            .height(150.dp)
                            .width(32.dp)
                    )
                }
            }

            HorizontalDivider()

            // Colors
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = colors,
                        key = { it.toArgb() }
                    ) { color ->
                        ColorItem(
                            color = color,
                            isSelected = state.currentColor == color && !state.isEraserMode,
                            onClick = { viewModel.onEvent(DrawingEvent.ChangeColor(color)) },
                            size = 28.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 34.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    Color.LightGray.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable { onClick() }
            .springPress()
    )
}
