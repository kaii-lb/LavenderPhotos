package com.kaii.photos.compose

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import com.kaii.photos.R
import kotlin.random.Random
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "SNACK_BARS"

interface LavenderSnackbarEvent {
    val message: String
    val duration: SnackbarDuration
    val id: Int
}

interface LavenderSnackbarData {
    val event: LavenderSnackbarEvent

    fun performAction() {}
    fun dismiss() {}
}

object LavenderSnackbarEvents {
    /** Shows a [SnackbarWithLoadingIndicator] */
    data class LoadingEvent(
        override val message: String,
        @DrawableRes val iconResId: Int,
        val isLoading: MutableState<Boolean>,
        override val id: Int = Random.nextInt()
    ) : LavenderSnackbarEvent {
        override val duration: SnackbarDuration = SnackbarDuration.Indefinite
    }

    /** Shows a [SnackBarWithMessage] */
    data class MessageEvent(
        override val message: String,
        override val duration: SnackbarDuration,
        @DrawableRes val iconResId: Int,
        override val id: Int = Random.nextInt()
    ) : LavenderSnackbarEvent

    /** Shows a [SnackBarWithAction] */
    data class ActionEvent(
        override val message: String,
        override val duration: SnackbarDuration = SnackbarDuration.Indefinite,
        @DrawableRes val iconResId: Int,
        @DrawableRes val actionIconResId: Int,
        val action: () -> Unit,
        override val id: Int = Random.nextInt()
    ) : LavenderSnackbarEvent
}

/** Allows sending event to the snackbar controller from any place, composable or not */
object LavenderSnackbarController {
    private val _events = Channel<LavenderSnackbarEvent>(1)
    val events = _events.receiveAsFlow()

	private var currentId = 0

    /** queue a snackbar event to be displayed */
    suspend fun pushEvent(event: LavenderSnackbarEvent) {
        _events.send(event)
    }
}

/** Takes events from [LavenderSnackbarController.events] and displays them
 * latest added event removes whatever is before it, and shows it self
 * there is a 300ms delay between each event*/
@Composable
fun LavenderSnackbarHost(snackbarHostState: LavenderSnackbarHostState) {
    val lifecycleOwner = LocalLifecycleOwner.current

	val inChannel by LavenderSnackbarController.events.collectAsStateWithLifecycle(initialValue = null)

	Log.d(TAG, "in channel $inChannel")
	// LaunchedEffect cancels whenever the keys change, meaning the suspendCancellableCoroutine is also canceled
	// this way we don't have to deal with stupid dismissal rules to make sure latest snackbar is always shown
	// even though this might by hacky
	// note: look into DisposableEffect maybe its onDispose method can help cleanup the suspendCancellableCoroutine with a dismiss()
    LaunchedEffect(inChannel, lifecycleOwner.lifecycle, snackbarHostState) {
    	if (inChannel == null) return@LaunchedEffect

        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
	    	Log.d(TAG, "Trying to show snackbar $inChannel")
	        snackbarHostState.currentSnackbarEvent?.dismiss()
	        delay(300)
	        snackbarHostState.showSnackbar(inChannel!!)
        }
    }

    val currentEvent = snackbarHostState.currentSnackbarEvent
    LaunchedEffect(currentEvent) {
        if (currentEvent != null && currentEvent.event.duration != SnackbarDuration.Indefinite) {
            val delay = currentEvent.event.duration.toMillis()
            delay(delay)
            currentEvent.dismiss()
        }
    }
}

/** copy paste from composes default private function */
fun SnackbarDuration.toMillis() = when (this) {
    SnackbarDuration.Indefinite -> Long.MAX_VALUE
    SnackbarDuration.Long -> 10000L
    SnackbarDuration.Short -> 4000L
}

/** Sets the currently visible snackbar
 *
 * Handles the [LavenderSnackbarData.dismiss] and [LavenderSnackbarData.performAction] calls when displaying a snackbar
 *
 * Should be remembered, use in conjunction with [LavenderSnackbarHost] */
class LavenderSnackbarHostState {
    var currentSnackbarEvent: LavenderSnackbarData? by mutableStateOf(null)
        private set

    suspend fun showSnackbar(
        event: LavenderSnackbarEvent
    ): SnackbarResult = run {
        val result = try {
        	suspendCancellableCoroutine { continuation ->
        		continuation.invokeOnCancellation {
        		    Log.d(TAG, "Snackbar cancelled: $event")
        		    currentSnackbarEvent = null
        		}

	            currentSnackbarEvent = LavenderSnackbarDataImpl(
	                event,
	                continuation
	            )
        	}
        } finally {
            Log.d(TAG, "Snackbar finished: $event")
            currentSnackbarEvent = null
        }

        return@run result
    }

