package com.example.myhw1

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var main_GRID_lanes: GridLayout
    private lateinit var main_IMG_heart1: ImageView
    private lateinit var main_IMG_heart2: ImageView
    private lateinit var main_IMG_heart3: ImageView
    private lateinit var main_FAB_left: FloatingActionButton
    private lateinit var main_FAB_right: FloatingActionButton

    private val handler = Handler(Looper.getMainLooper())

    private val GAME_ROWS = 12
    private val GAME_COLS = 3
    private val CAR_ROW = GAME_ROWS - 1

    private val SPAWN_INTERVAL = 380L
    private val SPAWN_CHANCE_PERCENT = 35

    private val OBSTACLE_SIZE_DP = 52
    private val CAR_SIZE_DP = 64
    private val CELL_HEIGHT_FALLBACK_DP = 64

    private lateinit var cells: Array<Array<ImageView?>>

    private data class Obstacle(var row: Int, val col: Int)
    private val obstacles = ArrayList<Obstacle>()

    private var carLane = 1
    private var lives = 3
    private var gridCellHeightPx = 0

    private val gameRunnable = object : Runnable {
        override fun run() {
            moveObstaclesDown()
            spawnObstacle()
            redrawBoard()
            checkCollision()
            handler.postDelayed(this, SPAWN_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_GRID_lanes = findViewById(R.id.main_GRID_lanes)
        main_IMG_heart1 = findViewById(R.id.main_IMG_heart1)
        main_IMG_heart2 = findViewById(R.id.main_IMG_heart2)
        main_IMG_heart3 = findViewById(R.id.main_IMG_heart3)
        main_FAB_left = findViewById(R.id.main_FAB_left)
        main_FAB_right = findViewById(R.id.main_FAB_right)

        main_GRID_lanes.rowCount = GAME_ROWS
        main_GRID_lanes.columnCount = GAME_COLS

        main_FAB_left.setOnClickListener { moveCar(-1) }
        main_FAB_right.setOnClickListener { moveCar(1) }

        main_GRID_lanes.post {
            buildGrid()
            updateHeartsDisplay()
            redrawBoard()
            startGame()
        }
    }

    private fun startGame() {
        handler.removeCallbacks(gameRunnable)
        handler.postDelayed(gameRunnable, 500)
    }

    private fun buildGrid() {
        main_GRID_lanes.removeAllViews()

        val heightPx = if (main_GRID_lanes.height > 0) main_GRID_lanes.height else dpToPx(CELL_HEIGHT_FALLBACK_DP * GAME_ROWS)
        gridCellHeightPx = heightPx / GAME_ROWS

        cells = Array(GAME_ROWS) { arrayOfNulls<ImageView>(GAME_COLS) }

        for (r in 0 until GAME_ROWS) {
            for (c in 0 until GAME_COLS) {
                val cell = ImageView(this).apply {
                    visibility = View.INVISIBLE
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }

                val lp = GridLayout.LayoutParams().apply {
                    width = 0
                    height = gridCellHeightPx
                    rowSpec = GridLayout.spec(r, 1)
                    columnSpec = GridLayout.spec(c, 1, 1f)
                    setGravity(Gravity.CENTER)
                }

                cell.layoutParams = lp
                main_GRID_lanes.addView(cell)
                cells[r][c] = cell
            }
        }
    }

    private fun moveCar(direction: Int) {
        val nextLane = carLane + direction
        if (nextLane in 0 until GAME_COLS) {
            carLane = nextLane
            redrawBoard()
        }
    }

    private fun spawnObstacle() {
        if (Random.nextInt(100) >= SPAWN_CHANCE_PERCENT) return
        val lane = Random.nextInt(GAME_COLS)
        if (obstacles.none { it.col == lane && it.row == 0 }) {
            obstacles.add(Obstacle(0, lane))
        }
    }

    private fun moveObstaclesDown() {
        for (o in obstacles) o.row += 1
        obstacles.removeAll { it.row >= GAME_ROWS }
    }

    private fun redrawBoard() {
        for (r in 0 until GAME_ROWS) {
            for (c in 0 until GAME_COLS) {
                cells[r][c]?.visibility = View.INVISIBLE
            }
        }

        for (o in obstacles) {
            cells[o.row][o.col]?.apply {
                setImageResource(R.drawable.ic_obstacle)
                val lp = layoutParams
                lp.width = dpToPx(OBSTACLE_SIZE_DP)
                lp.height = dpToPx(OBSTACLE_SIZE_DP)
                layoutParams = lp
                visibility = View.VISIBLE
            }
        }

        cells[CAR_ROW][carLane]?.apply {
            setImageResource(R.drawable.ic_car)
            val lp = layoutParams
            lp.width = dpToPx(CAR_SIZE_DP)
            lp.height = dpToPx(CAR_SIZE_DP)
            layoutParams = lp
            visibility = View.VISIBLE
        }
    }

    private fun checkCollision() {
        val hit = obstacles.any { it.row == CAR_ROW && it.col == carLane }
        if (!hit) return

        obstacles.removeAll { it.row == CAR_ROW && it.col == carLane }

        lives--
        vibrate()
        Toast.makeText(this, "Crash!", Toast.LENGTH_SHORT).show()
        updateHeartsDisplay()

        if (lives <= 0) {
            handler.removeCallbacks(gameRunnable)
            Toast.makeText(this, "Game Over!", Toast.LENGTH_LONG).show()
            handler.postDelayed({ restartGame() }, 1500)
        }
    }

    private fun restartGame() {
        lives = 3
        carLane = 1
        obstacles.clear()
        updateHeartsDisplay()
        redrawBoard()
        startGame()
    }

    private fun updateHeartsDisplay() {
        main_IMG_heart1.visibility = if (lives >= 1) View.VISIBLE else View.INVISIBLE
        main_IMG_heart2.visibility = if (lives >= 2) View.VISIBLE else View.INVISIBLE
        main_IMG_heart3.visibility = if (lives >= 3) View.VISIBLE else View.INVISIBLE
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(250)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(gameRunnable)
    }
}
