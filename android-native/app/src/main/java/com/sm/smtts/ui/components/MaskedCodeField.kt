package com.sm.smtts.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

/**
 * Звичайне (не "комірки") поле для кодів виду XXXXX-XXXXX-XXXX: під капотом це одне
 * реальне [OutlinedTextField] (вставити повністю, виділити, прокрутити курсором - все
 * працює само собою, на відміну від попередньої реалізації з прихованим текстом під
 * окремими квадратиками, яка не вміщалась на вузьких екранах). Дефіси користувач не
 * друкує сам - вони домальовуються через [VisualTransformation], а ще не введені позиції
 * лишаються видимі як приглушений [placeholderChar], тож завжди видно, скільки символів
 * ще лишилось, а не просто порожній напис.
 */
@Composable
fun MaskedCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    codeLength: Int,
    modifier: Modifier = Modifier,
    groupSize: Int = 5,
    placeholderChar: Char = 'X',
    enabled: Boolean = true,
) {
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
    val transformation = remember(codeLength, groupSize, placeholderChar, placeholderColor) {
        MaskedCodeVisualTransformation(codeLength, groupSize, placeholderChar, placeholderColor)
    }
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            onValueChange(raw.uppercase().filter { it.isLetterOrDigit() }.take(codeLength))
        },
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
        ),
    )
}

private class MaskedCodeVisualTransformation(
    private val codeLength: Int,
    private val groupSize: Int,
    private val placeholderChar: Char,
    private val placeholderColor: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val builder = AnnotatedString.Builder()
        for (i in 0 until codeLength) {
            if (i > 0 && i % groupSize == 0) {
                builder.withStyle(SpanStyle(color = placeholderColor)) { append('-') }
            }
            if (i < raw.length) {
                builder.append(raw[i])
            } else {
                builder.withStyle(SpanStyle(color = placeholderColor)) { append(placeholderChar) }
            }
        }
        return TransformedText(builder.toAnnotatedString(), offsetMapping(raw.length, codeLength, groupSize))
    }
}

/**
 * Дефіс вставляється перед слотом [i], коли i>0 && i%groupSize==0 - і це НЕ залежить
 * від того, скільки символів уже введено (дефіс - структурний роздільник груп, а не межа
 * введеного/невведеного). Курсор "перестрибує" щойно вставлений дефіс одразу - так само,
 * як у типових масках номера картки/телефону.
 *
 * Важливо: Compose вимагає, щоб [OffsetMapping] повертала позиції лише в межах РЕАЛЬНО
 * введеного тексту (0..rawLength), навіть якщо тап стався десь у ще не введеній
 * (плейсхолдерній) частині маски - у цьому разі курсор просто "прилипає" до кінця
 * введеного. Раніше тут помилково рахувалось відносно повної довжини коду (codeLength),
 * через що Compose кидав IllegalStateException і застосунок крашився.
 */
private fun offsetMapping(rawLength: Int, codeLength: Int, groupSize: Int) = object : OffsetMapping {
    private fun dashesBefore(offset: Int): Int {
        if (codeLength <= 0) return 0
        val capped = offset.coerceIn(0, codeLength - 1)
        return capped / groupSize
    }

    override fun originalToTransformed(offset: Int): Int {
        val capped = offset.coerceIn(0, rawLength)
        return capped + dashesBefore(capped)
    }

    override fun transformedToOriginal(offset: Int): Int {
        for (raw in 0..rawLength) {
            if (originalToTransformed(raw) >= offset) return raw
        }
        return rawLength
    }
}