    // implements the actual dismiss() and performAction()
    private class LavenderSnackbarDataImpl(
        override val event: LavenderSnackbarEvent,
        private val continuation: CancellableContinuation<SnackbarResult>
    ) : LavenderSnackbarData {
        override fun performAction() {
            if (continuation.isActive) continuation.resumeWith(Result.success(SnackbarResult.ActionPerformed))
        }

        override fun dismiss() {
            if (continuation.isActive) {
                continuation.resumeWith(Result.success(SnackbarResult.Dismissed))
            } else {
                Log.d(TAG, "Dismiss ignored because continuation is not active")
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as LavenderSnackbarDataImpl

            if (event != other.event) return false
            if (continuation != other.continuation) return false

            return true
        }

        override fun hashCode(): Int {
            var result = event.hashCode()
            result = 31 * result + continuation.hashCode()
            return result
        }
    }
}

/** Wrapper for easy displaying of [LavenderSnackbarEvent]s.
 * wrap around the top-most component of your UI, usually a [NavHost]*/
@Composable
fun LavenderSnackbarBox(
    snackbarHostState: LavenderSnackbarHostState,
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = slideInVertically { height -> height } + expandHorizontally { width -> (width * 0.2f).toInt() },
    exitTransition: ExitTransition = slideOutVertically { height -> height } + shrinkHorizontally { width -> (width * 0.2f).toInt() },
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize(1f),
        contentAlignment = Alignment.Center
    ) {
        content()

        LavenderSnackbarHost(snackbarHostState = snackbarHostState)

        AnimatedVisibility(
            visible = snackbarHostState.currentSnackbarEvent != null,
            enter = enterTransition,
            exit = exitTransition,
            modifier = Modifier
                .systemBarsPadding()
                .align(Alignment.BottomCenter)
                .fillMaxWidth(1f)
                .wrapContentHeight()
                .padding(12.dp)
        ) {
            // keep last event in memory so the exit animation actually works
            // not proud of this but oh well it works
            var lastEvent by remember { mutableStateOf(snackbarHostState.currentSnackbarEvent!!) }
            val currentEvent = remember(snackbarHostState.currentSnackbarEvent) {
                if (snackbarHostState.currentSnackbarEvent == null) {
                    lastEvent
                } else {
                    lastEvent = snackbarHostState.currentSnackbarEvent!!
                    lastEvent
                }
            }

            when (currentEvent.event) {
                is LavenderSnackbarEvents.LoadingEvent -> {
                    val event = currentEvent.event as LavenderSnackbarEvents.LoadingEvent

                    SnackbarWithLoadingIndicator(
                        message = event.message,
                        iconResId = event.iconResId,
                        isLoading = event.isLoading.value
                    ) {
                        snackbarHostState.currentSnackbarEvent?.dismiss()
                    }
                }

                is LavenderSnackbarEvents.MessageEvent -> {
                    val event = currentEvent.event as LavenderSnackbarEvents.MessageEvent

                    SnackBarWithMessage(
                        message = event.message,
                        iconResId = event.iconResId
                    ) {
                        snackbarHostState.currentSnackbarEvent?.dismiss()
                    }
                }

                is LavenderSnackbarEvents.ActionEvent -> {
                    val event = currentEvent.event as LavenderSnackbarEvents.ActionEvent

                    SnackBarWithAction(
                        message = event.message,
                        iconResId = event.iconResId,
                        actionIconResId = event.actionIconResId,
                        action = event.action
                    )
                }

                else -> {
                    Text(
                        text = "THIS SHOULD NOT BE VISIBLE",
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(8.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

/** Base snackbar with an icon and a message */
@Composable
private fun LavenderSnackbar(
    message: String,
    @DrawableRes iconResId: Int,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        contentColor = contentColor,
        color = containerColor,
        shape = CircleShape,
        shadowElevation = 8.dp,
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth(1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(1f)
                .fillMaxHeight(1f)
                .padding(16.dp, 8.dp, 12.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = "Snackbar icon",
                modifier = Modifier
                    .size(32.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = message,
                fontSize = TextUnit(16f, TextUnitType.Sp),
                modifier = Modifier
                    .weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            content()
        }
    }
}

/** A snackbar displaying an icon, a message and its dismiss button*/
@Composable
private fun SnackBarWithMessage(
    message: String,
    @DrawableRes iconResId: Int,
    onDismiss: () -> Unit
) {
    LavenderSnackbar(
        message = message,
        iconResId = iconResId
    ) {
        IconButton(
            onClick = {
                onDismiss()
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.close),
                contentDescription = "Dismiss this snackbar",
                modifier = Modifier
                    .size(32.dp)
            )
        }
    }
}

/** A snackbar displaying an icon, a message and an action */
@Composable
fun SnackBarWithAction(
    message: String,
    @DrawableRes iconResId: Int,
    @DrawableRes actionIconResId: Int,
    action: () -> Unit
) {
    LavenderSnackbar(
        message = message,
        iconResId = iconResId
    ) {
        IconButton(
            onClick = {
                action()
            }
        ) {
            Icon(
                painter = painterResource(id = actionIconResId),
                contentDescription = "Run this snackbar's action",
                modifier = Modifier
                    .size(32.dp)
            )
        }
    }
}

/** A snackbar showing an infinite loading indicator along with a message and an icon */
@Composable
private fun SnackbarWithLoadingIndicator(
    message: String,
    @DrawableRes iconResId: Int,
    isLoading: Boolean,
    dismiss: () -> Unit
) {
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            delay(2000)
            dismiss()
        }
    }

    LavenderSnackbar(
        message = message,
        iconResId = iconResId
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                (scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ) + fadeIn()
                        ).togetherWith(
                        scaleOut(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            )
                        ) + fadeOut()
                    )
            },
            label = "Animate between loading and loaded states in snackbar",
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(4.dp, 0.dp)
        ) { loading ->
            if (loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeCap = StrokeCap.Round,
                    strokeWidth = 4.dp,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterVertically)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.file_is_selected_foreground),
                    contentDescription = "Loading done",
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}
