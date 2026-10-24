package io.github.bigrirason_debug.platen.brush

import io.github.bigrirason_debug.platen.model.BrushEngine
import io.github.bigrirason_debug.platen.model.BrushRole
import io.github.bigrirason_debug.platen.model.BrushSize
import io.github.bigrirason_debug.platen.model.BrushSource
import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.CurvePoint
import io.github.bigrirason_debug.platen.model.ExposedParam
import io.github.bigrirason_debug.platen.model.Grain
import io.github.bigrirason_debug.platen.model.GrainMode
import io.github.bigrirason_debug.platen.model.PressureMapping
import io.github.bigrirason_debug.platen.model.Stamp
import io.github.bigrirason_debug.platen.model.StampTip
import io.github.bigrirason_debug.platen.model.Taper
import io.github.bigrirason_debug.platen.model.Tilt
import kotlin.math.roundToInt

/**
 * Photoshop `.abr` 임포터(순수 바이트 파싱, brush-formats §2).
 * - v1/v2: 공식 6.0 스펙 경로(샘플 팁; computed 는 합성 원 + profile).
 * - v6+(6.1/6.2, 7, 9, 10): `8BIM` 블록 워킹 → `samp`(팁) + `desc`(동역학) + `patt`(그레인).
 * - 디스크립터 해석 실패 시 "팁만 복구"(§7 축소안): samp 만으로 이름·팁·기본 동역학.
 * 참고 구조: Adobe 6.0 스펙(공식) + MIT 파서(ag-psd/abrkit) 구조. GPL(GIMP/Krita) 코드 미참조.
 */
object AbrImporter {
    /** `Intr=false`(간격 미사용) 시 기본 간격(§3.3). */
    private const val DEFAULT_SPACING = 0.15f
    /** v6 `samp` 레코드의 ID 뒤 미지 필드: 서브버전 1 → 10 B, 2 → 264 B(§2.2; 길이 필드 직후부터 세면 47/301 B). */
    private const val SAMP_UNKNOWN_SUB1 = 10
    private const val SAMP_UNKNOWN_SUB2 = 264
    private const val PASCAL_ID_BYTES = 37

    class SampledTip(val id: String, val image: GrayImage, val notes: List<String>)
    class Pattern(val id: String, val name: String, val image: GrayImage?)

    private class Parsed(
        val version: Int, val subversion: Int,
        val tips: List<SampledTip>, val presets: List<Desc>, val patterns: List<Pattern>,
        val notes: List<String>,
    )

    fun import(fileName: String, bytes: ByteArray, importedAt: Long): ImportResult {
        val r = ByteReader(bytes)
        if (bytes.size < 4) throw BrushFormatException("ABR 헤더 부족")
        val version = r.i16()
        val second = r.i16()
        val base = fileName.substringAfterLast('/').substringBeforeLast('.')
        val parsed = when (version) {
            1, 2 -> parseV12(r, version, second)
            6, 7, 9, 10 -> parseV6(r, version, second)
            else -> throw BrushFormatException("지원하지 않는 ABR 버전 $version")
        }
        val notes = ArrayList(parsed.notes)
        notes += "ABR v$version.${parsed.subversion}: 팁 ${parsed.tips.size}, 프리셋 ${parsed.presets.size}, 패턴 ${parsed.patterns.size}"
        val brushes = ArrayList<LoadedBrush>()
        if (parsed.presets.isNotEmpty()) {
            val used = HashSet<String>()
            parsed.presets.forEachIndexed { i, preset ->
                brushes += mapPreset(preset, i, parsed, used, base, importedAt)
            }
            // desc 에서 참조되지 않은 팁도 버리지 않는다(팁만 복구)
            parsed.tips.filter { it.id !in used }.forEachIndexed { i, tip ->
                brushes += tipOnly(tip, "$base ${brushes.size + 1}", DEFAULT_SPACING, listOf("desc 에 프리셋 없음 → 팁만 복구"), importedAt)
            }
        } else {
            if (version >= 6) notes += "desc 없음/해독 실패 → 팁만 복구 모드"
            parsed.tips.forEachIndexed { i, tip ->
                brushes += tipOnly(tip, if (parsed.tips.size == 1) base else "$base ${i + 1}", DEFAULT_SPACING, tip.notes, importedAt)
            }
        }
        return ImportResult(brushes, notes)
    }

