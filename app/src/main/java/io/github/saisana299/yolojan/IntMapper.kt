package io.github.saisana299.yolojan

import org.mahjong4j.tile.MahjongTile

class IntMapper {
    // マッピングを内部で定義
    private val mapping = mapOf(
        0 to MahjongTile.M1.code,
        1 to MahjongTile.P1.code,
        2 to MahjongTile.S1.code,
        3 to MahjongTile.M2.code,
        4 to MahjongTile.P2.code,
        5 to MahjongTile.S2.code,
        6 to MahjongTile.M3.code,
        7 to MahjongTile.P3.code,
        8 to MahjongTile.S3.code,
        9 to MahjongTile.M4.code,
        10 to MahjongTile.P4.code,
        11 to MahjongTile.S4.code,
        12 to MahjongTile.M5.code,
        13 to MahjongTile.M5.code,
        14 to MahjongTile.P5.code,
        15 to MahjongTile.P5.code,
        16 to MahjongTile.S5.code,
        17 to MahjongTile.S5.code,
        18 to MahjongTile.M6.code,
        19 to MahjongTile.P6.code,
        20 to MahjongTile.S6.code,
        21 to MahjongTile.M7.code,
        22 to MahjongTile.P7.code,
        23 to MahjongTile.S7.code,
        24 to MahjongTile.M8.code,
        25 to MahjongTile.P8.code,
        26 to MahjongTile.S8.code,
        27 to MahjongTile.M9.code,
        28 to MahjongTile.P9.code,
        29 to MahjongTile.S9.code,
        30 to MahjongTile.CHN.code,
        31 to MahjongTile.HAK.code,
        32 to MahjongTile.HAT.code,
        33 to MahjongTile.NAN.code,
        34 to MahjongTile.PEI.code,
        35 to MahjongTile.SHA.code,
        36 to MahjongTile.TON.code,
    )

    fun getInt(input: Int): Int {
        return mapping[input] ?: throw IllegalArgumentException("Mapping not found for $input")
    }
}