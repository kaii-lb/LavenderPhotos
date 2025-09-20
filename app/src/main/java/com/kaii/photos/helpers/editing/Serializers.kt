package com.kaii.photos.helpers.editing

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object OffsetSerializer : KSerializer<Offset> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName = "androidx.compose.ui.geometry.Offset") {
            element(
                elementName = "x",
                descriptor = PrimitiveSerialDescriptor(serialName = "x", kind = PrimitiveKind.FLOAT)
            )

            element(
                elementName = "y",
                descriptor = PrimitiveSerialDescriptor(serialName = "y", kind = PrimitiveKind.FLOAT)
            )
        }

    override fun serialize(encoder: Encoder, value: Offset) {
        encoder.encodeStructure(descriptor = descriptor) {
            encodeFloatElement(
                descriptor = descriptor,
                index = 0,
                value = value.x
            )

            encodeFloatElement(
                descriptor = descriptor,
                index = 1,
                value = value.y
            )
        }
    }

    override fun deserialize(decoder: Decoder): Offset =
        decoder.decodeStructure(descriptor = descriptor) {
            val x = decodeFloatElement(descriptor = descriptor, index = 0)
            val y = decodeFloatElement(descriptor = descriptor, index = 1)
            Offset(x, y)
        }
}

object IntSizeSerializer : KSerializer<IntSize> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName = "androidx.compose.ui.unit.IntSize") {
            element(
                elementName = "width",
                descriptor = PrimitiveSerialDescriptor(serialName = "width", kind = PrimitiveKind.INT)
            )

            element(
                elementName = "height",
                descriptor = PrimitiveSerialDescriptor(serialName = "height", kind = PrimitiveKind.INT)
            )
        }

    override fun serialize(encoder: Encoder, value: IntSize) {
        encoder.encodeStructure(descriptor = descriptor) {
            encodeIntElement(
                descriptor = descriptor,
                index = 0,
                value = value.width
            )

            encodeIntElement(
                descriptor = descriptor,
                index = 1,
                value = value.height
            )
        }
    }

    override fun deserialize(decoder: Decoder): IntSize =
        decoder.decodeStructure(descriptor = descriptor) {
            val width = decodeIntElement(descriptor = descriptor, index = 0)
            val height = decodeIntElement(descriptor = descriptor, index = 1)
            IntSize(width, height)
        }
}

object UriSerializer : KSerializer<Uri> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName = "android.net.Uri") {
            element(
                elementName = "uri",
                descriptor = PrimitiveSerialDescriptor(serialName = "uri", kind = PrimitiveKind.STRING)
            )
        }

    override fun serialize(encoder: Encoder, value: Uri) {
        encoder.encodeStructure(descriptor = descriptor) {
            encodeStringElement(
                descriptor = descriptor,
                index = 0,
                value = value.toString()
            )
        }
    }

    override fun deserialize(decoder: Decoder): Uri =
        decoder.decodeStructure(descriptor = descriptor) {
            decodeStringElement(
                descriptor = descriptor,
                index = 0
            ).toUri()
        }
}

object PaintingStyleSerializer : KSerializer<PaintingStyle> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName = "androidx.compose.ui.graphics.PaintingStyle") {
            element(
                elementName = "style",
                descriptor = PrimitiveSerialDescriptor(serialName = "style", kind = PrimitiveKind.INT)
            )
        }

    override fun serialize(encoder: Encoder, value: PaintingStyle) {
        encoder.encodeStructure(descriptor = descriptor) {
            encodeIntElement(
                descriptor = descriptor,
                index = 0,
                value =
                    if (value == PaintingStyle.Fill) 0
                    else 1
            )
        }
    }

    override fun deserialize(decoder: Decoder): PaintingStyle =
        decoder.decodeStructure(descriptor = descriptor) {
            val style = decodeIntElement(descriptor = descriptor, index = 0)

            if (style == 0) PaintingStyle.Fill
            else PaintingStyle.Stroke
        }
}

