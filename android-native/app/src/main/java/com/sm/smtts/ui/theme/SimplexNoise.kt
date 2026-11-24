package com.sm.smtts.ui.theme

/**
 * 3D simplex noise (класична реалізація на таблиці перестановок), портована з
 * веб-версії топографічного фону (frontend/src/theme/topoBackground.ts) — той самий
 * фіксований seed і перемішування, щоб візерунок був тим самим "видом" шуму на обох
 * платформах, а не просто "якимось" шумом.
 */
internal object SimplexNoise {
    private val GRAD3 = floatArrayOf(
        1f, 1f, 0f, -1f, 1f, 0f, 1f, -1f, 0f, -1f, -1f, 0f,
        1f, 0f, 1f, -1f, 0f, 1f, 1f, 0f, -1f, -1f, 0f, -1f,
        0f, 1f, 1f, 0f, -1f, 1f, 0f, 1f, -1f, 0f, -1f, -1f,
    )

    private val PERM = IntArray(512)

    init {
        val base = IntArray(256) { it }
        var seed = 1337L
        fun rand(): Double {
            seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
            return seed / 0x7fffffff.toDouble()
        }
        for (i in 255 downTo 1) {
            val j = (rand() * (i + 1)).toInt()
            val tmp = base[i]
            base[i] = base[j]
            base[j] = tmp
        }
        for (i in 0 until 512) PERM[i] = base[i and 255]
    }

    private const val F3 = 1f / 3f
    private const val G3 = 1f / 6f

    private fun dot3(gi: Int, x: Float, y: Float, z: Float): Float {
        val o = gi * 3
        return GRAD3[o] * x + GRAD3[o + 1] * y + GRAD3[o + 2] * z
    }

    fun noise3(xin: Float, yin: Float, zin: Float): Float {
        val s = (xin + yin + zin) * F3
        val i = Math.floor((xin + s).toDouble()).toInt()
        val j = Math.floor((yin + s).toDouble()).toInt()
        val k = Math.floor((zin + s).toDouble()).toInt()
        val t = (i + j + k) * G3
        val x0 = xin - (i - t)
        val y0 = yin - (j - t)
        val z0 = zin - (k - t)

        var i1: Int; var j1: Int; var k1: Int
        var i2: Int; var j2: Int; var k2: Int
        if (x0 >= y0) {
            if (y0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0 }
            else if (x0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1 }
            else { i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1 }
        } else {
            if (y0 < z0) { i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1 }
            else if (x0 < z0) { i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1 }
            else { i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0 }
        }

        val x1 = x0 - i1 + G3
        val y1 = y0 - j1 + G3
        val z1 = z0 - k1 + G3
        val x2 = x0 - i2 + 2 * G3
        val y2 = y0 - j2 + 2 * G3
        val z2 = z0 - k2 + 2 * G3
        val x3 = x0 - 1 + 3 * G3
        val y3 = y0 - 1 + 3 * G3
        val z3 = z0 - 1 + 3 * G3

        val ii = i and 255
        val jj = j and 255
        val kk = k and 255

        val gi0 = PERM[ii + PERM[jj + PERM[kk]]] % 12
        val gi1 = PERM[ii + i1 + PERM[jj + j1 + PERM[kk + k1]]] % 12
        val gi2 = PERM[ii + i2 + PERM[jj + j2 + PERM[kk + k2]]] % 12
        val gi3 = PERM[ii + 1 + PERM[jj + 1 + PERM[kk + 1]]] % 12

        var n0 = 0f
        var n1 = 0f
        var n2 = 0f
        var n3 = 0f

        var t0 = 0.6f - x0 * x0 - y0 * y0 - z0 * z0
        if (t0 >= 0) { t0 *= t0; n0 = t0 * t0 * dot3(gi0, x0, y0, z0) }

        var t1 = 0.6f - x1 * x1 - y1 * y1 - z1 * z1
        if (t1 >= 0) { t1 *= t1; n1 = t1 * t1 * dot3(gi1, x1, y1, z1) }

        var t2 = 0.6f - x2 * x2 - y2 * y2 - z2 * z2
        if (t2 >= 0) { t2 *= t2; n2 = t2 * t2 * dot3(gi2, x2, y2, z2) }

        var t3 = 0.6f - x3 * x3 - y3 * y3 - z3 * z3
        if (t3 >= 0) { t3 *= t3; n3 = t3 * t3 * dot3(gi3, x3, y3, z3) }

        return 32f * (n0 + n1 + n2 + n3)
    }
}
