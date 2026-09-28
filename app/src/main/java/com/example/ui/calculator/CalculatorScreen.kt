package com.example.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
  var display by remember { mutableStateOf("0") }
  var operand1 by remember { mutableStateOf<Double?>(null) }
  var operator by remember { mutableStateOf<String?>(null) }
  var isNewNumber by remember { mutableStateOf(true) }

  val buttons = listOf(
    listOf("C", "±", "%", "÷"),
    listOf("7", "8", "9", "×"),
    listOf("4", "5", "6", "−"),
    listOf("1", "2", "3", "+"),
    listOf("0", ".", "=")
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      .padding(12.dp),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Calculator Display
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(90.dp)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        .padding(16.dp),
      contentAlignment = Alignment.BottomEnd
    ) {
      Text(
        text = display,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End,
        maxLines = 1
      )
    }

    // Calculator Buttons Keypad
    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      buttons.forEach { row ->
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          row.forEach { btn ->
            val isOp = btn in listOf("÷", "×", "−", "+", "=")
            val isAction = btn in listOf("C", "±", "%")
            val weight = if (btn == "0") 2f else 1f

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = when {
                btn == "=" -> MaterialTheme.colorScheme.primary
                isOp -> MaterialTheme.colorScheme.primaryContainer
                isAction -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              },
              modifier = Modifier
                .weight(weight)
                .height(48.dp)
                .clickable {
                  when (btn) {
                    "C" -> {
                      display = "0"
                      operand1 = null
                      operator = null
                      isNewNumber = true
                    }
                    "±" -> {
                      val v = display.toDoubleOrNull() ?: 0.0
                      display = (-v).toString().removeSuffix(".0")
                    }
                    "%" -> {
                      val v = display.toDoubleOrNull() ?: 0.0
                      display = (v / 100.0).toString()
                    }
                    "÷", "×", "−", "+" -> {
                      operand1 = display.toDoubleOrNull()
                      operator = btn
                      isNewNumber = true
                    }
                    "=" -> {
                      val op2 = display.toDoubleOrNull()
                      if (operand1 != null && operator != null && op2 != null) {
                        val res = when (operator) {
                          "+" -> operand1!! + op2
                          "−" -> operand1!! - op2
                          "×" -> operand1!! * op2
                          "÷" -> if (op2 != 0.0) operand1!! / op2 else Double.NaN
                          else -> op2
                        }
                        display = if (res.isNaN()) "Error" else res.toString().removeSuffix(".0")
                        operand1 = null
                        operator = null
                        isNewNumber = true
                      }
                    }
                    else -> {
                      if (isNewNumber) {
                        display = btn
                        isNewNumber = false
                      } else {
                        display = if (display == "0" && btn != ".") btn else display + btn
                      }
                    }
                  }
                }
            ) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                  text = btn,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (btn == "=") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }
    }
  }
}