object StrokeJoinSerializer : KSerializer<StrokeJoin> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName = "androidx.compose.ui.graphics.StrokeJoin") {
            element(
                elementName = "style",
                descriptor = PrimitiveSerialDescriptor(serialName = "style", kind = PrimitiveKind.INT)
            )
        }

    override fun serialize(encoder: Encoder, value: StrokeJoin) {
        encoder.encodeStructure(descriptor = descriptor) {
            encodeIntElement(
                descriptor = descriptor,
                index = 0,
                value =
                    when (value) {
                        StrokeJoin.Miter -> 0
                        StrokeJoin.Round -> 1
                        else -> 2
                    }
            )
        }
    }

    override fun deserialize(decoder: Decoder): StrokeJoin =
        decoder.decodeStructure(descriptor = descriptor) {
            val style = decodeIntElement(descriptor = descriptor, index = 0)

            when (style) {
                0 -> StrokeJoin.Miter
                1 -> StrokeJoin.Round
                else -> StrokeJoin.Bevel
            }
        }
}

object StrokeCapSerializer : KSerializer<StrokeCap> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName = "androidx.compose.ui.graphics.StrokeCap") {
            element(
                elementName = "style",
                descriptor = PrimitiveSerialDescriptor(serialName = "style", kind = PrimitiveKind.INT)
            )
        }

    override fun serialize(encoder: Encoder, value: StrokeCap) {
        encoder.encodeStructure(descriptor = descriptor) {
            encodeIntElement(
                descriptor = descriptor,
                index = 0,
                value =
                    when (value) {
                        StrokeCap.Butt -> 0
                        StrokeCap.Round -> 1
                        else -> 2
                    }
            )
        }
    }

    override fun deserialize(decoder: Decoder): StrokeCap =
        decoder.decodeStructure(descriptor = descriptor) {
            val style = decodeIntElement(descriptor = descriptor, index = 0)

            when (style) {
                0 -> StrokeCap.Butt
                1 -> StrokeCap.Round
                else -> StrokeCap.Square
            }
        }
}

object AlwaysNullSerializer : KSerializer<Any?> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName = "com.kaii.photos.AlwaysNull")

    override fun serialize(encoder: Encoder, value: Any?) {}

    override fun deserialize(decoder: Decoder): Any? =
        decoder.decodeStructure(descriptor = descriptor) {
            null
        }
}

object FilterQualitySerializer : KSerializer<FilterQuality> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName = "androidx.compose.ui.graphics.FilterQuality") {
            element(
                elementName = "style",
                descriptor = PrimitiveSerialDescriptor(serialName = "style", kind = PrimitiveKind.INT)
            )
        }

    override fun serialize(encoder: Encoder, value: FilterQuality) {
        encoder.encodeStructure(descriptor = descriptor) {
            encodeIntElement(
                descriptor = descriptor,
                index = 0,
                value =
                    when (value) {
                        FilterQuality.None -> 0
                        FilterQuality.Low -> 1
                        FilterQuality.Medium -> 2
                        else -> 3
                    }
            )
        }
    }

    override fun deserialize(decoder: Decoder): FilterQuality =
        decoder.decodeStructure(descriptor = descriptor) {
            val style = decodeIntElement(descriptor = descriptor, index = 0)

            when (style) {
                0 -> FilterQuality.None
                1 -> FilterQuality.Low
                2 -> FilterQuality.Medium
                else -> FilterQuality.High
            }
        }
}

