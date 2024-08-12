package com.example.kittensvsrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kittensvsrain.ui.theme.KittensVsRainTheme
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

@JsonClass(generateAdapter = true)
data class Position(
    val x: Float,
    val y: Float
)

@JsonClass(generateAdapter = true)
data class Level(
    val number: Int,
    val cats: List<Position>,
    val house: Position
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KittensVsRainTheme {
                var levels by remember { mutableStateOf<List<Level>>(emptyList()) }
                var currentLevel by remember { mutableStateOf(0) }
                var completedLevels by remember { mutableStateOf<Set<Int>>(emptySet()) }

                LaunchedEffect(Unit) {
                    levels = loadLevels()
                }

                if (currentLevel == 0) {
                    LevelSelectionScreen(levels, completedLevels) { selectedLevel ->
                        currentLevel = selectedLevel
                    }
                } else {
                    GameScreen(levels, currentLevel, onLevelChange = { newLevel ->
                        currentLevel = newLevel
                    }, onLevelComplete = { completedLevel ->
                        completedLevels = completedLevels + completedLevel
                        currentLevel = 0
                    })
                    DrawingCanvas()
                }
            }
        }
    }

    private suspend fun loadLevels(): List<Level> {
        return withContext(Dispatchers.IO) {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val jsonAdapter = moshi.adapter<List<Level>>(Types.newParameterizedType(List::class.java, Level::class.java))
            val inputStream: InputStream = assets.open("levels.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            jsonAdapter.fromJson(json) ?: emptyList()
        }
    }
}

@Composable
fun LevelSelectionScreen(levels: List<Level>, completedLevels: Set<Int>, onLevelSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Level",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        levels.forEach { level ->
            val isCompleted = completedLevels.contains(level.number)
            val backgroundColor = if (isCompleted) Color.Green else Color.Gray

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(backgroundColor, shape = CircleShape)
                    .clickable { onLevelSelect(level.number) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level.number.toString(),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DrawingCanvas() {
    var paths by remember { mutableStateOf(mutableListOf<Path>()) }
    var currentPath by remember { mutableStateOf(Path()) }
    var lastPoint by remember { mutableStateOf<Offset?>(null) }

    Canvas(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    currentPath.moveTo(offset.x, offset.y)
                    lastPoint = offset
                },
                onDrag = { change, _ ->
                    val newPoint = change.position
                    lastPoint?.let {
                        currentPath.lineTo(newPoint.x, newPoint.y)
                    }
                    lastPoint = newPoint
                },
                onDragEnd = {
                    paths.add(currentPath)
                    currentPath = Path()
                    lastPoint = null
                }
            )
        }
    ) {
        for (path in paths) {
            drawPath(
                path = path,
                color = Color.Black,
                style = Stroke(width = 4f)
            )
        }
        drawPath(
            path = currentPath,
            color = Color.Black,
            style = Stroke(width = 4f)
        )
    }
}


@Composable
fun GameScreen(levels: List<Level>, currentLevel: Int, onLevelChange: (Int) -> Unit, onLevelComplete: (Int) -> Unit) {
    val level = levels.firstOrNull { it.number == currentLevel }

    DrawingCanvas()

    if (level == null) {
        Text("Level not found")
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row {
                if (currentLevel > 1) {
                    Button(onClick = { onLevelChange(currentLevel - 1) }) {
                        Text("Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (currentLevel < levels.size) {
                    Button(onClick = { onLevelChange(currentLevel + 1) }) {
                        Text("Next")
                    }
                }
                Button(onClick = { onLevelChange(0) }) {
                    Text("To all")
                }
                Button(onClick = { onLevelComplete(currentLevel) }) {
                    Text("Complete")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Current Level: ${level.number}", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp)
        ) {
            level.cats.forEach { cat ->
                Image(
                    painter = painterResource(id = R.drawable.cat),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(cat.x.dp, cat.y.dp)
                        .size(50.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Image(
                painter = painterResource(id = R.drawable.house),
                contentDescription = null,
                modifier = Modifier
                    .offset(level.house.x.dp, level.house.y.dp)
                    .size(100.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LevelSelectionScreenPreview() {
    KittensVsRainTheme {
        val levels = listOf(
            Level(1, listOf(Position(50f, 100f), Position(150f, 200f)), Position(300f, 400f)),
            Level(2, listOf(Position(70f, 120f), Position(160f, 220f)), Position(320f, 420f)),
            Level(3, listOf(Position(90f, 140f), Position(170f, 240f)), Position(340f, 440f))
        )
        LevelSelectionScreen(levels, setOf(1, 3)) {}
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    KittensVsRainTheme {
        val levels = listOf(
            Level(1, listOf(Position(50f, 100f), Position(150f, 200f)), Position(300f, 400f)),
            Level(2, listOf(Position(70f, 120f), Position(160f, 220f)), Position(320f, 420f)),
            Level(3, listOf(Position(90f, 140f), Position(170f, 240f)), Position(340f, 440f))
        )
        GameScreen(levels, 1, onLevelChange = {}, onLevelComplete = {})
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDrawingCanvas() {
    DrawingCanvas()
}
