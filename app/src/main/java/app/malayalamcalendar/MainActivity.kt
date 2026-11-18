package app.malayalamcalendar

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

val ML_MONTHS = listOf("ചിങ്ങം", "കന്നി", "തുലാം", "വൃശ്ചികം", "ധനു", "മകരം", "കുംഭം", "മീനം", "മേടം", "ഇടവം", "മിഥുനം", "കർക്കടകം")
val NAKSHATRA = listOf("അശ്വതി", "ഭരണി", "കാർത്തിക", "രോഹിണി", "മകയിരം", "തിരുവാതിര", "പുണർതം", "പൂയം", "ആയില്യം", "മകം", "പൂരം", "ഉത്രം", "അത്തം", "ചിത്തിര", "ചോതി", "വിശാഖം", "അനിഴം", "തൃക്കേട്ട", "മൂലം", "പൂരാടം", "ഉത്രാടം", "തിരുവോണം", "അവിട്ടം", "ചതയം", "പൂരുരുട്ടാതി", "ഉത്രട്ടാതി", "രേവതി")
val ML_WEEK = listOf("ഞായർ", "തിങ്കൾ", "ചൊവ്വ", "ബുധൻ", "വ്യാഴം", "വെള്ളി", "ശനി")
val RED = Color(0xFFC62828)

data class Holiday(val ml: String, val en: String)
data class Day(val date: LocalDate, val mMonth: Int, val mDay: Int, val mYear: Int, val nak: Int, val holidays: List<Holiday>)

fun loadDays(ctx: Context): Map<LocalDate, Day> =
    ctx.assets.open("cal2027.csv").bufferedReader(Charsets.UTF_8).readLines().filter { it.isNotBlank() }.associate { line ->
        val p = line.split(",", limit = 6)
        val hs = p[5].trim().removeSurrounding("\"").split(";").filter { it.isNotBlank() }.map {
            val (ml, en) = it.split("|"); Holiday(ml, en)
        }
        val d = LocalDate.parse(p[0])
        d to Day(d, p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), hs)
    }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val days = loadDays(this)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { CalendarApp(days) } } }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarApp(days: Map<LocalDate, Day>) {
    val today = LocalDate.now()
    val pager = rememberPagerState(initialPage = if (today.year == 2027) today.monthValue - 1 else 0) { 12 }
    var selected by remember { mutableStateOf<LocalDate?>(if (today.year == 2027) today else null) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(12.dp)) {
        val ym = YearMonth.of(2027, pager.currentPage + 1)
        val first = days[ym.atDay(1)]!!
        val last = days[ym.atEndOfMonth()]!!
        Text("${ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} 2027", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        val ml = if (first.mMonth == last.mMonth) "${ML_MONTHS[first.mMonth]} ${first.mYear}"
        else "${ML_MONTHS[first.mMonth]} ${first.mYear} – ${ML_MONTHS[last.mMonth]} ${last.mYear}"
        Text(ml, fontSize = 18.sp, color = RED)
        Spacer(Modifier.height(8.dp))
        HorizontalPager(pager, Modifier.fillMaxWidth()) { page ->
            MonthGrid(YearMonth.of(2027, page + 1), days, selected, today) { selected = it }
        }
        Spacer(Modifier.height(12.dp))
        selected?.let { DetailCard(days[it]!!) }
    }
}

@Composable
fun MonthGrid(ym: YearMonth, days: Map<LocalDate, Day>, selected: LocalDate?, today: LocalDate, onSelect: (LocalDate) -> Unit) {
    val offset = ym.atDay(1).dayOfWeek.value % 7
    val rows = (offset + ym.lengthOfMonth() + 6) / 7
    Column {
        Row(Modifier.fillMaxWidth()) {
            ML_WEEK.forEachIndexed { i, w ->
                Text(w, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = if (i == 0) RED else Color.Unspecified)
            }
        }
        Spacer(Modifier.height(4.dp))
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0..6) {
                    val n = r * 7 + c - offset + 1
                    if (n < 1 || n > ym.lengthOfMonth()) Spacer(Modifier.weight(1f).height(66.dp))
                    else {
                        val d = days[ym.atDay(n)]!!
                        DayCell(d, c == 0, d.date == selected, d.date == today, onSelect)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.DayCell(d: Day, sunday: Boolean, isSelected: Boolean, isToday: Boolean, onSelect: (LocalDate) -> Unit) {
    val special = sunday || d.holidays.isNotEmpty()
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> Color(0xFFFFF3E0)
        else -> Color.Transparent
    }
    Column(
        Modifier.weight(1f).height(66.dp).padding(1.dp).clip(RoundedCornerShape(8.dp)).background(bg)
            .clickable { onSelect(d.date) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("${d.date.dayOfMonth}", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = if (special) RED else Color.Unspecified)
        Text(if (d.mDay == 1) ML_MONTHS[d.mMonth] else "${d.mDay}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
        if (d.holidays.isNotEmpty()) Box(Modifier.padding(top = 2.dp).size(5.dp).clip(RoundedCornerShape(50)).background(RED))
    }
}

@Composable
fun DetailCard(d: Day) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${d.date.dayOfMonth} ${d.date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} 2027, ${ML_WEEK[d.date.dayOfWeek.value % 7]}", fontWeight = FontWeight.Bold)
            Text("${ML_MONTHS[d.mMonth]} ${d.mDay}, ${d.mYear}", fontSize = 20.sp, color = RED)
            Text("നക്ഷത്രം: ${NAKSHATRA[d.nak]}")
            d.holidays.forEach { Text("${it.ml} · ${it.en}", color = RED) }
        }
    }
}