object BlendModeSerializer : KSerializer<BlendMode> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName = "androidx.compose.ui.graphics.BlendMode") {
            element(
                elementName = "type",
                descriptor = PrimitiveSerialDescriptor(serialName = "type", kind = PrimitiveKind.STRING)
            )
        }

    override fun serialize(encoder: Encoder, value: BlendMode) {
        encoder.encodeStructure(descriptor = descriptor) {
            encodeStringElement(
                descriptor = descriptor,
                index = 0,
                value =
                    when (value) {
                        BlendMode.Clear -> "Clear"
                        BlendMode.Src -> "Src"
                        BlendMode.Dst -> "Dst"
                        BlendMode.SrcOver -> "SrcOver"
                        BlendMode.DstOver -> "DstOver"
                        BlendMode.SrcIn -> "SrcIn"
                        BlendMode.DstIn -> "DstIn"
                        BlendMode.SrcOut -> "SrcOut"
                        BlendMode.DstOut -> "DstOut"
                        BlendMode.SrcAtop -> "SrcAtop"
                        BlendMode.DstAtop -> "DstAtop"
                        BlendMode.Xor -> "Xor"
                        BlendMode.Plus -> "Plus"
                        BlendMode.Modulate -> "Modulate"
                        BlendMode.Screen -> "Screen"
                        BlendMode.Overlay -> "Overlay"
                        BlendMode.Darken -> "Darken"
                        BlendMode.Lighten -> "Lighten"
                        BlendMode.ColorDodge -> "ColorDodge"
                        BlendMode.ColorBurn -> "ColorBurn"
                        BlendMode.Hardlight -> "HardLight"
                        BlendMode.Softlight -> "Softlight"
                        BlendMode.Difference -> "Difference"
                        BlendMode.Exclusion -> "Exclusion"
                        BlendMode.Multiply -> "Multiply"
                        BlendMode.Hue -> "Hue"
                        BlendMode.Saturation -> "Saturation"
                        BlendMode.Color -> "Color"
                        else -> "Luminosity"
                    }
            )
        }
    }

    override fun deserialize(decoder: Decoder): BlendMode =
        decoder.decodeStructure(descriptor = descriptor) {
            val type = decodeStringElement(descriptor = descriptor, index = 0)

            when (type) {
                "Clear" -> BlendMode.Clear
                "Src" -> BlendMode.Src
                "Dst" -> BlendMode.Dst
                "SrcOver" -> BlendMode.SrcOver
                "DstOver" -> BlendMode.DstOver
                "SrcIn" -> BlendMode.SrcIn
                "DstIn" -> BlendMode.DstIn
                "SrcOut" -> BlendMode.SrcOut
                "DstOut" -> BlendMode.DstOut
                "SrcAtop" -> BlendMode.SrcAtop
                "DstAtop" -> BlendMode.DstAtop
                "Xor" -> BlendMode.Xor
                "Plus" -> BlendMode.Plus
                "Modulate" -> BlendMode.Modulate
                "Screen" -> BlendMode.Screen
                "Overlay" -> BlendMode.Overlay
                "Darken" -> BlendMode.Darken
                "Lighten" -> BlendMode.Lighten
                "ColorDodge" -> BlendMode.ColorDodge
                "ColorBurn" -> BlendMode.ColorBurn
                "HardLight" -> BlendMode.Hardlight
                "Softlight" -> BlendMode.Softlight
                "Difference" -> BlendMode.Difference
                "Exclusion" -> BlendMode.Exclusion
                "Multiply" -> BlendMode.Multiply
                "Hue" -> BlendMode.Hue
                "Saturation" -> BlendMode.Saturation
                "Color" -> BlendMode.Color
                else -> BlendMode.Luminosity
            }
        }
}

object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName = "androidx.compose.ui.graphics.Color") {
            element(
                elementName = "Color",
                descriptor = PrimitiveSerialDescriptor(serialName = "Color", kind = PrimitiveKind.INT)
            )
        }

    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeStructure(descriptor = descriptor) {
            encodeIntElement(
                descriptor = descriptor,
                index = 0,
                value = value.toArgb()
            )
        }
    }

    override fun deserialize(decoder: Decoder): Color =
        decoder.decodeStructure(descriptor = descriptor) {
            val code = decodeIntElement(descriptor = descriptor, index = 0)

            Color(code)
        }
}