    // ------------------------------------------------------------------ v1/v2

    private fun parseV12(r: ByteReader, version: Int, count: Int): Parsed {
        val tips = ArrayList<SampledTip>()
        val presets = ArrayList<Desc>()
        val notes = ArrayList<String>()
        if (count < 0 || count > ImportLimits.ABR_MAX_RECORDS) throw ImportLimitExceeded("ABR 브러시 수 $count > ${ImportLimits.ABR_MAX_RECORDS}")
        var index = 0
        while (index < count && r.remaining >= 6) {
            val type = r.i16()
            val size = r.i32()
            if (size < 0 || size > r.remaining) { notes += "레코드 $index 길이 $size 가 파일 끝을 넘음 → 중단"; break }
            val rec = r.sub(size)
            try {
                when (type) {
                    1 -> presets += computedV1(rec, index)
                    2 -> {
                        rec.i32() // misc
                        val spacing = rec.i16()
                        val name = if (version == 2) AbrDescriptor.unicode(rec) else "brush ${index + 1}"
                        rec.u8() // antialias
                        rec.skip(8) // bounds int16×4
                        val top = rec.i32(); val left = rec.i32(); val bottom = rec.i32(); val right = rec.i32()
                        val depth = rec.i16()
                        val compression = rec.u8() // 6.0 스펙 Table 3-6: 1 byte(§2.2 표기는 int16이나 스펙·관찰 우세는 1 byte)
                        val img = readImage(rec, top, left, bottom, right, depth, compression)
                        val id = "v$version-$index"
                        tips += SampledTip(id, img, listOf())
                        // v1/v2 는 디스크립터가 없으므로 최소 프리셋을 합성
                        presets += Desc("sampledBrush", "sampledBrush", linkedMapOf<String, Any?>(
                            "Nm  " to name, "Spcn" to UnitValue("#Prc", spacing.toDouble()), "sampledData" to id,
                            "Dmtr" to UnitValue("#Pxl", maxOf(right - left, bottom - top).toDouble()),
                        ))
                    }
                    else -> notes += "레코드 $index: 알 수 없는 타입 $type 건너뜀"
                }
            } catch (e: BrushFormatException) {
                notes += "레코드 $index 해석 실패: ${e.message}"
            }
            index++
        }
        return Parsed(version, 0, tips, presets, emptyList(), notes)
    }

    /** computed(14 B): misc i32, spacing i16, diameter i16, roundness i16, angle i16, hardness i16(§2.2). */
    private fun computedV1(rec: ByteReader, index: Int): Desc {
        rec.i32()
        val spacing = rec.i16(); val diameter = rec.i16(); val roundness = rec.i16(); val angle = rec.i16(); val hardness = rec.i16()
        return Desc("computedBrush", "computedBrush", linkedMapOf<String, Any?>(
            "Nm  " to "computed ${index + 1}",
            "Spcn" to UnitValue("#Prc", spacing.toDouble()), "Dmtr" to UnitValue("#Pxl", diameter.toDouble()),
            "Rndn" to UnitValue("#Prc", roundness.toDouble()), "Angl" to UnitValue("#Ang", angle.toDouble()),
            "Hrdn" to UnitValue("#Prc", hardness.toDouble()),
        ))
    }

    // ------------------------------------------------------------------ v6+

