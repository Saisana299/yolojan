package io.github.saisana299.yolojan

import org.mahjong4j.tile.Tile

class IntMapper {
    // マッピングを内部で定義
    private val mapping = mapOf(
        0 to Tile.M1.code,
        1 to Tile.P1.code,
        2 to Tile.S1.code,
        3 to Tile.M2.code,
        4 to Tile.P2.code,
        5 to Tile.S2.code,
        6 to Tile.M3.code,
        7 to Tile.P3.code,
        8 to Tile.S3.code,
        9 to Tile.M4.code,
        10 to Tile.P4.code,
        11 to Tile.S4.code,
        12 to Tile.M5.code,
        13 to Tile.M5.code,
        14 to Tile.P5.code,
        15 to Tile.P5.code,
        16 to Tile.S5.code,
        17 to Tile.S5.code,
        18 to Tile.M6.code,
        19 to Tile.P6.code,
        20 to Tile.S6.code,
        21 to Tile.M7.code,
        22 to Tile.P7.code,
        23 to Tile.S7.code,
        24 to Tile.M8.code,
        25 to Tile.P8.code,
        26 to Tile.S8.code,
        27 to Tile.M9.code,
        28 to Tile.P9.code,
        29 to Tile.S9.code,
        30 to Tile.CHN.code,
        31 to Tile.HAK.code,
        32 to Tile.HAT.code,
        33 to Tile.NAN.code,
        34 to Tile.PEI.code,
        35 to Tile.SHA.code,
        36 to Tile.TON.code,
    )

    fun getInt(input: Int): Int {
        return mapping[input] ?: throw IllegalArgumentException("Mapping not found for $input")
    }
}