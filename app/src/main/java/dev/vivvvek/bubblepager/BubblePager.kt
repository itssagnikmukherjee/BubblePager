/*
 * MIT License
 *
 * Copyright (c) 2022 Vivek Singh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.vivvvek.bubblepager

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerDefaults
import com.google.accompanist.pager.PagerScope
import com.google.accompanist.pager.PagerState
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import kotlin.math.absoluteValue

@OptIn(ExperimentalPagerApi::class)
@Composable
fun BubblePager(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
    bubbleMinRadius: Dp = 48.dp,
    bubbleMaxRadius: Dp = 12000.dp,
    bubbleBottomPadding: Dp = 140.dp,
    bubbleColors: List<Color>,
    vector: ImageVector = ImageVector.vectorResource(id = R.drawable.ic_chevron_right),
    content: @Composable PagerScope.(Int) -> Unit
) {
    val icon = rememberVectorPainter(vector)
    val arrowBubbleRadius by animateDpAsState(
        targetValue = if (pagerState.isScrollAboutToStop) bubbleMinRadius else 0.dp,
        animationSpec = tween(350)
    )
    Box(modifier = modifier) {
        HorizontalPager(
            count = pageCount,
            state = pagerState,
            flingBehavior = bubblePagerFlingBehavior(pagerState),
            modifier = Modifier.drawBehind {
                drawRect(color = bubbleColors[pagerState.currentPage], size = size)
                val (radius, centerX) = calculateBubbleDimensions(
                    swipeProgress = pagerState.currentPageOffset,
                    easing = CubicBezierEasing(1f, 0f, .92f, .62f),
                    minRadius = bubbleMinRadius,
                    maxRadius = bubbleMaxRadius
                )
                drawBubble(
                    radius = radius,
                    centerX = centerX,
                    bottomPadding = bubbleBottomPadding,
                    color = pagerState.getBubbleColor(bubbleColors)
                )
                drawBubbleWithIcon(
                    radius = arrowBubbleRadius,
                    bottomPadding = bubbleBottomPadding,
                    color = pagerState.getNextBubbleColor(bubbleColors),
                    icon = icon,
                )
            }
        ) { page ->
            content(page)
        }
    }
}

fun DrawScope.drawBubbleWithIcon(
    radius: Dp,
    bottomPadding: Dp,
    color: Color,
    icon: VectorPainter
) {
    translate(size.width / 2) {
        drawCircle(
            radius = radius.toPx(),
            color = color,
            center = Offset(0.dp.toPx(), size.height - bottomPadding.toPx())
        )
        with(icon) {
            intrinsicSize.let { iconSize ->
                translate(
                    top = size.height - bottomPadding.toPx() - iconSize.height / 2,
                    left = -(iconSize.width / 2) + 8 // adding a magic number to optically center the icon
                ) {
                    draw(size = iconSize, colorFilter = ColorFilter.tint(Color.Blue))
                }
            }
        }
    }
}

fun DrawScope.drawBubble(
    radius: Dp,
    centerX: Dp,
    bottomPadding: Dp,
    color: Color
) {
    translate(size.width / 2) {
        drawCircle(
            color = color,
            radius = radius.toPx(),
            center = Offset(centerX.toPx(), size.height - bottomPadding.toPx())
        )
    }
}

fun calculateBubbleDimensions(
    swipeProgress: Float,
    easing: Easing,
    minRadius: Dp,
    maxRadius: Dp
): Pair<Dp, Dp> {
    // swipe value ranges between 0 to 1.0 for half of the swipe
    // and 1.0 to 0 for the other half of the swipe
    val swipeValue = lerp(0f, 2f, swipeProgress.absoluteValue)
    val radius = lerp(
        minRadius,
        maxRadius,
        easing.transform(swipeValue)
    )
    var centerX = lerp(
        0.dp,
        maxRadius,
        easing.transform(swipeValue)
    )
    if (swipeProgress < 0) {
        centerX = -centerX
    }
    return Pair(radius, centerX)
}

@OptIn(ExperimentalPagerApi::class, ExperimentalSnapperApi::class)
@Composable
fun bubblePagerFlingBehavior(pagerState: PagerState) =
    PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(dampingRatio = 1.9f, stiffness = 600f),
    )

@OptIn(ExperimentalPagerApi::class)
fun PagerState.getBubbleColor(bubbleColors: List<Color>): Color {
    var index = nextSwipeablePageIndex
    if (currentPageOffset < 0) {
        index = currentPage - 1
    }
    return bubbleColors[index]
}

@OptIn(ExperimentalPagerApi::class)
fun PagerState.getNextBubbleColor(bubbleColors: List<Color>): Color {
    return bubbleColors[nextSwipeablePageIndex]
}

@OptIn(ExperimentalPagerApi::class)
val PagerState.isScrollAboutToStop: Boolean
    get() = derivedStateOf {
        currentPageOffset.absoluteValue < 0.1
    }.value

/*
* Returns the next swipeable page index. If the last page is reached, then the index of the
*  previous page is returned.
* */
@OptIn(ExperimentalPagerApi::class)
val PagerState.nextSwipeablePageIndex: Int
    get() = if ((currentPage + 1) == pageCount) currentPage - 1 else currentPage + 1

fun lerp(start: Float, end: Float, value: Float): Float {
    return start + (end - start) * value
}
