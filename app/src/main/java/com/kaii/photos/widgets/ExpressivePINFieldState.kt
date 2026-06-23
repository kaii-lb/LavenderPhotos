package com.kaii.photos.widgets

import android.os.Vibrator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.graphics.shapes.RoundedPolygon
import com.kaii.photos.helpers.PinHasher
import com.kaii.photos.helpers.SingleJobRunner
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateShort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ExpressivePINFieldState(
    private val action: Action,
    passwordBytes: Flow<ByteArray?>,
    saltBytes: Flow<ByteArray?>,
    coroutineScope: CoroutineScope,
    private val vibrator: Vibrator
) {
    data class Code(
        val data: Int,
        val shape: RoundedPolygon
    )

    sealed interface Event {
        object Failure : Event

        data class Success(val password: ByteArray?, val salt: ByteArray?) : Event {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Success

                if (!password.contentEquals(other.password)) return false
                if (!salt.contentEquals(other.salt)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = password?.contentHashCode() ?: 0
                result = 31 * result + (salt?.contentHashCode() ?: 0)
                return result
            }
        }
    }

    enum class Status {
        Successful,
        Error,
        Idle
    }

    enum class Action {
        Unlock,
        Verify,
        Set,
        Confirm
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

    private var pin: ByteArray? = null
    private var salt: ByteArray? = null
    private val availableShapes = mutableListOf<RoundedPolygon>()
    private val runner = SingleJobRunner(coroutineScope)
    private val hasher = PinHasher()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _code = mutableStateListOf<Code>()
    val code: List<Code>
        get() = _code

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
                    pin = it
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
            setStatus(success = false, clear = false, hash = null, salt = null)
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
            setStatus(success = false, clear = false, hash = null, salt = null)
            return
        }

        _code.removeAt(_code.size - 1)
        availableShapes.add(
            index = if (availableShapes.isEmpty()) 0 else Random.nextInt(0, availableShapes.size),
            element = last.shape
        )
    }

    fun submit() {
        if (action != Action.Confirm && _code.size < MAX_CODE_LENGTH) {
            // if setting an empty pin, then pass an empty byte array for the confirmation
            // to differentiate it from null (which will fall back to the previous pin)
            setStatus(
                success = action == Action.Set && _code.isEmpty(),
                clear = false,
                hash = ByteArray(0).takeIf { action == Action.Set },
                salt = ByteArray(0).takeIf { action == Action.Set }
            )

            return
        }

        val chars = getCharPIN()

        when (action) {
            else if (chars == null) -> {
                setStatus(success = true, clear = false, hash = null, salt = null)
            }

            Action.Set -> {
                val (newHash, newSalt) = hasher.hashNewPin(chars)
                setStatus(success = true, clear = false, hash = newHash, salt = newSalt)
            }

            // To account for the lack of this case above
            Action.Confirm if (chars.size < MAX_CODE_LENGTH) -> {
                setStatus(success = false, clear = false, hash = null, salt = null)
            }

            else -> {
                if (pin == null || salt == null) {
                    setStatus(success = false, clear = false, hash = null, salt = null)
                    return
                }

                val attemptHash = hasher.hashPinWithSalt(chars, salt!!)
                val isSuccess = attemptHash.contentEquals(pin)

                setStatus(success = isSuccess, clear = true, hash = pin, salt = salt)
            }
        }
    }

    private fun getCharPIN(): CharArray? {
        if (_code.isEmpty()) return null

        val charArray = CharArray(_code.size)
        for (i in _code.indices) {
            charArray[i] = _code[i].data.digitToChar()
        }

        return charArray
    }

    private fun setStatus(
        success: Boolean,
        clear: Boolean,
        hash: ByteArray?,
        salt: ByteArray?
    ) {
        _status.value = Status.Idle

        if (success) {
            _status.value = Status.Successful
            _events.trySend(Event.Success(hash, salt))
        } else {
            _events.trySend(Event.Failure)
            _status.value = Status.Error
            vibrator.vibrateShort()
        }

        runner.run {
            delay(1000)
            _status.value = Status.Idle

            if (clear) reset()
        }
    }
}

@Composable
fun rememberExpressivePINFieldState(
    action: ExpressivePINFieldState.Action,
    pinBytes: Flow<ByteArray?>,
    saltBytes: Flow<ByteArray?>
): ExpressivePINFieldState {
    val coroutineScope = rememberCoroutineScope()
    val vibrator = rememberVibratorManager()

    return remember(action, pinBytes, saltBytes) {
        ExpressivePINFieldState(
            action = action,
            passwordBytes = pinBytes,
            saltBytes = saltBytes,
            coroutineScope = coroutineScope,
            vibrator = vibrator
        )
    }
}