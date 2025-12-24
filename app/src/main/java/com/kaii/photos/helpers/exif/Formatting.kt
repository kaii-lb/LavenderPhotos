package com.kaii.photos.helpers.exif

import java.math.BigInteger

fun Double.toFraction(): String {
    val s = this.toString()

    if (!s.contains('.')) {
        return "$s/1"
    }

    val parts = s.split('.')
    val whole = BigInteger(parts[0])
    val fractional = BigInteger(parts[1])
    val decimalPlaces = parts[1].length

    val denominator = BigInteger.TEN.pow(decimalPlaces)

    val totalNumerator = if (whole.signum() == -1) {
        (whole * denominator) - fractional
    } else {
        (whole * denominator) + fractional
    }

    val gcd = totalNumerator.gcd(denominator)

    val finalNumerator = totalNumerator / gcd
    val finalDenominator = denominator / gcd

    return "$finalNumerator/$finalDenominator"
}

