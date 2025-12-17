package com.example.myhw1

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myhw1.ui.theme.MyHW1Theme
import kotlinx.coroutines.delay
import kotlin.random.Random

const val NUM_LANES = 3
const val INITIAL_LIVES = 3

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyHW1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen()
                }
            }
        }
    }
}

@Composable
fun GameScreen() {
    val context = LocalContext.current
    var lives by remember { mutableIntStateOf(INITIAL_LIVES) }
    var carLane by remember { mutableIntStateOf(1) }
    var obstacles by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }
    var gameRunning by remember { mutableStateOf(true) }

    fun showCrashNotification() {
        Toast.makeText(context, "Crash!", Toast.LENGTH_SHORT).show()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }
    }

    LaunchedEffect(gameRunning) {
        if (!gameRunning) return@LaunchedEffect
        while (true) {
            delay(500)
            // Move obstacles
            obstacles = obstacles.map { it.first to it.second + 1 }.filter { it.second < 10 }

            // Add new obstacle
            if (Random.nextInt(5) == 0) {
                val lane = Random.nextInt(NUM_LANES)
                if (obstacles.none { it.first == lane && it.second == 0 }) {
                    obstacles = obstacles + (lane to 0)
                }
            }

            // Check for collision
            if (obstacles.any { it.first == carLane && it.second == 8 }) {
                lives--
                showCrashNotification()
                if (lives == 0) {
                    gameRunning = false
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "Lives: $lives", style = MaterialTheme.typography.headlineMedium)

        Box(modifier = Modifier.weight(1f)) {
            Road(carLane, obstacles)
        }

        if (!gameRunning) {
            Button(onClick = {
                lives = INITIAL_LIVES
                carLane = 1
                obstacles = emptyList()
                gameRunning = true
            }) {
                Text(text = "Restart")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { if (carLane > 0) carLane-- }) {
                Text("Left")
            }
            Button(onClick = { if (carLane < NUM_LANES - 1) carLane++ }) {
                Text("Right")
            }
        }
    }
}

@Composable
fun Road(carLane: Int, obstacles: List<Pair<Int, Int>>) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxHeight()) {
            for (lane in 0 until NUM_LANES) {
                Lane(
                    modifier = Modifier.weight(1f),
                    isCarInLane = carLane == lane,
                    obstaclesInLane = obstacles.filter { it.first == lane }.map { it.second }
                )
            }
        }
    }
}

@Composable
fun Lane(modifier: Modifier = Modifier, isCarInLane: Boolean, obstaclesInLane: List<Int>) {
    Box(modifier = modifier.fillMaxHeight()) {
        for (obstaclePos in obstaclesInLane) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (obstaclePos * 50).dp)
            ) {
                Image(
                    painter = painterResource(id = android.R.drawable.ic_delete),
                    contentDescription = "Obstacle",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (isCarInLane) {
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Image(
                    painter = painterResource(id = android.R.drawable.sym_def_app_icon),
                    contentDescription = "Car",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyHW1Theme {
        Text("Game Preview")
    }
}