    private fun parseV6(r: ByteReader, version: Int, subversion: Int): Parsed {
        val tips = ArrayList<SampledTip>()
        var presets: List<Desc> = emptyList()
        val patterns = ArrayList<Pattern>()
        val notes = ArrayList<String>()
        var blocks = 0
        while (r.remaining >= 12) {
            val sig = r.ascii(4)
            if (sig != "8BIM") { notes += "8BIM 시그니처 아님 @${r.pos - 4} → 중단"; break }
            val key = r.ascii(4)
            val len = r.u32()
            if (len > r.remaining) { notes += "블록 '$key' 길이 $len 가 파일 끝을 넘음 → 이후 블록 무시(팁만 복구)"; break }
            if (++blocks > ImportLimits.ABR_MAX_RECORDS) throw ImportLimitExceeded("8BIM 블록 수 > ${ImportLimits.ABR_MAX_RECORDS}")
            val block = r.sub(len.toInt())
            when (key) {
                "samp" -> tips += readSamp(block, subversion, notes)
                "desc" -> presets = try { readDesc(block) } catch (e: BrushFormatException) { notes += "desc 해석 실패(${e.message}) → 팁만 복구"; emptyList() }
                "patt" -> try { patterns += readPatt(block, notes) } catch (e: BrushFormatException) { notes += "patt 해석 실패: ${e.message}" }
                "phry", "lPdc", "titl" -> {}
                else -> notes += "8BIM '$key' 무시"
            }
            // 4바이트 정렬
            val pad = (4 - (len % 4).toInt()) % 4
            if (pad > 0 && r.remaining >= pad) r.skip(pad)
        }
        return Parsed(version, subversion, tips, presets, patterns, notes)
    }

    private fun readSamp(b: ByteReader, subversion: Int, mutableNotes: MutableList<String>): List<SampledTip> {
        val out = ArrayList<SampledTip>()
        var count = 0
        while (b.remaining >= 4) {
            val len = b.u32()
            if (len <= 0 || len > b.remaining) { mutableNotes.add("samp 레코드 길이 $len 이 블록 끝을 넘음 → 이후 팁 무시"); break }
            if (++count > ImportLimits.ABR_MAX_RECORDS) throw ImportLimitExceeded("samp 레코드 수 > ${ImportLimits.ABR_MAX_RECORDS}")
            val rec = b.sub(len.toInt())
            val tipNotes = ArrayList<String>()
            try {
                val idLen = rec.u8()
                val id = rec.ascii(idLen)
                rec.skip(PASCAL_ID_BYTES - 1 - idLen) // Pascal ID 영역은 37 B 고정(UUID 36 + 길이 1)
                rec.skip(if (subversion == 1) SAMP_UNKNOWN_SUB1 else SAMP_UNKNOWN_SUB2)
                val top = rec.i32(); val left = rec.i32(); val bottom = rec.i32(); val right = rec.i32()
                val depth = rec.u16()
                val compression = rec.u8()
                val img = readImage(rec, top, left, bottom, right, depth, compression)
                out += SampledTip(id, img, tipNotes)
            } catch (e: BrushFormatException) {
                mutableNotes.add("samp 레코드 $count 해석 실패: ${e.message}")
            }
            val pad = (4 - (len % 4).toInt()) % 4
            if (pad > 0 && b.remaining >= pad) b.skip(pad)
        }
        return out
    }

    /** bounds·depth·압축 검증 후 픽셀 읽기(8/16-bit raw/RLE). 16-bit 는 상위 바이트(§3.5 5항). */
    private fun readImage(rec: ByteReader, top: Int, left: Int, bottom: Int, right: Int, depth: Int, compression: Int): GrayImage {
        val w = right - left; val h = bottom - top
        if (w <= 0 || h <= 0 || w > ImportLimits.ABR_MAX_SIDE || h > ImportLimits.ABR_MAX_SIDE) throw BrushFormatException("bounds 이상: ${w}x$h")
        if (w.toLong() * h > ImportLimits.ABR_MAX_AREA) throw ImportLimitExceeded("팁 면적 ${w.toLong() * h} > ${ImportLimits.ABR_MAX_AREA}")
        if (depth != 8 && depth != 16) throw BrushFormatException("depth $depth 미지원")
        val bpp = depth / 8
        val rowBytes = w * bpp
        val raw: ByteArray = when (compression) {
            0 -> rec.bytes(rowBytes * h)
            1 -> {
                val lens = IntArray(h) { rec.u16() }
                val out = ByteArray(rowBytes * h)
                for (y in 0 until h) {
                    val row = PackBits.decode(rec.sub(minOf(lens[y], rec.remaining)), rowBytes)
                    row.copyInto(out, y * rowBytes)
                }
                out
            }
            else -> throw BrushFormatException("압축 $compression 미지원")
        }
        if (bpp == 1) return GrayImage(w, h, raw)
        return GrayImage(w, h, ByteArray(w * h) { raw[it * 2] })
    }

