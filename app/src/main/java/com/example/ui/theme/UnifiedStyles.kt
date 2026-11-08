package com.example.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Step 1 of MELODY1_AI_MASTER_SPEC:
 * Central Design System Reference extracted directly from [com.example.ui.components.GlassyBottomNavBar].
 * Unifies all buttons, cards, inputs, headers, icon orbs, and dialogs across the entire application.
 */
object UnifiedStyles {

    val CardShape: Shape = RoundedCornerShape(22.dp)
    val ButtonShape: Shape = RoundedCornerShape(18.dp)
    val PillShape: Shape = RoundedCornerShape(50)
    val InputShape: Shape = RoundedCornerShape(16.dp)
    val DialogShape: Shape = RoundedCornerShape(26.dp)

    /**
     * Frosted smoky dark glass fill extracted from GlassyBottomNavBar capsule body.
     */
    val glassCardBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            GlassCapsuleTop,
            GlassCapsuleMid,
            GlassCapsuleBottom
        )
    )

    /**
     * Elevated teal-emerald glassy orb fill extracted from the selected tab orb in GlassyBottomNavBar.
     */
    val glassActiveOrbBrush: Brush = Brush.radialGradient(
        colors = listOf(
            GlassOrbCore,
            GlassOrbMid,
            GlassOrbDeep
        )
    )

    /**
     * Primary action button brush combining the glassy teal-emerald depth of the bottom bar orb.
     */
    val glassPrimaryButtonBrush: Brush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF285A58),
            Color(0xFF3B6E6C),
            Color(0xFF285A58)
        )
    )

    /**
     * Gold-accented glassy button brush for highlighted actions.
     */
    val glassGoldButtonBrush: Brush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF4A3B1C),
            Color(0xFF6B5322),
            Color(0xFF4A3B1C)
        )
    )

    /**
     * Crimson red glassy brush for the 3-Day Free Trial button on LoginScreen.
     */
    val glassRedTrialButtonBrush: Brush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFB71C1C),
            Color(0xFFE53935),
            Color(0xFFC62828)
        )
    )

    /**
     * Specular white-teal glass border rim extracted from GlassyBottomNavBar contour stroke.
     */
    val glassRimBorderBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xB8FFFFFF),
            Color(0x66B2DFDB),
            Color(0x38FFFFFF)
        )
    )

    /**
     * Brighter active rim border for selected pills, active cards, and primary buttons.
     */
    val glassActiveBorderBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xEEFFFFFF),
            Color(0xAA80CBC4),
            Color(0x66FFB800)
        )
    )

    /**
     * Subtle input field border brush.
     */
    val glassSubtleBorderBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0x66FFFFFF),
            Color(0x3880CBC4),
            Color(0x24FFFFFF)
        )
    )

    fun screenBackgroundBrush(): Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF131920),
            OledBackground,
            Color(0xFF07090C)
        )
    )

    fun glowingOrbBrush(): Brush = glassActiveOrbBrush

    fun glassCardBrush(isElevated: Boolean = false): Brush =
        if (isElevated) glassPrimaryButtonBrush else glassCardBrush

    fun glassBorderBrush(isHighlighted: Boolean = false): Brush =
        if (isHighlighted) glassActiveBorderBrush else glassRimBorderBrush
}

/**
 * Applies the unified frosted-glass surface, ambient shadow, and specular rim border
 * matching the Bottom Navigation Bar capsule aesthetic.
 */
fun Modifier.glassyCard(
    cornerRadius: Dp = 22.dp,
    isHighlighted: Boolean = false,
    elevation: Dp = 6.dp
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = if (isHighlighted) GlassTealGlow else Color.Black,
            spotColor = if (isHighlighted) GlassTealGlow else Color.Black
        )
        .clip(shape)
        .background(
            brush = if (isHighlighted) UnifiedStyles.glassPrimaryButtonBrush else UnifiedStyles.glassCardBrush,
            shape = shape
        )
        .border(
            width = if (isHighlighted) 1.5.dp else 1.1.dp,
            brush = if (isHighlighted) UnifiedStyles.glassActiveBorderBrush else UnifiedStyles.glassRimBorderBrush,
            shape = shape
        )
}

/**
 * Applies the circular glowing orb style from the active tab of [GlassyBottomNavBar].
 */
fun Modifier.glassyIconOrb(
    size: Dp = 44.dp,
    isActive: Boolean = true,
    isSelected: Boolean = isActive
): Modifier = composed {
    val active = isSelected
    this
        .size(size)
        .shadow(
            elevation = if (active) 8.dp else 3.dp,
            shape = CircleShape,
            ambientColor = if (active) GlassTealGlow else Color.Black,
            spotColor = if (active) GlassTealGlow else Color.Black
        )
        .clip(CircleShape)
        .background(
            brush = if (active) UnifiedStyles.glassActiveOrbBrush else UnifiedStyles.glassCardBrush,
            shape = CircleShape
        )
        .border(
            width = 1.3.dp,
            brush = if (active) UnifiedStyles.glassActiveBorderBrush else UnifiedStyles.glassSubtleBorderBrush,
            shape = CircleShape
        )
}

/**
 * Unified Glassy Button with spring scale feedback and inner ringed icon orb (matching GlassyBottomNavBar).
 */
@Composable
fun GlassyActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    height: Dp? = null,
    testTag: String? = null,
    brush: Brush = UnifiedStyles.glassPrimaryButtonBrush,
    borderBrush: Brush = UnifiedStyles.glassActiveBorderBrush,
    contentColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glassyBtnScale"
    )

    val shape = UnifiedStyles.ButtonShape
    val baseModifier = modifier
        .let { if (height != null) it.height(height) else it }
        .let { if (!testTag.isNullOrBlank()) it.testTag(testTag) else it }
    Box(
        modifier = baseModifier
            .scale(scale)
            .shadow(
                elevation = if (enabled) 8.dp else 2.dp,
                shape = shape,
                ambientColor = GlassTealGlow,
                spotColor = GlassTealGlow
            )
            .clip(shape)
            .background(
                brush = if (enabled) brush else UnifiedStyles.glassCardBrush,
                shape = shape
            )
            .border(
                width = 1.4.dp,
                brush = if (enabled) borderBrush else UnifiedStyles.glassSubtleBorderBrush,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(
                            width = 1.4.dp,
                            color = contentColor.copy(alpha = if (enabled) 0.9f else 0.4f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = if (enabled) 1f else 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = text,
                    color = contentColor.copy(alpha = if (enabled) 1f else 0.5f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = contentColor.copy(alpha = if (enabled) 0.8f else 0.4f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Circular Glassy Icon Button with inner ring (matching the active orb in GlassyBottomNavBar).
 */
@Composable
fun GlassyIconOrbButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isActive: Boolean = false,
    isSelected: Boolean = isActive,
    testTag: String? = null,
    tint: Color = Color.White
) {
    val active = isSelected
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "orbBtnScale"
    )

    val baseMod = if (!testTag.isNullOrBlank()) {
        modifier.testTag(testTag)
    } else {
        modifier
    }

    Box(
        modifier = baseMod
            .scale(scale)
            .glassyIconOrb(size = size, isActive = active)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.64f)
                .border(
                    width = 1.2.dp,
                    color = if (active) Color.White.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.35f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size * 0.38f)
            )
        }
    }
}
