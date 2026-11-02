package com.robo.robotuner.data.audio

import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

class HpsPitchDetector {

    data class Result(val frequency: Double, val confidence: Float)

    private val LN_2 = ln(2.0)

    fun detectPitch(audioSamples: FloatArray, sampleRate: Int): Result? {
        if (audioSamples.size < MIN_BUFFER_SIZE) return null

        val rms = calculateRMS(audioSamples)
        if (rms < NOISE_THRESHOLD) return null

        val fftSize = nextPowerOf2(audioSamples.size)

        // Apply Hanning window
        val windowed = FloatArray(fftSize)
        for (i in audioSamples.indices) {
            val window = 0.5f * (1 - cos(2.0 * Math.PI * i / (audioSamples.size - 1))).toFloat()
            windowed[i] = audioSamples[i] * window
        }

        val spectrum = computeMagnitudeSpectrum(windowed)

        val minBin = (MIN_FREQUENCY * fftSize / sampleRate).toInt().coerceAtLeast(1)
        val maxBin = (MAX_FREQUENCY * fftSize / sampleRate).toInt().coerceAtMost(spectrum.size / NUM_HARMONICS - 1)

        // Step 1: Find FFT peak (strongest frequency component)
        var fftPeakValue = 0f
        var fftPeakBin = minBin
        for (i in minBin..maxBin) {
            if (spectrum[i] > fftPeakValue) {
                fftPeakValue = spectrum[i]
                fftPeakBin = i
            }
        }

        if (fftPeakValue < PEAK_THRESHOLD) return null

        // Step 2: Check if FFT peak has subharmonics
        // If yes: the peak is likely a harmonic, use HPS to find true fundamental
        // If no: the peak IS the fundamental (or a pure tone), use it directly
        val hasSubharmonics = hasSignificantSubharmonics(spectrum, fftPeakBin, minBin, fftPeakValue)

        val finalBin: Int
        val finalValue: Float
        val dataForInterpolation: FloatArray

        if (hasSubharmonics) {
            // Signal has subharmonics - FFT peak might be a harmonic, use HPS
            val hps = computeHarmonicProductSpectrum(spectrum, NUM_HARMONICS, maxBin)

            var hpsMaxValue = 0f
            var hpsMaxBin = minBin
            for (i in minBin..maxBin) {
                if (hps[i] > hpsMaxValue) {
                    hpsMaxValue = hps[i]
                    hpsMaxBin = i
                }
            }
            finalBin = hpsMaxBin
            finalValue = hpsMaxValue
            dataForInterpolation = hps
        } else {
            // No subharmonics - FFT peak is the fundamental (or pure tone)
            finalBin = fftPeakBin
            finalValue = fftPeakValue
            dataForInterpolation = spectrum
        }

        // Calculate confidence based on peak prominence
        var totalEnergy = 0f
        for (i in minBin..maxBin) {
            totalEnergy += spectrum[i]
        }
        val avgEnergy = totalEnergy / (maxBin - minBin + 1)
        val confidence = if (avgEnergy > 0) {
            (finalValue / avgEnergy).coerceIn(0f, 20f) / 20f
        } else 0f

        // Use parabolic interpolation for better accuracy
        val refinedIndex = parabolicInterpolation(dataForInterpolation, finalBin)
        val frequency = refinedIndex * sampleRate / fftSize

        if (frequency < MIN_FREQUENCY || frequency > MAX_FREQUENCY) return null

        return Result(frequency, confidence)
    }

    // Check if the peak has significant energy at subharmonic frequencies
    // If yes, the peak is likely a harmonic (not the fundamental)
    private fun hasSignificantSubharmonics(spectrum: FloatArray, peakBin: Int, minBin: Int, peakValue: Float): Boolean {
        for (divisor in 2..4) {
            val subBin = peakBin / divisor
            if (subBin >= minBin && subBin < spectrum.size) {
                // If there's significant energy at a subharmonic, the peak might be a harmonic
                // For guitar, fundamental can be weaker than harmonics, so use low threshold
                if (spectrum[subBin] > peakValue * SUBHARMONIC_THRESHOLD) {
                    return true
                }
            }
        }
        return false
    }