    private fun readDesc(b: ByteReader): List<Desc> {
        val ver = b.i32()
        if (ver != 16) throw BrushFormatException("디스크립터 버전 $ver (16 기대)")
        val root = AbrDescriptor.read(b)
        val list = root["Brsh"] as? List<*> ?: return emptyList()
        if (list.size > ImportLimits.ABR_MAX_RECORDS) throw ImportLimitExceeded("Brsh 프리셋 수 > ${ImportLimits.ABR_MAX_RECORDS}")
        return list.filterIsInstance<Desc>()
    }

    /**
     * `patt` 블록(§2.2): 레코드 `u32 length, u32 version, u32 mode, u16 height, u16 width, UTF-16 이름, Pascal ID, (mode 2 → 768 B 색인표)`,
     * 그 뒤 Virtual Memory Array List. VMA 구조는 문서에 없어 PSD 스펙 기억에 따른 **추정**:
     * `u32 version(3), u32 length, rect i32×4, u32 channels`, 채널마다 `u32 written; written≠0 이면 u32 length, u32 depth, rect i32×4, u16 depth, u8 compression, data`.
     * 실패하면 해당 패턴은 image=null(이름만).
     */
    private fun readPatt(b: ByteReader, notes: MutableList<String>): List<Pattern> {
        val out = ArrayList<Pattern>()
        var count = 0
        while (b.remaining >= 4) {
            val len = b.u32()
            if (len <= 0 || len > b.remaining) { notes += "patt 레코드 길이 $len 이 블록 끝을 넘음"; break }
            if (++count > ImportLimits.ABR_MAX_RECORDS) throw ImportLimitExceeded("patt 레코드 수 > ${ImportLimits.ABR_MAX_RECORDS}")
            val rec = b.sub(len.toInt())
            var name = "pattern $count"; var id = ""
            try {
                rec.i32() // version
                val mode = rec.i32()
                val height = rec.u16(); val width = rec.u16()
                name = AbrDescriptor.unicode(rec)
                val idLen = rec.u8(); id = rec.ascii(idLen)
                if (mode == 2) rec.skip(768)
                if (width <= 0 || height <= 0 || width > ImportLimits.ABR_MAX_SIDE || height > ImportLimits.ABR_MAX_SIDE) throw BrushFormatException("패턴 크기 이상 ${width}x$height")
                out += Pattern(id, name, readVma(rec, width, height))
            } catch (e: BrushFormatException) {
                notes += "패턴 '$name' 픽셀 해석 실패(${e.message}) → 이름만"
                out += Pattern(id, name, null)
            }
            val pad = (4 - (len % 4).toInt()) % 4
            if (pad > 0 && b.remaining >= pad) b.skip(pad)
        }
        return out
    }

    private fun readVma(rec: ByteReader, width: Int, height: Int): GrayImage? {
        rec.i32() // VMA list version (3)
        rec.i32() // total length
        rec.skip(16) // rect
        val channels = rec.i32()
        if (channels < 0 || channels > 64) throw BrushFormatException("채널 수 이상 $channels")
        var first: GrayImage? = null
        val total = channels + 2 // 사용자 마스크·시트 마스크 포함(추정)
        for (c in 0 until total) {
            if (rec.remaining < 4) break
            val written = rec.i32()
            if (written == 0) continue
            val clen = rec.i32()
            val depth32 = rec.i32()
            val top = rec.i32(); val left = rec.i32(); val bottom = rec.i32(); val right = rec.i32()
            val depth = rec.u16()
            val compression = rec.u8()
            val body = rec.sub(minOf(maxOf(0, clen - 23), rec.remaining))
            if (first == null) {
                val w = right - left; val h = bottom - top
                val d = if (depth == 8 || depth == 16) depth else if (depth32 == 8 || depth32 == 16) depth32 else 8
                first = try { readImage(body, 0, 0, if (h > 0) h else height, if (w > 0) w else width, d, compression) } catch (e: BrushFormatException) { null }
            }
        }
        return first
    }

