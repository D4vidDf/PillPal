package com.d4viddf.medicationreminder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.d4viddf.medicationreminder.data.MedicationType
import com.d4viddf.medicationreminder.viewmodel.MedicationTypeViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MedicationTypeSelector(
    selectedTypeId: Int,
    onTypeSelected: (Int) -> Unit,
    viewModel: MedicationTypeViewModel = hiltViewModel()
) {
    val medicationTypes by viewModel.medicationTypes.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    if (medicationTypes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading medication types...",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
    } else {
        val itemWidth = 200.dp // Aumentado el ancho del elemento individual
        val itemHeight = 230.dp // Aumentado la altura del selector

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight) // Ajustar la altura del contenedor
                .padding(horizontal = 0.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = Color(0xFF264443),
                        shape = RoundedCornerShape(30.dp)
                    )
            )

            val state = rememberCircularRowState()
            CircularList(
                state = state,
                itemWidthDp = itemWidth,
                visibleItems = 3,
                modifier = Modifier.fillMaxWidth(),
                onItemCentered = { index ->
                    val type = medicationTypes[index]
                    onTypeSelected(type.id)
                }
            ) {
                medicationTypes.forEachIndexed { index, type ->
                    MedicationTypeItem(
                        type = type,
                        isSelected = type.id == selectedTypeId,
                        onClick = {
                            coroutineScope.launch {
                                state.snapToIndex(index)
                                onTypeSelected(type.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MedicationTypeItem(
    type: MedicationType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 0.dp)
            .clickable(onClick = onClick)
            .size(180.dp), // Aumentada la altura del elemento
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val backgroundColor = if (isSelected) Color(0xFFF0BF70) else Color(0xFFB4CFC6)

        Box(
            modifier = Modifier
                .size(if (isSelected) 110.dp else 100.dp) // Aumentado el tamaño del contenedor
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(50.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = type.imageUrl),
                contentDescription = type.name,
                modifier = Modifier.size(70.dp) // Aumentado el tamaño de la imagen
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = type.name,
            fontSize = if (isSelected) 22.sp else 18.sp, // Aumentado el tamaño de la fuente
            color = if (isSelected) Color.White else Color.Gray
        )
    }
}

@Composable
fun CircularList(
    state: CircularRowState,
    itemWidthDp: Dp,
    visibleItems: Int,
    modifier: Modifier = Modifier,
    onItemCentered: (Int) -> Unit,
    content: @Composable () -> Unit,
) {
    val itemWidth = with(LocalDensity.current) { itemWidthDp.toPx() }

    Layout(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                coroutineScope {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val centerOffset = -state.horizontalOffset + (itemWidth / 2)
                            val centeredIndex = (centerOffset / itemWidth).roundToInt().coerceIn(0, state.config.numItems - 1)
                            launch {
                                state.snapToIndex(centeredIndex)
                                onItemCentered(centeredIndex)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            launch {
                                state.snapTo(state.horizontalOffset + dragAmount)
                            }
                        }
                    )
                }
            },
        content = content
    ) { measurables, constraints ->
        val itemConstraints =
            Constraints.fixed(width = itemWidth.roundToInt(), height = constraints.maxHeight)
        val placeables = measurables.map { measurable -> measurable.measure(itemConstraints) }
        val contentWidth = constraints.maxWidth.toFloat()

        state.setup(
            CircularRowConfig(
                contentWidth = contentWidth,
                numItems = placeables.size,
                visibleItems = visibleItems,
                itemWidth = itemWidth.roundToInt()
            )
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            for (i in state.firstVisibleItem..state.lastVisibleItem) {
                placeables[i % placeables.size].place(
                    x = state.offsetFor(i).x,
                    y = state.offsetFor(i).y
                )
            }
        }
    }
}

@Composable
fun rememberCircularRowState(): CircularRowState {
    val state = rememberSaveable(saver = CircularRowStateImpl.Saver) {
        CircularRowStateImpl()
    }
    return state
}

// No se realizaron cambios en CircularRowState, CircularRowConfig y CircularRowStateImpl


interface CircularRowState {
    val horizontalOffset: Float
    val firstVisibleItem: Int
    val lastVisibleItem: Int
    val config: CircularRowConfig

    suspend fun snapTo(value: Float)
    suspend fun snapToIndex(index: Int)
    fun offsetFor(index: Int): IntOffset
    fun setup(config: CircularRowConfig)
}

data class CircularRowConfig(
    val contentWidth: Float = 0f,
    val numItems: Int = 0,
    val visibleItems: Int = 0,
    val itemWidth: Int = 0,
)

class CircularRowStateImpl(
    currentOffset: Float = 0f,
) : CircularRowState {
    private val animatable = Animatable(currentOffset)
    private var itemWidth = 0f
    override lateinit var config: CircularRowConfig
    private var initialOffset = 0f

    private val minOffset: Float
        get() = -(config.numItems - 1) * itemWidth

    override val horizontalOffset: Float
        get() = animatable.value

    override val firstVisibleItem: Int
        get() = ((-horizontalOffset - initialOffset) / itemWidth).toInt().coerceAtLeast(0)

    override val lastVisibleItem: Int
        get() = (((-horizontalOffset - initialOffset) / itemWidth).toInt() + config.visibleItems)
            .coerceAtMost(config.numItems - 1)

    override suspend fun snapTo(value: Float) {
        animatable.snapTo(value.coerceIn(minOffset, 0f))
    }

    override suspend fun snapToIndex(index: Int) {
        val targetOffset = -(index * itemWidth)
        snapTo(targetOffset)
    }

    override fun setup(config: CircularRowConfig) {
        this.config = config
        itemWidth = config.itemWidth.toFloat()
        initialOffset = (config.contentWidth - config.itemWidth) / 2f
    }

    override fun offsetFor(index: Int): IntOffset {
        val x = (horizontalOffset + initialOffset + (index * itemWidth)).roundToInt()
        return IntOffset(x, 0)
    }

    companion object {
        val Saver = Saver<CircularRowStateImpl, List<Any>>(
            save = { listOf(it.horizontalOffset) },
            restore = {
                CircularRowStateImpl(it[0] as Float)
            }
        )
    }
}