    private fun computeMagnitudeSpectrum(samples: FloatArray): FloatArray {
        val n = samples.size
        val real = samples.copyOf()
        val imag = FloatArray(n)

        fft(real, imag)

        val magnitude = FloatArray(n / 2)
        for (i in 0 until n / 2) {
            magnitude[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }
        return magnitude
    }

    // Harmonic Product Spectrum - multiplies downsampled spectra to find fundamental
    // Better at finding weak fundamentals because harmonics MULTIPLY (not add)
    private fun computeHarmonicProductSpectrum(spectrum: FloatArray, numHarmonics: Int, maxBin: Int): FloatArray {
        val hps = FloatArray(maxBin + 1)

        for (i in 1..maxBin) {
            // Start with the fundamental
            var product = spectrum[i].toDouble()
            var validHarmonics = 1

            // Multiply by each harmonic (downsampled spectrum)
            for (h in 2..numHarmonics) {
                val index = i * h
                if (index < spectrum.size && spectrum[index] > 1e-10f) {
                    product *= spectrum[index]
                    validHarmonics++
                }
            }

            // Take the geometric mean to normalize for number of harmonics
            if (validHarmonics > 1 && product > 0) {
                hps[i] = product.pow(1.0 / validHarmonics).toFloat()
            } else {
                hps[i] = spectrum[i]
            }
        }
        return hps
    }

    private fun parabolicInterpolation(data: FloatArray, peak: Int): Double {
        if (peak <= 0 || peak >= data.size - 1) return peak.toDouble()

        val y0 = data[peak - 1]
        val y1 = data[peak]
        val y2 = data[peak + 1]

        val denom = y0 - 2 * y1 + y2
        if (denom == 0f) return peak.toDouble()

        val d = (y0 - y2) / (2 * denom)
        return peak.toDouble() + d.toDouble()
    }

    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n == 1) return

        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                var temp = real[i]
                real[i] = real[j]
                real[j] = temp
                temp = imag[i]
                imag[i] = imag[j]
                imag[j] = temp
            }
            var k = n / 2
            while (k <= j) {
                j -= k
                k /= 2
            }
            j += k
        }

        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val angle = -2.0 * Math.PI / len
            val wReal = cos(angle).toFloat()
            val wImag = kotlin.math.sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var curReal = 1f
                var curImag = 0f

                for (k in 0 until halfLen) {
                    val evenIdx = i + k
                    val oddIdx = i + k + halfLen

                    val tReal = curReal * real[oddIdx] - curImag * imag[oddIdx]
                    val tImag = curReal * imag[oddIdx] + curImag * real[oddIdx]

                    real[oddIdx] = real[evenIdx] - tReal
                    imag[oddIdx] = imag[evenIdx] - tImag
                    real[evenIdx] = real[evenIdx] + tReal
                    imag[evenIdx] = imag[evenIdx] + tImag

                    val newReal = curReal * wReal - curImag * wImag
                    val newImag = curReal * wImag + curImag * wReal
                    curReal = newReal
                    curImag = newImag
                }
                i += len
            }
            len *= 2
        }
    }

    private fun nextPowerOf2(n: Int): Int {
        var result = 1
        while (result < n) result = result shl 1
        return result
    }

    private fun calculateRMS(samples: FloatArray): Float {
        var sum = 0f
        for (sample in samples) {
            sum += sample * sample
        }
        return sqrt(sum / samples.size)
    }

    companion object {
        private const val MIN_BUFFER_SIZE = 2048
        private const val NOISE_THRESHOLD = 0.03f   // Lower for sensitivity
        private const val PEAK_THRESHOLD = 1e-10f
        private const val NUM_HARMONICS = 4         // Reduced for better low freq detection
        private const val MIN_FREQUENCY = 65.0      // Match YIN for low E string
        private const val MAX_FREQUENCY = 1400.0
        private const val SUBHARMONIC_THRESHOLD = 0.05f  // 5% - detect weak fundamentals
    }
}
