package com.kaii.photos.widgets

import android.os.Vibrator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.graphics.shapes.RoundedPolygon
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.PinHasher
import com.kaii.photos.helpers.SingleJobRunner
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateShort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ExpressivePasswordFieldState(
    private val action: Action,
    passwordBytes: Flow<ByteArray?>,
    saltBytes: Flow<ByteArray?>,
    coroutineScope: CoroutineScope,
    private val vibrator: Vibrator,
    private val onSuccess: (password: ByteArray?, salt: ByteArray?) -> Unit
) {
    data class Code(
        val data: Int,
        val shape: RoundedPolygon
    )

    enum class Status {
        Successful,
        Error,
        Idle
    }

    enum class Action {
        Unlock,
        Verify,
        Set
    }

    companion object {
        const val MAX_CODE_LENGTH = 6
    }

    private val shapes = listOf(
        MaterialShapes.Circle,
        MaterialShapes.Arrow,
        MaterialShapes.Triangle,
        MaterialShapes.Diamond,
        MaterialShapes.Sunny,
        MaterialShapes.Pentagon
    )

    private var password: ByteArray? = null
    private var salt: ByteArray? = null
    private val availableShapes = mutableListOf<RoundedPolygon>()
    private val runner = SingleJobRunner(coroutineScope)
    private val hasher = PinHasher()

    private val _code = mutableStateListOf<Code>()
    val code = snapshotFlow { _code.toList() }

    private val _status = MutableStateFlow(Status.Idle)
    val status = _status.asStateFlow()

    private fun nextShape(): RoundedPolygon {
        val new = availableShapes.last()
        availableShapes.removeAt(availableShapes.size - 1)
        return new
    }

    init {
        reset()

        coroutineScope.launch {
            launch {
                passwordBytes.collect {
                    password = it
                }
            }

            launch {
                saltBytes.collect {
                    salt = it
                }
            }
        }
    }

    fun reset() {
        _code.clear()
        availableShapes.clear()
        availableShapes.addAll(shapes.shuffled())
    }

    fun addCode(data: Int) {
        if (_code.size >= MAX_CODE_LENGTH) {
            setStatus(false, null, null)
            return
        }

        _code.add(
            Code(
                data = data,
                shape = nextShape()
            )
        )
    }

    fun deleteCode() {
        val last = _code.lastOrNull()

        if (last == null) {
            setStatus(false, null, null)
            return
        }

        _code.removeAt(_code.size - 1)
        availableShapes.add(
            index = if (availableShapes.isEmpty()) 0 else Random.nextInt(0, availableShapes.size),
            element = last.shape
        )
    }

    fun submit() {
        if (_code.size < MAX_CODE_LENGTH) {
            setStatus(action == Action.Set && _code.isEmpty(), null, null)
            return
        }

        val chars = getCharPassword()

        if (chars != null) {
            when (action) {
                Action.Set -> {
                    val (newHash, newSalt) = hasher.hashNewPin(chars)
                    setStatus(true, newHash, newSalt)
                }

                else -> {
                    if (password == null || salt == null) {
                        setStatus(false, null, null)
                        return
                    }

                    val attemptHash = hasher.hashPinWithSalt(chars, salt!!)
                    val isSuccess = attemptHash.contentEquals(password)

                    setStatus(isSuccess, password, salt)
                }
            }
        } else {
            setStatus(true, null, null)
        }
    }

    private fun getCharPassword() = _code.joinToString("") { it.data.toString() }.toCharArray().takeIf { _code.isNotEmpty() }

    private fun setStatus(success: Boolean, hash: ByteArray?, salt: ByteArray?) {
        _status.value = Status.Idle

        if (success) {
            _status.value = Status.Successful
            onSuccess(hash, salt)
        } else {
            _status.value = Status.Error
            vibrator.vibrateShort()
        }

        runner.run {
            delay(1000)
            _status.value = Status.Idle
            reset()
        }
    }
}

@Composable
fun rememberExpressivePasswordFieldState(
    action: ExpressivePasswordFieldState.Action,
    passwordBytes: Flow<ByteArray?> = LocalContext.current.appModule.settings.permissions.getPassword(),
    saltBytes: Flow<ByteArray?> = LocalContext.current.appModule.settings.permissions.getSalt(),
    onSuccess: (password: ByteArray?, saltBytes: ByteArray?) -> Unit
): ExpressivePasswordFieldState {
    val coroutineScope = rememberCoroutineScope()
    val vibrator = rememberVibratorManager()

    return remember {
        ExpressivePasswordFieldState(
            action = action,
            passwordBytes = passwordBytes,
            saltBytes = saltBytes,
            coroutineScope = coroutineScope,
            vibrator = vibrator,
            onSuccess = onSuccess
        )
    }
}