package com.sonnik.topower.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Значки, которых нет в базовом наборе Material (расширенный набор не подключаем ради размера приложения). */
object AppIcons {
    private fun icon(name: String, pathData: String, evenOdd: Boolean = false): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = SolidColor(Color.Black),
            pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
        ).build()

    val Dumbbell: ImageVector by lazy {
        icon(
            "Dumbbell",
            "M20.57,14.86L22,13.43 20.57,12 17,15.57 8.43,7 12,3.43 10.57,2 9.14,3.43 7.71,2 5.57,4.14 4.14,2.71 2.71,4.14l1.43,1.43L2,7.71l1.43,1.43L2,10.57 3.43,12 7,8.43 15.57,17 12,20.57 13.43,22l1.43,-1.43L16.29,22l2.14,-2.14 1.43,1.43 1.43,-1.43 -1.43,-1.43L22,16.29z",
        )
    }

    val Flame: ImageVector by lazy {
        icon(
            "Flame",
            "M13.5,0.67s0.74,2.65 0.74,4.8c0,2.06 -1.35,3.73 -3.41,3.73 -2.07,0 -3.63,-1.67 -3.63,-3.73l0.03,-0.36C5.21,7.51 4,10.62 4,14c0,4.42 3.58,8 8,8s8,-3.58 8,-8C20,8.61 17.41,3.8 13.5,0.67zM11.71,19c-1.78,0 -3.22,-1.4 -3.22,-3.14 0,-1.62 1.05,-2.76 2.81,-3.12 1.77,-0.36 3.6,-1.21 4.62,-2.58 0.39,1.29 0.59,2.65 0.59,4.04 0,2.65 -2.15,4.8 -4.8,4.8z",
            evenOdd = true,
        )
    }

    val Clock: ImageVector by lazy {
        icon(
            "Clock",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z",
            evenOdd = true,
        )
    }

    /** Гиря-разновес — для тоннажа. */
    val Weight: ImageVector by lazy {
        icon(
            "Weight",
            "M12,3a3,3 0 0 0 -2.83,4H6.5L4,21h16L17.5,7h-2.67A3,3 0 0 0 12,3zM12,5a1,1 0 0 1 0,2a1,1 0 0 1 0,-2z",
            evenOdd = true,
        )
    }

    /** Повторить тренировку (значок «replay» из набора Material). */
    val Repeat: ImageVector by lazy {
        icon(
            "Repeat",
            "M12,5V1L7,6l5,5V7c3.31,0 6,2.69 6,6s-2.69,6 -6,6 -6,-2.69 -6,-6H4c0,4.42 3.58,8 8,8s8,-3.58 8,-8 -3.58,-8 -8,-8z",
        )
    }

    /** Минус — кнопка − в диалоге подхода (в базовом наборе Material его нет). */
    val Minus: ImageVector by lazy {
        icon("Minus", "M19,13H5v-2h14V13z")
    }
}
