package com.example.oyl_fs.screens
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.R
import com.example.oyl_fs.ui.theme.OYL_FSTheme

@Composable
fun BienvenidaScreen(onComenzar: () -> Unit) {

    val fondoSuperior = Color(0xFFF5E4D1)
    val fondoInferior = Color(0xFFFFFFFF)
    val marron = Color(0xFF472F19)
    val marronClaro = Color(0xFF99643D)
    val beige = Color(0xFFD9B895)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(fondoSuperior)
            .safeDrawingPadding()
    ) {

        val compacto = maxHeight < 700.dp
        val altoTarjeta = maxHeight * 0.62f

        // DECORACIONES
        Canvas(modifier = Modifier.fillMaxSize()) {

            val curva = Path().apply {
                moveTo(size.width * 0.18f, 0f)
                cubicTo(
                    size.width * -0.10f,
                    size.height * 0.18f,
                    size.width * 0.08f,
                    size.height * 0.37f,
                    size.width * 0.55f,
                    size.height * 0.43f
                )
            }

            drawPath(
                path = curva,
                color = beige.copy(alpha = 0.55f),
                style = Stroke(width = 2.dp.toPx())
            )

            val segundaCurva = Path().apply {
                moveTo(size.width * 0.75f, 0f)
                cubicTo(
                    size.width * 0.35f,
                    size.height * 0.14f,
                    size.width * 0.62f,
                    size.height * 0.32f,
                    size.width * 1.1f,
                    size.height * 0.36f
                )
            }

            drawPath(
                path = segundaCurva,
                color = beige.copy(alpha = 0.45f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            drawCircle(
                color = beige,
                radius = 3.dp.toPx(),
                center = Offset(
                    size.width * 0.12f,
                    size.height * 0.55f
                )
            )
        }

        // CONTENIDO
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {

            // LOGO
            Image(
                painter = painterResource(
                    id = R.drawable.logo_oyl_vector
                ),
                contentDescription = "Logo OYL Frutos Secos",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(start = 24.dp, top = 12.dp)
                    .width(if (compacto) 125.dp else 145.dp)
                    .height(if (compacto) 54.dp else 65.dp)
            )

            // BOWL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.bienvenida_frutos
                    ),
                    contentDescription = "Bowl de frutos secos",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                )
            }

            // TARJETA INFERIOR BLANCA
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = altoTarjeta),
                shape = RoundedCornerShape(
                    topStart = 46.dp,
                    topEnd = 46.dp
                ),
                color = fondoInferior,
                shadowElevation = 0.dp
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = if (compacto) 20.dp else 26.dp,
                            bottom = 20.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // El texto puede desplazarse con letra grande; el botón queda visible.
                    Column(
                        modifier = Modifier.weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(if (compacto) 10.dp else 14.dp)
                    ) {
                    Text(
                        text = "BIENVENIDO A OYL",
                        color = marronClaro,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Text(
                        text = "Lo bueno\nempieza aquí.",
                        color = marron,
                        fontSize = if (compacto) 30.sp else 36.sp,
                        lineHeight = if (compacto) 35.sp else 41.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.3).sp
                    )

                    Text(
                        text = "Encuentra tus frutos secos favoritos y disfruta cada mezcla.",
                        color = marronClaro,
                        fontSize = if (compacto) 14.sp else 16.sp,
                        lineHeight = if (compacto) 20.sp else 24.sp
                    )

                    }

                    // BOTÓN
                    Button(
                        onClick = onComenzar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = marron,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(
                            start = 24.dp,
                            end = 8.dp,
                            top = 7.dp,
                            bottom = 7.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = "Comenzar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        color = Color(0xFFFFFFFF),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Continuar",
                                    tint = marron,
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// PREVIEW
@Preview(
    showBackground = true,
    name = "OYL - Crema y blanco",
    widthDp = 360,
    heightDp = 760
)
@Preview(
    showBackground = true,
    name = "OYL - Pantalla pequeña",
    widthDp = 320,
    heightDp = 640
)
@Composable
private fun BienvenidaPreview() {
    OYL_FSTheme {
        BienvenidaScreen(onComenzar = {})
    }
}
