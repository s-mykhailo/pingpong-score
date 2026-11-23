package com.sm.smtts.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Код з комірок на символ (як типовий OTP-інпут), але насправді - одне приховане [BasicTextField]
 * під капотом. Завдяки цьому довге натискання і "Вставити" підставляють увесь скопійований код
 * одразу, а не по символу, як було б з окремим [androidx.compose.material3.OutlinedTextField]
 * на кожну комірку.
 *
 * [codeLength] - довжина коду без дефісів (напр. 10 для join/admin-коду, 14 для recovery-коду),
 * формат бекенду групує символи по [groupSize] через дефіс (див. backend/Code.gs fmtCode_).
 */
@Composable
fun SegmentedCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    codeLength: Int,
    modifier: Modifier = Modifier,
    groupSize: Int = 5,
    enabled: Boolean = true,
) {
    BasicTextField(
        value = value,
        onValueChange = { raw ->
            onValueChange(raw.uppercase().filter { it.isLetterOrDigit() }.take(codeLength))
        },
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
        ),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (i in 0 until codeLength) {
                        if (i > 0 && i % groupSize == 0) {
                            Text(
                                "-",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                        val focused = i == value.length
                        Box(
                            modifier = Modifier
                                .size(width = 28.dp, height = 44.dp)
                                .clip(MaterialTheme.shapes.small)
                                .border(
                                    width = if (focused) 2.dp else 1.dp,
                                    color = if (focused) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = MaterialTheme.shapes.small,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = value.getOrNull(i)?.toString() ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                // Реальне текстове поле лишається змонтованим (для фокусу/IME/вставки),
                // але невидиме - весь текст і курсор прозорі, видно тільки комірки вище.
                Box(modifier = Modifier.fillMaxWidth()) { innerTextField() }
            }
        },
    )
}
