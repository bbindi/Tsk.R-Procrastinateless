package com.brixavier.tskr.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.brixavier.tskr.ui.theme.TskRMotion

/**
 * Refined 5-slide "What's New in Tsk.R V2" walkthrough.
 *
 * Slide 1: Welcome to Tsk.R V2
 * Slide 2: Tasks & Plans
 * Slide 3: Plans Got Serious (Plan Workspace)
 * Slide 4: Calendar 2.0
 * Slide 5: I Moved Onto Your Home Screen (Widget)
 */
@Composable
fun WhatsNewWalkthrough(
    isSettingsReplay: Boolean = false,
    onComplete: () -> Unit,
    onDismiss: () -> Unit = onComplete
) {
    var currentSlide by remember { mutableIntStateOf(0) }

    val slides = remember {
        listOf(
            WalkthroughSlide(
                icon = Icons.Default.AutoAwesome,
                title = "Welcome to Tsk.R V2",
                subtitle = "A few things changed while you weren't looking.",
                body = "I got tired of watching you procrastinate, so I upgraded myself. New tools, better Plans, and more ways for me to bother you productively."
            ),
            WalkthroughSlide(
                icon = Icons.Default.FolderSpecial,
                title = "Tasks & Plans",
                subtitle = "Tasks are what you need to do. Plans are what you're actually trying to accomplish.",
                body = "Tasks keep me on your back for daily commitments. Plans give your overarching goals somewhere to live so your big ideas stop dying in your head."
            ),
            WalkthroughSlide(
                icon = Icons.Default.Description,
                title = "Plans Got Serious",
                subtitle = "Steps. Notes. Tasks. One place.",
                body = "Plans now provide a structured workspace where you can break goals into Plan Steps, keep rich Notes, and attach Tsk.R tasks. Try not to abandon them."
            ),
            WalkthroughSlide(
                icon = Icons.Default.CalendarMonth,
                title = "Calendar 2.0",
                subtitle = "See the chaos before it sees you.",
                body = "Scan task density across Month grids, Week strips, and Day agendas with visual indicator dots. Tap any date to inspect its agenda before deadlines sneak up on you."
            ),
            WalkthroughSlide(
                icon = Icons.Default.Widgets,
                title = "I Moved Onto Your Home Screen",
                subtitle = "Apparently opening the app was too much work.",
                body = "Check off tasks directly from your home screen widget, view Plan progress, and receive accountability quotes without even opening the app."
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / Back Button Bar for Settings Replay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSettingsReplay) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings")
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Text(
                    text = "WHAT'S NEW IN V2",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.width(48.dp))
            }

            // Slide Content
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "WalkthroughSlideTransition",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { slideIdx ->
                val current = slides[slideIdx]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = current.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = current.subtitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = current.body,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }

            // Bottom Navigation & Progress Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // 5-Dot Progress Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slides.indices.forEach { idx ->
                        Box(
                            modifier = Modifier
                                .size(if (idx == currentSlide) 10.dp else 8.dp)
                                .background(
                                    color = if (idx == currentSlide) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentSlide > 0) {
                        OutlinedButton(
                            onClick = { currentSlide-- },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }

                    if (currentSlide < slides.lastIndex) {
                        Button(
                            onClick = { currentSlide++ },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        val btnInteractionSource = remember { MutableInteractionSource() }
                        val isBtnPressed by btnInteractionSource.collectIsPressedAsState()
                        val btnScale by animateFloatAsState(
                            targetValue = if (isBtnPressed) 0.97f else 1.0f,
                            animationSpec = tween(durationMillis = TskRMotion.DURATION_MICRO, easing = TskRMotion.EasingStandard),
                            label = "GetToWorkPressScaleAnimation"
                        )

                        Button(
                            onClick = onComplete,
                            interactionSource = btnInteractionSource,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.graphicsLayer {
                                scaleX = btnScale
                                scaleY = btnScale
                            }
                        ) {
                            Text("GET TO WORK", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private data class WalkthroughSlide(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val body: String
)
