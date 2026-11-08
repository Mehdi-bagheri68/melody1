package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NavItem
import com.example.ui.Screen

/**
 * Glassy Navigation Bar (V5 - Row 3 Style):
 * Features a floating frosted-glass capsule with a dynamic concave S-curve cradle
 * that smoothly glides to whichever tab is currently selected, lifting the selected
 * tab into a glowing glassy circular orb nestled inside the top cutout.
 */
@Composable
fun GlassyBottomNavBar(
    navItems: List<NavItem>,
    currentScreen: Screen,
    onSelectScreen: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val itemCount = navItems.size.coerceAtLeast(1)

    val selectedIndex = remember(currentScreen, navItems) {
        val idx = navItems.indexOfFirst { it.screen::class == currentScreen::class }
        if (idx >= 0) idx else 0
    }

    // Visual horizontal fraction from left edge (0f..1f) accounting for LTR / RTL layout direction
    val targetVisualFraction = remember(selectedIndex, isRtl, itemCount) {
        val visualColumn = if (isRtl) (itemCount - 1 - selectedIndex) else selectedIndex
        (visualColumn + 0.5f) / itemCount.toFloat()
    }

    val animatedFraction by animateFloatAsState(
        targetValue = targetVisualFraction,
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cradleCenterFraction"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .height(86.dp)
    ) {
        // 1. Glassy Capsule Body + Dynamic Concave Cradle Cutout
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val totalW = size.width
            val totalH = size.height
            val barTop = 22.dp.toPx()
            val barBottom = totalH - 2.dp.toPx()
            val barHeight = (barBottom - barTop).coerceAtLeast(1f)
            val cornerRadius = barHeight / 2f

            val cx = (animatedFraction * totalW).coerceIn(
                cornerRadius + 16.dp.toPx(),
                totalW - cornerRadius - 16.dp.toPx()
            )
            val cradleHalfWidth = 42.dp.toPx()
            val cradleDepth = 27.dp.toPx()

            val cradleStart = (cx - cradleHalfWidth).coerceAtLeast(cornerRadius * 0.75f)
            val cradleEnd = (cx + cradleHalfWidth).coerceAtMost(totalW - cornerRadius * 0.75f)

            val capsulePath = Path().apply {
                // Start at top-left after left cap
                moveTo(cornerRadius, barTop)

                // Flat top segment up to the concave cradle start
                if (cradleStart > cornerRadius) {
                    lineTo(cradleStart, barTop)
                }

                // Smooth S-curve dipping down into the cradle center (cx, barTop + cradleDepth)
                val leftSpan = cx - cradleStart
                cubicTo(
                    x1 = cradleStart + leftSpan * 0.42f,
                    y1 = barTop,
                    x2 = cx - leftSpan * 0.46f,
                    y2 = barTop + cradleDepth,
                    x3 = cx,
                    y3 = barTop + cradleDepth
                )

                // Smooth S-curve rising back up from cradle center to cradleEnd
                val rightSpan = cradleEnd - cx
                cubicTo(
                    x1 = cx + rightSpan * 0.46f,
                    y1 = barTop + cradleDepth,
                    x2 = cradleEnd - rightSpan * 0.42f,
                    y2 = barTop,
                    x3 = cradleEnd,
                    y3 = barTop
                )

                // Flat top segment to top-right cap
                if (cradleEnd < totalW - cornerRadius) {
                    lineTo(totalW - cornerRadius, barTop)
                }

                // Right semicircular cap
                arcTo(
                    rect = Rect(
                        left = totalW - 2f * cornerRadius,
                        top = barTop,
                        right = totalW,
                        bottom = barBottom
                    ),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )

                // Flat bottom edge from right cap to left cap
                lineTo(cornerRadius, barBottom)

                // Left semicircular cap
                arcTo(
                    rect = Rect(
                        left = 0f,
                        top = barTop,
                        right = 2f * cornerRadius,
                        bottom = barBottom
                    ),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )

                close()
            }

            // Soft ambient drop shadow under the capsule
            drawPath(
                path = capsulePath,
                color = Color(0x88000000)
            )

            // Frosted smoky dark glass fill (matching Row 3 of reference image)
            drawPath(
                path = capsulePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xEB2B2D33),
                        Color(0xF21E2025),
                        Color(0xF7141519)
                    ),
                    startY = barTop,
                    endY = barBottom
                )
            )

            // Subtle warm bottom-center ambient sheen inside the glass capsule
            drawPath(
                path = capsulePath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x28D7B98E),
                        Color(0x124DB6AC),
                        Color.Transparent
                    ),
                    center = Offset(totalW * 0.5f, barBottom),
                    radius = totalW * 0.55f
                )
            )

            // Ambient teal/emerald glow around the active cradle notch
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x404DB6AC),
                        Color(0x1826A69A),
                        Color.Transparent
                    ),
                    center = Offset(cx, barTop + 4.dp.toPx()),
                    radius = 44.dp.toPx()
                ),
                radius = 44.dp.toPx(),
                center = Offset(cx, barTop + 4.dp.toPx())
            )

            // Crisp frosted glass border rim following the capsule & concave cradle contour
            drawPath(
                path = capsulePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xC8FFFFFF),
                        Color(0x66B2DFDB),
                        Color(0x45FFFFFF)
                    ),
                    startY = barTop,
                    endY = barBottom
                ),
                style = Stroke(
                    width = 1.4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Sleek horizontal home-indicator glass pill at the bottom center (as in Row 3)
            val pillWidth = 92.dp.toPx()
            val pillHeight = 3.2.dp.toPx()
            drawRoundRect(
                color = Color(0x70FFFFFF),
                topLeft = Offset(
                    x = (totalW - pillWidth) / 2f,
                    y = barBottom - 8.dp.toPx()
                ),
                size = Size(pillWidth, pillHeight),
                cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f)
            )
        }

        // 2. Interactive Tab Items Row (Automatically respects LTR / RTL)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            navItems.forEach { item ->
                val isSelected = currentScreen::class == item.screen::class
                val interactionSource = remember { MutableInteractionSource() }

                val verticalOffset by animateDpAsState(
                    targetValue = if (isSelected) (-23).dp else (-7).dp,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "tabVerticalOffset"
                )

                val orbScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.88f,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "orbScale"
                )

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color(0xE6F5F5F7),
                    animationSpec = tween(220),
                    label = "iconTint"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(86.dp)
                        .testTag(item.tag)
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                            contentDescription = item.title
                        }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelectScreen(item.screen) }
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.offset(y = verticalOffset)
                    ) {
                        if (isSelected) {
                            // Elevated Circular Glassy Orb nestled in the concave cradle (Row 3 style)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(orbScale)
                                    .shadow(
                                        elevation = 10.dp,
                                        shape = CircleShape,
                                        ambientColor = Color(0xFF26A69A),
                                        spotColor = Color(0xFF26A69A)
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF3B6E6C),
                                                Color(0xFF244B49),
                                                Color(0xFF183332)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xEEFFFFFF),
                                                Color(0x8880CBC4),
                                                Color(0x44FFFFFF)
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Inner circular ring + icon (matching Row 3's ringed icon aesthetic)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .border(
                                            width = 1.6.dp,
                                            color = Color.White.copy(alpha = 0.92f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.selectedIcon,
                                        contentDescription = item.title,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFFE0F2F1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            // Unselected clean icon inside the glassy capsule
                            Box(
                                modifier = Modifier.size(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.unselectedIcon,
                                    contentDescription = item.title,
                                    tint = iconTint,
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = Color(0xB3E0E0E5),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