    // ------------------------------------------------------------------ 매핑(§3.3 .abr → 내부)

    private fun mapPreset(p: Desc, index: Int, parsed: Parsed, used: MutableSet<String>, base: String, importedAt: Long): LoadedBrush {
        val notes = ArrayList<String>()
        // 프리셋은 `Brsh` 객체 안에 다시 `Brsh`(팁 정의)가 있을 수 있다(toolPreset 형태) — 둘 다 수용
        val tipDesc = (p["Brsh"] as? Desc) ?: p
        val name = AbrDescriptor.text(p["Nm  "]) ?: AbrDescriptor.text(tipDesc["Nm  "]) ?: "$base ${index + 1}"
        val dmtr = AbrDescriptor.number(tipDesc["Dmtr"])?.toFloat()
        val spacingPct = AbrDescriptor.number(tipDesc["Spcn"])
        val intr = if (tipDesc.has("Intr")) AbrDescriptor.bool(tipDesc["Intr"]) else true
        val spacing = if (!intr) { notes += "Intr=false(간격 미사용) → 기본 $DEFAULT_SPACING"; DEFAULT_SPACING }
        else if (spacingPct == null) DEFAULT_SPACING else SpecUtil.clampSpacing((spacingPct / 100.0).toFloat())
        if (spacingPct != null && spacingPct / 100.0 < SpecUtil.MIN_SPACING) notes += "Spcn=$spacingPct% → 하한 ${SpecUtil.MIN_SPACING}"
        val angle = ((AbrDescriptor.number(tipDesc["Angl"]) ?: 0.0) % 360.0 + 360.0) % 360.0
        val roundness = ((AbrDescriptor.number(tipDesc["Rndn"]) ?: 100.0) / 100.0).coerceIn(0.01, 1.0)
        val flipX = AbrDescriptor.bool(tipDesc["flipX"]); val flipY = AbrDescriptor.bool(tipDesc["flipY"])
        val hardness = AbrDescriptor.number(tipDesc["Hrdn"])

        // ---- 팁 이미지
        val sampleId = AbrDescriptor.text(tipDesc["sampledData"])
        var tip: SampledTip? = sampleId?.let { id -> parsed.tips.firstOrNull { it.id == id } }
        if (tip == null && sampleId != null) { tip = parsed.tips.firstOrNull { it.id !in used }; if (tip != null) notes += "sampledData '$sampleId' 불일치 → 미사용 팁 순서 대응" }
        var computed = false
        val tipImage: GrayImage = if (tip != null) {
            used += tip.id
            tip.image
        } else {
            computed = true
            if (sampleId != null) notes += "samp 레코드 없음 → 합성 원 팁"
            val h = ((hardness ?: 100.0) / 100.0).toFloat()
            ProceduralImages.roundTip(128, hardness = 0.2f + 0.78f * h.coerceIn(0f, 1f), roundness = roundness.toFloat())
        }
        val sizeMax = (dmtr ?: maxOf(tipImage.width, tipImage.height).toFloat()).coerceIn(0.5f, 200f)
        if (dmtr != null && dmtr > 200f) notes += "Dmtr=$dmtr → 200 으로 클램프"
        val normalized = ImageNormalizer.normalizeTip(tipImage, ImageNormalizer.tipLimitFor(sizeMax))

        // ---- 셰이프 다이내믹스
        var pressureSize = PressureMapping(amount = 0f)
        var tiltSize = 0f
        var sizeJitter = 0f
        var taperEnd = 0f
        if (AbrDescriptor.bool(p["useTipDynamics"]) || p.has("szVr")) {
            val szVr = p["szVr"] as? Desc
            if (szVr != null) {
                sizeJitter = ((AbrDescriptor.number(szVr["jitter"]) ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
                val minD = ((AbrDescriptor.number(p["minimumDiameter"]) ?: AbrDescriptor.number(szVr["Mnm"]) ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
                when (control(szVr)) {
                    2 -> pressureSize = PressureMapping(amount = 1f - minD, curve = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)))
                    3 -> { tiltSize = 1f - minD; notes += "szVr 틸트 제어 → tilt.size(부호 추정)" }
                    1 -> { val steps = AbrDescriptor.number(szVr["fStp"]) ?: 0.0; taperEnd = (steps * spacing * sizeMax).toFloat().coerceIn(0f, 300f); notes += "szVr Fade → taper.end.length 근사" }
                    0 -> {}
                    else -> notes += "szVr 제어 ${control(szVr)} 미지원"
                }
            }
        }
        var scatterAngle = 0f; var followStroke = 0f; var randomStart = false
        (p["angleDynamics"] as? Desc)?.let { ad ->
            scatterAngle = ((AbrDescriptor.number(ad["jitter"]) ?: 0.0) / 100.0 * 180.0).toFloat().coerceIn(0f, 180f)
            val c = control(ad)
            if (c in 5..8) { followStroke = 1f; notes += "angleDynamics bVTy=$c → followStroke=1(enum 번호 미확정, §7)" }
            else if (c == 2 || c == 3) notes += "angleDynamics 필압/틸트 제어 버림"
        }
        if (p.has("roundnessDynamics")) notes += "roundnessDynamics 버림"

        // ---- 산란
        var jitter = 0f; var count = 1
        if (AbrDescriptor.bool(p["useScatter"])) {
            (p["scatterDynamics"] as? Desc)?.let { sd -> jitter = ((AbrDescriptor.number(sd["jitter"]) ?: 0.0) / 100.0).toFloat().coerceIn(0f, 2f) }
            val cnt = AbrDescriptor.number(p["Cnt "])?.roundToInt() ?: 1
            count = cnt.coerceIn(1, 4)
            if (cnt > 4) notes += "Cnt=$cnt → 4 로 클램프"
        }

        // ---- 텍스처 → 그레인
        var grainPng: ByteArray? = null
        var grain = Grain()
        if (AbrDescriptor.bool(p["useTexture"])) {
            val txtr = p["Txtr"] as? Desc
            val wantId = txtr?.let { AbrDescriptor.text(it["Idnt"]) }
            val wantName = txtr?.let { AbrDescriptor.text(it["Nm  "]) }
            val pat = parsed.patterns.firstOrNull { it.id == wantId && wantId != null }
                ?: parsed.patterns.firstOrNull { it.name == wantName && wantName != null }
                ?: parsed.patterns.firstOrNull()
            val depthPct = AbrDescriptor.number(p["textureDepth"]) ?: 100.0
            val scalePct = AbrDescriptor.number(p["textureScale"]) ?: 100.0
            if (pat?.image != null) {
                var g = pat.image
                if (AbrDescriptor.bool(p["InvT"])) g = ImageNormalizer.invert(g)
                if (g.width != g.height) notes += "패턴 '${pat.name}' 비정사각 ${g.width}x${g.height} → 중앙 크롭"
                g = ImageNormalizer.normalizeGrain(g)
                grainPng = ImageCodec.encodePng(g)
                grain = Grain(image = "grain.png", depth = (depthPct / 100.0).toFloat().coerceIn(0f, 1f), scale = (scalePct / 100.0).toFloat().coerceIn(0.25f, 4f), mode = GrainMode.textured)
                (p["textureBlendMode"] as? EnumValue)?.let { if (it.value != "Mltp") notes += "textureBlendMode=${it.value} → multiply 로 대체" }
            } else {
                notes += "텍스처 패턴 '${wantName ?: wantId ?: "?"}' 픽셀 없음 → 그레인 생략(depth=${depthPct.toInt()}%)"
            }
        }

        // ---- 전송
        var pressureOpacity = PressureMapping(amount = 0f)
        if (AbrDescriptor.bool(p["usePaintDynamics"]) || p.has("opVr") || p.has("prVr")) {
            val opVr = p["opVr"] as? Desc
            val prVr = p["prVr"] as? Desc
            val src = if (opVr != null && control(opVr) == 2) opVr else if (prVr != null && control(prVr) == 2) prVr.also { notes += "opVr 없음 → prVr(플로우)로 pressure.opacity 근사" } else null
            if (src != null) {
                val mn = ((AbrDescriptor.number(src["Mnm"]) ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
                pressureOpacity = PressureMapping(amount = 1f - mn)
            }
        }
        if (p.has("useDualBrush") && AbrDescriptor.bool(p["useDualBrush"])) notes += "듀얼 브러시 버림"
        if (p.has("useColorDynamics") && AbrDescriptor.bool(p["useColorDynamics"])) notes += "컬러 다이내믹스 버림"
        for (k in listOf("Wtdg", "Nose", "Rpt ", "useBrushPose", "toolOptions")) if (p.has(k)) notes += "$k 버림"

        // ---- 엔진
        var engine = BrushEngine.stamp
        if (computed || (grainPng == null && count == 1 && ImageNormalizer.isRoundHardTip(normalized))) {
            engine = BrushEngine.profile
            notes += if (computed) "computed 팁 → profile 엔진" else "원형 팁·그레인 없음 → profile 엔진으로 승격"
        }

        val spec = BrushSpec(
            id = "abr-" + SpecUtil.slug(name),
            name = name,
            source = BrushSource(format = "abr", originalName = name, importedAt = importedAt, lossNotes = notes),
            engine = engine,
            size = BrushSize(base = sizeMax, min = 0.5f, max = sizeMax),
            pressureSize = pressureSize, pressureOpacity = pressureOpacity,
            tilt = Tilt(size = tiltSize),
            taper = Taper(startLength = 0f, endLength = taperEnd, auto = true),
            stamp = Stamp(
                spacing = spacing, jitter = jitter, sizeJitter = sizeJitter,
                tip = StampTip(angle = angle.toFloat(), roundness = roundness.toFloat(), followStroke = followStroke, scatterAngle = scatterAngle, randomStart = randomStart, flipX = flipX, flipY = flipY, count = count),
            ),
            grain = grain,
            exposed = if (grainPng != null) listOf(ExposedParam.size, ExposedParam.pressureSize, ExposedParam.grainDepth) else listOf(ExposedParam.size, ExposedParam.pressureSize, ExposedParam.streamline),
            role = if (grainPng != null) BrushRole.pencil else BrushRole.pen,
        )
        return LoadedBrush(spec, ImageCodec.encodePng(normalized), grainPng)
    }

    /** 다이내믹스 제어 enum `bVTy`(§2.3): 0 Off, 1 Fade, 2 Pressure, 3 Tilt, 4 Wheel, 5–8 Direction/Rotation. */
    private fun control(d: Desc): Int = when (val v = d["bVTy"]) {
        is EnumValue -> v.value.trim().toIntOrNull() ?: when (v.value) {
            "Prss" -> 2; "Tilt" -> 3; "Fade" -> 1; "Whl " -> 4; else -> -1
        }
        else -> AbrDescriptor.number(v)?.roundToInt() ?: 0
    }

    private fun tipOnly(tip: SampledTip, name: String, spacing: Float, notes: List<String>, importedAt: Long): LoadedBrush {
        val sizeMax = maxOf(tip.image.width, tip.image.height).toFloat().coerceIn(0.5f, 200f)
        val normalized = ImageNormalizer.normalizeTip(tip.image, ImageNormalizer.tipLimitFor(sizeMax))
        val spec = BrushSpec(
            id = "abr-" + SpecUtil.slug(name),
            name = name,
            source = BrushSource(format = "abr", originalName = name, importedAt = importedAt, lossNotes = notes + "팁만 복구(동역학 = 기본값)"),
            engine = BrushEngine.stamp,
            size = BrushSize(base = sizeMax, min = 0.5f, max = sizeMax),
            stamp = Stamp(spacing = spacing),
        )
        return LoadedBrush(spec, ImageCodec.encodePng(normalized), null)
    }
}
